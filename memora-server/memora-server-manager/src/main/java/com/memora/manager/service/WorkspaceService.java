package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.entity.Document;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.entity.Tenant;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.mapper.DocumentMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.mapper.TenantMapper;
import com.memora.manager.mapper.TenantMemberMapper;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.DocumentContentSupport;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.KnowledgeBaseVO;
import com.memora.manager.vo.DocumentVO;
import com.memora.manager.vo.TenantMemberVO;
import com.memora.manager.vo.WorkspaceDashboardVO;
import com.memora.manager.vo.WorkspaceMembershipVO;
import com.memora.manager.vo.WorkspaceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceService {
    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final CurrentAccessContext currentAccessContext;
    private final TenantAccessService tenantAccessService;

    public WorkspaceDashboardVO getCurrentWorkspaceDashboard() {
        Tenant tenant = getCurrentTenant();
        tenantAccessService.requireTenantMember(tenant.getId());

        LambdaQueryWrapper<TenantMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(TenantMember::getTenantId, tenant.getId())
            .eq(TenantMember::getStatus, 1)
            .orderByDesc(TenantMember::getLastActiveAt);
        List<TenantMember> members = tenantMemberMapper.selectList(memberQuery);

        List<KnowledgeBaseVO> knowledgeBaseVOs = knowledgeBaseService.listByTenantId(tenant.getId());
        List<KnowledgeBase> knowledgeBases = knowledgeBaseVOs.stream().map(this::convertKnowledgeBase).toList();
        Set<Long> accessibleKnowledgeBaseIds = knowledgeBases.stream().map(KnowledgeBase::getId).collect(Collectors.toSet());

        long documentCount = 0;
        List<DocumentVO> recentDocuments = List.of();
        if (!accessibleKnowledgeBaseIds.isEmpty()) {
            LambdaQueryWrapper<Document> docQuery = new LambdaQueryWrapper<>();
            docQuery.eq(Document::getTenantId, tenant.getId())
                .in(Document::getKnowledgeBaseId, accessibleKnowledgeBaseIds)
                .eq(Document::getStatus, 1)
                .eq(Document::getDocType, "DOC");
            documentCount = documentMapper.selectCount(docQuery);

            LambdaQueryWrapper<Document> recentDocumentQuery = new LambdaQueryWrapper<>();
            recentDocumentQuery.eq(Document::getTenantId, tenant.getId())
                .in(Document::getKnowledgeBaseId, accessibleKnowledgeBaseIds)
                .eq(Document::getStatus, 1)
                .eq(Document::getDocType, "DOC")
                .orderByDesc(Document::getUpdatedAt)
                .last("LIMIT 6");
            recentDocuments = documentMapper.selectList(recentDocumentQuery).stream()
                .map(this::convertDocument)
                .toList();
        }

        WorkspaceDashboardVO dashboard = new WorkspaceDashboardVO();
        dashboard.setWorkspace(convertWorkspace(tenant, members.size()));
        dashboard.setKnowledgeBaseCount(knowledgeBases.size());
        dashboard.setDocumentCount(documentCount);
        dashboard.setKnowledgeBases(knowledgeBaseVOs);
        dashboard.setRecentDocuments(recentDocuments);
        dashboard.setMembers(members.stream().map(this::convertMember).collect(Collectors.toList()));
        return dashboard;
    }

    public List<WorkspaceMembershipVO> listJoinedWorkspaces() {
        Long currentUserId = currentAccessContext.getCurrentUserId();
        Long currentTenantId = currentAccessContext.getCurrentTenantId();

        LambdaQueryWrapper<TenantMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(TenantMember::getUserId, currentUserId)
            .eq(TenantMember::getStatus, 1)
            .orderByDesc(TenantMember::getLastActiveAt)
            .orderByDesc(TenantMember::getJoinedAt);
        List<TenantMember> memberships = tenantMemberMapper.selectList(memberQuery);
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> tenantIds = memberships.stream()
            .map(TenantMember::getTenantId)
            .distinct()
            .toList();

        LambdaQueryWrapper<Tenant> tenantQuery = new LambdaQueryWrapper<>();
        tenantQuery.in(Tenant::getId, tenantIds)
            .eq(Tenant::getStatus, 1);
        Map<Long, Tenant> tenantMap = tenantMapper.selectList(tenantQuery).stream()
            .collect(Collectors.toMap(Tenant::getId, item -> item));

        Map<Long, Integer> knowledgeBaseCountByTenantId = knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .in(KnowledgeBase::getTenantId, tenantIds)
                .eq(KnowledgeBase::getStatus, 1))
            .stream()
            .collect(Collectors.groupingBy(KnowledgeBase::getTenantId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<Long, Integer> documentCountByTenantId = documentMapper.selectList(new LambdaQueryWrapper<Document>()
                .in(Document::getTenantId, tenantIds)
                .eq(Document::getStatus, 1)
                .eq(Document::getDocType, "DOC"))
            .stream()
            .collect(Collectors.groupingBy(Document::getTenantId, Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        return memberships.stream()
            .map(member -> {
                Tenant tenant = tenantMap.get(member.getTenantId());
                if (tenant == null) {
                    return null;
                }

                WorkspaceMembershipVO vo = new WorkspaceMembershipVO();
                vo.setTenantId(tenant.getId());
                vo.setTenantName(tenant.getName());
                vo.setTenantSlug(tenant.getSlug());
                vo.setRole(member.getRole());
                vo.setDisplayName(member.getDisplayName());
                vo.setCurrent(tenant.getId().equals(currentTenantId));
                vo.setKnowledgeBaseCount(knowledgeBaseCountByTenantId.getOrDefault(tenant.getId(), 0));
                vo.setDocumentCount(documentCountByTenantId.getOrDefault(tenant.getId(), 0));
                vo.setJoinedAt(member.getJoinedAt());
                vo.setLastActiveAt(member.getLastActiveAt());
                return vo;
            })
            .filter(item -> item != null)
            .sorted(Comparator
                .comparing(WorkspaceMembershipVO::getCurrent, Comparator.reverseOrder())
                .thenComparing(WorkspaceMembershipVO::getLastActiveAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(WorkspaceMembershipVO::getTenantName))
            .toList();
    }

    private Tenant getCurrentTenant() {
        Tenant tenant = tenantMapper.selectById(currentAccessContext.getCurrentTenantId());
        if (tenant == null || tenant.getStatus() == null || tenant.getStatus() == 0) {
            throw new BusinessException(404, "当前工作区不存在");
        }
        return tenant;
    }

    private WorkspaceVO convertWorkspace(Tenant tenant, long activeMemberCount) {
        WorkspaceVO vo = new WorkspaceVO();
        BeanUtils.copyProperties(tenant, vo);
        vo.setActiveMemberCount(activeMemberCount);
        return vo;
    }

    private TenantMemberVO convertMember(TenantMember member) {
        TenantMemberVO vo = new TenantMemberVO();
        BeanUtils.copyProperties(member, vo);
        return vo;
    }

    private KnowledgeBase convertKnowledgeBase(KnowledgeBaseVO knowledgeBaseVO) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(knowledgeBaseVO, knowledgeBase);
        return knowledgeBase;
    }

    private DocumentVO convertDocument(Document document) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(document, vo);
        DocumentContentSupport.NormalizedStoredDocument normalized = DocumentContentSupport.normalizeStoredDocument(
            document.getDocType(),
            document.getFormat(),
            document.getContent(),
            document.getContentText(),
            document.getSummary()
        );
        vo.setDocType(normalized.docType());
        vo.setFormat(normalized.format());
        vo.setContentText(normalized.contentText());
        vo.setSummary(normalized.summary());
        return vo;
    }
}

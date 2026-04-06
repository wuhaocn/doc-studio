package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.entity.Document;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.entity.SyncJob;
import com.memora.manager.entity.Tenant;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.mapper.DocumentMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.mapper.SyncJobMapper;
import com.memora.manager.mapper.TenantMapper;
import com.memora.manager.mapper.TenantMemberMapper;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.KnowledgeBaseVO;
import com.memora.manager.vo.DocumentVO;
import com.memora.manager.vo.SyncJobVO;
import com.memora.manager.vo.TenantMemberVO;
import com.memora.manager.vo.WorkspaceDashboardVO;
import com.memora.manager.vo.WorkspaceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceService {
    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final SyncJobMapper syncJobMapper;
    private final KnowledgeBaseService knowledgeBaseService;
    private final SyncJobService syncJobService;
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
        long pendingSyncJobCount = 0;
        List<DocumentVO> recentDocuments = List.of();
        List<SyncJobVO> recentSyncJobs = List.of();
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

            LambdaQueryWrapper<SyncJob> pendingSyncQuery = new LambdaQueryWrapper<>();
            pendingSyncQuery.eq(SyncJob::getTenantId, tenant.getId())
                .in(SyncJob::getKnowledgeBaseId, accessibleKnowledgeBaseIds)
                .ne(SyncJob::getStatus, "SUCCESS");
            pendingSyncJobCount = syncJobMapper.selectCount(pendingSyncQuery);

            LambdaQueryWrapper<SyncJob> recentSyncQuery = new LambdaQueryWrapper<>();
            recentSyncQuery.eq(SyncJob::getTenantId, tenant.getId())
                .in(SyncJob::getKnowledgeBaseId, accessibleKnowledgeBaseIds)
                .orderByDesc(SyncJob::getCreatedAt)
                .last("LIMIT 5");
            recentSyncJobs = syncJobMapper.selectList(recentSyncQuery).stream().map(syncJobService::convertToVO).toList();
        }

        WorkspaceDashboardVO dashboard = new WorkspaceDashboardVO();
        dashboard.setWorkspace(convertWorkspace(tenant, members.size()));
        dashboard.setKnowledgeBaseCount(knowledgeBases.size());
        dashboard.setDocumentCount(documentCount);
        dashboard.setPublicKnowledgeBaseCount(knowledgeBases.stream().filter(item -> item.getIsPublic() != null && item.getIsPublic() == 1).count());
        dashboard.setSyncEnabledKnowledgeBaseCount(knowledgeBases.stream().filter(item -> item.getSyncEnabled() != null && item.getSyncEnabled() == 1).count());
        dashboard.setPendingSyncJobCount(pendingSyncJobCount);
        dashboard.setKnowledgeBases(knowledgeBaseVOs);
        dashboard.setRecentDocuments(recentDocuments);
        dashboard.setRecentSyncJobs(recentSyncJobs);
        dashboard.setMembers(members.stream().map(this::convertMember).collect(Collectors.toList()));
        return dashboard;
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
        return vo;
    }
}

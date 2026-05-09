package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.KnowledgeBaseMemberBatchUpdateDTO;
import com.memora.manager.dto.KnowledgeBaseMemberItemDTO;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.entity.KnowledgeBaseMember;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.mapper.KnowledgeBaseMemberMapper;
import com.memora.manager.mapper.TenantMemberMapper;
import com.memora.manager.support.AuditLogCommand;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.KnowledgeBaseMemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseMemberService {
    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "EDITOR", "VIEWER");
    private static final Set<String> MANAGE_ROLES = Set.of("OWNER", "ADMIN");

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final CurrentAccessContext currentAccessContext;
    private final TenantAccessService tenantAccessService;
    private final AuditLogService auditLogService;

    public List<KnowledgeBaseMemberVO> listMembers(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = getAccessibleKnowledgeBase(knowledgeBaseId);
        tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
        return buildMemberVOs(knowledgeBaseId, knowledgeBase.getTenantId());
    }

    @Transactional(rollbackFor = Exception.class)
    public List<KnowledgeBaseMemberVO> replaceMembers(Long knowledgeBaseId, KnowledgeBaseMemberBatchUpdateDTO dto) {
        KnowledgeBase knowledgeBase = getAccessibleKnowledgeBase(knowledgeBaseId);
        String actorRole = tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);

        List<KnowledgeBaseMemberItemDTO> members = dto == null || dto.getMembers() == null
            ? List.of()
            : dto.getMembers();
        validateMembers(knowledgeBase, members);

        LambdaQueryWrapper<KnowledgeBaseMember> beforeQuery = new LambdaQueryWrapper<>();
        beforeQuery.eq(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
            .eq(KnowledgeBaseMember::getTenantId, knowledgeBase.getTenantId())
            .eq(KnowledgeBaseMember::getStatus, 1);
        int previousCount = knowledgeBaseMemberMapper.selectList(beforeQuery).size();

        LambdaQueryWrapper<KnowledgeBaseMember> deleteQuery = new LambdaQueryWrapper<>();
        deleteQuery.eq(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
            .eq(KnowledgeBaseMember::getTenantId, knowledgeBase.getTenantId());
        knowledgeBaseMemberMapper.delete(deleteQuery);

        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeBaseMemberItemDTO item : members) {
            KnowledgeBaseMember member = new KnowledgeBaseMember();
            member.setKnowledgeBaseId(knowledgeBaseId);
            member.setTenantId(knowledgeBase.getTenantId());
            member.setUserId(item.getUserId());
            member.setRole(item.getRole());
            member.setStatus(1);
            member.setCreatedAt(now);
            member.setUpdatedAt(now);
            knowledgeBaseMemberMapper.insert(member);
        }

        long manageRoleCount = members.stream()
            .filter(item -> MANAGE_ROLES.contains(item.getRole()))
            .count();
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(knowledgeBase.getTenantId())
            .knowledgeBaseId(knowledgeBase.getId())
            .knowledgeBaseName(knowledgeBase.getName())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_KNOWLEDGE_BASE)
            .objectId(knowledgeBase.getId())
            .objectTitle(knowledgeBase.getName())
            .actionType(AuditLogConstants.ACTION_UPDATE_KNOWLEDGE_BASE_MEMBERS)
            .detail("知识库成员权限从 " + previousCount + " 条调整为 " + members.size() + " 条，管理角色 " + manageRoleCount + " 人")
            .build());

        return buildMemberVOs(knowledgeBaseId, knowledgeBase.getTenantId());
    }

    private void validateMembers(KnowledgeBase knowledgeBase, List<KnowledgeBaseMemberItemDTO> members) {
        if (members.isEmpty()) {
            return;
        }

        Set<Long> userIds = new HashSet<>();
        boolean hasManageRole = false;
        boolean currentUserKeepsManageRole = false;
        Long currentUserId = currentAccessContext.getCurrentUserId();

        for (KnowledgeBaseMemberItemDTO item : members) {
            if (!VALID_ROLES.contains(item.getRole())) {
                throw new BusinessException(400, "知识库成员角色不合法");
            }
            if (!userIds.add(item.getUserId())) {
                throw new BusinessException(400, "知识库成员不能重复配置");
            }
            if (MANAGE_ROLES.contains(item.getRole())) {
                hasManageRole = true;
                if (item.getUserId().equals(currentUserId)) {
                    currentUserKeepsManageRole = true;
                }
            }
        }

        if (!hasManageRole) {
            throw new BusinessException(400, "至少保留一个知识库管理员");
        }
        if (!currentUserKeepsManageRole) {
            throw new BusinessException(400, "必须保留当前用户的知识库管理权限");
        }

        LambdaQueryWrapper<TenantMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(TenantMember::getTenantId, knowledgeBase.getTenantId())
            .in(TenantMember::getUserId, userIds)
            .eq(TenantMember::getStatus, 1);
        List<TenantMember> tenantMembers = tenantMemberMapper.selectList(memberQuery);
        if (tenantMembers.size() != userIds.size()) {
            throw new BusinessException(400, "存在不属于当前租户的成员");
        }
    }

    private List<KnowledgeBaseMemberVO> buildMemberVOs(Long knowledgeBaseId, Long tenantId) {
        LambdaQueryWrapper<KnowledgeBaseMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
            .eq(KnowledgeBaseMember::getTenantId, tenantId)
            .eq(KnowledgeBaseMember::getStatus, 1)
            .orderByAsc(KnowledgeBaseMember::getUserId);
        List<KnowledgeBaseMember> members = knowledgeBaseMemberMapper.selectList(queryWrapper);
        if (members.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = members.stream().map(KnowledgeBaseMember::getUserId).collect(Collectors.toSet());
        LambdaQueryWrapper<TenantMember> tenantQuery = new LambdaQueryWrapper<>();
        tenantQuery.eq(TenantMember::getTenantId, tenantId)
            .in(TenantMember::getUserId, userIds)
            .eq(TenantMember::getStatus, 1);
        Map<Long, String> displayNameMap = tenantMemberMapper.selectList(tenantQuery).stream()
            .collect(Collectors.toMap(TenantMember::getUserId, TenantMember::getDisplayName, (left, right) -> left, HashMap::new));

        return members.stream().map(member -> {
            KnowledgeBaseMemberVO vo = new KnowledgeBaseMemberVO();
            BeanUtils.copyProperties(member, vo);
            vo.setDisplayName(displayNameMap.getOrDefault(member.getUserId(), "用户" + member.getUserId()));
            return vo;
        }).toList();
    }

    private KnowledgeBase getAccessibleKnowledgeBase(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || knowledgeBase.getStatus() == null || knowledgeBase.getStatus() == 0) {
            throw new BusinessException(404, "知识库不存在");
        }
        if (!knowledgeBase.getTenantId().equals(currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该知识库");
        }
        return knowledgeBase;
    }
}

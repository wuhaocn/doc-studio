package com.memora.manager.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.entity.KnowledgeBaseMember;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.mapper.KnowledgeBaseMemberMapper;
import com.memora.manager.mapper.TenantMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TenantAccessService {
    private static final Set<String> TENANT_WRITE_ROLES = Set.of("OWNER", "ADMIN", "EDITOR");
    private static final Set<String> TENANT_MANAGE_ROLES = Set.of("OWNER", "ADMIN");
    private static final Set<String> OWNER_ROLES = Set.of("OWNER");
    private static final Set<String> KNOWLEDGE_BASE_WRITE_ROLES = Set.of("OWNER", "ADMIN", "EDITOR");
    private static final Set<String> KNOWLEDGE_BASE_MANAGE_ROLES = Set.of("OWNER", "ADMIN");

    private final TenantMemberMapper tenantMemberMapper;
    private final KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;
    private final CurrentAccessContext currentAccessContext;

    public TenantMember requireTenantMember(Long tenantId) {
        LambdaQueryWrapper<TenantMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantMember::getTenantId, tenantId)
            .eq(TenantMember::getUserId, currentAccessContext.getCurrentUserId())
            .eq(TenantMember::getStatus, 1)
            .last("LIMIT 1");

        TenantMember member = tenantMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            throw new BusinessException(403, "当前用户不属于该租户");
        }
        return member;
    }

    public TenantMember requireEditor(Long tenantId) {
        TenantMember member = requireTenantMember(tenantId);
        if (!TENANT_WRITE_ROLES.contains(member.getRole())) {
            throw new BusinessException(403, "当前角色无写权限");
        }
        return member;
    }

    public TenantMember requireTenantManage(Long tenantId) {
        TenantMember member = requireTenantMember(tenantId);
        if (!TENANT_MANAGE_ROLES.contains(member.getRole())) {
            throw new BusinessException(403, "当前角色无管理权限");
        }
        return member;
    }

    public TenantMember requireOwner(Long tenantId) {
        TenantMember member = requireTenantMember(tenantId);
        if (!OWNER_ROLES.contains(member.getRole())) {
            throw new BusinessException(403, "当前角色无管理权限");
        }
        return member;
    }

    public String requireKnowledgeBaseReadAccess(KnowledgeBase knowledgeBase) {
        return getKnowledgeBaseRole(knowledgeBase);
    }

    public String requireKnowledgeBaseWriteAccess(KnowledgeBase knowledgeBase) {
        String role = requireKnowledgeBaseReadAccess(knowledgeBase);
        if (!KNOWLEDGE_BASE_WRITE_ROLES.contains(role)) {
            throw new BusinessException(403, "当前知识库角色无写权限");
        }
        return role;
    }

    public String requireKnowledgeBaseManageAccess(KnowledgeBase knowledgeBase) {
        String role = requireKnowledgeBaseReadAccess(knowledgeBase);
        if (!KNOWLEDGE_BASE_MANAGE_ROLES.contains(role)) {
            throw new BusinessException(403, "当前知识库角色无管理权限");
        }
        return role;
    }

    public List<KnowledgeBase> filterReadableKnowledgeBases(List<KnowledgeBase> knowledgeBases) {
        return filterKnowledgeBasesByResolvedRole(knowledgeBases, role -> role != null);
    }

    public List<KnowledgeBase> filterWritableKnowledgeBases(List<KnowledgeBase> knowledgeBases) {
        return filterKnowledgeBasesByResolvedRole(knowledgeBases, KNOWLEDGE_BASE_WRITE_ROLES::contains);
    }

    public List<KnowledgeBase> filterManageableKnowledgeBases(List<KnowledgeBase> knowledgeBases) {
        return filterKnowledgeBasesByResolvedRole(knowledgeBases, KNOWLEDGE_BASE_MANAGE_ROLES::contains);
    }

    public String getKnowledgeBaseRole(KnowledgeBase knowledgeBase) {
        TenantMember tenantMember = requireTenantMember(knowledgeBase.getTenantId());
        return resolveKnowledgeBaseRole(knowledgeBase, tenantMember);
    }

    public boolean canWriteKnowledgeBase(String role) {
        return role != null && KNOWLEDGE_BASE_WRITE_ROLES.contains(role);
    }

    public boolean canManageKnowledgeBase(String role) {
        return role != null && KNOWLEDGE_BASE_MANAGE_ROLES.contains(role);
    }

    public boolean hasKnowledgeBaseRestrictions(Long knowledgeBaseId) {
        LambdaQueryWrapper<KnowledgeBaseMember> restrictedQuery = new LambdaQueryWrapper<>();
        restrictedQuery.eq(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseId)
            .eq(KnowledgeBaseMember::getStatus, 1)
            .last("LIMIT 1");
        return knowledgeBaseMemberMapper.selectOne(restrictedQuery) != null;
    }

    private String resolveKnowledgeBaseRole(KnowledgeBase knowledgeBase, TenantMember tenantMember) {
        if (!hasKnowledgeBaseRestrictions(knowledgeBase.getId())) {
            return tenantMember.getRole();
        }

        LambdaQueryWrapper<KnowledgeBaseMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBase.getId())
            .eq(KnowledgeBaseMember::getTenantId, knowledgeBase.getTenantId())
            .eq(KnowledgeBaseMember::getUserId, currentAccessContext.getCurrentUserId())
            .eq(KnowledgeBaseMember::getStatus, 1)
            .last("LIMIT 1");
        KnowledgeBaseMember knowledgeBaseMember = knowledgeBaseMemberMapper.selectOne(memberQuery);
        if (knowledgeBaseMember == null) {
            throw new BusinessException(403, "当前用户无权访问该知识库");
        }
        return knowledgeBaseMember.getRole();
    }

    private List<KnowledgeBaseMember> listActiveKnowledgeBaseMembers(List<Long> knowledgeBaseIds) {
        LambdaQueryWrapper<KnowledgeBaseMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseIds)
            .eq(KnowledgeBaseMember::getStatus, 1);
        return knowledgeBaseMemberMapper.selectList(queryWrapper);
    }

    private List<KnowledgeBaseMember> listCurrentUserKnowledgeBaseMembers(List<Long> knowledgeBaseIds) {
        LambdaQueryWrapper<KnowledgeBaseMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(KnowledgeBaseMember::getKnowledgeBaseId, knowledgeBaseIds)
            .eq(KnowledgeBaseMember::getUserId, currentAccessContext.getCurrentUserId())
            .eq(KnowledgeBaseMember::getStatus, 1);
        return knowledgeBaseMemberMapper.selectList(queryWrapper);
    }

    private List<KnowledgeBase> filterKnowledgeBasesByResolvedRole(
        List<KnowledgeBase> knowledgeBases,
        Predicate<String> rolePredicate) {
        if (knowledgeBases.isEmpty()) {
            return knowledgeBases;
        }

        Long tenantId = knowledgeBases.get(0).getTenantId();
        TenantMember tenantMember = requireTenantMember(tenantId);

        List<Long> knowledgeBaseIds = knowledgeBases.stream()
            .map(KnowledgeBase::getId)
            .toList();
        Set<Long> restrictedKnowledgeBaseIds = listActiveKnowledgeBaseMembers(knowledgeBaseIds).stream()
            .map(KnowledgeBaseMember::getKnowledgeBaseId)
            .collect(Collectors.toSet());
        Map<Long, String> currentUserKnowledgeBaseRoles = listCurrentUserKnowledgeBaseMembers(knowledgeBaseIds).stream()
            .collect(Collectors.toMap(KnowledgeBaseMember::getKnowledgeBaseId, KnowledgeBaseMember::getRole));

        return knowledgeBases.stream()
            .filter(item -> {
                String resolvedRole = restrictedKnowledgeBaseIds.contains(item.getId())
                    ? currentUserKnowledgeBaseRoles.get(item.getId())
                    : tenantMember.getRole();
                return resolvedRole != null && rolePredicate.test(resolvedRole);
            })
            .toList();
    }
}

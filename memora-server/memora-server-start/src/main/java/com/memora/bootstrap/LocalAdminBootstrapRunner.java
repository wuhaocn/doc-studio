package com.memora.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.manager.entity.Tenant;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.entity.UserAccount;
import com.memora.manager.mapper.TenantMapper;
import com.memora.manager.mapper.TenantMemberMapper;
import com.memora.manager.mapper.UserAccountMapper;
import com.memora.manager.support.LocalAdminBootstrapProperties;
import com.memora.manager.support.PasswordCodec;
import com.memora.manager.support.RuntimeAuthModeSupport;
import com.memora.manager.support.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalAdminBootstrapRunner implements ApplicationRunner {
    private final RuntimeAuthModeSupport runtimeAuthModeSupport;
    private final LocalAdminBootstrapProperties properties;
    private final UserAccountMapper userAccountMapper;
    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final PasswordCodec passwordCodec;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (!runtimeAuthModeSupport.isLocalAdminBootstrapActive()) {
            return;
        }

        UserAccount admin = findAdminUser();
        if (admin == null) {
            admin = createAdminUser();
            log.info("已为默认本地运行态引导管理员账号 username={}", admin.getUsername());
        }

        if (ensureUsableWorkspace(admin)) {
            log.info("已为本地管理员账号补齐默认工作区 username={}", admin.getUsername());
        }
    }

    private UserAccount findAdminUser() {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
            .eq(UserAccount::getUsername, normalizeUsername()));
    }

    private UserAccount createAdminUser() {
        LocalDateTime now = LocalDateTime.now();
        UserAccount user = new UserAccount();
        user.setUsername(normalizeUsername());
        user.setEmail(resolveAvailableEmail());
        user.setPasswordHash(passwordCodec.hash(normalizePassword()));
        user.setDisplayName(normalizeDisplayName());
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userAccountMapper.insert(user);
        return user;
    }

    private boolean ensureUsableWorkspace(UserAccount admin) {
        if (hasActiveWorkspaceMembership(admin.getId())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        Tenant tenant = new Tenant();
        tenant.setName(normalizeTenantName());
        tenant.setSlug(resolveAvailableTenantSlug());
        tenant.setIndustry(normalizeIndustry());
        tenant.setPlanName(normalizePlanName());
        tenant.setOwnerUserId(admin.getId());
        tenant.setStatus(1);
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);
        tenantMapper.insert(tenant);

        TenantMember member = new TenantMember();
        member.setTenantId(tenant.getId());
        member.setUserId(admin.getId());
        member.setDisplayName(resolveMemberDisplayName(admin));
        member.setRole("OWNER");
        member.setStatus(1);
        member.setJoinedAt(now);
        member.setLastActiveAt(now);
        tenantMemberMapper.insert(member);
        return true;
    }

    private boolean hasActiveWorkspaceMembership(Long userId) {
        List<TenantMember> members = tenantMemberMapper.selectList(new LambdaQueryWrapper<TenantMember>()
            .eq(TenantMember::getUserId, userId)
            .eq(TenantMember::getStatus, 1));
        if (members.isEmpty()) {
            return false;
        }

        List<Long> tenantIds = members.stream()
            .map(TenantMember::getTenantId)
            .distinct()
            .toList();
        return tenantMapper.selectBatchIds(tenantIds).stream()
            .anyMatch(tenant -> tenant != null && Integer.valueOf(1).equals(tenant.getStatus()));
    }

    private String resolveAvailableEmail() {
        String candidate = normalizeEmail(properties.getEmail(), normalizeUsername());
        if (!emailExists(candidate)) {
            return candidate;
        }

        String localPart = candidate;
        String domain = "memora.local";
        int atIndex = candidate.indexOf('@');
        if (atIndex > 0) {
            localPart = candidate.substring(0, atIndex);
            domain = candidate.substring(atIndex + 1);
        }

        int suffix = 1;
        String nextCandidate = candidate;
        while (emailExists(nextCandidate)) {
            nextCandidate = localPart + "+" + suffix + "@" + domain;
            suffix++;
        }
        return nextCandidate;
    }

    private boolean emailExists(String email) {
        return userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccount>()
            .eq(UserAccount::getEmail, email)) > 0;
    }

    private String resolveAvailableTenantSlug() {
        String baseSlug = SlugUtils.toSlug(StringUtils.hasText(properties.getTenantSlug())
            ? properties.getTenantSlug().trim()
            : normalizeTenantName());
        String candidate = baseSlug;
        int suffix = 1;
        while (tenantMapper.selectCount(new LambdaQueryWrapper<Tenant>().eq(Tenant::getSlug, candidate)) > 0) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String resolveMemberDisplayName(UserAccount admin) {
        return StringUtils.hasText(admin.getDisplayName()) ? admin.getDisplayName().trim() : normalizeDisplayName();
    }

    private String normalizeUsername() {
        return StringUtils.hasText(properties.getUsername()) ? properties.getUsername().trim() : "admin";
    }

    private String normalizePassword() {
        return StringUtils.hasText(properties.getPassword()) ? properties.getPassword() : "123456";
    }

    private String normalizeDisplayName() {
        return StringUtils.hasText(properties.getDisplayName()) ? properties.getDisplayName().trim() : "本地管理员";
    }

    private String normalizeTenantName() {
        return StringUtils.hasText(properties.getTenantName()) ? properties.getTenantName().trim() : "Memora 默认工作区";
    }

    private String normalizePlanName() {
        return StringUtils.hasText(properties.getPlanName()) ? properties.getPlanName().trim() : "TEAM";
    }

    private String normalizeIndustry() {
        return StringUtils.hasText(properties.getIndustry()) ? properties.getIndustry().trim() : "本地联调";
    }

    private String normalizeEmail(String configuredEmail, String username) {
        if (StringUtils.hasText(configuredEmail)) {
            return configuredEmail.trim();
        }
        return username + "@memora.local";
    }
}

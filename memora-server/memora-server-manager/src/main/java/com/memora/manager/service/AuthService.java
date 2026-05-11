package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.AuthLoginDTO;
import com.memora.manager.dto.AuthRegisterOwnerDTO;
import com.memora.manager.entity.Tenant;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.entity.UserAccount;
import com.memora.manager.entity.UserSession;
import com.memora.manager.mapper.TenantMapper;
import com.memora.manager.mapper.TenantMemberMapper;
import com.memora.manager.mapper.UserAccountMapper;
import com.memora.manager.mapper.UserSessionMapper;
import com.memora.manager.support.AuditLogCommand;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.OpaqueTokenCodec;
import com.memora.manager.support.PasswordCodec;
import com.memora.manager.support.SlugUtils;
import com.memora.manager.vo.AuthSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int SESSION_EXPIRES_IN_DAYS = 30;

    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserSessionMapper userSessionMapper;
    private final CurrentAccessContext currentAccessContext;
    private final OpaqueTokenCodec opaqueTokenCodec;
    private final PasswordCodec passwordCodec;
    private final AuditLogService auditLogService;

    @Transactional(rollbackFor = Exception.class)
    public AuthSessionVO registerOwner(AuthRegisterOwnerDTO dto) {
        String username = normalizeUsername(dto.getUsername());
        String email = normalizeEmail(dto.getEmail());
        ensureUsernameAvailable(username);
        ensureEmailAvailable(email);

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordCodec.hash(dto.getPassword()));
        user.setDisplayName(dto.getDisplayName().trim());
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.insert(user);

        Tenant tenant = new Tenant();
        tenant.setName(dto.getTenantName().trim());
        tenant.setSlug(resolveTenantSlug(dto.getTenantName(), dto.getTenantSlug()));
        tenant.setIndustry(StringUtils.hasText(dto.getIndustry()) ? dto.getIndustry().trim() : null);
        tenant.setPlanName(StringUtils.hasText(dto.getPlanName()) ? dto.getPlanName().trim() : "TEAM");
        tenant.setOwnerUserId(user.getId());
        tenant.setStatus(1);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantMapper.insert(tenant);

        TenantMember member = new TenantMember();
        member.setTenantId(tenant.getId());
        member.setUserId(user.getId());
        member.setDisplayName(user.getDisplayName());
        member.setRole("OWNER");
        member.setStatus(1);
        member.setJoinedAt(LocalDateTime.now());
        member.setLastActiveAt(LocalDateTime.now());
        tenantMemberMapper.insert(member);

        AuthSessionVO session = openSessionForTenantMember(user, tenant, member);
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(tenant.getId())
            .actorUserId(user.getId())
            .actorRole(member.getRole())
            .objectType(AuditLogConstants.OBJECT_TENANT)
            .objectId(tenant.getId())
            .objectTitle(tenant.getName())
            .actionType(AuditLogConstants.ACTION_REGISTER_OWNER)
            .detail("创建工作区并建立首个所有者会话")
            .build());
        return session;
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthSessionVO login(AuthLoginDTO dto) {
        UserAccount user = findUserByUsernameOrEmail(dto.getUsername());
        if (user == null || !passwordCodec.matches(dto.getPassword(), user.getPasswordHash())) {
            auditFailedLogin(user, dto, "用户名或密码错误");
            throw new BusinessException(401, "用户名或密码错误");
        }

        TenantMember member;
        try {
            member = resolveLoginTenantMember(user.getId(), dto.getTenantSlug());
        } catch (BusinessException ex) {
            auditFailedLogin(user, dto, ex.getMessage());
            throw ex;
        }
        Tenant tenant = requireActiveTenant(member.getTenantId());
        upgradePasswordHashIfNeeded(user, dto.getPassword());
        AuthSessionVO session = openSessionForTenantMember(user, tenant, member);
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(tenant.getId())
            .actorUserId(user.getId())
            .actorRole(member.getRole())
            .objectType(AuditLogConstants.OBJECT_USER_SESSION)
            .objectTitle(user.getUsername())
            .actionType(AuditLogConstants.ACTION_LOGIN)
            .detail("登录当前工作区并建立新会话")
            .build());
        return session;
    }

    public AuthSessionVO getCurrentSession() {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        Long userId = currentAccessContext.getCurrentUserId();
        UserAccount user = requireActiveUser(userId);
        Tenant tenant = requireActiveTenant(tenantId);
        TenantMember member = requireActiveTenantMember(tenantId, userId);

        if (currentAccessContext.isCurrentSessionToken()) {
            touchSession(currentAccessContext.getCurrentAccessToken(), member);
        }

        return buildSession(user, tenant, member, currentAccessContext.getCurrentAccessToken());
    }

    @Transactional(rollbackFor = Exception.class)
    public void logout() {
        closeCurrentSession(true);
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthSessionVO refreshSession() {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        Long userId = currentAccessContext.getCurrentUserId();
        UserAccount user = requireActiveUser(userId);
        Tenant tenant = requireActiveTenant(tenantId);
        TenantMember member = requireActiveTenantMember(tenantId, userId);
        if (currentAccessContext.isCurrentSessionToken()) {
            closeCurrentSession(false);
        }
        return openSessionForTenantMember(user, tenant, member);
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthSessionVO switchWorkspace(Long tenantId) {
        Long userId = currentAccessContext.getCurrentUserId();
        UserAccount user = requireActiveUser(userId);
        Tenant tenant = requireActiveTenant(tenantId);
        TenantMember member = requireActiveTenantMember(tenantId, userId);
        if (currentAccessContext.isCurrentSessionToken()) {
            closeCurrentSession(false);
        }
        AuthSessionVO session = openSessionForTenantMember(user, tenant, member);
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(tenant.getId())
            .actorUserId(user.getId())
            .actorRole(member.getRole())
            .objectType(AuditLogConstants.OBJECT_TENANT)
            .objectId(tenant.getId())
            .objectTitle(tenant.getName())
            .actionType(AuditLogConstants.ACTION_SWITCH_WORKSPACE)
            .detail("切换当前工作区并建立新会话")
            .build());
        return session;
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthSessionVO openSessionForTenantMember(UserAccount user, Tenant tenant, TenantMember member) {
        String accessToken = "session:" + UUID.randomUUID().toString().replace("-", "");
        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setTenantId(tenant.getId());
        session.setAccessToken(opaqueTokenCodec.hash(accessToken));
        session.setStatus(1);
        session.setExpiresAt(LocalDateTime.now().plusDays(SESSION_EXPIRES_IN_DAYS));
        session.setLastActiveAt(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        userSessionMapper.insert(session);

        member.setLastActiveAt(LocalDateTime.now());
        tenantMemberMapper.updateById(member);
        return buildSession(user, tenant, member, accessToken);
    }

    private AuthSessionVO buildSession(UserAccount user, Tenant tenant, TenantMember member, String accessToken) {
        AuthSessionVO session = new AuthSessionVO();
        session.setUsername(user.getUsername());
        session.setEmail(user.getEmail());
        session.setUserId(member.getUserId());
        session.setTenantId(member.getTenantId());
        session.setDisplayName(member.getDisplayName());
        session.setRole(member.getRole());
        session.setTenantName(tenant.getName());
        session.setTenantSlug(tenant.getSlug());
        session.setIndustry(tenant.getIndustry());
        session.setPlanName(tenant.getPlanName());
        session.setAccessToken(accessToken);
        return session;
    }

    private void touchSession(String accessToken, TenantMember member) {
        UserSession session = findActiveSessionByRawToken(accessToken);
        if (session != null) {
            session.setLastActiveAt(LocalDateTime.now());
            userSessionMapper.updateById(session);
        }

        member.setLastActiveAt(LocalDateTime.now());
        tenantMemberMapper.updateById(member);
    }

    private void closeCurrentSession(boolean audit) {
        if (!currentAccessContext.isCurrentSessionToken()) {
            return;
        }

        UserSession session = findActiveSessionByRawToken(currentAccessContext.getCurrentAccessToken());
        if (session == null) {
            return;
        }

        UserAccount user = null;
        Tenant tenant = null;
        TenantMember member = null;
        if (audit) {
            user = requireActiveUser(session.getUserId());
            tenant = requireActiveTenant(session.getTenantId());
            member = requireActiveTenantMember(session.getTenantId(), session.getUserId());
        }

        session.setStatus(0);
        userSessionMapper.updateById(session);

        if (audit && user != null && tenant != null && member != null) {
            auditLogService.recordSuccess(AuditLogCommand.builder()
                .tenantId(tenant.getId())
                .actorUserId(user.getId())
                .actorRole(member.getRole())
                .objectType(AuditLogConstants.OBJECT_USER_SESSION)
                .objectId(session.getId())
                .objectTitle(user.getUsername())
                .actionType(AuditLogConstants.ACTION_LOGOUT)
                .detail("主动退出当前工作区会话")
                .build());
        }
    }

    private void upgradePasswordHashIfNeeded(UserAccount user, String rawPassword) {
        if (user == null || !passwordCodec.needsRehash(user.getPasswordHash())) {
            return;
        }
        user.setPasswordHash(passwordCodec.hash(rawPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);
    }

    private UserSession findActiveSessionByRawToken(String rawToken) {
        String hashedToken = opaqueTokenCodec.hash(rawToken);
        UserSession session = findActiveSessionByStoredToken(hashedToken);
        if (session != null) {
            return session;
        }
        session = findActiveSessionByStoredToken(rawToken);
        if (session != null && Objects.equals(session.getAccessToken(), rawToken)) {
            session.setAccessToken(hashedToken);
            userSessionMapper.updateById(session);
        }
        return session;
    }

    private UserSession findActiveSessionByStoredToken(String storedToken) {
        LambdaQueryWrapper<UserSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSession::getAccessToken, storedToken)
            .eq(UserSession::getStatus, 1)
            .last("LIMIT 1");
        return userSessionMapper.selectOne(queryWrapper);
    }

    private void auditFailedLogin(UserAccount user, AuthLoginDTO dto, String reason) {
        if (user == null) {
            return;
        }

        TenantMember tenantMember = tryResolveAuditTenantMember(user.getId(), dto == null ? null : dto.getTenantSlug());
        if (tenantMember == null) {
            return;
        }

        Tenant tenant = tenantMapper.selectById(tenantMember.getTenantId());
        if (tenant == null || tenant.getStatus() == null || tenant.getStatus() == 0) {
            return;
        }

        auditLogService.recordFailureSafely(AuditLogCommand.builder()
            .tenantId(tenant.getId())
            .actorUserId(user.getId())
            .actorRole(tenantMember.getRole())
            .objectType(AuditLogConstants.OBJECT_USER_SESSION)
            .objectTitle(user.getUsername())
            .actionType(AuditLogConstants.ACTION_LOGIN)
            .detail("登录失败：" + reason)
            .build());
    }

    private TenantMember tryResolveAuditTenantMember(Long userId, String tenantSlug) {
        if (StringUtils.hasText(tenantSlug)) {
            LambdaQueryWrapper<Tenant> tenantQuery = new LambdaQueryWrapper<>();
            tenantQuery.eq(Tenant::getSlug, tenantSlug.trim())
                .eq(Tenant::getStatus, 1)
                .last("LIMIT 1");
            Tenant tenant = tenantMapper.selectOne(tenantQuery);
            if (tenant == null) {
                return null;
            }

            LambdaQueryWrapper<TenantMember> memberQuery = new LambdaQueryWrapper<>();
            memberQuery.eq(TenantMember::getTenantId, tenant.getId())
                .eq(TenantMember::getUserId, userId)
                .eq(TenantMember::getStatus, 1)
                .last("LIMIT 1");
            return tenantMemberMapper.selectOne(memberQuery);
        }

        LambdaQueryWrapper<TenantMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(TenantMember::getUserId, userId)
            .eq(TenantMember::getStatus, 1)
            .orderByDesc(TenantMember::getLastActiveAt)
            .orderByDesc(TenantMember::getJoinedAt)
            .last("LIMIT 1");
        return tenantMemberMapper.selectOne(memberQuery);
    }

    private UserAccount findUserByUsernameOrEmail(String usernameOrEmail) {
        String normalizedValue = normalizeLoginKey(usernameOrEmail);
        LambdaQueryWrapper<UserAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .eq(UserAccount::getUsername, normalizedValue)
                .or()
                .eq(UserAccount::getEmail, normalizedValue))
            .eq(UserAccount::getStatus, 1)
            .last("LIMIT 1");
        return userAccountMapper.selectOne(queryWrapper);
    }

    private UserAccount requireActiveUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(404, "当前用户不存在");
        }
        return user;
    }

    private TenantMember resolveLoginTenantMember(Long userId, String tenantSlug) {
        if (StringUtils.hasText(tenantSlug)) {
            Tenant tenant = requireActiveTenantBySlug(tenantSlug.trim());
            return requireActiveTenantMember(tenant.getId(), userId);
        }

        LambdaQueryWrapper<TenantMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantMember::getUserId, userId)
            .eq(TenantMember::getStatus, 1)
            .orderByDesc(TenantMember::getLastActiveAt)
            .orderByDesc(TenantMember::getJoinedAt)
            .last("LIMIT 1");
        TenantMember member = tenantMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            throw new BusinessException(403, "当前账号未加入任何工作区");
        }
        return member;
    }

    private TenantMember requireActiveTenantMember(Long tenantId, Long userId) {
        LambdaQueryWrapper<TenantMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantMember::getTenantId, tenantId)
            .eq(TenantMember::getUserId, userId)
            .eq(TenantMember::getStatus, 1)
            .last("LIMIT 1");
        TenantMember member = tenantMemberMapper.selectOne(queryWrapper);
        if (member == null) {
            throw new BusinessException(403, "当前用户不属于该租户");
        }
        return member;
    }

    private Tenant requireActiveTenant(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getStatus() == null || tenant.getStatus() == 0) {
            throw new BusinessException(404, "当前租户不存在");
        }
        return tenant;
    }

    private Tenant requireActiveTenantBySlug(String tenantSlug) {
        LambdaQueryWrapper<Tenant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tenant::getSlug, tenantSlug)
            .eq(Tenant::getStatus, 1)
            .last("LIMIT 1");
        Tenant tenant = tenantMapper.selectOne(queryWrapper);
        if (tenant == null) {
            throw new BusinessException(404, "当前工作区不存在");
        }
        return tenant;
    }

    private void ensureUsernameAvailable(String username) {
        LambdaQueryWrapper<UserAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAccount::getUsername, username).last("LIMIT 1");
        if (userAccountMapper.selectOne(queryWrapper) != null) {
            throw new BusinessException(409, "当前用户名已被占用");
        }
    }

    private void ensureEmailAvailable(String email) {
        LambdaQueryWrapper<UserAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAccount::getEmail, email).last("LIMIT 1");
        if (userAccountMapper.selectOne(queryWrapper) != null) {
            throw new BusinessException(409, "当前邮箱已被占用");
        }
    }

    private String resolveTenantSlug(String tenantName, String tenantSlug) {
        String normalizedSlug = StringUtils.hasText(tenantSlug) ? SlugUtils.toSlug(tenantSlug) : SlugUtils.toSlug(tenantName);
        LambdaQueryWrapper<Tenant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Tenant::getSlug, normalizedSlug).last("LIMIT 1");
        if (tenantMapper.selectOne(queryWrapper) != null) {
            throw new BusinessException(409, "当前工作区标识已被占用");
        }
        return normalizedSlug;
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(400, "用户名不能为空");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(400, "邮箱不能为空");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeLoginKey(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, "用户名不能为空");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

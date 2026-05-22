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
import com.memora.manager.support.BrowserSessionSupport;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.OpaqueTokenCodec;
import com.memora.manager.support.PasswordCodec;
import com.memora.manager.support.SlugUtils;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.AuthSessionVO;
import com.memora.manager.vo.UserSessionRevocationVO;
import com.memora.manager.vo.UserSessionVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int SESSION_EXPIRES_IN_DAYS = 30;
    private static final int CLIENT_TYPE_MAX_LENGTH = 60;
    private static final int USER_AGENT_MAX_LENGTH = 255;
    private static final int IP_ADDRESS_MAX_LENGTH = 64;

    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserSessionMapper userSessionMapper;
    private final CurrentAccessContext currentAccessContext;
    private final OpaqueTokenCodec opaqueTokenCodec;
    private final PasswordCodec passwordCodec;
    private final AuditLogService auditLogService;
    private final BrowserSessionSupport browserSessionSupport;
    private final TenantAccessService tenantAccessService;

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

    public List<UserSessionVO> listCurrentUserSessions() {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        Long userId = currentAccessContext.getCurrentUserId();
        Long currentSessionId = currentAccessContext.getCurrentSessionIdOrNull();
        UserAccount currentUser = requireActiveUser(userId);
        TenantMember currentMember = requireActiveTenantMember(tenantId, userId);

        return listActiveSessions(tenantId, userId).stream()
            .map(session -> convertToSessionVO(session, currentSessionId, userId, currentUser, currentMember))
            .toList();
    }

    public List<UserSessionVO> listTenantSessions() {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        Long currentUserId = currentAccessContext.getCurrentUserId();
        Long currentSessionId = currentAccessContext.getCurrentSessionIdOrNull();
        tenantAccessService.requireTenantManage(tenantId);

        List<UserSession> sessions = listActiveSessions(tenantId, null);
        List<Long> userIds = sessions.stream()
            .map(UserSession::getUserId)
            .distinct()
            .toList();
        Map<Long, UserAccount> usersById = listActiveUsers(userIds);
        Map<Long, TenantMember> membersByUserId = listActiveTenantMembers(tenantId, userIds);

        return sessions.stream()
            .map(session -> convertToSessionVO(
                session,
                currentSessionId,
                currentUserId,
                usersById.get(session.getUserId()),
                membersByUserId.get(session.getUserId())
            ))
            .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public UserSessionRevocationVO revokeSession(Long sessionId) {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        Long userId = currentAccessContext.getCurrentUserId();
        UserSession session = requireActiveTenantSession(sessionId, tenantId, userId);
        Tenant tenant = requireActiveTenant(tenantId);
        TenantMember member = requireActiveTenantMember(tenantId, userId);
        Long currentSessionId = currentAccessContext.getCurrentSessionIdOrNull();

        session.setStatus(0);
        session.setRevokedAt(LocalDateTime.now());
        userSessionMapper.updateById(session);

        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(tenantId)
            .actorUserId(userId)
            .actorRole(member.getRole())
            .objectType(AuditLogConstants.OBJECT_USER_SESSION)
            .objectId(session.getId())
            .objectTitle("会话 #" + session.getId())
            .actionType(AuditLogConstants.ACTION_REVOKE_USER_SESSION)
            .detail("移除会话 " + resolveSessionSummary(session))
            .build());

        UserSessionRevocationVO result = new UserSessionRevocationVO();
        result.setRevokedCount(1L);
        result.setCurrentSessionRevoked(Objects.equals(currentSessionId, session.getId()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserSessionRevocationVO revokeOtherSessions() {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        Long userId = currentAccessContext.getCurrentUserId();
        Long currentSessionId = currentAccessContext.getCurrentSessionIdOrNull();
        Tenant tenant = requireActiveTenant(tenantId);
        TenantMember member = requireActiveTenantMember(tenantId, userId);

        LambdaQueryWrapper<UserSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSession::getTenantId, tenantId)
            .eq(UserSession::getUserId, userId)
            .eq(UserSession::getStatus, 1)
            .gt(UserSession::getExpiresAt, LocalDateTime.now());
        if (currentSessionId != null) {
            queryWrapper.ne(UserSession::getId, currentSessionId);
        }
        List<UserSession> otherSessions = userSessionMapper.selectList(queryWrapper);
        revokePersistedSessions(otherSessions, LocalDateTime.now());

        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(tenantId)
            .actorUserId(userId)
            .actorRole(member.getRole())
            .objectType(AuditLogConstants.OBJECT_USER_SESSION)
            .objectTitle(tenant.getName())
            .actionType(AuditLogConstants.ACTION_REVOKE_OTHER_USER_SESSIONS)
            .detail(otherSessions.isEmpty()
                ? "尝试移除其他设备会话，当前没有可移除的活跃会话"
                : "移除其他设备会话 " + otherSessions.size() + " 个")
            .build());

        UserSessionRevocationVO result = new UserSessionRevocationVO();
        result.setRevokedCount((long) otherSessions.size());
        result.setCurrentSessionRevoked(false);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserSessionRevocationVO revokeTenantSession(Long sessionId) {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        Long currentUserId = currentAccessContext.getCurrentUserId();
        Long currentSessionId = currentAccessContext.getCurrentSessionIdOrNull();
        TenantMember actorMember = tenantAccessService.requireTenantManage(tenantId);
        UserSession session = requireActiveTenantSession(sessionId, tenantId);
        UserAccount targetUser = requireActiveUser(session.getUserId());
        TenantMember targetMember = requireActiveTenantMember(tenantId, session.getUserId());

        revokePersistedSessions(List.of(session), LocalDateTime.now());
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(tenantId)
            .actorUserId(currentUserId)
            .actorRole(actorMember.getRole())
            .objectType(AuditLogConstants.OBJECT_USER_SESSION)
            .objectId(session.getId())
            .objectTitle(resolveSessionObjectTitle(targetUser, session))
            .actionType(AuditLogConstants.ACTION_REVOKE_TENANT_USER_SESSION)
            .detail("管理员移除成员“" + resolveSessionActorLabel(targetUser, targetMember) + "”会话 " + resolveSessionSummary(session))
            .build());

        UserSessionRevocationVO result = new UserSessionRevocationVO();
        result.setRevokedCount(1L);
        result.setCurrentSessionRevoked(Objects.equals(currentSessionId, session.getId()));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserSessionRevocationVO revokeTenantUserSessions(Long userId) {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        Long currentUserId = currentAccessContext.getCurrentUserId();
        Long currentSessionId = currentAccessContext.getCurrentSessionIdOrNull();
        TenantMember actorMember = tenantAccessService.requireTenantManage(tenantId);
        UserAccount targetUser = requireActiveUser(userId);
        TenantMember targetMember = requireActiveTenantMember(tenantId, userId);

        List<UserSession> sessions = listActiveSessions(tenantId, userId);
        revokePersistedSessions(sessions, LocalDateTime.now());

        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(tenantId)
            .actorUserId(currentUserId)
            .actorRole(actorMember.getRole())
            .objectType(AuditLogConstants.OBJECT_USER_SESSION)
            .objectTitle(resolveSessionActorLabel(targetUser, targetMember))
            .actionType(AuditLogConstants.ACTION_REVOKE_TENANT_MEMBER_SESSIONS)
            .detail(sessions.isEmpty()
                ? "尝试移除成员“" + resolveSessionActorLabel(targetUser, targetMember) + "”全部会话，当前没有可移除的活跃会话"
                : "管理员移除成员“" + resolveSessionActorLabel(targetUser, targetMember) + "”活跃会话 " + sessions.size() + " 个")
            .build());

        UserSessionRevocationVO result = new UserSessionRevocationVO();
        result.setRevokedCount((long) sessions.size());
        result.setCurrentSessionRevoked(currentSessionId != null && sessions.stream().anyMatch(item -> Objects.equals(item.getId(), currentSessionId)));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthSessionVO openSessionForTenantMember(UserAccount user, Tenant tenant, TenantMember member) {
        String accessToken = "session:" + UUID.randomUUID().toString().replace("-", "");
        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setTenantId(tenant.getId());
        session.setAccessToken(opaqueTokenCodec.hash(accessToken));
        session.setClientType(resolveSessionClientType());
        session.setUserAgent(resolveSessionUserAgent());
        session.setIpAddress(resolveSessionIpAddress());
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
        session.setRevokedAt(LocalDateTime.now());
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

    private UserSessionVO convertToSessionVO(
        UserSession session,
        Long currentSessionId,
        Long currentUserId,
        UserAccount user,
        TenantMember member
    ) {
        UserSessionVO vo = new UserSessionVO();
        BeanUtils.copyProperties(session, vo);
        vo.setUserId(session.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setDisplayName(member != null && StringUtils.hasText(member.getDisplayName())
            ? member.getDisplayName()
            : user == null ? null : user.getDisplayName());
        vo.setRole(member == null ? null : member.getRole());
        vo.setCurrent(Objects.equals(currentSessionId, session.getId()));
        vo.setOwnedByCurrentUser(Objects.equals(currentUserId, session.getUserId()));
        return vo;
    }

    private List<UserSession> listActiveSessions(Long tenantId, Long userId) {
        LambdaQueryWrapper<UserSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSession::getTenantId, tenantId)
            .eq(UserSession::getStatus, 1)
            .gt(UserSession::getExpiresAt, LocalDateTime.now())
            .orderByDesc(UserSession::getLastActiveAt)
            .orderByDesc(UserSession::getCreatedAt)
            .orderByDesc(UserSession::getId);
        if (userId != null) {
            queryWrapper.eq(UserSession::getUserId, userId);
        }
        return userSessionMapper.selectList(queryWrapper);
    }

    private Map<Long, UserAccount> listActiveUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<UserAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UserAccount::getId, userIds)
            .eq(UserAccount::getStatus, 1);
        return userAccountMapper.selectList(queryWrapper).stream()
            .collect(Collectors.toMap(UserAccount::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, TenantMember> listActiveTenantMembers(Long tenantId, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<TenantMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantMember::getTenantId, tenantId)
            .in(TenantMember::getUserId, userIds)
            .eq(TenantMember::getStatus, 1);
        return tenantMemberMapper.selectList(queryWrapper).stream()
            .collect(Collectors.toMap(TenantMember::getUserId, Function.identity(), (left, right) -> left));
    }

    private void revokePersistedSessions(List<UserSession> sessions, LocalDateTime revokedAt) {
        sessions.forEach(session -> {
            session.setStatus(0);
            session.setRevokedAt(revokedAt);
            userSessionMapper.updateById(session);
        });
    }

    private UserSession requireActiveTenantSession(Long sessionId, Long tenantId) {
        UserSession session = userSessionMapper.selectById(sessionId);
        if (session == null || !Objects.equals(session.getTenantId(), tenantId)) {
            throw new BusinessException(404, "当前会话不存在");
        }
        if (session.getStatus() == null || session.getStatus() == 0 || session.getExpiresAt() == null || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(409, "当前会话已经失效");
        }
        return session;
    }

    private UserSession requireActiveTenantSession(Long sessionId, Long tenantId, Long userId) {
        UserSession session = requireActiveTenantSession(sessionId, tenantId);
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new BusinessException(404, "当前会话不存在");
        }
        return session;
    }

    private String resolveSessionClientType() {
        HttpServletRequest request = currentRequest();
        return truncate(browserSessionSupport.resolveClientType(request), CLIENT_TYPE_MAX_LENGTH);
    }

    private String resolveSessionUserAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        return truncate(request.getHeader("User-Agent"), USER_AGENT_MAX_LENGTH);
    }

    private String resolveSessionIpAddress() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        String candidate = null;
        if (StringUtils.hasText(forwardedFor)) {
            candidate = forwardedFor.split(",")[0].trim();
        }
        if (!StringUtils.hasText(candidate)) {
            candidate = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(candidate)) {
            candidate = request.getRemoteAddr();
        }
        return truncate(maskIpAddress(candidate), IP_ADDRESS_MAX_LENGTH);
    }

    private String resolveSessionSummary(UserSession session) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(session.getClientType())) {
            parts.add(session.getClientType());
        }
        if (StringUtils.hasText(session.getIpAddress())) {
            parts.add(session.getIpAddress());
        }
        if (session.getLastActiveAt() != null) {
            parts.add("最近活跃 " + session.getLastActiveAt());
        }
        return parts.isEmpty() ? "#" + session.getId() : String.join(" / ", parts);
    }

    private String resolveSessionObjectTitle(UserAccount user, UserSession session) {
        return (user == null ? "用户" : user.getUsername()) + " / 会话 #" + session.getId();
    }

    private String resolveSessionActorLabel(UserAccount user, TenantMember member) {
        if (member != null && StringUtils.hasText(member.getDisplayName())) {
            return member.getDisplayName();
        }
        if (user != null && StringUtils.hasText(user.getDisplayName())) {
            return user.getDisplayName();
        }
        return user == null ? "未知成员" : user.getUsername();
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String maskIpAddress(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return ipAddress;
        }
        String normalized = ipAddress.trim();
        if (normalized.contains(".")) {
            int lastDot = normalized.lastIndexOf('.');
            return lastDot > 0 ? normalized.substring(0, lastDot) + ".*" : normalized;
        }
        if (normalized.contains(":")) {
            String[] parts = normalized.split(":");
            if (parts.length <= 2) {
                return normalized;
            }
            StringBuilder builder = new StringBuilder();
            int visibleCount = Math.min(4, parts.length - 1);
            for (int index = 0; index < visibleCount; index++) {
                if (index > 0) {
                    builder.append(':');
                }
                builder.append(parts[index]);
            }
            builder.append(":*");
            return builder.toString();
        }
        return normalized;
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
            .gt(UserSession::getExpiresAt, LocalDateTime.now())
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

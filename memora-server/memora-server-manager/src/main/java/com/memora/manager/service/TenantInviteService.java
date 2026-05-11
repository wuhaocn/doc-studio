package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.TenantInviteAcceptDTO;
import com.memora.manager.dto.TenantInviteCreateDTO;
import com.memora.manager.entity.Tenant;
import com.memora.manager.entity.TenantInvite;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.entity.UserAccount;
import com.memora.manager.mapper.TenantInviteMapper;
import com.memora.manager.mapper.TenantMapper;
import com.memora.manager.mapper.TenantMemberMapper;
import com.memora.manager.mapper.UserAccountMapper;
import com.memora.manager.support.AuditLogCommand;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.OpaqueTokenCodec;
import com.memora.manager.support.PasswordCodec;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.AuthSessionVO;
import com.memora.manager.vo.TenantInviteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantInviteService {
    private static final Set<String> OWNER_INVITABLE_ROLES = Set.of("OWNER", "ADMIN", "EDITOR", "VIEWER");
    private static final Set<String> ADMIN_INVITABLE_ROLES = Set.of("ADMIN", "EDITOR", "VIEWER");
    private static final int DEFAULT_EXPIRES_IN_DAYS = 7;
    private static final int MAX_EXPIRES_IN_DAYS = 30;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_ACCEPTED = 2;
    private static final int STATUS_REVOKED = 3;

    private final TenantInviteMapper tenantInviteMapper;
    private final TenantMapper tenantMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final UserAccountMapper userAccountMapper;
    private final TenantAccessService tenantAccessService;
    private final CurrentAccessContext currentAccessContext;
    private final OpaqueTokenCodec opaqueTokenCodec;
    private final PasswordCodec passwordCodec;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    @Transactional(rollbackFor = Exception.class)
    public TenantInviteVO createInvite(TenantInviteCreateDTO dto) {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        TenantMember inviter = tenantAccessService.requireTenantManage(tenantId);
        Tenant tenant = requireActiveTenant(tenantId);

        String inviteeEmail = normalizeEmail(dto.getEmail());
        String inviteRole = normalizeRole(dto.getRole());
        ensureInviterCanGrantRole(inviter.getRole(), inviteRole);

        UserAccount existingUser = findUserByEmail(inviteeEmail);
        if (existingUser != null && isTenantMember(tenantId, existingUser.getId())) {
            throw new BusinessException(409, "该成员已在当前工作区内");
        }

        revokeActiveInvitesForEmail(tenantId, inviteeEmail, inviter.getUserId());

        String rawInviteToken = UUID.randomUUID().toString().replace("-", "");
        TenantInvite invite = new TenantInvite();
        invite.setTenantId(tenantId);
        invite.setInviterUserId(inviter.getUserId());
        invite.setInviteeEmail(inviteeEmail);
        invite.setInviteeDisplayName(normalizeOptional(dto.getDisplayName()));
        invite.setRole(inviteRole);
        invite.setInviteToken(opaqueTokenCodec.hash(rawInviteToken));
        invite.setStatus(STATUS_ACTIVE);
        invite.setExpiresAt(LocalDateTime.now().plusDays(resolveExpiresInDays(dto.getExpiresInDays())));
        invite.setCreatedAt(LocalDateTime.now());
        tenantInviteMapper.insert(invite);

        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(tenantId)
            .knowledgeBaseName(null)
            .actorUserId(inviter.getUserId())
            .actorRole(inviter.getRole())
            .objectType(AuditLogConstants.OBJECT_TENANT_INVITE)
            .objectId(invite.getId())
            .objectTitle(invite.getInviteeEmail())
            .actionType(AuditLogConstants.ACTION_CREATE_INVITE)
            .detail("创建工作区邀请，角色：" + inviteRole + "，接收邮箱：" + inviteeEmail)
            .build());

        return convertInvite(invite, tenant, inviter.getDisplayName(), rawInviteToken);
    }

    public List<TenantInviteVO> listInvites() {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        tenantAccessService.requireTenantManage(tenantId);
        Tenant tenant = requireActiveTenant(tenantId);

        LambdaQueryWrapper<TenantInvite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantInvite::getTenantId, tenantId)
            .orderByDesc(TenantInvite::getCreatedAt)
            .orderByDesc(TenantInvite::getId)
            .last("LIMIT 50");

        return tenantInviteMapper.selectList(queryWrapper).stream()
            .map(invite -> convertInvite(invite, tenant, resolveInviterDisplayName(invite), null))
            .collect(Collectors.toList());
    }

    public TenantInviteVO getInvite(String inviteToken) {
        TenantInvite invite = requireActiveInvite(inviteToken);
        Tenant tenant = requireActiveTenant(invite.getTenantId());
        return convertInvite(invite, tenant, resolveInviterDisplayName(invite), null);
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthSessionVO acceptInvite(TenantInviteAcceptDTO dto) {
        TenantInvite invite = requireActiveInvite(dto.getToken());
        Tenant tenant = requireActiveTenant(invite.getTenantId());

        String inviteeEmail = normalizeEmail(dto.getEmail());
        if (!invite.getInviteeEmail().equals(inviteeEmail)) {
            throw new BusinessException(409, "邀请邮箱与当前提交邮箱不一致");
        }

        UserAccount user = resolveOrCreateUser(dto);
        if (isTenantMember(invite.getTenantId(), user.getId())) {
            throw new BusinessException(409, "当前账号已属于该工作区");
        }

        String displayName = normalizeOptional(dto.getDisplayName());
        if (StringUtils.hasText(displayName) && !displayName.equals(user.getDisplayName())) {
            user.setDisplayName(displayName);
            user.setUpdatedAt(LocalDateTime.now());
            userAccountMapper.updateById(user);
        }

        TenantMember member = new TenantMember();
        member.setTenantId(invite.getTenantId());
        member.setUserId(user.getId());
        member.setDisplayName(StringUtils.hasText(displayName) ? displayName : user.getDisplayName());
        member.setRole(invite.getRole());
        member.setStatus(1);
        member.setJoinedAt(LocalDateTime.now());
        member.setLastActiveAt(LocalDateTime.now());
        tenantMemberMapper.insert(member);

        invite.setStatus(STATUS_ACCEPTED);
        invite.setAcceptedByUserId(user.getId());
        invite.setAcceptedAt(LocalDateTime.now());
        tenantInviteMapper.updateById(invite);

        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(invite.getTenantId())
            .actorUserId(user.getId())
            .actorDisplayName(member.getDisplayName())
            .actorRole(member.getRole())
            .objectType(AuditLogConstants.OBJECT_TENANT_INVITE)
            .objectId(invite.getId())
            .objectTitle(invite.getInviteeEmail())
            .actionType(AuditLogConstants.ACTION_ACCEPT_INVITE)
            .detail("接受工作区邀请并加入当前工作区，角色：" + invite.getRole())
            .build());

        return authService.openSessionForTenantMember(user, tenant, member);
    }

    @Transactional(rollbackFor = Exception.class)
    public TenantInviteVO revokeInvite(Long inviteId) {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        TenantMember operator = tenantAccessService.requireTenantManage(tenantId);
        Tenant tenant = requireActiveTenant(tenantId);
        TenantInvite invite = requireInviteInTenant(inviteId, tenantId);

        if (invite.getStatus() != null && invite.getStatus() == STATUS_ACCEPTED) {
            throw new BusinessException(409, "已接受的邀请不能撤销");
        }

        if (invite.getStatus() == null || invite.getStatus() != STATUS_REVOKED) {
            invite.setStatus(STATUS_REVOKED);
            invite.setRevokedByUserId(operator.getUserId());
            invite.setRevokedAt(LocalDateTime.now());
            tenantInviteMapper.updateById(invite);

            auditLogService.recordSuccess(AuditLogCommand.builder()
                .tenantId(tenantId)
                .actorUserId(operator.getUserId())
                .actorRole(operator.getRole())
                .objectType(AuditLogConstants.OBJECT_TENANT_INVITE)
                .objectId(invite.getId())
                .objectTitle(invite.getInviteeEmail())
                .actionType(AuditLogConstants.ACTION_REVOKE_INVITE)
                .detail("撤销工作区邀请，原邀请链接不再可用")
                .build());
        }

        return convertInvite(invite, tenant, resolveInviterDisplayName(invite), null);
    }

    private UserAccount resolveOrCreateUser(TenantInviteAcceptDTO dto) {
        String username = normalizeUsername(dto.getUsername());
        String email = normalizeEmail(dto.getEmail());
        String displayName = normalizeOptional(dto.getDisplayName());

        UserAccount userByUsername = findUserByUsername(username);
        UserAccount userByEmail = findUserByEmail(email);

        if (userByUsername == null && userByEmail == null) {
            UserAccount user = new UserAccount();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordCodec.hash(dto.getPassword()));
            user.setDisplayName(StringUtils.hasText(displayName) ? displayName : username);
            user.setStatus(1);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userAccountMapper.insert(user);
            return user;
        }

        if (userByUsername == null || userByEmail == null || !userByUsername.getId().equals(userByEmail.getId())) {
            throw new BusinessException(409, "用户名或邮箱已被其他账号占用");
        }

        if (!passwordCodec.matches(dto.getPassword(), userByUsername.getPasswordHash())) {
            throw new BusinessException(401, "密码错误，请使用该账号原有密码接受邀请");
        }

        if (passwordCodec.needsRehash(userByUsername.getPasswordHash())) {
            userByUsername.setPasswordHash(passwordCodec.hash(dto.getPassword()));
            userByUsername.setUpdatedAt(LocalDateTime.now());
            userAccountMapper.updateById(userByUsername);
        }

        return userByUsername;
    }

    private TenantInviteVO convertInvite(TenantInvite invite, Tenant tenant, String inviterDisplayName, String rawInviteToken) {
        TenantInviteVO vo = new TenantInviteVO();
        vo.setId(invite.getId());
        vo.setTenantId(invite.getTenantId());
        vo.setTenantName(tenant.getName());
        vo.setTenantSlug(tenant.getSlug());
        vo.setInviterDisplayName(inviterDisplayName);
        vo.setInviteeEmail(invite.getInviteeEmail());
        vo.setInviteeDisplayName(invite.getInviteeDisplayName());
        vo.setRole(invite.getRole());
        vo.setInviteToken(StringUtils.hasText(rawInviteToken) ? rawInviteToken : null);
        vo.setStatus(invite.getStatus());
        vo.setExpiresAt(invite.getExpiresAt());
        vo.setAcceptedByUserId(invite.getAcceptedByUserId());
        vo.setAcceptedAt(invite.getAcceptedAt());
        vo.setRevokedByUserId(invite.getRevokedByUserId());
        vo.setRevokedAt(invite.getRevokedAt());
        vo.setCreatedAt(invite.getCreatedAt());
        return vo;
    }

    private TenantInvite requireActiveInvite(String inviteToken) {
        if (!StringUtils.hasText(inviteToken)) {
            throw new BusinessException(404, "当前邀请不存在、已失效或已被使用");
        }
        String normalizedToken = inviteToken.trim();
        String hashedToken = opaqueTokenCodec.hash(normalizedToken);
        TenantInvite invite = findActiveInviteByStoredToken(hashedToken);
        if (invite == null) {
            invite = findActiveInviteByStoredToken(normalizedToken);
            if (invite != null && normalizedToken.equals(invite.getInviteToken())) {
                invite.setInviteToken(hashedToken);
                tenantInviteMapper.updateById(invite);
            }
        }
        if (invite == null) {
            throw new BusinessException(404, "当前邀请不存在、已失效或已被使用");
        }
        return invite;
    }

    private TenantInvite findActiveInviteByStoredToken(String storedToken) {
        LambdaQueryWrapper<TenantInvite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantInvite::getInviteToken, storedToken)
            .eq(TenantInvite::getStatus, STATUS_ACTIVE)
            .gt(TenantInvite::getExpiresAt, LocalDateTime.now())
            .last("LIMIT 1");
        return tenantInviteMapper.selectOne(queryWrapper);
    }

    private Tenant requireActiveTenant(Long tenantId) {
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || tenant.getStatus() == null || tenant.getStatus() == 0) {
            throw new BusinessException(404, "当前工作区不存在");
        }
        return tenant;
    }

    private TenantInvite requireInviteInTenant(Long inviteId, Long tenantId) {
        TenantInvite invite = tenantInviteMapper.selectById(inviteId);
        if (invite == null || !tenantId.equals(invite.getTenantId())) {
            throw new BusinessException(404, "当前邀请不存在");
        }
        return invite;
    }

    private boolean isTenantMember(Long tenantId, Long userId) {
        LambdaQueryWrapper<TenantMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantMember::getTenantId, tenantId)
            .eq(TenantMember::getUserId, userId)
            .eq(TenantMember::getStatus, 1)
            .last("LIMIT 1");
        return tenantMemberMapper.selectOne(queryWrapper) != null;
    }

    private String resolveInviterDisplayName(TenantInvite invite) {
        LambdaQueryWrapper<TenantMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantMember::getTenantId, invite.getTenantId())
            .eq(TenantMember::getUserId, invite.getInviterUserId())
            .eq(TenantMember::getStatus, 1)
            .last("LIMIT 1");
        TenantMember inviter = tenantMemberMapper.selectOne(queryWrapper);
        return inviter != null ? inviter.getDisplayName() : "工作区管理员";
    }

    private UserAccount findUserByUsername(String username) {
        LambdaQueryWrapper<UserAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAccount::getUsername, username)
            .eq(UserAccount::getStatus, 1)
            .last("LIMIT 1");
        return userAccountMapper.selectOne(queryWrapper);
    }

    private UserAccount findUserByEmail(String email) {
        LambdaQueryWrapper<UserAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAccount::getEmail, email)
            .eq(UserAccount::getStatus, 1)
            .last("LIMIT 1");
        return userAccountMapper.selectOne(queryWrapper);
    }

    private void ensureInviterCanGrantRole(String inviterRole, String inviteRole) {
        Set<String> allowedRoles = "OWNER".equals(inviterRole) ? OWNER_INVITABLE_ROLES : ADMIN_INVITABLE_ROLES;
        if (!allowedRoles.contains(inviteRole)) {
            throw new BusinessException(403, "当前角色不能邀请该类型成员");
        }
    }

    private void revokeActiveInvitesForEmail(Long tenantId, String inviteeEmail, Long revokedByUserId) {
        LambdaQueryWrapper<TenantInvite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TenantInvite::getTenantId, tenantId)
            .eq(TenantInvite::getInviteeEmail, inviteeEmail)
            .eq(TenantInvite::getStatus, STATUS_ACTIVE);

        tenantInviteMapper.selectList(queryWrapper).forEach(invite -> {
            invite.setStatus(STATUS_REVOKED);
            invite.setRevokedByUserId(revokedByUserId);
            invite.setRevokedAt(LocalDateTime.now());
            tenantInviteMapper.updateById(invite);
        });
    }

    private int resolveExpiresInDays(Integer expiresInDays) {
        if (expiresInDays == null) {
            return DEFAULT_EXPIRES_IN_DAYS;
        }
        return Math.max(1, Math.min(expiresInDays, MAX_EXPIRES_IN_DAYS));
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new BusinessException(400, "邀请角色不能为空");
        }
        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (!OWNER_INVITABLE_ROLES.contains(normalizedRole)) {
            throw new BusinessException(400, "当前邀请角色不受支持");
        }
        return normalizedRole;
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

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

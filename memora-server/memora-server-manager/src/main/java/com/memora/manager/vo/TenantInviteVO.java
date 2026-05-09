package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantInviteVO {
    private Long id;

    private Long tenantId;

    private String tenantName;

    private String tenantSlug;

    private String inviterDisplayName;

    private String inviteeEmail;

    private String inviteeDisplayName;

    private String role;

    private String inviteToken;

    private Integer status;

    private LocalDateTime expiresAt;

    private Long acceptedByUserId;

    private LocalDateTime acceptedAt;

    private Long revokedByUserId;

    private LocalDateTime revokedAt;

    private LocalDateTime createdAt;
}

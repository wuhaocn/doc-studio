package com.memora.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tenant_invite")
public class TenantInvite {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long inviterUserId;

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

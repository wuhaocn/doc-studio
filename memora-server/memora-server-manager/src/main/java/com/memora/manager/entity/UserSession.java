package com.memora.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_session")
public class UserSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long tenantId;

    private String accessToken;

    private String clientType;

    private String userAgent;

    private String ipAddress;

    private Integer status;

    private LocalDateTime expiresAt;

    private LocalDateTime lastActiveAt;

    private LocalDateTime createdAt;

    private LocalDateTime revokedAt;
}

package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserSessionVO {
    private Long id;

    private Long userId;

    private String username;

    private String displayName;

    private String role;

    private String clientType;

    private String userAgent;

    private String ipAddress;

    private Boolean current;

    private Boolean ownedByCurrentUser;

    private LocalDateTime expiresAt;

    private LocalDateTime lastActiveAt;

    private LocalDateTime createdAt;
}

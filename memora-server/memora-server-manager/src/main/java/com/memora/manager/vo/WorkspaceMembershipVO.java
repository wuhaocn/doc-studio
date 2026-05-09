package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkspaceMembershipVO {
    private Long tenantId;

    private String tenantName;

    private String tenantSlug;

    private String role;

    private String displayName;

    private Boolean current;

    private Integer knowledgeBaseCount;

    private Integer documentCount;

    private LocalDateTime joinedAt;

    private LocalDateTime lastActiveAt;
}

package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApiKeyVO {
    private Long id;

    private Long serviceAccountId;

    private Long tenantId;

    private String name;

    private String keyPrefix;

    private Integer status;

    private Boolean expired;

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;

    private LocalDateTime revokedAt;

    private LocalDateTime createdAt;

    private List<Long> knowledgeBaseIds;

    private List<String> accessModes;
}

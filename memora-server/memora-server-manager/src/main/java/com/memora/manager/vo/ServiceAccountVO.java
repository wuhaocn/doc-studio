package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ServiceAccountVO {
    private Long id;

    private Long tenantId;

    private String name;

    private String description;

    private Integer status;

    private Long createdByUserId;

    private LocalDateTime revokedAt;

    private LocalDateTime createdAt;

    private List<ApiKeyVO> apiKeys;
}

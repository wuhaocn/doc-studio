package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentShareLinkVO {
    private Long id;

    private Long tenantId;

    private Long knowledgeBaseId;

    private Long documentId;

    private String documentTitle;

    private String shareToken;

    private String shareUrl;

    private Integer status;

    private Boolean expired;

    private Boolean accessCodeProtected;

    private LocalDateTime expiresAt;

    private Long createdByUserId;

    private Long revokedByUserId;

    private LocalDateTime revokedAt;

    private LocalDateTime lastAccessedAt;

    private LocalDateTime createdAt;
}

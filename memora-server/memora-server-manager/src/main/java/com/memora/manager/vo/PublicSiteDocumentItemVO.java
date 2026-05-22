package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublicSiteDocumentItemVO {
    private Long documentId;

    private Long parentId;

    private String title;

    private String summary;

    private String publicSlug;

    private String path;

    private Integer depth;

    private Integer sortOrder;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;
}


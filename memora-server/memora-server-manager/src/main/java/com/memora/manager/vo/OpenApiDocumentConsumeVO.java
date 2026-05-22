package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OpenApiDocumentConsumeVO {
    private Long knowledgeBaseId;

    private String knowledgeBaseName;

    private Long documentId;

    private String title;

    private String format;

    private Integer versionNo;

    private String sourceExternalId;

    private String sourceRevision;

    private Boolean published;

    private String publicSlug;

    private String siteUrl;

    private String publicUrl;

    private String readerUrl;

    private String representation;

    private String mimeType;

    private String payload;

    private String contentText;

    private String summary;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;
}

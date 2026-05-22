package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublicShareDocumentVO {
    private Long shareId;

    private Long documentId;

    private Long knowledgeBaseId;

    private String knowledgeBaseName;

    private String title;

    private String format;

    private String content;

    private String contentText;

    private String summary;

    private String renderedHtml;

    private Integer versionNo;

    private LocalDateTime updatedAt;

    private LocalDateTime expiresAt;
}

package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PublicSiteDocumentVO {
    private Long knowledgeBaseId;

    private String knowledgeBaseName;

    private String siteSlug;

    private String siteTitle;

    private String siteDescription;

    private String siteUrl;

    private Long documentId;

    private String title;

    private String summary;

    private String publicSlug;

    private String publicUrl;

    private String format;

    private String contentText;

    private String renderedHtml;

    private Integer versionNo;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;

    private List<PublicSiteDocumentItemVO> navigation;
}

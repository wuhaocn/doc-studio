package com.memora.manager.vo;

import lombok.Data;

import java.util.List;

@Data
public class PublicSiteVO {
    private Long knowledgeBaseId;

    private String knowledgeBaseName;

    private String siteSlug;

    private String siteTitle;

    private String siteDescription;

    private String siteUrl;

    private List<PublicSiteDocumentItemVO> documents;
}


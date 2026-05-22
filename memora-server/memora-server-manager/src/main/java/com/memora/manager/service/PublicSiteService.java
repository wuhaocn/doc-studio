package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.entity.Document;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.mapper.DocumentMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.support.DocumentContentSupport;
import com.memora.manager.support.DocumentRenderSupport;
import com.memora.manager.vo.PublicSiteDocumentItemVO;
import com.memora.manager.vo.PublicSiteDocumentVO;
import com.memora.manager.vo.PublicSiteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicSiteService {
    private static final int SITE_ENABLED = 1;
    private static final String PUBLISH_STATUS_PUBLISHED = "PUBLISHED";

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;

    public PublicSiteVO getSite(String siteSlug) {
        KnowledgeBase knowledgeBase = requirePublishedSite(siteSlug);
        List<Document> documents = listPublishedDocuments(knowledgeBase.getId());

        PublicSiteVO vo = new PublicSiteVO();
        vo.setKnowledgeBaseId(knowledgeBase.getId());
        vo.setKnowledgeBaseName(knowledgeBase.getName());
        vo.setSiteSlug(knowledgeBase.getSiteSlug());
        vo.setSiteTitle(StringUtils.hasText(knowledgeBase.getSiteTitle()) ? knowledgeBase.getSiteTitle() : knowledgeBase.getName());
        vo.setSiteDescription(knowledgeBase.getSiteDescription() != null ? knowledgeBase.getSiteDescription() : knowledgeBase.getDescription());
        vo.setSiteUrl("/site/" + knowledgeBase.getSiteSlug());
        vo.setDocuments(documents.stream().map(this::convertDocumentItem).toList());
        return vo;
    }

    public PublicSiteDocumentVO getDocument(String siteSlug, String publicSlug) {
        KnowledgeBase knowledgeBase = requirePublishedSite(siteSlug);
        Document document = documentMapper.selectOne(new LambdaQueryWrapper<Document>()
            .eq(Document::getKnowledgeBaseId, knowledgeBase.getId())
            .eq(Document::getStatus, 1)
            .eq(Document::getDocType, "DOC")
            .eq(Document::getPublishStatus, PUBLISH_STATUS_PUBLISHED)
            .eq(Document::getPublicSlug, publicSlug)
            .last("LIMIT 1"));
        if (document == null) {
            throw new BusinessException(404, "公开文档不存在");
        }

        DocumentContentSupport.NormalizedStoredDocument normalized = DocumentContentSupport.normalizeStoredDocument(
            document.getDocType(),
            document.getFormat(),
            document.getContent(),
            document.getContentText(),
            document.getSummary()
        );
        PublicSiteDocumentVO vo = new PublicSiteDocumentVO();
        vo.setKnowledgeBaseId(knowledgeBase.getId());
        vo.setKnowledgeBaseName(knowledgeBase.getName());
        vo.setSiteSlug(knowledgeBase.getSiteSlug());
        vo.setSiteTitle(StringUtils.hasText(knowledgeBase.getSiteTitle()) ? knowledgeBase.getSiteTitle() : knowledgeBase.getName());
        vo.setSiteDescription(knowledgeBase.getSiteDescription() != null ? knowledgeBase.getSiteDescription() : knowledgeBase.getDescription());
        vo.setSiteUrl("/site/" + knowledgeBase.getSiteSlug());
        vo.setDocumentId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setSummary(normalized.summary());
        vo.setPublicSlug(document.getPublicSlug());
        vo.setPublicUrl("/site/" + knowledgeBase.getSiteSlug() + "/" + document.getPublicSlug());
        vo.setFormat(normalized.format());
        vo.setContentText(normalized.contentText());
        vo.setRenderedHtml(DocumentRenderSupport.resolveStoredRenderedHtml(
            normalized.docType(),
            normalized.format(),
            normalized.content(),
            document.getRenderedHtml()
        ));
        vo.setVersionNo(document.getVersionNo());
        vo.setPublishedAt(document.getPublishedAt());
        vo.setUpdatedAt(document.getUpdatedAt());
        vo.setNavigation(listPublishedDocuments(knowledgeBase.getId()).stream().map(this::convertDocumentItem).toList());
        return vo;
    }

    private KnowledgeBase requirePublishedSite(String siteSlug) {
        if (!StringUtils.hasText(siteSlug)) {
            throw new BusinessException(404, "公开站点不存在");
        }
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
            .eq(KnowledgeBase::getSiteSlug, siteSlug.trim())
            .eq(KnowledgeBase::getStatus, 1)
            .last("LIMIT 1"));
        if (knowledgeBase == null || knowledgeBase.getSiteEnabled() == null || knowledgeBase.getSiteEnabled() != SITE_ENABLED) {
            throw new BusinessException(404, "公开站点不存在");
        }
        return knowledgeBase;
    }

    private List<Document> listPublishedDocuments(Long knowledgeBaseId) {
        return documentMapper.selectList(new LambdaQueryWrapper<Document>()
            .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1)
            .eq(Document::getDocType, "DOC")
            .eq(Document::getPublishStatus, PUBLISH_STATUS_PUBLISHED)
            .orderByAsc(Document::getParentId)
            .orderByAsc(Document::getSortOrder)
            .orderByAsc(Document::getPath));
    }

    private PublicSiteDocumentItemVO convertDocumentItem(Document document) {
        DocumentContentSupport.NormalizedStoredDocument normalized = DocumentContentSupport.normalizeStoredDocument(
            document.getDocType(),
            document.getFormat(),
            document.getContent(),
            document.getContentText(),
            document.getSummary()
        );
        PublicSiteDocumentItemVO item = new PublicSiteDocumentItemVO();
        item.setDocumentId(document.getId());
        item.setParentId(document.getParentId());
        item.setTitle(document.getTitle());
        item.setSummary(normalized.summary());
        item.setPublicSlug(document.getPublicSlug());
        item.setPath(document.getPath());
        item.setDepth(document.getDepth());
        item.setSortOrder(document.getSortOrder());
        item.setPublishedAt(document.getPublishedAt());
        item.setUpdatedAt(document.getUpdatedAt());
        return item;
    }
}

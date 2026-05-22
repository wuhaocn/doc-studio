package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.OpenApiDocumentBatchUpsertDTO;
import com.memora.manager.dto.OpenApiDocumentCreateDTO;
import com.memora.manager.dto.OpenApiDocumentUpsertDTO;
import com.memora.manager.dto.OpenApiDocumentUpdateDTO;
import com.memora.manager.entity.Document;
import com.memora.manager.entity.DocumentVersion;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.mapper.DocumentMapper;
import com.memora.manager.mapper.DocumentVersionMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.support.ApiKeyAccessService;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.DocumentContentSupport;
import com.memora.manager.support.DocumentFormat;
import com.memora.manager.support.DocumentRenderSupport;
import com.memora.manager.support.DocumentSearchSupport;
import com.memora.manager.support.SlugUtils;
import com.memora.manager.vo.DocumentVO;
import com.memora.manager.vo.DocumentVersionVO;
import com.memora.manager.vo.KnowledgeBaseVO;
import com.memora.manager.vo.OpenApiDocumentBatchUpsertVO;
import com.memora.manager.vo.OpenApiDocumentConsumeVO;
import com.memora.manager.vo.OpenApiDocumentUpsertResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OpenApiDocumentService {
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 100;
    private static final String PUBLISH_STATUS_DRAFT = "DRAFT";
    private static final String PUBLISH_STATUS_PUBLISHED = "PUBLISHED";
    private static final String UPSERT_STATUS_CREATED = "CREATED";
    private static final String UPSERT_STATUS_UPDATED = "UPDATED";
    private static final String UPSERT_STATUS_SKIPPED = "SKIPPED";
    private static final String UPSERT_STATUS_CONFLICTED = "CONFLICTED";
    private static final String CONSUME_VIEW_METADATA = "metadata";
    private static final String CONSUME_VIEW_RENDERED = "rendered";
    private static final String CONSUME_VIEW_SOURCE = "source";
    private static final String CONSUME_VIEW_PLAIN = "plain";
    private static final String CONSUME_VIEW_SUMMARY = "summary";

    private final ApiKeyAccessService apiKeyAccessService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final AuditLogService auditLogService;

    public List<KnowledgeBaseVO> listKnowledgeBases() {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_LIST_KNOWLEDGE_BASES);
        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBase>()
            .in(KnowledgeBase::getId, principal.knowledgeBaseModes().keySet())
            .eq(KnowledgeBase::getTenantId, principal.tenantId())
            .eq(KnowledgeBase::getStatus, 1)
            .orderByAsc(KnowledgeBase::getSortOrder)
            .orderByDesc(KnowledgeBase::getUpdatedAt));
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            null,
            AuditLogConstants.OBJECT_TENANT,
            principal.tenantId(),
            "租户开放 API",
            AuditLogConstants.ACTION_OPEN_API_LIST_KNOWLEDGE_BASES,
            "列出可访问知识库 " + knowledgeBases.size() + " 个"
        ));
        return knowledgeBases.stream().map(item -> convertKnowledgeBase(item, principal)).toList();
    }

    public List<DocumentVO> listDocuments(Long knowledgeBaseId, Long parentId) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_LIST_DOCUMENTS);
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseRead(principal, knowledgeBaseId, AuditLogConstants.ACTION_OPEN_API_LIST_DOCUMENTS);

        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getTenantId, principal.tenantId())
            .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1);
        if (parentId != null) {
            queryWrapper.eq(Document::getParentId, parentId);
        }
        queryWrapper.orderByAsc(Document::getSortOrder).orderByAsc(Document::getPath);
        List<DocumentVO> documents = documentMapper.selectList(queryWrapper).stream().map(this::convertDocument).toList();
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_KNOWLEDGE_BASE,
            knowledgeBase.getId(),
            knowledgeBase.getName(),
            AuditLogConstants.ACTION_OPEN_API_LIST_DOCUMENTS,
            "列出知识库文档"
        ));
        return documents;
    }

    public List<DocumentVO> searchDocuments(String keyword, Long knowledgeBaseId, Integer size) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_SEARCH_DOCUMENTS);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (!StringUtils.hasText(normalizedKeyword)) {
            throw new BusinessException(400, "搜索关键词不能为空");
        }

        KnowledgeBase knowledgeBase = null;
        Set<Long> accessibleKnowledgeBaseIds = principal.knowledgeBaseModes().keySet();
        if (knowledgeBaseId != null) {
            knowledgeBase = apiKeyAccessService.requireKnowledgeBaseRead(principal, knowledgeBaseId, AuditLogConstants.ACTION_OPEN_API_SEARCH_DOCUMENTS);
            accessibleKnowledgeBaseIds = Set.of(knowledgeBaseId);
        }
        QueryWrapper<Document> queryWrapper = buildSearchQuery(principal.tenantId(), accessibleKnowledgeBaseIds, normalizedKeyword);
        queryWrapper.orderByDesc("updated_at")
            .orderByDesc("id")
            .last("LIMIT " + resolveSearchLimit(size));
        List<DocumentVO> documents = documentMapper.selectList(queryWrapper).stream()
            .filter(document -> DocumentSearchSupport.matches(document, normalizedKeyword))
            .sorted(DocumentSearchSupport.relevanceComparator(normalizedKeyword))
            .limit(resolveSearchLimit(size))
            .map(this::convertDocument)
            .toList();
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            knowledgeBase == null ? AuditLogConstants.OBJECT_TENANT : AuditLogConstants.OBJECT_KNOWLEDGE_BASE,
            knowledgeBase == null ? principal.tenantId() : knowledgeBase.getId(),
            knowledgeBase == null ? "租户开放 API" : knowledgeBase.getName(),
            AuditLogConstants.ACTION_OPEN_API_SEARCH_DOCUMENTS,
            "搜索关键词 \"" + normalizedKeyword + "\"，命中 " + documents.size() + " 篇正文"
        ));
        return documents;
    }

    private QueryWrapper<Document> buildSearchQuery(Long tenantId, Set<Long> knowledgeBaseIds, String keyword) {
        QueryWrapper<Document> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tenant_id", tenantId)
            .eq("status", 1)
            .eq("doc_type", "DOC")
            .in("knowledge_base_id", knowledgeBaseIds);
        List<String> keywordTokens = resolveSearchQueryTokens(keyword);
        if (!keywordTokens.isEmpty()) {
            queryWrapper.and(root -> {
                for (String token : keywordTokens) {
                    root.and(tokenWrapper -> tokenWrapper.like("title", token)
                        .or()
                        .like("content_text", token)
                        .or()
                        .like("summary", token)
                        .or()
                        .like("path", token));
                }
            });
        }
        return queryWrapper;
    }

    private List<String> resolveSearchQueryTokens(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        Set<String> tokens = new HashSet<>();
        List<String> results = new ArrayList<>();
        String fullKeyword = keyword.trim();
        if (tokens.add(fullKeyword.toLowerCase(Locale.ROOT))) {
            results.add(fullKeyword);
        }
        for (String part : fullKeyword.split("\\s+")) {
            String normalizedPart = part.trim();
            if (!normalizedPart.isEmpty() && tokens.add(normalizedPart.toLowerCase(Locale.ROOT))) {
                results.add(normalizedPart);
            }
        }
        return results;
    }

    public DocumentVO getDocument(Long documentId) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_GET_DOCUMENT);
        Document document = requireActiveDocument(principal.tenantId(), documentId);
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseRead(principal, document.getKnowledgeBaseId(), AuditLogConstants.ACTION_OPEN_API_GET_DOCUMENT);
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_GET_DOCUMENT,
            "读取文档详情"
        ));
        return convertDocument(document);
    }

    public OpenApiDocumentConsumeVO getDocumentBySource(Long knowledgeBaseId, String sourceExternalId, String view) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_GET_DOCUMENT_BY_SOURCE);
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseRead(principal, knowledgeBaseId, AuditLogConstants.ACTION_OPEN_API_GET_DOCUMENT_BY_SOURCE);
        Document document = requireReadableDocumentBySource(principal.tenantId(), knowledgeBase.getId(), sourceExternalId);
        String normalizedView = normalizeConsumeView(
            principal,
            knowledgeBase,
            document,
            AuditLogConstants.ACTION_OPEN_API_GET_DOCUMENT_BY_SOURCE,
            view
        );
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_GET_DOCUMENT_BY_SOURCE,
            "按 sourceExternalId 读取文档，视图 " + normalizedView
        ));
        return buildConsumeResponse(knowledgeBase, document, normalizedView);
    }

    public List<DocumentVersionVO> getVersions(Long documentId) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_GET_DOCUMENT_VERSIONS);
        Document document = requireActiveDocument(principal.tenantId(), documentId);
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseRead(principal, document.getKnowledgeBaseId(), AuditLogConstants.ACTION_OPEN_API_GET_DOCUMENT_VERSIONS);
        List<DocumentVersionVO> versions = documentVersionMapper.selectList(new LambdaQueryWrapper<DocumentVersion>()
                .eq(DocumentVersion::getDocumentId, documentId)
                .orderByDesc(DocumentVersion::getVersion))
            .stream()
            .map(this::convertDocumentVersion)
            .toList();
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_GET_DOCUMENT_VERSIONS,
            "读取文档版本列表"
        ));
        return versions;
    }

    public OpenApiDocumentConsumeVO consumeDocument(Long documentId, String view) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_CONSUME_DOCUMENT);
        Document document = requireActiveDocument(principal.tenantId(), documentId);
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseRead(principal, document.getKnowledgeBaseId(), AuditLogConstants.ACTION_OPEN_API_CONSUME_DOCUMENT);
        String normalizedView = normalizeConsumeView(
            principal,
            knowledgeBase,
            document,
            AuditLogConstants.ACTION_OPEN_API_CONSUME_DOCUMENT,
            view
        );
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_CONSUME_DOCUMENT,
            "消费文档视图 " + normalizedView
        ));
        return buildConsumeResponse(knowledgeBase, document, normalizedView);
    }

    public DocumentVO createDocument(OpenApiDocumentCreateDTO dto) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_CREATE_DOCUMENT);
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseWrite(principal, dto.getKnowledgeBaseId(), AuditLogConstants.ACTION_OPEN_API_CREATE_DOCUMENT);
        Document parent = resolveParent(principal.tenantId(), dto.getParentId(), knowledgeBase.getId());
        DocumentContentSupport.NormalizedDocumentContent normalizedContent =
            DocumentContentSupport.normalizeDocument("DOC", dto.getFormat(), dto.getContent());
        String normalizedTitle = dto.getTitle().trim();
        String normalizedSourceExternalId = normalizeOptionalValue(dto.getSourceExternalId());
        String normalizedSourceRevision = normalizeOptionalValue(dto.getSourceRevision());
        validateSourceExternalIdUniqueness(principal.tenantId(), knowledgeBase.getId(), normalizedSourceExternalId, null);

        Document document = new Document();
        document.setTenantId(principal.tenantId());
        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setTitle(normalizedTitle);
        document.setSlug(resolveDocumentSlug(normalizedTitle, dto.getSlug()));
        document.setDocType("DOC");
        document.setFormat(normalizedContent.format());
        document.setContent(normalizedContent.content());
        document.setContentText(normalizedContent.contentText());
        applyRenderedArtifact(document, normalizedContent);
        document.setSummary(DocumentContentSupport.resolveSummary(dto.getSummary(), null, normalizedContent.contentText(), true));
        document.setUserId(principal.serviceAccountCreatedByUserId());
        document.setParentId(parent == null ? 0L : parent.getId());
        document.setPath(buildPath(parent, document.getSlug()));
        document.setDepth(parent == null ? 0 : parent.getDepth() + 1);
        document.setVersionNo(1);
        document.setStatus(1);
        document.setViewCount(0);
        document.setPublishStatus(PUBLISH_STATUS_DRAFT);
        document.setSortOrder(resolveNextSortOrder(knowledgeBase.getId(), parent == null ? 0L : parent.getId()));
        document.setSourceExternalId(normalizedSourceExternalId);
        document.setSourceRevision(normalizedSourceRevision);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(document);
        refreshKnowledgeBaseDocumentCount(knowledgeBase.getId());
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_CREATE_DOCUMENT,
            "通过 Open API 创建文档"
        ));
        return convertDocument(document);
    }

    public DocumentVO updateDocument(Long documentId, OpenApiDocumentUpdateDTO dto) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_UPDATE_DOCUMENT);
        Document document = requireActiveDocument(principal.tenantId(), documentId);
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseWrite(principal, document.getKnowledgeBaseId(), AuditLogConstants.ACTION_OPEN_API_UPDATE_DOCUMENT);
        if (!Objects.equals(document.getVersionNo(), dto.getExpectedVersionNo())) {
            apiKeyAccessService.recordFailure(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_DOCUMENT,
                document.getId(),
                document.getTitle(),
                AuditLogConstants.ACTION_OPEN_API_UPDATE_DOCUMENT,
                "expectedVersionNo 不匹配，当前版本为 v" + document.getVersionNo()
            );
            throw new BusinessException(409, "文档版本已变化，请基于最新 versionNo 重试");
        }

        Document versionSnapshot = new Document();
        BeanUtils.copyProperties(document, versionSnapshot);
        String nextTitle = StringUtils.hasText(dto.getTitle()) ? dto.getTitle().trim() : document.getTitle();
        String nextFormat = dto.getFormat() != null ? dto.getFormat() : document.getFormat();
        String nextContent = dto.getContent() != null ? dto.getContent() : document.getContent();
        String normalizedNextFormat = DocumentFormat.resolve(nextFormat).name();
        if (!Objects.equals(document.getFormat(), normalizedNextFormat) && dto.getContent() == null) {
            throw new BusinessException(400, "修改文档格式时必须同时提交对应正文内容");
        }
        DocumentContentSupport.NormalizedDocumentContent normalizedContent =
            DocumentContentSupport.normalizeDocument("DOC", nextFormat, nextContent);
        boolean contentSourceChanged = !Objects.equals(document.getFormat(), normalizedContent.format())
            || !Objects.equals(document.getContent(), normalizedContent.content());
        String nextSummary = DocumentContentSupport.resolveSummary(
            dto.getSummary(),
            document.getSummary(),
            normalizedContent.contentText(),
            contentSourceChanged
        );

        boolean versionContentChanged = !Objects.equals(document.getTitle(), nextTitle)
            || !Objects.equals(document.getFormat(), normalizedContent.format())
            || !Objects.equals(document.getContent(), normalizedContent.content())
            || !Objects.equals(document.getContentText(), normalizedContent.contentText());
        String nextSourceExternalId = dto.getSourceExternalId() != null
            ? normalizeOptionalValue(dto.getSourceExternalId())
            : document.getSourceExternalId();
        String nextSourceRevision = dto.getSourceRevision() != null
            ? normalizeOptionalValue(dto.getSourceRevision())
            : document.getSourceRevision();
        validateSourceMetadataTransition(document, nextSourceExternalId, nextSourceRevision);
        validateSourceExternalIdUniqueness(principal.tenantId(), knowledgeBase.getId(), nextSourceExternalId, document.getId());
        document.setTitle(nextTitle);
        document.setFormat(normalizedContent.format());
        document.setContent(normalizedContent.content());
        document.setContentText(normalizedContent.contentText());
        applyRenderedArtifact(document, normalizedContent);
        document.setSummary(nextSummary);
        document.setSourceExternalId(nextSourceExternalId);
        document.setSourceRevision(nextSourceRevision);
        if (versionContentChanged) {
            createDocumentVersion(versionSnapshot, "Open API 更新前快照");
            document.setVersionNo((document.getVersionNo() == null ? 1 : document.getVersionNo()) + 1);
        }
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_UPDATE_DOCUMENT,
            "通过 Open API 更新文档"
        ));
        return convertDocument(document);
    }

    @Transactional
    public OpenApiDocumentUpsertResultVO upsertDocument(OpenApiDocumentUpsertDTO dto) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT);
        return upsertDocumentInternal(principal, dto, null);
    }

    @Transactional
    public OpenApiDocumentBatchUpsertVO batchUpsertDocuments(OpenApiDocumentBatchUpsertDTO dto) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_BATCH_UPSERT_DOCUMENTS);
        List<OpenApiDocumentUpsertResultVO> items = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int conflictedCount = 0;
        int index = 0;
        for (OpenApiDocumentUpsertDTO document : dto.getDocuments()) {
            OpenApiDocumentUpsertResultVO result = upsertDocumentInternal(principal, document, index);
            items.add(result);
            switch (result.getStatus()) {
                case UPSERT_STATUS_CREATED -> createdCount++;
                case UPSERT_STATUS_UPDATED -> updatedCount++;
                case UPSERT_STATUS_SKIPPED -> skippedCount++;
                case UPSERT_STATUS_CONFLICTED -> conflictedCount++;
                default -> {
                }
            }
            index++;
        }

        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            null,
            AuditLogConstants.OBJECT_TENANT,
            principal.tenantId(),
            "租户开放 API",
            AuditLogConstants.ACTION_OPEN_API_BATCH_UPSERT_DOCUMENTS,
            "批量同步完成 created=%d updated=%d skipped=%d conflicted=%d".formatted(
                createdCount,
                updatedCount,
                skippedCount,
                conflictedCount
            )
        ));

        OpenApiDocumentBatchUpsertVO response = new OpenApiDocumentBatchUpsertVO();
        response.setTotalCount(items.size());
        response.setCreatedCount(createdCount);
        response.setUpdatedCount(updatedCount);
        response.setSkippedCount(skippedCount);
        response.setConflictedCount(conflictedCount);
        response.setItems(items);
        return response;
    }

    public void deleteDocument(Long documentId) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_DELETE_DOCUMENT);
        Document document = requireActiveDocument(principal.tenantId(), documentId);
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseWrite(principal, document.getKnowledgeBaseId(), AuditLogConstants.ACTION_OPEN_API_DELETE_DOCUMENT);
        if (hasActiveChildren(documentId)) {
            apiKeyAccessService.recordFailure(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_DOCUMENT,
                document.getId(),
                document.getTitle(),
                AuditLogConstants.ACTION_OPEN_API_DELETE_DOCUMENT,
                "当前节点下仍有子文档，无法直接删除"
            );
            throw new BusinessException(400, "请先删除或移动子文档");
        }

        document.setStatus(0);
        document.setDeletedAt(LocalDateTime.now());
        document.setDeletedBy(principal.serviceAccountCreatedByUserId());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        refreshKnowledgeBaseDocumentCount(document.getKnowledgeBaseId());
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_DELETE_DOCUMENT,
            "通过 Open API 删除" + resolveDocumentTypeLabel(document.getDocType())
        ));
    }

    public DocumentVO restoreDocument(Long documentId) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_RESTORE_DOCUMENT);
        Document document = requireDeletedDocument(principal.tenantId(), documentId);
        KnowledgeBase knowledgeBase = requireRestorableKnowledgeBase(principal, document, AuditLogConstants.ACTION_OPEN_API_RESTORE_DOCUMENT);
        if (!parentCanBeRestored(principal.tenantId(), document)) {
            apiKeyAccessService.recordFailure(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_DOCUMENT,
                document.getId(),
                document.getTitle(),
                AuditLogConstants.ACTION_OPEN_API_RESTORE_DOCUMENT,
                "父级节点仍不可用，无法恢复当前文档"
            );
            throw new BusinessException(400, "父级节点仍在回收站，请先恢复父级节点");
        }

        document.setStatus(1);
        document.setDeletedAt(null);
        document.setDeletedBy(null);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        refreshKnowledgeBaseDocumentCount(document.getKnowledgeBaseId());
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_RESTORE_DOCUMENT,
            "通过 Open API 恢复" + resolveDocumentTypeLabel(document.getDocType())
        ));
        return convertDocument(document);
    }

    public DocumentVO rollbackDocument(Long documentId, Long versionId) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_ROLLBACK_DOCUMENT);
        Document document = requireActiveDocument(principal.tenantId(), documentId);
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseWrite(principal, document.getKnowledgeBaseId(), AuditLogConstants.ACTION_OPEN_API_ROLLBACK_DOCUMENT);
        DocumentVersion version = documentVersionMapper.selectById(versionId);
        if (version == null || !Objects.equals(version.getDocumentId(), documentId)) {
            apiKeyAccessService.recordFailure(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_DOCUMENT,
                document.getId(),
                document.getTitle(),
                AuditLogConstants.ACTION_OPEN_API_ROLLBACK_DOCUMENT,
                "目标版本不存在"
            );
            throw new BusinessException(404, "版本不存在");
        }

        createDocumentVersion(document, "Open API 回滚前快照");
        DocumentContentSupport.NormalizedDocumentContent normalizedContent =
            DocumentContentSupport.normalizeDocument(document.getDocType(), version.getFormat(), version.getContent());
        document.setTitle(version.getTitle());
        document.setFormat(normalizedContent.format());
        document.setContent(normalizedContent.content());
        document.setContentText(normalizedContent.contentText());
        applyRenderedArtifact(document, normalizedContent);
        document.setSummary(DocumentContentSupport.resolveSummary(null, null, normalizedContent.contentText(), true));
        document.setVersionNo((document.getVersionNo() == null ? 1 : document.getVersionNo()) + 1);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_ROLLBACK_DOCUMENT,
            "通过 Open API 回滚到历史版本 v" + version.getVersion()
        ));
        return convertDocument(document);
    }

    private KnowledgeBaseVO convertKnowledgeBase(KnowledgeBase knowledgeBase, ApiKeyAccessService.ApiKeyPrincipal principal) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, vo);
        String accessMode = principal.knowledgeBaseModes().get(knowledgeBase.getId());
        vo.setCurrentRole("API_KEY_" + accessMode);
        vo.setCanWrite(ApiKeyAccessService.ACCESS_MODE_WRITE.equals(accessMode));
        vo.setCanManage(false);
        vo.setPermissionRestricted(true);
        return vo;
    }

    private DocumentVO convertDocument(Document document) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(document, vo);
        DocumentContentSupport.NormalizedStoredDocument normalized = DocumentContentSupport.normalizeStoredDocument(
            document.getDocType(),
            document.getFormat(),
            document.getContent(),
            document.getContentText(),
            document.getSummary()
        );
        vo.setDocType(normalized.docType());
        vo.setFormat(normalized.format());
        vo.setContentText(normalized.contentText());
        vo.setSummary(normalized.summary());
        vo.setRenderedHtml(DocumentRenderSupport.resolveStoredRenderedHtml(
            normalized.docType(),
            normalized.format(),
            normalized.content(),
            document.getRenderedHtml()
        ));
        vo.setPublished("PUBLISHED".equals(document.getPublishStatus()));
        return vo;
    }

    private OpenApiDocumentUpsertResultVO upsertDocumentInternal(
        ApiKeyAccessService.ApiKeyPrincipal principal,
        OpenApiDocumentUpsertDTO dto,
        Integer itemIndex
    ) {
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseWrite(principal, dto.getKnowledgeBaseId(), AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT);
        String normalizedSourceExternalId = normalizeRequiredValue(dto.getSourceExternalId(), "来源外部ID不能为空");
        String normalizedSourceRevision = normalizeOptionalValue(dto.getSourceRevision());
        List<Document> matchedDocuments = listDocumentsBySourceExternalId(principal.tenantId(), knowledgeBase.getId(), normalizedSourceExternalId);
        if (matchedDocuments.size() > 1) {
            String detail = "sourceExternalId 已关联多篇文档，请先治理历史数据: " + normalizedSourceExternalId;
            apiKeyAccessService.recordFailure(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_KNOWLEDGE_BASE,
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT,
                detail
            );
            return buildUpsertResult(itemIndex, normalizedSourceExternalId, normalizedSourceRevision, UPSERT_STATUS_CONFLICTED, detail, null);
        }

        Document existingDocument = matchedDocuments.isEmpty() ? null : matchedDocuments.get(0);
        if (existingDocument == null) {
            DocumentVO createdDocument = createDocumentByUpsert(principal, knowledgeBase, dto, normalizedSourceExternalId, normalizedSourceRevision);
            return buildUpsertResult(
                itemIndex,
                normalizedSourceExternalId,
                normalizedSourceRevision,
                UPSERT_STATUS_CREATED,
                "已创建新文档",
                createdDocument
            );
        }

        if (!"DOC".equals(existingDocument.getDocType())) {
            String detail = "sourceExternalId 已绑定非正文节点，当前 upsert 仅支持 DOC: " + normalizedSourceExternalId;
            apiKeyAccessService.recordFailure(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_DOCUMENT,
                existingDocument.getId(),
                existingDocument.getTitle(),
                AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT,
                detail
            );
            return buildUpsertResult(itemIndex, normalizedSourceExternalId, normalizedSourceRevision, UPSERT_STATUS_CONFLICTED, detail, convertDocument(existingDocument));
        }

        return updateDocumentByUpsert(principal, knowledgeBase, existingDocument, dto, itemIndex, normalizedSourceExternalId, normalizedSourceRevision);
    }

    private DocumentVO createDocumentByUpsert(
        ApiKeyAccessService.ApiKeyPrincipal principal,
        KnowledgeBase knowledgeBase,
        OpenApiDocumentUpsertDTO dto,
        String sourceExternalId,
        String sourceRevision
    ) {
        Document parent = resolveParent(principal.tenantId(), dto.getParentId(), knowledgeBase.getId());
        DocumentContentSupport.NormalizedDocumentContent normalizedContent =
            DocumentContentSupport.normalizeDocument("DOC", dto.getFormat(), dto.getContent());
        String normalizedTitle = dto.getTitle().trim();

        Document document = new Document();
        document.setTenantId(principal.tenantId());
        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setTitle(normalizedTitle);
        document.setSlug(resolveDocumentSlug(normalizedTitle, dto.getSlug()));
        document.setDocType("DOC");
        document.setFormat(normalizedContent.format());
        document.setContent(normalizedContent.content());
        document.setContentText(normalizedContent.contentText());
        applyRenderedArtifact(document, normalizedContent);
        document.setSummary(DocumentContentSupport.resolveSummary(dto.getSummary(), null, normalizedContent.contentText(), true));
        document.setUserId(principal.serviceAccountCreatedByUserId());
        document.setParentId(parent == null ? 0L : parent.getId());
        document.setPath(buildPath(parent, document.getSlug()));
        document.setDepth(parent == null ? 0 : parent.getDepth() + 1);
        document.setVersionNo(1);
        document.setStatus(1);
        document.setViewCount(0);
        document.setPublishStatus(PUBLISH_STATUS_DRAFT);
        document.setSortOrder(resolveNextSortOrder(knowledgeBase.getId(), parent == null ? 0L : parent.getId()));
        document.setSourceExternalId(sourceExternalId);
        document.setSourceRevision(sourceRevision);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(document);
        refreshKnowledgeBaseDocumentCount(knowledgeBase.getId());
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT,
            "通过 Open API upsert 创建文档"
        ));
        return convertDocument(document);
    }

    private OpenApiDocumentUpsertResultVO updateDocumentByUpsert(
        ApiKeyAccessService.ApiKeyPrincipal principal,
        KnowledgeBase knowledgeBase,
        Document document,
        OpenApiDocumentUpsertDTO dto,
        Integer itemIndex,
        String sourceExternalId,
        String sourceRevision
    ) {
        String currentSourceRevision = normalizeOptionalValue(document.getSourceRevision());
        if (sourceRevision == null && currentSourceRevision != null) {
            String detail = "当前文档已启用 sourceRevision，后续 upsert 必须继续提供来源版本";
            apiKeyAccessService.recordFailure(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_DOCUMENT,
                document.getId(),
                document.getTitle(),
                AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT,
                detail
            );
            return buildUpsertResult(itemIndex, sourceExternalId, null, UPSERT_STATUS_CONFLICTED, detail, convertDocument(document));
        }
        int revisionComparison = compareSourceRevisions(sourceRevision, currentSourceRevision);
        if (revisionComparison < 0) {
            String detail = "sourceRevision 落后于当前文档，当前版本为 " + currentSourceRevision;
            apiKeyAccessService.recordFailure(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_DOCUMENT,
                document.getId(),
                document.getTitle(),
                AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT,
                detail
            );
            return buildUpsertResult(itemIndex, sourceExternalId, sourceRevision, UPSERT_STATUS_CONFLICTED, detail, convertDocument(document));
        }

        Document versionSnapshot = new Document();
        BeanUtils.copyProperties(document, versionSnapshot);
        Document parent = resolveUpsertParent(principal.tenantId(), knowledgeBase.getId(), document, dto.getParentId());
        String nextTitle = dto.getTitle().trim();
        String nextSlug = StringUtils.hasText(dto.getSlug()) ? resolveDocumentSlug(nextTitle, dto.getSlug()) : document.getSlug();
        String nextFormat = dto.getFormat() != null ? dto.getFormat() : document.getFormat();
        String nextContent = dto.getContent() != null ? dto.getContent() : document.getContent();
        String normalizedNextFormat = DocumentFormat.resolve(nextFormat).name();
        if (!Objects.equals(document.getFormat(), normalizedNextFormat) && dto.getContent() == null) {
            throw new BusinessException(400, "修改文档格式时必须同时提交对应正文内容");
        }
        DocumentContentSupport.NormalizedDocumentContent normalizedContent =
            DocumentContentSupport.normalizeDocument("DOC", nextFormat, nextContent);
        boolean contentSourceChanged = !Objects.equals(document.getFormat(), normalizedContent.format())
            || !Objects.equals(document.getContent(), normalizedContent.content());
        String nextSummary = DocumentContentSupport.resolveSummary(
            dto.getSummary(),
            document.getSummary(),
            normalizedContent.contentText(),
            contentSourceChanged
        );
        Long nextParentId = parent == null ? 0L : parent.getId();
        String nextPath = buildPath(parent, nextSlug);
        boolean versionContentChanged = !Objects.equals(document.getTitle(), nextTitle)
            || !Objects.equals(document.getFormat(), normalizedContent.format())
            || !Objects.equals(document.getContent(), normalizedContent.content())
            || !Objects.equals(document.getContentText(), normalizedContent.contentText());
        boolean structureChanged = !Objects.equals(document.getSlug(), nextSlug)
            || !Objects.equals(document.getParentId(), nextParentId)
            || !Objects.equals(document.getPath(), nextPath)
            || !Objects.equals(document.getDepth(), parent == null ? 0 : parent.getDepth() + 1);
        boolean metadataChanged = !Objects.equals(document.getSummary(), nextSummary)
            || !Objects.equals(document.getSourceRevision(), sourceRevision);
        boolean restoringDeletedDocument = document.getStatus() == null || document.getStatus() == 0;
        boolean effectiveStateChanged = versionContentChanged || structureChanged || metadataChanged || restoringDeletedDocument;

        if (revisionComparison == 0) {
            if (!effectiveStateChanged) {
                auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
                    principal,
                    knowledgeBase,
                    AuditLogConstants.OBJECT_DOCUMENT,
                    document.getId(),
                    document.getTitle(),
                    AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT,
                    "sourceRevision 未变化，跳过同步"
                ));
                return buildUpsertResult(itemIndex, sourceExternalId, sourceRevision, UPSERT_STATUS_SKIPPED, "sourceRevision 未变化，已跳过", convertDocument(document));
            }
            String detail = "sourceRevision 与当前文档相同，但请求内容发生变化";
            apiKeyAccessService.recordFailure(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_DOCUMENT,
                document.getId(),
                document.getTitle(),
                AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT,
                detail
            );
            return buildUpsertResult(itemIndex, sourceExternalId, sourceRevision, UPSERT_STATUS_CONFLICTED, detail, convertDocument(document));
        }

        if (!effectiveStateChanged) {
            auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_DOCUMENT,
                document.getId(),
                document.getTitle(),
                AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT,
                "请求内容未变化，跳过同步"
            ));
            return buildUpsertResult(itemIndex, sourceExternalId, sourceRevision, UPSERT_STATUS_SKIPPED, "请求内容未变化，已跳过", convertDocument(document));
        }

        document.setTitle(nextTitle);
        document.setSlug(nextSlug);
        document.setFormat(normalizedContent.format());
        document.setContent(normalizedContent.content());
        document.setContentText(normalizedContent.contentText());
        applyRenderedArtifact(document, normalizedContent);
        document.setSummary(nextSummary);
        document.setParentId(nextParentId);
        document.setPath(nextPath);
        document.setDepth(parent == null ? 0 : parent.getDepth() + 1);
        document.setSourceExternalId(sourceExternalId);
        document.setSourceRevision(sourceRevision);
        if (document.getStatus() == null || document.getStatus() == 0) {
            document.setStatus(1);
            document.setDeletedAt(null);
            document.setDeletedBy(null);
            document.setPublishStatus(PUBLISH_STATUS_DRAFT);
            document.setPublishedAt(null);
        }
        if (!Objects.equals(versionSnapshot.getParentId(), nextParentId)) {
            document.setSortOrder(resolveNextSortOrder(knowledgeBase.getId(), nextParentId, document.getId()));
        }
        if (versionContentChanged) {
            createDocumentVersion(versionSnapshot, "Open API upsert 更新前快照");
            document.setVersionNo((document.getVersionNo() == null ? 1 : document.getVersionNo()) + 1);
        }
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        refreshKnowledgeBaseDocumentCount(knowledgeBase.getId());
        auditLogService.recordSuccess(apiKeyAccessService.buildAuditCommand(
            principal,
            knowledgeBase,
            AuditLogConstants.OBJECT_DOCUMENT,
            document.getId(),
            document.getTitle(),
            AuditLogConstants.ACTION_OPEN_API_UPSERT_DOCUMENT,
            restoringDeletedDocument ? "通过 Open API upsert 恢复并更新文档" : "通过 Open API upsert 更新文档"
        ));
        return buildUpsertResult(itemIndex, sourceExternalId, sourceRevision, UPSERT_STATUS_UPDATED, "已更新文档", convertDocument(document));
    }

    private DocumentVersionVO convertDocumentVersion(DocumentVersion version) {
        DocumentVersionVO vo = new DocumentVersionVO();
        BeanUtils.copyProperties(version, vo);
        DocumentContentSupport.NormalizedStoredDocument normalized = DocumentContentSupport.normalizeStoredDocument(
            "DOC",
            version.getFormat(),
            version.getContent(),
            version.getContentText(),
            null
        );
        vo.setFormat(normalized.format());
        vo.setContentText(normalized.contentText());
        return vo;
    }

    private OpenApiDocumentConsumeVO buildConsumeResponse(KnowledgeBase knowledgeBase, Document document, String normalizedView) {
        DocumentContentSupport.NormalizedStoredDocument normalized = DocumentContentSupport.normalizeStoredDocument(
            document.getDocType(),
            document.getFormat(),
            document.getContent(),
            document.getContentText(),
            document.getSummary()
        );
        String siteUrl = resolveSiteUrl(knowledgeBase);
        String publicUrl = resolvePublicUrl(knowledgeBase, document);

        OpenApiDocumentConsumeVO vo = new OpenApiDocumentConsumeVO();
        vo.setKnowledgeBaseId(knowledgeBase.getId());
        vo.setKnowledgeBaseName(knowledgeBase.getName());
        vo.setDocumentId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setFormat(normalized.format());
        vo.setVersionNo(document.getVersionNo());
        vo.setSourceExternalId(document.getSourceExternalId());
        vo.setSourceRevision(document.getSourceRevision());
        vo.setPublished(PUBLISH_STATUS_PUBLISHED.equals(document.getPublishStatus()));
        vo.setPublicSlug(document.getPublicSlug());
        vo.setSiteUrl(siteUrl);
        vo.setPublicUrl(publicUrl);
        vo.setReaderUrl("/docs/" + document.getId());
        vo.setRepresentation(normalizedView);
        vo.setMimeType(resolveConsumeMimeType(normalizedView, normalized.format()));
        vo.setPayload(resolveConsumePayload(normalizedView, normalized, document));
        vo.setContentText(normalized.contentText());
        vo.setSummary(normalized.summary());
        vo.setPublishedAt(document.getPublishedAt());
        vo.setUpdatedAt(document.getUpdatedAt());
        return vo;
    }

    private Document requireActiveDocument(Long tenantId, Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null || document.getStatus() == null || document.getStatus() == 0) {
            throw new BusinessException(404, "文档不存在");
        }
        if (!Objects.equals(document.getTenantId(), tenantId)) {
            throw new BusinessException(403, "当前 API key 无权访问该文档");
        }
        return document;
    }

    private Document requireReadableDocumentBySource(Long tenantId, Long knowledgeBaseId, String sourceExternalId) {
        String normalizedSourceExternalId = normalizeRequiredValue(sourceExternalId, "sourceExternalId 不能为空");
        List<Document> documents = documentMapper.selectList(new LambdaQueryWrapper<Document>()
            .eq(Document::getTenantId, tenantId)
            .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1)
            .eq(Document::getDocType, "DOC")
            .eq(Document::getSourceExternalId, normalizedSourceExternalId)
            .orderByDesc(Document::getUpdatedAt)
            .orderByDesc(Document::getId));
        if (documents.isEmpty()) {
            throw new BusinessException(404, "当前 sourceExternalId 对应文档不存在");
        }
        if (documents.size() > 1) {
            throw new BusinessException(409, "当前 sourceExternalId 关联了多篇文档，请先治理历史数据");
        }
        return documents.get(0);
    }

    private Document requireDeletedDocument(Long tenantId, Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null || document.getStatus() == null || document.getStatus() != 0) {
            throw new BusinessException(404, "回收站中没有该文档");
        }
        if (!Objects.equals(document.getTenantId(), tenantId)) {
            throw new BusinessException(403, "当前 API key 无权访问该文档");
        }
        return document;
    }

    private Document resolveParent(Long tenantId, Long parentId, Long knowledgeBaseId) {
        if (parentId == null || parentId == 0) {
            return null;
        }
        Document parent = requireActiveDocument(tenantId, parentId);
        if (!Objects.equals(parent.getKnowledgeBaseId(), knowledgeBaseId)) {
            throw new BusinessException(400, "父级节点不属于当前知识库");
        }
        if (!"FOLDER".equals(parent.getDocType())) {
            throw new BusinessException(400, "父级节点必须是目录");
        }
        return parent;
    }

    private Document resolveUpsertParent(Long tenantId, Long knowledgeBaseId, Document currentDocument, Long requestedParentId) {
        Long effectiveParentId = requestedParentId;
        if (effectiveParentId == null) {
            effectiveParentId = currentDocument.getParentId();
        }
        return resolveParent(tenantId, effectiveParentId, knowledgeBaseId);
    }

    private KnowledgeBase requireRestorableKnowledgeBase(
        ApiKeyAccessService.ApiKeyPrincipal principal,
        Document document,
        String actionType
    ) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(document.getKnowledgeBaseId());
        if (knowledgeBase == null || !Objects.equals(knowledgeBase.getTenantId(), principal.tenantId())) {
            throw new BusinessException(403, "当前 API key 无权访问该知识库");
        }
        if (knowledgeBase.getStatus() == null || knowledgeBase.getStatus() == 0) {
            throw new BusinessException(400, "所属知识库已删除，请先恢复知识库");
        }
        return apiKeyAccessService.requireKnowledgeBaseWrite(principal, knowledgeBase.getId(), actionType);
    }

    private boolean hasActiveChildren(Long documentId) {
        return documentMapper.selectCount(new LambdaQueryWrapper<Document>()
            .eq(Document::getParentId, documentId)
            .eq(Document::getStatus, 1)) > 0;
    }

    private boolean parentCanBeRestored(Long tenantId, Document document) {
        if (document.getParentId() == null || document.getParentId() == 0) {
            return true;
        }
        Document parent = documentMapper.selectById(document.getParentId());
        return parent != null
            && Objects.equals(parent.getTenantId(), tenantId)
            && parent.getStatus() != null
            && parent.getStatus() == 1;
    }

    private String resolveDocumentSlug(String title, String slug) {
        return StringUtils.hasText(slug) ? SlugUtils.toSlug(slug) : SlugUtils.toSlug(title);
    }

    private String buildPath(Document parent, String slug) {
        if (parent == null || !StringUtils.hasText(parent.getPath())) {
            return "/" + slug;
        }
        return parent.getPath() + "/" + slug;
    }

    private Integer resolveNextSortOrder(Long knowledgeBaseId, Long parentId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1)
            .eq(Document::getParentId, parentId)
            .orderByDesc(Document::getSortOrder)
            .last("LIMIT 1");
        Document lastDocument = documentMapper.selectOne(queryWrapper);
        return lastDocument == null || lastDocument.getSortOrder() == null ? 0 : lastDocument.getSortOrder() + 1;
    }

    private Integer resolveNextSortOrder(Long knowledgeBaseId, Long parentId, Long excludeDocumentId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1)
            .eq(Document::getParentId, parentId);
        if (excludeDocumentId != null) {
            queryWrapper.ne(Document::getId, excludeDocumentId);
        }
        List<Document> siblings = documentMapper.selectList(queryWrapper);
        return siblings.stream()
            .map(Document::getSortOrder)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(-1) + 1;
    }

    private void createDocumentVersion(Document document, String remark) {
        Integer latestVersion = documentVersionMapper.getLatestVersion(document.getId());
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(document.getId());
        version.setVersion(latestVersion == null ? 1 : latestVersion + 1);
        version.setTitle(document.getTitle());
        version.setFormat(document.getFormat());
        version.setContent(document.getContent());
        version.setContentText(document.getContentText());
        version.setUserId(document.getUserId());
        version.setRemark(remark);
        version.setCreatedAt(LocalDateTime.now());
        documentVersionMapper.insert(version);
    }

    private void applyRenderedArtifact(Document document, DocumentContentSupport.NormalizedDocumentContent normalizedContent) {
        document.setRenderedHtml(normalizedContent.renderedHtml());
        document.setRenderChecksum(normalizedContent.renderChecksum());
    }

    private void refreshKnowledgeBaseDocumentCount(Long knowledgeBaseId) {
        long documentCount = documentMapper.selectCount(new LambdaQueryWrapper<Document>()
            .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1)
            .eq(Document::getDocType, "DOC"));
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase != null) {
            knowledgeBase.setDocumentCount((int) documentCount);
            knowledgeBase.setUpdatedAt(LocalDateTime.now());
            knowledgeBaseMapper.updateById(knowledgeBase);
        }
    }

    private int resolveSearchLimit(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SEARCH_LIMIT;
        }
        return Math.min(size, MAX_SEARCH_LIMIT);
    }

    private String resolveDocumentTypeLabel(String docType) {
        return "FOLDER".equals(docType) ? "目录" : "文档";
    }

    private String normalizeConsumeView(String rawView) {
        String normalized = rawView == null ? CONSUME_VIEW_RENDERED : rawView.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            normalized = CONSUME_VIEW_RENDERED;
        }
        if (!CONSUME_VIEW_METADATA.equals(normalized)
            && !CONSUME_VIEW_RENDERED.equals(normalized)
            && !CONSUME_VIEW_SOURCE.equals(normalized)
            && !CONSUME_VIEW_PLAIN.equals(normalized)
            && !CONSUME_VIEW_SUMMARY.equals(normalized)) {
            throw new BusinessException(400, "当前 view 仅支持 metadata / rendered / source / plain / summary");
        }
        return normalized;
    }

    private String normalizeConsumeView(
        ApiKeyAccessService.ApiKeyPrincipal principal,
        KnowledgeBase knowledgeBase,
        Document document,
        String actionType,
        String rawView
    ) {
        try {
            return normalizeConsumeView(rawView);
        } catch (BusinessException exception) {
            apiKeyAccessService.recordFailure(
                principal,
                knowledgeBase,
                AuditLogConstants.OBJECT_DOCUMENT,
                document.getId(),
                document.getTitle(),
                actionType,
                exception.getMessage()
            );
            throw exception;
        }
    }

    private String resolveConsumeMimeType(String normalizedView, String format) {
        return switch (normalizedView) {
            case CONSUME_VIEW_METADATA -> "application/json";
            case CONSUME_VIEW_RENDERED -> "text/html";
            case CONSUME_VIEW_PLAIN, CONSUME_VIEW_SUMMARY -> "text/plain";
            case CONSUME_VIEW_SOURCE -> switch (DocumentFormat.resolve(format).name()) {
                case "MARKDOWN" -> "text/markdown";
                case "HTML", "RICH_TEXT" -> "text/html";
                default -> "text/plain";
            };
            default -> "text/plain";
        };
    }

    private String resolveConsumePayload(
        String normalizedView,
        DocumentContentSupport.NormalizedStoredDocument normalized,
        Document document
    ) {
        return switch (normalizedView) {
            case CONSUME_VIEW_METADATA -> null;
            case CONSUME_VIEW_RENDERED -> DocumentRenderSupport.resolveStoredRenderedHtml(
                normalized.docType(),
                normalized.format(),
                normalized.content(),
                document.getRenderedHtml()
            );
            case CONSUME_VIEW_SOURCE -> normalized.content();
            case CONSUME_VIEW_PLAIN -> normalized.contentText();
            case CONSUME_VIEW_SUMMARY -> normalized.summary();
            default -> null;
        };
    }

    private String resolveSiteUrl(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null
            || knowledgeBase.getSiteEnabled() == null
            || knowledgeBase.getSiteEnabled() != 1
            || !StringUtils.hasText(knowledgeBase.getSiteSlug())) {
            return null;
        }
        return "/site/" + knowledgeBase.getSiteSlug();
    }

    private String resolvePublicUrl(KnowledgeBase knowledgeBase, Document document) {
        String siteUrl = resolveSiteUrl(knowledgeBase);
        if (siteUrl == null
            || document == null
            || !PUBLISH_STATUS_PUBLISHED.equals(document.getPublishStatus())
            || !StringUtils.hasText(document.getPublicSlug())) {
            return null;
        }
        return siteUrl + "/" + document.getPublicSlug();
    }

    private List<Document> listDocumentsBySourceExternalId(Long tenantId, Long knowledgeBaseId, String sourceExternalId) {
        if (!StringUtils.hasText(sourceExternalId)) {
            return List.of();
        }
        return documentMapper.selectList(new LambdaQueryWrapper<Document>()
            .eq(Document::getTenantId, tenantId)
            .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getSourceExternalId, sourceExternalId)
            .orderByDesc(Document::getUpdatedAt)
            .orderByDesc(Document::getId));
    }

    private void validateSourceExternalIdUniqueness(Long tenantId, Long knowledgeBaseId, String sourceExternalId, Long excludeDocumentId) {
        if (!StringUtils.hasText(sourceExternalId)) {
            return;
        }
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<Document>()
            .eq(Document::getTenantId, tenantId)
            .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getSourceExternalId, sourceExternalId);
        if (excludeDocumentId != null) {
            queryWrapper.ne(Document::getId, excludeDocumentId);
        }
        if (documentMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(409, "当前 sourceExternalId 已存在，请改用 upsert 或更换来源外部ID");
        }
    }

    private void validateSourceMetadataTransition(Document document, String nextSourceExternalId, String nextSourceRevision) {
        if (nextSourceRevision != null && nextSourceExternalId == null && !StringUtils.hasText(document.getSourceExternalId())) {
            throw new BusinessException(400, "设置 sourceRevision 前必须先绑定 sourceExternalId");
        }
    }

    private OpenApiDocumentUpsertResultVO buildUpsertResult(
        Integer itemIndex,
        String sourceExternalId,
        String sourceRevision,
        String status,
        String message,
        DocumentVO document
    ) {
        OpenApiDocumentUpsertResultVO result = new OpenApiDocumentUpsertResultVO();
        result.setItemIndex(itemIndex);
        result.setStatus(status);
        result.setMessage(message);
        result.setSourceExternalId(sourceExternalId);
        result.setSourceRevision(sourceRevision);
        result.setDocument(document);
        return result;
    }

    private String normalizeOptionalValue(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String normalizeRequiredValue(String rawValue, String message) {
        String normalized = normalizeOptionalValue(rawValue);
        if (normalized == null) {
            throw new BusinessException(400, message);
        }
        return normalized;
    }

    private int compareSourceRevisions(String incomingRevision, String currentRevision) {
        String normalizedIncoming = normalizeOptionalValue(incomingRevision);
        String normalizedCurrent = normalizeOptionalValue(currentRevision);
        if (normalizedIncoming == null && normalizedCurrent == null) {
            return 0;
        }
        if (normalizedIncoming == null) {
            return -1;
        }
        if (normalizedCurrent == null) {
            return 1;
        }
        if (normalizedIncoming.equals(normalizedCurrent)) {
            return 0;
        }

        Integer numericComparison = compareNumericRevision(normalizedIncoming, normalizedCurrent);
        if (numericComparison != null) {
            return numericComparison;
        }

        Integer instantComparison = compareTemporalRevision(normalizedIncoming, normalizedCurrent);
        if (instantComparison != null) {
            return instantComparison;
        }

        return compareNaturalRevision(normalizedIncoming, normalizedCurrent);
    }

    private Integer compareNumericRevision(String incomingRevision, String currentRevision) {
        if (!incomingRevision.matches("\\d+") || !currentRevision.matches("\\d+")) {
            return null;
        }
        return new BigInteger(incomingRevision).compareTo(new BigInteger(currentRevision));
    }

    private Integer compareTemporalRevision(String incomingRevision, String currentRevision) {
        Instant incomingInstant = parseRevisionInstant(incomingRevision);
        Instant currentInstant = parseRevisionInstant(currentRevision);
        if (incomingInstant == null || currentInstant == null) {
            return null;
        }
        return incomingInstant.compareTo(currentInstant);
    }

    private Instant parseRevisionInstant(String revision) {
        try {
            return Instant.parse(revision);
        } catch (Exception ignored) {
        }
        try {
            return OffsetDateTime.parse(revision).toInstant();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(revision).atOffset(OffsetDateTime.now().getOffset()).toInstant();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(revision).atStartOfDay().atOffset(OffsetDateTime.now().getOffset()).toInstant();
        } catch (Exception ignored) {
        }
        return null;
    }

    private int compareNaturalRevision(String incomingRevision, String currentRevision) {
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < incomingRevision.length() && rightIndex < currentRevision.length()) {
            char left = incomingRevision.charAt(leftIndex);
            char right = currentRevision.charAt(rightIndex);
            if (Character.isDigit(left) && Character.isDigit(right)) {
                int leftEnd = leftIndex;
                int rightEnd = rightIndex;
                while (leftEnd < incomingRevision.length() && Character.isDigit(incomingRevision.charAt(leftEnd))) {
                    leftEnd++;
                }
                while (rightEnd < currentRevision.length() && Character.isDigit(currentRevision.charAt(rightEnd))) {
                    rightEnd++;
                }
                int compare = new BigInteger(incomingRevision.substring(leftIndex, leftEnd))
                    .compareTo(new BigInteger(currentRevision.substring(rightIndex, rightEnd)));
                if (compare != 0) {
                    return compare;
                }
                leftIndex = leftEnd;
                rightIndex = rightEnd;
                continue;
            }
            int compare = Character.compare(Character.toLowerCase(left), Character.toLowerCase(right));
            if (compare != 0) {
                return compare;
            }
            leftIndex++;
            rightIndex++;
        }
        return Integer.compare(incomingRevision.length(), currentRevision.length());
    }
}

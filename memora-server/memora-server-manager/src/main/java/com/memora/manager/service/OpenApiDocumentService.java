package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.OpenApiDocumentCreateDTO;
import com.memora.manager.dto.OpenApiDocumentUpdateDTO;
import com.memora.manager.entity.Document;
import com.memora.manager.entity.DocumentVersion;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.mapper.DocumentMapper;
import com.memora.manager.mapper.DocumentVersionMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.support.ApiKeyAccessService;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.SlugUtils;
import com.memora.manager.vo.DocumentVO;
import com.memora.manager.vo.DocumentVersionVO;
import com.memora.manager.vo.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenApiDocumentService {
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

    public DocumentVO createDocument(OpenApiDocumentCreateDTO dto) {
        ApiKeyAccessService.ApiKeyPrincipal principal = apiKeyAccessService.requirePrincipal(AuditLogConstants.ACTION_OPEN_API_CREATE_DOCUMENT);
        KnowledgeBase knowledgeBase = apiKeyAccessService.requireKnowledgeBaseWrite(principal, dto.getKnowledgeBaseId(), AuditLogConstants.ACTION_OPEN_API_CREATE_DOCUMENT);
        Document parent = resolveParent(principal.tenantId(), dto.getParentId(), knowledgeBase.getId());

        Document document = new Document();
        document.setTenantId(principal.tenantId());
        document.setKnowledgeBaseId(knowledgeBase.getId());
        document.setTitle(dto.getTitle().trim());
        document.setSlug(resolveDocumentSlug(dto.getTitle(), dto.getSlug()));
        document.setDocType("DOC");
        document.setFormat(StringUtils.hasText(dto.getFormat()) ? dto.getFormat() : "MARKDOWN");
        document.setContent(dto.getContent());
        document.setContentText(dto.getContentText());
        document.setSummary(resolveSummary(dto.getSummary(), dto.getContentText(), dto.getContent()));
        document.setUserId(principal.serviceAccountCreatedByUserId());
        document.setParentId(parent == null ? 0L : parent.getId());
        document.setPath(buildPath(parent, document.getSlug()));
        document.setDepth(parent == null ? 0 : parent.getDepth() + 1);
        document.setVersionNo(1);
        document.setStatus(1);
        document.setViewCount(0);
        document.setSortOrder(resolveNextSortOrder(knowledgeBase.getId(), parent == null ? 0L : parent.getId()));
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
        String nextContentText = dto.getContentText() != null ? dto.getContentText() : document.getContentText();
        String nextSummary = dto.getSummary() != null ? dto.getSummary() : resolveSummary(document.getSummary(), nextContentText, nextContent);

        boolean versionContentChanged = !Objects.equals(document.getTitle(), nextTitle)
            || !Objects.equals(document.getFormat(), nextFormat)
            || !Objects.equals(document.getContent(), nextContent)
            || !Objects.equals(document.getContentText(), nextContentText);
        document.setTitle(nextTitle);
        document.setFormat(nextFormat);
        document.setContent(nextContent);
        document.setContentText(nextContentText);
        document.setSummary(nextSummary);
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
        return vo;
    }

    private DocumentVersionVO convertDocumentVersion(DocumentVersion version) {
        DocumentVersionVO vo = new DocumentVersionVO();
        BeanUtils.copyProperties(version, vo);
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

    private String resolveSummary(String currentSummary, String contentText, String content) {
        if (StringUtils.hasText(currentSummary)) {
            return currentSummary;
        }
        String plainText = StringUtils.hasText(contentText) ? contentText : content;
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        String normalized = plainText.replaceAll("\\s+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
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
}

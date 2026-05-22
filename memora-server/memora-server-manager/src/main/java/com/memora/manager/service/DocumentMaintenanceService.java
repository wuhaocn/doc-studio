package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.DocumentContentNormalizeDTO;
import com.memora.manager.entity.Document;
import com.memora.manager.entity.DocumentVersion;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.mapper.DocumentMapper;
import com.memora.manager.mapper.DocumentVersionMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.support.AuditLogCommand;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.DocumentContentSupport;
import com.memora.manager.support.DocumentRenderSupport;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.DocumentContentNormalizationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentMaintenanceService {
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final TenantAccessService tenantAccessService;
    private final CurrentAccessContext currentAccessContext;
    private final AuditLogService auditLogService;

    public DocumentContentNormalizationVO normalizeContent(DocumentContentNormalizeDTO dto) {
        DocumentContentNormalizeDTO request = dto == null ? new DocumentContentNormalizeDTO() : dto;
        Scope scope = resolveScope(request.getKnowledgeBaseId());
        boolean dryRun = request.getDryRun() == null || request.getDryRun();

        List<Document> scopedDocuments = listScopedDocuments(scope.knowledgeBaseId());
        Map<Long, Document> documentMap = scopedDocuments.stream()
            .collect(Collectors.toMap(Document::getId, item -> item));
        List<DocumentVersion> scopedVersions = listScopedVersions(documentMap.keySet().stream().toList());

        Counter counter = new Counter();
        for (Document document : scopedDocuments) {
            normalizeDocument(document, dryRun, counter);
        }
        for (DocumentVersion version : scopedVersions) {
            normalizeVersion(version, documentMap.get(version.getDocumentId()), dryRun, counter);
        }

        DocumentContentNormalizationVO result = new DocumentContentNormalizationVO();
        result.setKnowledgeBaseId(scope.knowledgeBaseId());
        result.setKnowledgeBaseName(scope.knowledgeBaseName());
        result.setDryRun(dryRun);
        result.setScannedDocumentCount(counter.scannedDocumentCount);
        result.setUpdatedDocumentCount(counter.updatedDocumentCount);
        result.setDocumentDocTypeUpdatedCount(counter.documentDocTypeUpdatedCount);
        result.setDocumentFormatUpdatedCount(counter.documentFormatUpdatedCount);
        result.setDocumentContentTextUpdatedCount(counter.documentContentTextUpdatedCount);
        result.setDocumentSummaryUpdatedCount(counter.documentSummaryUpdatedCount);
        result.setScannedVersionCount(counter.scannedVersionCount);
        result.setUpdatedVersionCount(counter.updatedVersionCount);
        result.setVersionFormatUpdatedCount(counter.versionFormatUpdatedCount);
        result.setVersionContentTextUpdatedCount(counter.versionContentTextUpdatedCount);
        result.setExecutedAt(LocalDateTime.now());

        recordAudit(scope, result);
        return result;
    }

    private Scope resolveScope(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            TenantMember member = tenantAccessService.requireTenantManage(currentAccessContext.getCurrentTenantId());
            return new Scope(null, null, member.getRole());
        }

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || knowledgeBase.getStatus() == null || knowledgeBase.getStatus() == 0) {
            throw new BusinessException(404, "知识库不存在");
        }
        if (!Objects.equals(knowledgeBase.getTenantId(), currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该知识库");
        }
        String actorRole = tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
        return new Scope(knowledgeBase.getId(), knowledgeBase.getName(), actorRole);
    }

    private List<Document> listScopedDocuments(Long knowledgeBaseId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getTenantId, currentAccessContext.getCurrentTenantId())
            .orderByAsc(Document::getKnowledgeBaseId)
            .orderByAsc(Document::getId);
        if (knowledgeBaseId != null) {
            queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId);
        }
        return documentMapper.selectList(queryWrapper);
    }

    private List<DocumentVersion> listScopedVersions(List<Long> documentIds) {
        if (documentIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<DocumentVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DocumentVersion::getDocumentId, documentIds)
            .orderByAsc(DocumentVersion::getDocumentId)
            .orderByAsc(DocumentVersion::getId);
        return documentVersionMapper.selectList(queryWrapper);
    }

    private void normalizeDocument(Document document, boolean dryRun, Counter counter) {
        counter.scannedDocumentCount++;
        DocumentContentSupport.NormalizedStoredDocument normalized = DocumentContentSupport.normalizeStoredDocument(
            document.getDocType(),
            document.getFormat(),
            document.getContent(),
            document.getContentText(),
            document.getSummary()
        );

        boolean docTypeChanged = !Objects.equals(document.getDocType(), normalized.docType());
        boolean formatChanged = !Objects.equals(document.getFormat(), normalized.format());
        boolean contentTextChanged = !Objects.equals(document.getContentText(), normalized.contentText());
        boolean summaryChanged = !Objects.equals(document.getSummary(), normalized.summary());
        DocumentRenderSupport.RenderedDocument renderedDocument = DocumentRenderSupport.render(
            normalized.docType(),
            normalized.format(),
            normalized.content()
        );
        boolean renderedHtmlChanged = !Objects.equals(document.getRenderedHtml(), renderedDocument.renderedHtml());
        boolean renderChecksumChanged = !Objects.equals(document.getRenderChecksum(), renderedDocument.renderChecksum());
        if (!docTypeChanged && !formatChanged && !contentTextChanged && !summaryChanged && !renderedHtmlChanged && !renderChecksumChanged) {
            return;
        }

        counter.updatedDocumentCount++;
        if (docTypeChanged) {
            counter.documentDocTypeUpdatedCount++;
        }
        if (formatChanged) {
            counter.documentFormatUpdatedCount++;
        }
        if (contentTextChanged) {
            counter.documentContentTextUpdatedCount++;
        }
        if (summaryChanged) {
            counter.documentSummaryUpdatedCount++;
        }
        if (dryRun) {
            return;
        }

        document.setDocType(normalized.docType());
        document.setFormat(normalized.format());
        document.setContentText(normalized.contentText());
        document.setSummary(normalized.summary());
        document.setRenderedHtml(renderedDocument.renderedHtml());
        document.setRenderChecksum(renderedDocument.renderChecksum());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
    }

    private void normalizeVersion(DocumentVersion version, Document document, boolean dryRun, Counter counter) {
        counter.scannedVersionCount++;
        String docType = document == null ? "DOC" : document.getDocType();
        DocumentContentSupport.NormalizedStoredDocument normalized = DocumentContentSupport.normalizeStoredDocument(
            docType,
            version.getFormat(),
            version.getContent(),
            version.getContentText(),
            null
        );

        boolean formatChanged = !Objects.equals(version.getFormat(), normalized.format());
        boolean contentTextChanged = !Objects.equals(version.getContentText(), normalized.contentText());
        if (!formatChanged && !contentTextChanged) {
            return;
        }

        counter.updatedVersionCount++;
        if (formatChanged) {
            counter.versionFormatUpdatedCount++;
        }
        if (contentTextChanged) {
            counter.versionContentTextUpdatedCount++;
        }
        if (dryRun) {
            return;
        }

        version.setFormat(normalized.format());
        version.setContentText(normalized.contentText());
        documentVersionMapper.updateById(version);
    }

    private void recordAudit(Scope scope, DocumentContentNormalizationVO result) {
        String objectTitle = scope.knowledgeBaseId() == null ? "当前工作区文档内容" : "知识库“" + scope.knowledgeBaseName() + "”文档内容";
        String detail = (Boolean.TRUE.equals(result.getDryRun()) ? "试运行" : "执行")
            + "文档内容归一化：文档扫描 " + result.getScannedDocumentCount()
            + " 条，更新 " + result.getUpdatedDocumentCount()
            + " 条；历史版本扫描 " + result.getScannedVersionCount()
            + " 条，更新 " + result.getUpdatedVersionCount()
            + " 条";
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(currentAccessContext.getCurrentTenantId())
            .knowledgeBaseId(scope.knowledgeBaseId())
            .knowledgeBaseName(scope.knowledgeBaseName())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(scope.actorRole())
            .objectType(scope.knowledgeBaseId() == null ? AuditLogConstants.OBJECT_TENANT : AuditLogConstants.OBJECT_KNOWLEDGE_BASE)
            .objectId(scope.knowledgeBaseId() == null ? currentAccessContext.getCurrentTenantId() : scope.knowledgeBaseId())
            .objectTitle(objectTitle)
            .actionType(AuditLogConstants.ACTION_NORMALIZE_DOCUMENT_CONTENT)
            .detail(detail)
            .build());
    }

    private record Scope(Long knowledgeBaseId, String knowledgeBaseName, String actorRole) {
    }

    private static final class Counter {
        private long scannedDocumentCount;
        private long updatedDocumentCount;
        private long documentDocTypeUpdatedCount;
        private long documentFormatUpdatedCount;
        private long documentContentTextUpdatedCount;
        private long documentSummaryUpdatedCount;
        private long scannedVersionCount;
        private long updatedVersionCount;
        private long versionFormatUpdatedCount;
        private long versionContentTextUpdatedCount;
    }
}

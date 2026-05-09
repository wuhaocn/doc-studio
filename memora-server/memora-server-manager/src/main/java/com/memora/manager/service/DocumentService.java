package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memora.common.exception.BusinessException;
import com.memora.common.result.Result;
import com.memora.manager.dto.DocumentBatchDeleteDTO;
import com.memora.manager.dto.DocumentBatchMoveDTO;
import com.memora.manager.dto.DocumentCreateDTO;
import com.memora.manager.dto.DocumentSortDTO;
import com.memora.manager.dto.DocumentUpdateDTO;
import com.memora.manager.entity.Document;
import com.memora.manager.entity.DocumentVersion;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.mapper.DocumentMapper;
import com.memora.manager.mapper.DocumentVersionMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.support.AuditLogCommand;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.SlugUtils;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.DocumentVO;
import com.memora.manager.vo.DocumentVersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService extends ServiceImpl<DocumentMapper, Document> {
    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentVersionMapper documentVersionMapper;
    private final CurrentAccessContext currentAccessContext;
    private final TenantAccessService tenantAccessService;
    private final AuditLogService auditLogService;

    @Transactional(rollbackFor = Exception.class)
    public DocumentVO create(DocumentCreateDTO dto) {
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(dto.getKnowledgeBaseId());
        String actorRole = tenantAccessService.requireKnowledgeBaseWriteAccess(knowledgeBase);
        Document parent = resolveParent(dto.getParentId(), knowledgeBase.getId());

        Document document = new Document();
        BeanUtils.copyProperties(dto, document);
        document.setTenantId(knowledgeBase.getTenantId());
        document.setUserId(currentAccessContext.getCurrentUserId());
        document.setDocType(StringUtils.hasText(dto.getDocType()) ? dto.getDocType() : "DOC");
        document.setFormat(StringUtils.hasText(dto.getFormat()) ? dto.getFormat() : "MARKDOWN");
        document.setSlug(resolveDocumentSlug(dto.getTitle(), dto.getSlug()));
        document.setParentId(parent == null ? 0L : parent.getId());
        document.setPath(buildPath(parent, document.getSlug()));
        document.setDepth(parent == null ? 0 : parent.getDepth() + 1);
        document.setVersionNo(1);
        document.setSummary(resolveSummary(dto.getSummary(), dto.getContentText(), dto.getContent()));
        document.setStatus(1);
        document.setViewCount(0);
        document.setSortOrder(dto.getSortOrder() == null
            ? resolveNextSortOrder(knowledgeBase.getId(), parent == null ? 0L : parent.getId(), null)
            : dto.getSortOrder());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        this.save(document);
        refreshKnowledgeBaseDocumentCount(knowledgeBase.getId());
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(knowledgeBase.getTenantId())
            .knowledgeBaseId(knowledgeBase.getId())
            .knowledgeBaseName(knowledgeBase.getName())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_DOCUMENT)
            .objectId(document.getId())
            .objectTitle(document.getTitle())
            .actionType(AuditLogConstants.ACTION_CREATE_DOCUMENT)
            .detail(buildCreateDocumentDetail(document, parent))
            .build());
        return convertToVO(document);
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentVO update(Long id, DocumentUpdateDTO dto) {
        Document document = getActiveDocument(id);
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(document.getKnowledgeBaseId());
        String actorRole = tenantAccessService.requireKnowledgeBaseWriteAccess(knowledgeBase);
        Document versionSource = new Document();
        BeanUtils.copyProperties(document, versionSource);

        Document parent = dto.getParentId() == null ? resolveParent(document.getParentId(), document.getKnowledgeBaseId()) : resolveParent(dto.getParentId(), document.getKnowledgeBaseId());
        validateParentChange(document, parent);
        validateDocTypeChange(document, dto.getDocType());
        String oldPath = document.getPath();
        String oldTitle = document.getTitle();
        String oldSlug = document.getSlug();
        String oldDocType = document.getDocType();
        String oldFormat = document.getFormat();
        String oldContent = document.getContent();
        String oldContentText = document.getContentText();
        String oldSummary = document.getSummary();
        Integer oldSortOrder = document.getSortOrder();
        Long oldParentId = document.getParentId();
        Long currentParentId = document.getParentId() == null ? 0L : document.getParentId();
        Long nextParentId = parent == null ? 0L : parent.getId();

        if (StringUtils.hasText(dto.getTitle())) {
            document.setTitle(dto.getTitle());
            if (!StringUtils.hasText(dto.getSlug())) {
                document.setSlug(resolveDocumentSlug(dto.getTitle(), null));
            }
        }
        if (StringUtils.hasText(dto.getSlug())) {
            document.setSlug(resolveDocumentSlug(document.getTitle(), dto.getSlug()));
        }
        if (dto.getDocType() != null) {
            document.setDocType(dto.getDocType());
        }
        if (dto.getFormat() != null) {
            document.setFormat(dto.getFormat());
        }
        if (dto.getContent() != null) {
            document.setContent(dto.getContent());
        }
        if (dto.getContentText() != null) {
            document.setContentText(dto.getContentText());
        }
        if (dto.getSummary() != null) {
            document.setSummary(dto.getSummary());
        } else {
            document.setSummary(resolveSummary(document.getSummary(), document.getContentText(), document.getContent()));
        }
        if (dto.getParentId() != null) {
            document.setParentId(nextParentId);
        }
        if (dto.getSortOrder() != null) {
            document.setSortOrder(dto.getSortOrder());
        } else if (dto.getParentId() != null && !nextParentId.equals(currentParentId)) {
            document.setSortOrder(resolveNextSortOrder(document.getKnowledgeBaseId(), nextParentId, document.getId()));
        }

        document.setPath(buildPath(parent, document.getSlug()));
        document.setDepth(parent == null ? 0 : parent.getDepth() + 1);

        boolean structureOrContentChanged = !Objects.equals(oldTitle, document.getTitle())
            || !Objects.equals(oldSlug, document.getSlug())
            || !Objects.equals(oldDocType, document.getDocType())
            || !Objects.equals(oldFormat, document.getFormat())
            || !Objects.equals(oldContent, document.getContent())
            || !Objects.equals(oldContentText, document.getContentText())
            || !Objects.equals(oldSummary, document.getSummary())
            || !Objects.equals(oldParentId, document.getParentId())
            || !Objects.equals(oldPath, document.getPath());
        boolean versionContentChanged = !Objects.equals(oldTitle, document.getTitle())
            || !Objects.equals(oldFormat, document.getFormat())
            || !Objects.equals(oldContent, document.getContent())
            || !Objects.equals(oldContentText, document.getContentText());
        boolean parentChanged = !Objects.equals(oldParentId, document.getParentId());
        boolean sortOrderChanged = !Objects.equals(oldSortOrder, document.getSortOrder());
        List<String> changedFields = resolveDocumentChangedFields(
            oldTitle,
            oldSlug,
            oldDocType,
            oldFormat,
            oldContent,
            oldContentText,
            oldSummary,
            oldParentId,
            document,
            oldSortOrder
        );
        if (versionContentChanged) {
            createDocumentVersion(versionSource, "自动版本快照");
            document.setVersionNo((document.getVersionNo() == null ? 1 : document.getVersionNo()) + 1);
        }
        document.setUpdatedAt(LocalDateTime.now());

        this.updateById(document);
        if (structureOrContentChanged && !oldPath.equals(document.getPath())) {
            refreshDescendantPaths(document, oldPath);
        }
        refreshKnowledgeBaseDocumentCount(document.getKnowledgeBaseId());

        if (parentChanged) {
            auditLogService.recordSuccess(AuditLogCommand.builder()
                .tenantId(knowledgeBase.getTenantId())
                .knowledgeBaseId(knowledgeBase.getId())
                .knowledgeBaseName(knowledgeBase.getName())
                .actorUserId(currentAccessContext.getCurrentUserId())
                .actorRole(actorRole)
                .objectType(AuditLogConstants.OBJECT_DOCUMENT)
                .objectId(document.getId())
                .objectTitle(document.getTitle())
                .actionType(AuditLogConstants.ACTION_MOVE_DOCUMENT)
                .detail(buildMoveDocumentDetail(oldParentId, document.getParentId()))
                .build());
        } else if (sortOrderChanged && changedFields.size() == 1 && changedFields.contains("排序")) {
            auditLogService.recordSuccess(AuditLogCommand.builder()
                .tenantId(knowledgeBase.getTenantId())
                .knowledgeBaseId(knowledgeBase.getId())
                .knowledgeBaseName(knowledgeBase.getName())
                .actorUserId(currentAccessContext.getCurrentUserId())
                .actorRole(actorRole)
                .objectType(AuditLogConstants.OBJECT_DOCUMENT)
                .objectId(document.getId())
                .objectTitle(document.getTitle())
                .actionType(AuditLogConstants.ACTION_REORDER_DOCUMENT)
                .detail("调整目录内排序到位置 #" + (document.getSortOrder() == null ? 0 : document.getSortOrder()))
                .build());
        } else if (!changedFields.isEmpty()) {
            auditLogService.recordSuccess(AuditLogCommand.builder()
                .tenantId(knowledgeBase.getTenantId())
                .knowledgeBaseId(knowledgeBase.getId())
                .knowledgeBaseName(knowledgeBase.getName())
                .actorUserId(currentAccessContext.getCurrentUserId())
                .actorRole(actorRole)
                .objectType(AuditLogConstants.OBJECT_DOCUMENT)
                .objectId(document.getId())
                .objectTitle(document.getTitle())
                .actionType(AuditLogConstants.ACTION_UPDATE_DOCUMENT)
                .detail("更新文档字段：" + String.join("、", changedFields))
                .build());
        }
        return convertToVO(document);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createDocumentVersion(Document document, String remark) {
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

    public List<DocumentVersionVO> getVersions(Long documentId) {
        Document document = getActiveDocument(documentId);
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(document.getKnowledgeBaseId());
        tenantAccessService.requireKnowledgeBaseReadAccess(knowledgeBase);
        LambdaQueryWrapper<DocumentVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentVersion::getDocumentId, documentId).orderByDesc(DocumentVersion::getVersion);
        return documentVersionMapper.selectList(queryWrapper).stream()
            .map(this::convertToVersionVO)
            .collect(Collectors.toList());
    }

    public DocumentVersion getVersionById(Long versionId) {
        DocumentVersion version = documentVersionMapper.selectById(versionId);
        if (version == null) {
            return null;
        }
        Document document = getActiveDocument(version.getDocumentId());
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(document.getKnowledgeBaseId());
        tenantAccessService.requireKnowledgeBaseReadAccess(knowledgeBase);
        return version;
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentVO rollbackToVersion(Long documentId, Long versionId) {
        Document document = getActiveDocument(documentId);
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(document.getKnowledgeBaseId());
        String actorRole = tenantAccessService.requireKnowledgeBaseWriteAccess(knowledgeBase);
        DocumentVersion version = documentVersionMapper.selectById(versionId);
        if (version == null || !version.getDocumentId().equals(documentId)) {
            throw new BusinessException(404, "版本不存在");
        }

        createDocumentVersion(document, "回滚前快照");
        document.setTitle(version.getTitle());
        document.setFormat(version.getFormat());
        document.setContent(version.getContent());
        document.setContentText(version.getContentText());
        document.setSummary(resolveSummary(document.getSummary(), version.getContentText(), version.getContent()));
        document.setVersionNo((document.getVersionNo() == null ? 1 : document.getVersionNo()) + 1);
        document.setUpdatedAt(LocalDateTime.now());
        this.updateById(document);
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(knowledgeBase.getTenantId())
            .knowledgeBaseId(knowledgeBase.getId())
            .knowledgeBaseName(knowledgeBase.getName())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_DOCUMENT)
            .objectId(document.getId())
            .objectTitle(document.getTitle())
            .actionType(AuditLogConstants.ACTION_ROLLBACK_DOCUMENT)
            .detail("回滚到历史版本 v" + version.getVersion() + "，并生成回滚前快照")
            .build());
        return convertToVO(document);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Document document = getActiveDocument(id);
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(document.getKnowledgeBaseId());
        String actorRole = tenantAccessService.requireKnowledgeBaseWriteAccess(knowledgeBase);
        LambdaQueryWrapper<Document> childrenQuery = new LambdaQueryWrapper<>();
        childrenQuery.eq(Document::getParentId, id).eq(Document::getStatus, 1);
        if (this.count(childrenQuery) > 0) {
            throw new BusinessException(400, "请先删除或移动子文档");
        }

        document.setStatus(0);
        document.setDeletedAt(LocalDateTime.now());
        document.setDeletedBy(currentAccessContext.getCurrentUserId());
        document.setUpdatedAt(LocalDateTime.now());
        this.updateById(document);
        refreshKnowledgeBaseDocumentCount(document.getKnowledgeBaseId());
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(knowledgeBase.getTenantId())
            .knowledgeBaseId(knowledgeBase.getId())
            .knowledgeBaseName(knowledgeBase.getName())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_DOCUMENT)
            .objectId(document.getId())
            .objectTitle(document.getTitle())
            .actionType(AuditLogConstants.ACTION_DELETE_DOCUMENT)
            .detail("删除" + resolveDocumentTypeLabel(document.getDocType()) + "，可在回收站恢复")
            .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public DocumentVO restore(Long id) {
        Document document = getDeletedDocument(id);
        KnowledgeBase knowledgeBase = getKnowledgeBaseEntity(document.getKnowledgeBaseId());
        if (knowledgeBase == null || knowledgeBase.getStatus() == null || knowledgeBase.getStatus() == 0) {
            throw new BusinessException(400, "所属知识库已删除，请先恢复知识库");
        }
        String actorRole = tenantAccessService.requireKnowledgeBaseWriteAccess(knowledgeBase);
        ensureParentCanBeRestored(document);

        document.setStatus(1);
        document.setDeletedAt(null);
        document.setDeletedBy(null);
        document.setUpdatedAt(LocalDateTime.now());
        this.updateById(document);
        refreshKnowledgeBaseDocumentCount(document.getKnowledgeBaseId());
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(knowledgeBase.getTenantId())
            .knowledgeBaseId(knowledgeBase.getId())
            .knowledgeBaseName(knowledgeBase.getName())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_DOCUMENT)
            .objectId(document.getId())
            .objectTitle(document.getTitle())
            .actionType(AuditLogConstants.ACTION_RESTORE_DOCUMENT)
            .detail("从回收站恢复" + resolveDocumentTypeLabel(document.getDocType()))
            .build());
        return convertToVO(document);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchMove(DocumentBatchMoveDTO dto) {
        List<Document> selectedDocuments = getActiveDocuments(dto.getDocumentIds());
        Long knowledgeBaseId = validateBatchKnowledgeBase(selectedDocuments);
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(knowledgeBaseId);
        tenantAccessService.requireKnowledgeBaseWriteAccess(knowledgeBase);
        Document targetParent = resolveParent(dto.getParentId(), knowledgeBaseId);
        List<Document> topLevelDocuments = filterTopLevelDocuments(selectedDocuments);
        Long targetParentId = targetParent == null ? 0L : targetParent.getId();

        boolean alreadyInTargetParent = topLevelDocuments.stream()
            .allMatch(item -> Objects.equals(item.getParentId() == null ? 0L : item.getParentId(), targetParentId));
        if (alreadyInTargetParent) {
            throw new BusinessException(400, "所选节点已经位于目标目录");
        }

        for (Document document : topLevelDocuments) {
            DocumentUpdateDTO updateDTO = new DocumentUpdateDTO();
            updateDTO.setParentId(targetParentId);
            update(document.getId(), updateDTO);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(DocumentBatchDeleteDTO dto) {
        List<Document> selectedDocuments = getActiveDocuments(dto.getDocumentIds());
        Long knowledgeBaseId = validateBatchKnowledgeBase(selectedDocuments);
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(knowledgeBaseId);
        tenantAccessService.requireKnowledgeBaseWriteAccess(knowledgeBase);
        validateBatchDeleteSelection(knowledgeBaseId, selectedDocuments);

        selectedDocuments.stream()
            .sorted(Comparator.comparing(item -> item.getDepth() == null ? 0 : item.getDepth(), Comparator.reverseOrder()))
            .forEach(item -> delete(item.getId()));
    }

    public DocumentVO getById(Long id) {
        Document document = getActiveDocument(id);
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(document.getKnowledgeBaseId());
        tenantAccessService.requireKnowledgeBaseReadAccess(knowledgeBase);
        return convertToVO(document);
    }

    public IPage<DocumentVO> list(Integer page, Integer size, String keyword, Long knowledgeBaseId, Long parentId, Long userId) {
        tenantAccessService.requireTenantMember(currentAccessContext.getCurrentTenantId());
        Page<Document> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getStatus, 1);
        queryWrapper.eq(Document::getTenantId, currentAccessContext.getCurrentTenantId());
        if (knowledgeBaseId != null) {
            KnowledgeBase knowledgeBase = getActiveKnowledgeBase(knowledgeBaseId);
            tenantAccessService.requireKnowledgeBaseReadAccess(knowledgeBase);
            queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId);
        } else {
            Set<Long> readableKnowledgeBaseIds = listReadableKnowledgeBaseIds();
            if (readableKnowledgeBaseIds.isEmpty()) {
                return emptyDocumentPage(page, size);
            }
            queryWrapper.in(Document::getKnowledgeBaseId, readableKnowledgeBaseIds);
        }
        if (parentId != null) {
            if (parentId != 0) {
                Document parentDocument = getActiveDocument(parentId);
                if (knowledgeBaseId == null) {
                    KnowledgeBase parentKnowledgeBase = getActiveKnowledgeBase(parentDocument.getKnowledgeBaseId());
                    tenantAccessService.requireKnowledgeBaseReadAccess(parentKnowledgeBase);
                }
            }
            queryWrapper.eq(Document::getParentId, parentId);
        }
        if (userId != null) {
            queryWrapper.eq(Document::getUserId, userId);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.eq(Document::getDocType, "DOC");
            queryWrapper.and(wrapper -> wrapper.like(Document::getTitle, keyword).or().like(Document::getContentText, keyword));
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.orderByDesc(Document::getUpdatedAt).orderByAsc(Document::getId);
        } else {
            queryWrapper.orderByAsc(Document::getSortOrder).orderByDesc(Document::getUpdatedAt);
        }

        IPage<Document> result = this.page(pageParam, queryWrapper);
        IPage<DocumentVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    public List<DocumentVO> listDeleted(Long knowledgeBaseId) {
        tenantAccessService.requireTenantMember(currentAccessContext.getCurrentTenantId());

        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getStatus, 0)
            .eq(Document::getTenantId, currentAccessContext.getCurrentTenantId());
        if (knowledgeBaseId != null) {
            KnowledgeBase knowledgeBase = getActiveKnowledgeBase(knowledgeBaseId);
            tenantAccessService.requireKnowledgeBaseWriteAccess(knowledgeBase);
            queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId);
        } else {
            Set<Long> writableKnowledgeBaseIds = listWritableKnowledgeBaseIds();
            if (writableKnowledgeBaseIds.isEmpty()) {
                return List.of();
            }
            queryWrapper.in(Document::getKnowledgeBaseId, writableKnowledgeBaseIds);
        }
        queryWrapper.orderByDesc(Document::getDeletedAt).orderByDesc(Document::getUpdatedAt);
        return this.list(queryWrapper).stream().map(this::convertToVO).collect(Collectors.toList());
    }

    public List<DocumentVO> listByKnowledgeBaseId(Long knowledgeBaseId, Long parentId) {
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(knowledgeBaseId);
        tenantAccessService.requireKnowledgeBaseReadAccess(knowledgeBase);
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getTenantId, currentAccessContext.getCurrentTenantId())
            .eq(Document::getStatus, 1);
        if (parentId != null) {
            if (parentId != 0) {
                getActiveDocument(parentId);
            }
            queryWrapper.eq(Document::getParentId, parentId);
        } else {
            queryWrapper.eq(Document::getParentId, 0L);
        }
        queryWrapper.orderByAsc(Document::getSortOrder).orderByAsc(Document::getPath);
        return this.list(queryWrapper).stream().map(this::convertToVO).collect(Collectors.toList());
    }

    public List<DocumentVO> listTreeByKnowledgeBaseId(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = getActiveKnowledgeBase(knowledgeBaseId);
        tenantAccessService.requireKnowledgeBaseReadAccess(knowledgeBase);
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getTenantId, currentAccessContext.getCurrentTenantId())
            .eq(Document::getStatus, 1)
            .orderByAsc(Document::getParentId)
            .orderByAsc(Document::getSortOrder)
            .orderByAsc(Document::getPath);
        List<Document> documents = this.list(queryWrapper);
        Map<Long, List<Document>> documentMap = documents.stream()
            .collect(Collectors.groupingBy(item -> item.getParentId() == null ? 0L : item.getParentId()));

        List<DocumentVO> result = new ArrayList<>();
        appendTree(result, documentMap, 0L);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateSortOrder(List<DocumentSortDTO> sortList) {
        for (DocumentSortDTO sortItem : sortList) {
            Document document = getActiveDocument(sortItem.getId());
            KnowledgeBase knowledgeBase = getActiveKnowledgeBase(document.getKnowledgeBaseId());
            String actorRole = tenantAccessService.requireKnowledgeBaseWriteAccess(knowledgeBase);
            document.setSortOrder(sortItem.getSortOrder());
            document.setUpdatedAt(LocalDateTime.now());
            this.updateById(document);
            auditLogService.recordSuccess(AuditLogCommand.builder()
                .tenantId(knowledgeBase.getTenantId())
                .knowledgeBaseId(knowledgeBase.getId())
                .knowledgeBaseName(knowledgeBase.getName())
                .actorUserId(currentAccessContext.getCurrentUserId())
                .actorRole(actorRole)
                .objectType(AuditLogConstants.OBJECT_DOCUMENT)
                .objectId(document.getId())
                .objectTitle(document.getTitle())
                .actionType(AuditLogConstants.ACTION_REORDER_DOCUMENT)
                .detail("调整目录内排序到位置 #" + (sortItem.getSortOrder() == null ? 0 : sortItem.getSortOrder()))
                .build());
        }
        return Result.success();
    }

    public long countActiveDocumentsByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1)
            .eq(Document::getDocType, "DOC");
        return this.count(queryWrapper);
    }

    private KnowledgeBase getActiveKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseEntity(id);
        if (knowledgeBase == null || knowledgeBase.getStatus() == null || knowledgeBase.getStatus() == 0) {
            throw new BusinessException(404, "知识库不存在");
        }
        if (!knowledgeBase.getTenantId().equals(currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该知识库");
        }
        return knowledgeBase;
    }

    private Document getActiveDocument(Long id) {
        Document document = getDocumentEntity(id);
        if (document == null || document.getStatus() == null || document.getStatus() == 0) {
            throw new BusinessException(404, "文档不存在");
        }
        if (!document.getTenantId().equals(currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该文档");
        }
        return document;
    }

    private Document resolveParent(Long parentId, Long knowledgeBaseId) {
        if (parentId == null || parentId == 0) {
            return null;
        }
        Document parent = getActiveDocument(parentId);
        if (!parent.getKnowledgeBaseId().equals(knowledgeBaseId)) {
            throw new BusinessException(400, "父级文档不属于当前知识库");
        }
        if (!"FOLDER".equals(parent.getDocType())) {
            throw new BusinessException(400, "父级节点必须是目录");
        }
        return parent;
    }

    private KnowledgeBase getKnowledgeBaseEntity(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    private Document getDocumentEntity(Long id) {
        return super.getById(id);
    }

    private Document getDeletedDocument(Long id) {
        Document document = getDocumentEntity(id);
        if (document == null || document.getStatus() == null || document.getStatus() != 0) {
            throw new BusinessException(404, "文档不在回收站中");
        }
        if (!document.getTenantId().equals(currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该文档");
        }
        return document;
    }

    private void validateParentChange(Document document, Document parent) {
        if (parent == null) {
            return;
        }
        if (document.getId().equals(parent.getId())) {
            throw new BusinessException(400, "父级节点不能是当前文档");
        }
        if ("FOLDER".equals(document.getDocType()) && StringUtils.hasText(parent.getPath())
            && parent.getPath().startsWith(document.getPath() + "/")) {
            throw new BusinessException(400, "目录不能移动到自己的子级目录下");
        }
    }

    private List<Document> getActiveDocuments(List<Long> documentIds) {
        List<Long> normalizedIds = documentIds == null
            ? Collections.emptyList()
            : documentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (normalizedIds.isEmpty()) {
            throw new BusinessException(400, "文档列表不能为空");
        }

        List<Document> documents = normalizedIds.stream()
            .map(this::getActiveDocument)
            .collect(Collectors.toList());
        if (documents.size() != normalizedIds.size()) {
            throw new BusinessException(404, "存在无效文档");
        }
        return documents;
    }

    private void ensureParentCanBeRestored(Document document) {
        Long parentId = document.getParentId();
        if (parentId == null || parentId == 0) {
            return;
        }
        Document parent = getDocumentEntity(parentId);
        if (parent == null || parent.getStatus() == null || parent.getStatus() == 0) {
            throw new BusinessException(400, "父级目录已删除，请先恢复父级目录");
        }
    }

    private Long validateBatchKnowledgeBase(List<Document> documents) {
        Set<Long> knowledgeBaseIds = documents.stream()
            .map(Document::getKnowledgeBaseId)
            .collect(Collectors.toSet());
        if (knowledgeBaseIds.size() != 1) {
            throw new BusinessException(400, "批量操作仅支持同一知识库下的节点");
        }
        return knowledgeBaseIds.iterator().next();
    }

    private List<Document> filterTopLevelDocuments(List<Document> documents) {
        Map<Long, Document> documentMap = documents.stream()
            .collect(Collectors.toMap(Document::getId, item -> item));

        return documents.stream()
            .filter(item -> {
                Long parentId = item.getParentId() == null ? 0L : item.getParentId();
                while (parentId != 0) {
                    if (documentMap.containsKey(parentId)) {
                        return false;
                    }
                    Document parent = super.getById(parentId);
                    parentId = parent == null || parent.getParentId() == null ? 0L : parent.getParentId();
                }
                return true;
            })
            .sorted(Comparator.comparing(item -> item.getDepth() == null ? 0 : item.getDepth()))
            .toList();
    }

    private void validateBatchDeleteSelection(Long knowledgeBaseId, List<Document> selectedDocuments) {
        Set<Long> selectedIdSet = selectedDocuments.stream()
            .map(Document::getId)
            .collect(Collectors.toCollection(HashSet::new));

        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1);
        List<Document> knowledgeBaseDocuments = this.list(queryWrapper);

        for (Document document : selectedDocuments) {
            if (!"FOLDER".equals(document.getDocType())) {
                continue;
            }

            boolean hasUnselectedDescendant = knowledgeBaseDocuments.stream()
                .anyMatch(candidate -> !candidate.getId().equals(document.getId())
                    && StringUtils.hasText(candidate.getPath())
                    && candidate.getPath().startsWith(document.getPath() + "/")
                    && !selectedIdSet.contains(candidate.getId()));
            if (hasUnselectedDescendant) {
                throw new BusinessException(400, "目录“" + document.getTitle() + "”仍有未选中的子节点，不能批量删除");
            }
        }
    }

    private void validateDocTypeChange(Document document, String nextDocType) {
        if (!StringUtils.hasText(nextDocType) || nextDocType.equals(document.getDocType()) || !"DOC".equals(nextDocType)) {
            return;
        }
        LambdaQueryWrapper<Document> childrenQuery = new LambdaQueryWrapper<>();
        childrenQuery.eq(Document::getParentId, document.getId()).eq(Document::getStatus, 1);
        if (this.count(childrenQuery) > 0) {
            throw new BusinessException(400, "目录存在子节点，不能直接改为文档");
        }
    }

    private String resolveDocumentSlug(String title, String slug) {
        return StringUtils.hasText(slug) ? SlugUtils.toSlug(slug) : SlugUtils.toSlug(title);
    }

    private String buildPath(Document parent, String slug) {
        return parent == null ? "/" + slug : parent.getPath() + "/" + slug;
    }

    private String buildCreateDocumentDetail(Document document, Document parent) {
        return "创建" + resolveDocumentTypeLabel(document.getDocType()) + "到" + resolveParentTitle(parent);
    }

    private String buildMoveDocumentDetail(Long oldParentId, Long nextParentId) {
        return "从" + resolveParentTitle(oldParentId) + "移动到" + resolveParentTitle(nextParentId);
    }

    private String resolveParentTitle(Document parent) {
        if (parent == null) {
            return "根目录";
        }
        return "目录“" + parent.getTitle() + "”";
    }

    private String resolveParentTitle(Long parentId) {
        if (parentId == null || parentId == 0) {
            return "根目录";
        }
        Document parent = getDocumentEntity(parentId);
        if (parent == null || !StringUtils.hasText(parent.getTitle())) {
            return "目录#" + parentId;
        }
        return "目录“" + parent.getTitle() + "”";
    }

    private String resolveDocumentTypeLabel(String docType) {
        return "FOLDER".equals(docType) ? "目录" : "文档";
    }

    private List<String> resolveDocumentChangedFields(
        String oldTitle,
        String oldSlug,
        String oldDocType,
        String oldFormat,
        String oldContent,
        String oldContentText,
        String oldSummary,
        Long oldParentId,
        Document document,
        Integer oldSortOrder) {
        List<String> changedFields = new ArrayList<>();
        if (!Objects.equals(oldTitle, document.getTitle())) {
            changedFields.add("标题");
        }
        if (!Objects.equals(oldSlug, document.getSlug())) {
            changedFields.add("标识");
        }
        if (!Objects.equals(oldDocType, document.getDocType())) {
            changedFields.add("类型");
        }
        if (!Objects.equals(oldFormat, document.getFormat())) {
            changedFields.add("格式");
        }
        if (!Objects.equals(oldContent, document.getContent()) || !Objects.equals(oldContentText, document.getContentText())) {
            changedFields.add("正文");
        }
        if (!Objects.equals(oldSummary, document.getSummary())) {
            changedFields.add("摘要");
        }
        if (!Objects.equals(oldParentId, document.getParentId())) {
            changedFields.add("父级目录");
        }
        if (!Objects.equals(oldSortOrder, document.getSortOrder())) {
            changedFields.add("排序");
        }
        return changedFields;
    }

    private String resolveSummary(String summary, String contentText, String content) {
        if (StringUtils.hasText(summary)) {
            return summary.length() > 500 ? summary.substring(0, 500) : summary;
        }
        String fallback = StringUtils.hasText(contentText) ? contentText : content;
        if (!StringUtils.hasText(fallback)) {
            return null;
        }
        return fallback.length() > 180 ? fallback.substring(0, 180) : fallback;
    }

    private void refreshDescendantPaths(Document document, String oldPath) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, document.getKnowledgeBaseId())
            .eq(Document::getStatus, 1)
            .likeRight(Document::getPath, oldPath + "/");
        List<Document> descendants = this.list(queryWrapper);
        for (Document descendant : descendants) {
            String relativePath = descendant.getPath().substring(oldPath.length());
            descendant.setPath(document.getPath() + relativePath);
            descendant.setDepth(calculateDepth(descendant.getPath()));
            descendant.setUpdatedAt(LocalDateTime.now());
            this.updateById(descendant);
        }
    }

    private int resolveNextSortOrder(Long knowledgeBaseId, Long parentId, Long excludeId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId)
            .eq(Document::getStatus, 1)
            .eq(Document::getParentId, parentId == null ? 0L : parentId);
        if (excludeId != null) {
            queryWrapper.ne(Document::getId, excludeId);
        }
        List<Document> siblings = this.list(queryWrapper);
        return siblings.stream()
            .map(Document::getSortOrder)
            .filter(sortOrder -> sortOrder != null)
            .max(Integer::compareTo)
            .orElse(-1) + 1;
    }

    private int calculateDepth(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            return 0;
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (!StringUtils.hasText(normalized)) {
            return 0;
        }
        return normalized.split("/").length - 1;
    }

    private void appendTree(List<DocumentVO> result, Map<Long, List<Document>> documentMap, Long parentId) {
        List<Document> children = documentMap.getOrDefault(parentId, Collections.emptyList());
        for (Document child : children) {
            result.add(convertToVO(child));
            appendTree(result, documentMap, child.getId());
        }
    }

    private void refreshKnowledgeBaseDocumentCount(Long knowledgeBaseId) {
        knowledgeBaseMapper.updateDocumentCount(knowledgeBaseId, countActiveDocumentsByKnowledgeBaseId(knowledgeBaseId));
    }

    private Set<Long> listReadableKnowledgeBaseIds() {
        return tenantAccessService.filterReadableKnowledgeBases(listTenantKnowledgeBasesByStatus(1)).stream()
            .map(KnowledgeBase::getId)
            .collect(Collectors.toSet());
    }

    private Set<Long> listWritableKnowledgeBaseIds() {
        return tenantAccessService.filterWritableKnowledgeBases(listTenantKnowledgeBasesByStatus(1)).stream()
            .map(KnowledgeBase::getId)
            .collect(Collectors.toSet());
    }

    private List<KnowledgeBase> listTenantKnowledgeBasesByStatus(Integer status) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getTenantId, currentAccessContext.getCurrentTenantId())
            .eq(KnowledgeBase::getStatus, status);
        return knowledgeBaseMapper.selectList(queryWrapper);
    }

    private IPage<DocumentVO> emptyDocumentPage(Integer page, Integer size) {
        Page<DocumentVO> voPage = new Page<>(page, size, 0);
        voPage.setRecords(List.of());
        return voPage;
    }

    private DocumentVO convertToVO(Document document) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(document, vo);
        return vo;
    }

    private DocumentVersionVO convertToVersionVO(DocumentVersion version) {
        DocumentVersionVO vo = new DocumentVersionVO();
        BeanUtils.copyProperties(version, vo);
        return vo;
    }
}

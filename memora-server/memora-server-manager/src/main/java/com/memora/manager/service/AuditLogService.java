package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memora.common.exception.BusinessException;
import com.memora.manager.entity.AuditLog;
import com.memora.manager.entity.AuditLogArchive;
import com.memora.manager.entity.Document;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.entity.UserAccount;
import com.memora.manager.mapper.AuditLogArchiveMapper;
import com.memora.manager.mapper.AuditLogMapper;
import com.memora.manager.mapper.DocumentMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.mapper.TenantMemberMapper;
import com.memora.manager.mapper.UserAccountMapper;
import com.memora.manager.support.AuditLogCommand;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.AuditLogVO;
import com.memora.manager.vo.AuditRetentionExecutionVO;
import com.memora.manager.vo.AuditRetentionSummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private static final String CLIENT_HEADER = "X-Memora-Client";
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int DETAIL_MAX_LENGTH = 500;
    private static final int SOURCE_MAX_LENGTH = 60;
    private static final int METHOD_MAX_LENGTH = 20;
    private static final int PATH_MAX_LENGTH = 255;

    @Value("${memora.audit.retention-days:3650}")
    private Integer retentionDays;

    @Value("${memora.audit.archive-batch-size:200}")
    private Integer archiveBatchSize;

    @Value("${memora.audit.export-max-size:1000}")
    private Integer exportMaxSize;

    private final AuditLogMapper auditLogMapper;
    private final AuditLogArchiveMapper auditLogArchiveMapper;
    private final TenantMemberMapper tenantMemberMapper;
    private final UserAccountMapper userAccountMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final CurrentAccessContext currentAccessContext;
    private final TenantAccessService tenantAccessService;

    public void recordSuccess(AuditLogCommand command) {
        record(command, AuditLogConstants.RESULT_SUCCESS);
    }

    public void recordFailure(AuditLogCommand command) {
        record(command, AuditLogConstants.RESULT_FAILURE);
    }

    public void recordFailureSafely(AuditLogCommand command) {
        try {
            recordFailure(command);
        } catch (Exception ex) {
            // failure audit must not mask the original business error
        }
    }

    public IPage<AuditLogVO> list(
        Integer page,
        Integer size,
        Long knowledgeBaseId,
        String objectType,
        Long objectId,
        String resultType) {
        AuditQuery auditQuery = prepareAuditQuery(knowledgeBaseId, objectType, objectId, resultType);
        Page<AuditLog> pageParam = new Page<>(page, size);
        IPage<AuditLog> result = auditLogMapper.selectPage(pageParam, auditQuery.queryWrapper());
        Page<AuditLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).toList());
        return voPage;
    }

    public AuditRetentionSummaryVO getRetentionSummary(Long knowledgeBaseId, String objectType, Long objectId) {
        AuditQuery auditQuery = prepareAuditQuery(knowledgeBaseId, objectType, objectId, null);
        LocalDateTime archiveBeforeCreatedAt = resolveArchiveBeforeCreatedAt();

        long activeCount = countActive(auditQuery, null);
        long archivedCount = countArchived(auditQuery, null);

        AuditRetentionSummaryVO summary = new AuditRetentionSummaryVO();
        summary.setConfiguredRetentionDays(resolveRetentionDaysValue());
        summary.setArchiveBatchSize(resolveArchiveBatchSize());
        summary.setExportMaxSize(resolveExportMaxSize());
        summary.setActiveCount(activeCount);
        summary.setArchivedCount(archivedCount);
        summary.setTotalCount(activeCount + archivedCount);
        summary.setSuccessCount(countActive(auditQuery, AuditLogConstants.RESULT_SUCCESS)
            + countArchived(auditQuery, AuditLogConstants.RESULT_SUCCESS));
        summary.setFailureCount(countActive(auditQuery, AuditLogConstants.RESULT_FAILURE)
            + countArchived(auditQuery, AuditLogConstants.RESULT_FAILURE));
        summary.setPendingArchiveCount(countPendingArchive(auditQuery, archiveBeforeCreatedAt));
        summary.setEarliestCreatedAt(minTime(
            getActiveBoundaryCreatedAt(auditQuery, true),
            getArchivedBoundaryCreatedAt(auditQuery, true)
        ));
        summary.setLatestCreatedAt(maxTime(
            getActiveBoundaryCreatedAt(auditQuery, false),
            getArchivedBoundaryCreatedAt(auditQuery, false)
        ));
        summary.setArchiveBeforeCreatedAt(archiveBeforeCreatedAt);
        summary.setLastArchivedAt(getArchivedBoundaryArchivedAt(auditQuery, false));
        return summary;
    }

    @Transactional
    public AuditRetentionExecutionVO runRetention(Long knowledgeBaseId, String objectType, Long objectId) {
        AuditQuery auditQuery = prepareAuditQuery(knowledgeBaseId, objectType, objectId, null);
        LocalDateTime archiveBeforeCreatedAt = resolveArchiveBeforeCreatedAt();
        long eligibleCount = countPendingArchive(auditQuery, archiveBeforeCreatedAt);
        int batchSize = resolveArchiveBatchSize();
        List<AuditLog> pendingRecords = loadPendingArchiveRecords(auditQuery, archiveBeforeCreatedAt, batchSize);
        LocalDateTime executedAt = LocalDateTime.now();

        if (!pendingRecords.isEmpty()) {
            for (AuditLog auditLog : pendingRecords) {
                auditLogArchiveMapper.insert(copyToArchive(auditLog, executedAt));
            }
            auditLogMapper.deleteBatchIds(pendingRecords.stream().map(AuditLog::getId).toList());
        }

        recordSuccess(buildRetentionAuditCommand(
            auditQuery,
            archiveBeforeCreatedAt,
            eligibleCount,
            pendingRecords.size()
        ));

        AuditRetentionExecutionVO execution = new AuditRetentionExecutionVO();
        execution.setConfiguredRetentionDays(resolveRetentionDaysValue());
        execution.setArchiveBatchSize(batchSize);
        execution.setEligibleCount(eligibleCount);
        execution.setArchivedCount((long) pendingRecords.size());
        execution.setRemainingPendingArchiveCount(countPendingArchive(auditQuery, archiveBeforeCreatedAt));
        execution.setActiveCount(countActive(auditQuery, null));
        execution.setArchivedTotalCount(countArchived(auditQuery, null));
        execution.setArchiveBeforeCreatedAt(archiveBeforeCreatedAt);
        execution.setExecutedAt(executedAt);
        return execution;
    }

    public String exportCsv(Long knowledgeBaseId, String objectType, Long objectId, String resultType, String storageScope) {
        AuditQuery auditQuery = prepareAuditQuery(knowledgeBaseId, objectType, objectId, resultType);
        String normalizedStorageScope = normalizeStorageScope(storageScope);
        List<AuditCsvRow> rows = loadExportRows(auditQuery, normalizedStorageScope);
        recordSuccess(buildExportAuditCommand(auditQuery, resultType, normalizedStorageScope, rows.size()));
        String header = "\uFEFFid,tenantId,knowledgeBaseId,knowledgeBaseName,actorType,actorUserId,actorDisplayName,actorRole,objectType,objectId,objectTitle,actionType,resultType,detail,sourceType,requestMethod,requestPath,createdAt\n";
        return header + rows.stream()
            .map(this::toCsvRow)
            .collect(Collectors.joining("\n"));
    }

    private void record(AuditLogCommand command, String resultType) {
        validateCommand(command, resultType);
        ActorSnapshot actorSnapshot = resolveActorSnapshot(
            command.getTenantId(),
            command.getActorUserId(),
            command.getActorDisplayName(),
            command.getActorRole()
        );
        RequestSnapshot requestSnapshot = resolveRequestSnapshot(command);

        AuditLog auditLog = new AuditLog();
        auditLog.setTenantId(command.getTenantId());
        auditLog.setKnowledgeBaseId(command.getKnowledgeBaseId());
        auditLog.setKnowledgeBaseName(truncate(command.getKnowledgeBaseName(), 120));
        auditLog.setActorType(StringUtils.hasText(command.getActorType()) ? command.getActorType() : AuditLogConstants.ACTOR_USER);
        auditLog.setActorUserId(actorSnapshot.actorUserId());
        auditLog.setActorDisplayName(truncate(actorSnapshot.actorDisplayName(), 120));
        auditLog.setActorRole(truncate(actorSnapshot.actorRole(), 40));
        auditLog.setObjectType(command.getObjectType());
        auditLog.setObjectId(command.getObjectId());
        auditLog.setObjectTitle(truncate(command.getObjectTitle(), TITLE_MAX_LENGTH));
        auditLog.setActionType(command.getActionType());
        auditLog.setResultType(resultType);
        auditLog.setDetail(truncate(command.getDetail(), DETAIL_MAX_LENGTH));
        auditLog.setSourceType(truncate(requestSnapshot.sourceType(), SOURCE_MAX_LENGTH));
        auditLog.setRequestMethod(truncate(requestSnapshot.requestMethod(), METHOD_MAX_LENGTH));
        auditLog.setRequestPath(truncate(requestSnapshot.requestPath(), PATH_MAX_LENGTH));
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(auditLog);
    }

    private void validateCommand(AuditLogCommand command, String resultType) {
        if (command == null || command.getTenantId() == null) {
            throw new IllegalArgumentException("audit tenantId is required");
        }
        if (!StringUtils.hasText(command.getObjectType()) || !StringUtils.hasText(command.getActionType())) {
            throw new IllegalArgumentException("audit objectType and actionType are required");
        }
        if (!AuditLogConstants.RESULT_SUCCESS.equals(resultType) && !AuditLogConstants.RESULT_FAILURE.equals(resultType)) {
            throw new IllegalArgumentException("unsupported audit resultType");
        }
    }

    private AuditQuery prepareAuditQuery(Long knowledgeBaseId, String objectType, Long objectId, String resultType) {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        String normalizedObjectType = normalizeObjectType(objectType, objectId);
        String normalizedResultType = normalizeResultType(resultType);

        authorizeAuditQuery(tenantId, knowledgeBaseId, normalizedObjectType, objectId);

        LambdaQueryWrapper<AuditLog> queryWrapper = buildActiveAuditQuery(
            tenantId,
            knowledgeBaseId,
            normalizedObjectType,
            objectId,
            normalizedResultType
        );
        queryWrapper.orderByDesc(AuditLog::getCreatedAt).orderByDesc(AuditLog::getId);
        return new AuditQuery(tenantId, knowledgeBaseId, normalizedObjectType, objectId, normalizedResultType, queryWrapper);
    }

    private void authorizeAuditQuery(Long tenantId, Long knowledgeBaseId, String normalizedObjectType, Long objectId) {
        if (knowledgeBaseId != null) {
            KnowledgeBase knowledgeBase = requireKnowledgeBaseInCurrentTenant(knowledgeBaseId);
            tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
            return;
        }
        if (AuditLogConstants.OBJECT_DOCUMENT.equals(normalizedObjectType) && objectId != null) {
            Document document = requireDocumentInCurrentTenant(objectId);
            KnowledgeBase knowledgeBase = requireKnowledgeBaseInCurrentTenant(document.getKnowledgeBaseId());
            tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
            return;
        }
        if (AuditLogConstants.OBJECT_KNOWLEDGE_BASE.equals(normalizedObjectType) && objectId != null) {
            KnowledgeBase knowledgeBase = requireKnowledgeBaseInCurrentTenant(objectId);
            tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
            return;
        }
        tenantAccessService.requireTenantManage(tenantId);
    }

    private LambdaQueryWrapper<AuditLog> buildActiveAuditQuery(
        Long tenantId,
        Long knowledgeBaseId,
        String normalizedObjectType,
        Long objectId,
        String normalizedResultType) {
        LambdaQueryWrapper<AuditLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AuditLog::getTenantId, tenantId);
        if (knowledgeBaseId != null) {
            queryWrapper.eq(AuditLog::getKnowledgeBaseId, knowledgeBaseId);
        }
        if (StringUtils.hasText(normalizedObjectType)) {
            queryWrapper.eq(AuditLog::getObjectType, normalizedObjectType)
                .eq(AuditLog::getObjectId, objectId);
        }
        if (StringUtils.hasText(normalizedResultType)) {
            queryWrapper.eq(AuditLog::getResultType, normalizedResultType);
        }
        return queryWrapper;
    }

    private LambdaQueryWrapper<AuditLogArchive> buildArchivedAuditQuery(AuditQuery auditQuery, String resultType) {
        LambdaQueryWrapper<AuditLogArchive> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AuditLogArchive::getTenantId, auditQuery.tenantId());
        if (auditQuery.knowledgeBaseId() != null) {
            queryWrapper.eq(AuditLogArchive::getKnowledgeBaseId, auditQuery.knowledgeBaseId());
        }
        if (StringUtils.hasText(auditQuery.normalizedObjectType())) {
            queryWrapper.eq(AuditLogArchive::getObjectType, auditQuery.normalizedObjectType())
                .eq(AuditLogArchive::getObjectId, auditQuery.objectId());
        }
        String normalizedResultType = StringUtils.hasText(resultType) ? resultType : auditQuery.normalizedResultType();
        if (StringUtils.hasText(normalizedResultType)) {
            queryWrapper.eq(AuditLogArchive::getResultType, normalizedResultType);
        }
        return queryWrapper;
    }

    private String normalizeObjectType(String objectType, Long objectId) {
        if (objectId != null && !StringUtils.hasText(objectType)) {
            throw new BusinessException(400, "按对象查询时必须提供 objectType");
        }
        if (!StringUtils.hasText(objectType)) {
            return null;
        }

        String normalized = objectType.trim().toUpperCase(Locale.ROOT);
        if (!AuditLogConstants.QUERYABLE_OBJECT_TYPES.contains(normalized)) {
            throw new BusinessException(400, "当前 objectType 不支持审计查询");
        }
        if (objectId == null) {
            throw new BusinessException(400, "按对象查询时必须提供 objectId");
        }
        return normalized;
    }

    private String normalizeResultType(String resultType) {
        if (!StringUtils.hasText(resultType)) {
            return null;
        }
        String normalized = resultType.trim().toUpperCase(Locale.ROOT);
        if (!AuditLogConstants.RESULT_SUCCESS.equals(normalized) && !AuditLogConstants.RESULT_FAILURE.equals(normalized)) {
            throw new BusinessException(400, "当前 resultType 不支持审计查询");
        }
        return normalized;
    }

    private String normalizeStorageScope(String storageScope) {
        if (!StringUtils.hasText(storageScope)) {
            return AuditLogConstants.STORAGE_SCOPE_ACTIVE;
        }
        String normalized = storageScope.trim().toUpperCase(Locale.ROOT);
        if (!AuditLogConstants.STORAGE_SCOPE_ACTIVE.equals(normalized)
            && !AuditLogConstants.STORAGE_SCOPE_ARCHIVED.equals(normalized)
            && !AuditLogConstants.STORAGE_SCOPE_ALL.equals(normalized)) {
            throw new BusinessException(400, "当前 storageScope 不支持审计导出");
        }
        return normalized;
    }

    private KnowledgeBase requireKnowledgeBaseInCurrentTenant(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new BusinessException(404, "知识库不存在");
        }
        if (!Objects.equals(knowledgeBase.getTenantId(), currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该知识库");
        }
        return knowledgeBase;
    }

    private Document requireDocumentInCurrentTenant(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(404, "文档不存在");
        }
        if (!Objects.equals(document.getTenantId(), currentAccessContext.getCurrentTenantId())) {
            throw new BusinessException(403, "无权访问该文档");
        }
        return document;
    }

    private ActorSnapshot resolveActorSnapshot(Long tenantId, Long actorUserId, String actorDisplayName, String actorRole) {
        Long resolvedActorUserId = actorUserId != null ? actorUserId : currentAccessContext.getCurrentUserIdOrNull();
        String resolvedDisplayName = StringUtils.hasText(actorDisplayName) ? actorDisplayName.trim() : null;
        String resolvedRole = StringUtils.hasText(actorRole) ? actorRole.trim() : null;

        if (resolvedActorUserId == null) {
            return new ActorSnapshot(null, resolvedDisplayName, resolvedRole);
        }

        LambdaQueryWrapper<TenantMember> memberQuery = new LambdaQueryWrapper<>();
        memberQuery.eq(TenantMember::getTenantId, tenantId)
            .eq(TenantMember::getUserId, resolvedActorUserId)
            .eq(TenantMember::getStatus, 1)
            .last("LIMIT 1");
        TenantMember member = tenantMemberMapper.selectOne(memberQuery);
        if (member != null) {
            return new ActorSnapshot(
                resolvedActorUserId,
                resolvedDisplayName != null ? resolvedDisplayName : member.getDisplayName(),
                resolvedRole != null ? resolvedRole : member.getRole()
            );
        }

        UserAccount user = userAccountMapper.selectById(resolvedActorUserId);
        return new ActorSnapshot(
            resolvedActorUserId,
            resolvedDisplayName != null
                ? resolvedDisplayName
                : user != null && StringUtils.hasText(user.getDisplayName())
                    ? user.getDisplayName()
                    : "用户#" + resolvedActorUserId,
            resolvedRole
        );
    }

    private RequestSnapshot resolveRequestSnapshot(AuditLogCommand command) {
        if (StringUtils.hasText(command.getSourceType())
            || StringUtils.hasText(command.getRequestMethod())
            || StringUtils.hasText(command.getRequestPath())) {
            return new RequestSnapshot(
                normalizeSourceType(command.getSourceType()),
                command.getRequestMethod(),
                command.getRequestPath()
            );
        }
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return new RequestSnapshot(AuditLogConstants.SOURCE_DIRECT_API, null, null);
        }

        HttpServletRequest request = attributes.getRequest();
        String clientHeader = request.getHeader(CLIENT_HEADER);
        return new RequestSnapshot(
            normalizeSourceType(clientHeader),
            request.getMethod(),
            request.getRequestURI()
        );
    }

    private String normalizeSourceType(String sourceType) {
        if (!StringUtils.hasText(sourceType)) {
            return AuditLogConstants.SOURCE_DIRECT_API;
        }
        return sourceType.trim()
            .replaceAll("[^A-Za-z0-9]+", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "")
            .toUpperCase(Locale.ROOT);
    }

    private int resolveRetentionDaysValue() {
        return retentionDays == null ? 3650 : Math.max(retentionDays, 0);
    }

    private int resolveArchiveBatchSize() {
        return archiveBatchSize == null ? 200 : Math.max(archiveBatchSize, 1);
    }

    private int resolveExportMaxSize() {
        return exportMaxSize == null ? 1000 : Math.max(exportMaxSize, 1);
    }

    private LocalDateTime resolveArchiveBeforeCreatedAt() {
        return LocalDateTime.now().minusDays(resolveRetentionDaysValue());
    }

    private long countActive(AuditQuery auditQuery, String resultType) {
        return auditLogMapper.selectCount(buildActiveAuditQuery(
            auditQuery.tenantId(),
            auditQuery.knowledgeBaseId(),
            auditQuery.normalizedObjectType(),
            auditQuery.objectId(),
            StringUtils.hasText(resultType) ? resultType : auditQuery.normalizedResultType()
        ));
    }

    private long countArchived(AuditQuery auditQuery, String resultType) {
        return auditLogArchiveMapper.selectCount(buildArchivedAuditQuery(auditQuery, resultType));
    }

    private long countPendingArchive(AuditQuery auditQuery, LocalDateTime archiveBeforeCreatedAt) {
        return auditLogMapper.selectCount(buildActiveAuditQuery(
            auditQuery.tenantId(),
            auditQuery.knowledgeBaseId(),
            auditQuery.normalizedObjectType(),
            auditQuery.objectId(),
            null
        ).lt(AuditLog::getCreatedAt, archiveBeforeCreatedAt));
    }

    private List<AuditLog> loadPendingArchiveRecords(AuditQuery auditQuery, LocalDateTime archiveBeforeCreatedAt, int limit) {
        LambdaQueryWrapper<AuditLog> queryWrapper = buildActiveAuditQuery(
            auditQuery.tenantId(),
            auditQuery.knowledgeBaseId(),
            auditQuery.normalizedObjectType(),
            auditQuery.objectId(),
            null
        );
        queryWrapper.lt(AuditLog::getCreatedAt, archiveBeforeCreatedAt)
            .orderByAsc(AuditLog::getCreatedAt)
            .orderByAsc(AuditLog::getId)
            .last("LIMIT " + limit);
        return auditLogMapper.selectList(queryWrapper);
    }

    private LocalDateTime getActiveBoundaryCreatedAt(AuditQuery auditQuery, boolean earliest) {
        LambdaQueryWrapper<AuditLog> queryWrapper = buildActiveAuditQuery(
            auditQuery.tenantId(),
            auditQuery.knowledgeBaseId(),
            auditQuery.normalizedObjectType(),
            auditQuery.objectId(),
            null
        );
        if (earliest) {
            queryWrapper.orderByAsc(AuditLog::getCreatedAt).orderByAsc(AuditLog::getId);
        } else {
            queryWrapper.orderByDesc(AuditLog::getCreatedAt).orderByDesc(AuditLog::getId);
        }
        queryWrapper.last("LIMIT 1");
        AuditLog auditLog = auditLogMapper.selectOne(queryWrapper);
        return auditLog == null ? null : auditLog.getCreatedAt();
    }

    private LocalDateTime getArchivedBoundaryCreatedAt(AuditQuery auditQuery, boolean earliest) {
        LambdaQueryWrapper<AuditLogArchive> queryWrapper = buildArchivedAuditQuery(auditQuery, null);
        if (earliest) {
            queryWrapper.orderByAsc(AuditLogArchive::getCreatedAt).orderByAsc(AuditLogArchive::getId);
        } else {
            queryWrapper.orderByDesc(AuditLogArchive::getCreatedAt).orderByDesc(AuditLogArchive::getId);
        }
        queryWrapper.last("LIMIT 1");
        AuditLogArchive auditLogArchive = auditLogArchiveMapper.selectOne(queryWrapper);
        return auditLogArchive == null ? null : auditLogArchive.getCreatedAt();
    }

    private LocalDateTime getArchivedBoundaryArchivedAt(AuditQuery auditQuery, boolean earliest) {
        LambdaQueryWrapper<AuditLogArchive> queryWrapper = buildArchivedAuditQuery(auditQuery, null);
        if (earliest) {
            queryWrapper.orderByAsc(AuditLogArchive::getArchivedAt).orderByAsc(AuditLogArchive::getId);
        } else {
            queryWrapper.orderByDesc(AuditLogArchive::getArchivedAt).orderByDesc(AuditLogArchive::getId);
        }
        queryWrapper.last("LIMIT 1");
        AuditLogArchive auditLogArchive = auditLogArchiveMapper.selectOne(queryWrapper);
        return auditLogArchive == null ? null : auditLogArchive.getArchivedAt();
    }

    private List<AuditCsvRow> loadExportRows(AuditQuery auditQuery, String storageScope) {
        int limit = resolveExportMaxSize();
        List<AuditCsvRow> rows = new ArrayList<>();

        if (AuditLogConstants.STORAGE_SCOPE_ACTIVE.equals(storageScope)
            || AuditLogConstants.STORAGE_SCOPE_ALL.equals(storageScope)) {
            LambdaQueryWrapper<AuditLog> activeQuery = buildActiveAuditQuery(
                auditQuery.tenantId(),
                auditQuery.knowledgeBaseId(),
                auditQuery.normalizedObjectType(),
                auditQuery.objectId(),
                auditQuery.normalizedResultType()
            );
            activeQuery.orderByDesc(AuditLog::getCreatedAt)
                .orderByDesc(AuditLog::getId)
                .last("LIMIT " + limit);
            rows.addAll(auditLogMapper.selectList(activeQuery).stream().map(AuditCsvRow::fromActive).toList());
        }

        if (AuditLogConstants.STORAGE_SCOPE_ARCHIVED.equals(storageScope)
            || AuditLogConstants.STORAGE_SCOPE_ALL.equals(storageScope)) {
            LambdaQueryWrapper<AuditLogArchive> archivedQuery = buildArchivedAuditQuery(auditQuery, null);
            archivedQuery.orderByDesc(AuditLogArchive::getCreatedAt)
                .orderByDesc(AuditLogArchive::getId)
                .last("LIMIT " + limit);
            rows.addAll(auditLogArchiveMapper.selectList(archivedQuery).stream().map(AuditCsvRow::fromArchived).toList());
        }

        rows.sort(Comparator
            .comparing(AuditCsvRow::createdAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(AuditCsvRow::id, Comparator.nullsLast(Comparator.reverseOrder())));

        if (rows.size() > limit) {
            return new ArrayList<>(rows.subList(0, limit));
        }
        return rows;
    }

    private AuditLogArchive copyToArchive(AuditLog auditLog, LocalDateTime archivedAt) {
        AuditLogArchive archive = new AuditLogArchive();
        BeanUtils.copyProperties(auditLog, archive);
        archive.setArchivedAt(archivedAt);
        return archive;
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private AuditLogVO convertToVO(AuditLog auditLog) {
        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(auditLog, vo);
        return vo;
    }

    private String toCsvRow(AuditCsvRow auditLog) {
        return String.join(",",
            csvValue(auditLog.id()),
            csvValue(auditLog.tenantId()),
            csvValue(auditLog.knowledgeBaseId()),
            csvValue(auditLog.knowledgeBaseName()),
            csvValue(auditLog.actorType()),
            csvValue(auditLog.actorUserId()),
            csvValue(auditLog.actorDisplayName()),
            csvValue(auditLog.actorRole()),
            csvValue(auditLog.objectType()),
            csvValue(auditLog.objectId()),
            csvValue(auditLog.objectTitle()),
            csvValue(auditLog.actionType()),
            csvValue(auditLog.resultType()),
            csvValue(auditLog.detail()),
            csvValue(auditLog.sourceType()),
            csvValue(auditLog.requestMethod()),
            csvValue(auditLog.requestPath()),
            csvValue(auditLog.createdAt())
        );
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String text = String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private AuditLogCommand buildExportAuditCommand(
        AuditQuery auditQuery,
        String resultType,
        String storageScope,
        int exportedCount) {
        AuditTarget auditTarget = resolveAuditTarget(auditQuery);
        String detail = "导出 " + storageScope + " 审计 CSV " + exportedCount + " 条"
            + (StringUtils.hasText(resultType) ? "，结果过滤 " + resultType.trim().toUpperCase(Locale.ROOT) : "");
        return AuditLogCommand.builder()
            .tenantId(auditQuery.tenantId())
            .knowledgeBaseId(auditTarget.knowledgeBaseId())
            .knowledgeBaseName(auditTarget.knowledgeBaseName())
            .objectType(auditTarget.objectType())
            .objectId(auditTarget.objectId())
            .objectTitle(auditTarget.objectTitle())
            .actionType(AuditLogConstants.ACTION_EXPORT_AUDIT_LOG)
            .detail(detail)
            .build();
    }

    private AuditLogCommand buildRetentionAuditCommand(
        AuditQuery auditQuery,
        LocalDateTime archiveBeforeCreatedAt,
        long eligibleCount,
        int archivedCount) {
        AuditTarget auditTarget = resolveAuditTarget(auditQuery);
        String detail = "执行审计归档，归档 " + archivedCount + "/" + eligibleCount
            + " 条过期记录，保留阈值 " + resolveRetentionDaysValue()
            + " 天，截止 " + archiveBeforeCreatedAt;
        return AuditLogCommand.builder()
            .tenantId(auditQuery.tenantId())
            .knowledgeBaseId(auditTarget.knowledgeBaseId())
            .knowledgeBaseName(auditTarget.knowledgeBaseName())
            .objectType(auditTarget.objectType())
            .objectId(auditTarget.objectId())
            .objectTitle(auditTarget.objectTitle())
            .actionType(AuditLogConstants.ACTION_APPLY_AUDIT_RETENTION)
            .detail(detail)
            .build();
    }

    private AuditTarget resolveAuditTarget(AuditQuery auditQuery) {
        if (AuditLogConstants.OBJECT_DOCUMENT.equals(auditQuery.normalizedObjectType()) && auditQuery.objectId() != null) {
            Document document = requireDocumentInCurrentTenant(auditQuery.objectId());
            KnowledgeBase knowledgeBase = requireKnowledgeBaseInCurrentTenant(document.getKnowledgeBaseId());
            return new AuditTarget(
                AuditLogConstants.OBJECT_DOCUMENT,
                document.getId(),
                document.getTitle(),
                knowledgeBase.getId(),
                knowledgeBase.getName()
            );
        }
        if (AuditLogConstants.OBJECT_KNOWLEDGE_BASE.equals(auditQuery.normalizedObjectType()) && auditQuery.objectId() != null) {
            KnowledgeBase knowledgeBase = requireKnowledgeBaseInCurrentTenant(auditQuery.objectId());
            return new AuditTarget(
                AuditLogConstants.OBJECT_KNOWLEDGE_BASE,
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                knowledgeBase.getId(),
                knowledgeBase.getName()
            );
        }
        if (auditQuery.knowledgeBaseId() != null) {
            KnowledgeBase knowledgeBase = requireKnowledgeBaseInCurrentTenant(auditQuery.knowledgeBaseId());
            return new AuditTarget(
                AuditLogConstants.OBJECT_KNOWLEDGE_BASE,
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                knowledgeBase.getId(),
                knowledgeBase.getName()
            );
        }
        if (StringUtils.hasText(auditQuery.normalizedObjectType()) && auditQuery.objectId() != null) {
            return new AuditTarget(
                auditQuery.normalizedObjectType(),
                auditQuery.objectId(),
                "对象审计治理",
                null,
                null
            );
        }
        return new AuditTarget(
            AuditLogConstants.OBJECT_TENANT,
            auditQuery.tenantId(),
            "租户审计治理",
            null,
            null
        );
    }

    private LocalDateTime minTime(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    private LocalDateTime maxTime(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isAfter(right) ? left : right;
    }

    private record ActorSnapshot(Long actorUserId, String actorDisplayName, String actorRole) {
    }

    private record RequestSnapshot(String sourceType, String requestMethod, String requestPath) {
    }

    private record AuditTarget(
        String objectType,
        Long objectId,
        String objectTitle,
        Long knowledgeBaseId,
        String knowledgeBaseName) {
    }

    private record AuditQuery(
        Long tenantId,
        Long knowledgeBaseId,
        String normalizedObjectType,
        Long objectId,
        String normalizedResultType,
        LambdaQueryWrapper<AuditLog> queryWrapper) {
    }

    private record AuditCsvRow(
        Long id,
        Long tenantId,
        Long knowledgeBaseId,
        String knowledgeBaseName,
        String actorType,
        Long actorUserId,
        String actorDisplayName,
        String actorRole,
        String objectType,
        Long objectId,
        String objectTitle,
        String actionType,
        String resultType,
        String detail,
        String sourceType,
        String requestMethod,
        String requestPath,
        LocalDateTime createdAt) {
        private static AuditCsvRow fromActive(AuditLog auditLog) {
            return new AuditCsvRow(
                auditLog.getId(),
                auditLog.getTenantId(),
                auditLog.getKnowledgeBaseId(),
                auditLog.getKnowledgeBaseName(),
                auditLog.getActorType(),
                auditLog.getActorUserId(),
                auditLog.getActorDisplayName(),
                auditLog.getActorRole(),
                auditLog.getObjectType(),
                auditLog.getObjectId(),
                auditLog.getObjectTitle(),
                auditLog.getActionType(),
                auditLog.getResultType(),
                auditLog.getDetail(),
                auditLog.getSourceType(),
                auditLog.getRequestMethod(),
                auditLog.getRequestPath(),
                auditLog.getCreatedAt()
            );
        }

        private static AuditCsvRow fromArchived(AuditLogArchive auditLogArchive) {
            return new AuditCsvRow(
                auditLogArchive.getId(),
                auditLogArchive.getTenantId(),
                auditLogArchive.getKnowledgeBaseId(),
                auditLogArchive.getKnowledgeBaseName(),
                auditLogArchive.getActorType(),
                auditLogArchive.getActorUserId(),
                auditLogArchive.getActorDisplayName(),
                auditLogArchive.getActorRole(),
                auditLogArchive.getObjectType(),
                auditLogArchive.getObjectId(),
                auditLogArchive.getObjectTitle(),
                auditLogArchive.getActionType(),
                auditLogArchive.getResultType(),
                auditLogArchive.getDetail(),
                auditLogArchive.getSourceType(),
                auditLogArchive.getRequestMethod(),
                auditLogArchive.getRequestPath(),
                auditLogArchive.getCreatedAt()
            );
        }
    }
}

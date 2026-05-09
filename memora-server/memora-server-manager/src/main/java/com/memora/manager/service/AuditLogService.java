package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memora.common.exception.BusinessException;
import com.memora.manager.entity.AuditLog;
import com.memora.manager.entity.Document;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.entity.TenantMember;
import com.memora.manager.entity.UserAccount;
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
import com.memora.manager.vo.AuditRetentionSummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
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

    @Value("${memora.audit.export-max-size:1000}")
    private Integer exportMaxSize;

    private final AuditLogMapper auditLogMapper;
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
        LambdaQueryWrapper<AuditLog> totalQuery = buildAuditQuery(
            auditQuery.tenantId(),
            knowledgeBaseId,
            auditQuery.normalizedObjectType(),
            objectId,
            null
        );
        LambdaQueryWrapper<AuditLog> successQuery = buildAuditQuery(
            auditQuery.tenantId(),
            knowledgeBaseId,
            auditQuery.normalizedObjectType(),
            objectId,
            AuditLogConstants.RESULT_SUCCESS
        );
        LambdaQueryWrapper<AuditLog> failureQuery = buildAuditQuery(
            auditQuery.tenantId(),
            knowledgeBaseId,
            auditQuery.normalizedObjectType(),
            objectId,
            AuditLogConstants.RESULT_FAILURE
        );

        AuditRetentionSummaryVO summary = new AuditRetentionSummaryVO();
        summary.setConfiguredRetentionDays(retentionDays);
        summary.setExportMaxSize(exportMaxSize);
        summary.setTotalCount(auditLogMapper.selectCount(totalQuery));
        summary.setSuccessCount(auditLogMapper.selectCount(successQuery));
        summary.setFailureCount(auditLogMapper.selectCount(failureQuery));

        AuditLog earliest = auditLogMapper.selectOne(buildAuditQuery(
            auditQuery.tenantId(),
            knowledgeBaseId,
            auditQuery.normalizedObjectType(),
            objectId,
            null
        ).orderByAsc(AuditLog::getCreatedAt).orderByAsc(AuditLog::getId).last("LIMIT 1"));
        AuditLog latest = auditLogMapper.selectOne(buildAuditQuery(
            auditQuery.tenantId(),
            knowledgeBaseId,
            auditQuery.normalizedObjectType(),
            objectId,
            null
        ).orderByDesc(AuditLog::getCreatedAt).orderByDesc(AuditLog::getId).last("LIMIT 1"));
        summary.setEarliestCreatedAt(earliest == null ? null : earliest.getCreatedAt());
        summary.setLatestCreatedAt(latest == null ? null : latest.getCreatedAt());
        return summary;
    }

    public String exportCsv(Long knowledgeBaseId, String objectType, Long objectId, String resultType) {
        AuditQuery auditQuery = prepareAuditQuery(knowledgeBaseId, objectType, objectId, resultType);
        LambdaQueryWrapper<AuditLog> queryWrapper = auditQuery.queryWrapper();
        queryWrapper.last("LIMIT " + exportMaxSize);
        List<AuditLog> auditLogs = auditLogMapper.selectList(queryWrapper);
        recordSuccess(buildExportAuditCommand(auditQuery, knowledgeBaseId, objectId, resultType, auditLogs.size()));
        String header = "\uFEFFid,tenantId,knowledgeBaseId,knowledgeBaseName,actorType,actorUserId,actorDisplayName,actorRole,objectType,objectId,objectTitle,actionType,resultType,detail,sourceType,requestMethod,requestPath,createdAt\n";
        return header + auditLogs.stream()
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

        LambdaQueryWrapper<AuditLog> queryWrapper = buildAuditQuery(
            tenantId,
            knowledgeBaseId,
            normalizedObjectType,
            objectId,
            normalizedResultType
        );
        queryWrapper.orderByDesc(AuditLog::getCreatedAt).orderByDesc(AuditLog::getId);
        return new AuditQuery(tenantId, normalizedObjectType, normalizedResultType, queryWrapper);
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

    private LambdaQueryWrapper<AuditLog> buildAuditQuery(
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

    private String toCsvRow(AuditLog auditLog) {
        return String.join(",",
            csvValue(auditLog.getId()),
            csvValue(auditLog.getTenantId()),
            csvValue(auditLog.getKnowledgeBaseId()),
            csvValue(auditLog.getKnowledgeBaseName()),
            csvValue(auditLog.getActorType()),
            csvValue(auditLog.getActorUserId()),
            csvValue(auditLog.getActorDisplayName()),
            csvValue(auditLog.getActorRole()),
            csvValue(auditLog.getObjectType()),
            csvValue(auditLog.getObjectId()),
            csvValue(auditLog.getObjectTitle()),
            csvValue(auditLog.getActionType()),
            csvValue(auditLog.getResultType()),
            csvValue(auditLog.getDetail()),
            csvValue(auditLog.getSourceType()),
            csvValue(auditLog.getRequestMethod()),
            csvValue(auditLog.getRequestPath()),
            csvValue(auditLog.getCreatedAt())
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
        Long knowledgeBaseId,
        Long objectId,
        String resultType,
        int exportedCount) {
        String objectType = AuditLogConstants.OBJECT_TENANT;
        Long resolvedObjectId = auditQuery.tenantId();
        String objectTitle = "租户审计导出";

        if (AuditLogConstants.OBJECT_DOCUMENT.equals(auditQuery.normalizedObjectType()) && objectId != null) {
            Document document = requireDocumentInCurrentTenant(objectId);
            objectType = AuditLogConstants.OBJECT_DOCUMENT;
            resolvedObjectId = document.getId();
            objectTitle = document.getTitle();
        } else if (AuditLogConstants.OBJECT_KNOWLEDGE_BASE.equals(auditQuery.normalizedObjectType()) && objectId != null) {
            KnowledgeBase knowledgeBase = requireKnowledgeBaseInCurrentTenant(objectId);
            objectType = AuditLogConstants.OBJECT_KNOWLEDGE_BASE;
            resolvedObjectId = knowledgeBase.getId();
            objectTitle = knowledgeBase.getName();
        } else if (knowledgeBaseId != null) {
            KnowledgeBase knowledgeBase = requireKnowledgeBaseInCurrentTenant(knowledgeBaseId);
            objectType = AuditLogConstants.OBJECT_KNOWLEDGE_BASE;
            resolvedObjectId = knowledgeBase.getId();
            objectTitle = knowledgeBase.getName();
        } else if (StringUtils.hasText(auditQuery.normalizedObjectType()) && objectId != null) {
            objectType = auditQuery.normalizedObjectType();
            resolvedObjectId = objectId;
            objectTitle = "对象审计导出";
        }

        String detail = "导出审计 CSV " + exportedCount + " 条"
            + (StringUtils.hasText(resultType) ? "，结果过滤 " + resultType.trim().toUpperCase(Locale.ROOT) : "");
        return AuditLogCommand.builder()
            .tenantId(auditQuery.tenantId())
            .knowledgeBaseId(knowledgeBaseId)
            .knowledgeBaseName(knowledgeBaseId == null ? null : requireKnowledgeBaseInCurrentTenant(knowledgeBaseId).getName())
            .objectType(objectType)
            .objectId(resolvedObjectId)
            .objectTitle(objectTitle)
            .actionType(AuditLogConstants.ACTION_EXPORT_AUDIT_LOG)
            .detail(detail)
            .build();
    }

    private record ActorSnapshot(Long actorUserId, String actorDisplayName, String actorRole) {
    }

    private record RequestSnapshot(String sourceType, String requestMethod, String requestPath) {
    }

    private record AuditQuery(
        Long tenantId,
        String normalizedObjectType,
        String normalizedResultType,
        LambdaQueryWrapper<AuditLog> queryWrapper) {
    }
}

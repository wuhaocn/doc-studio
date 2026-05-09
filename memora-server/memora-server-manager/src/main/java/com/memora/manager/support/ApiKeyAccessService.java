package com.memora.manager.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.entity.ApiKey;
import com.memora.manager.entity.ApiKeyScope;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.entity.ServiceAccount;
import com.memora.manager.mapper.ApiKeyMapper;
import com.memora.manager.mapper.ApiKeyScopeMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.mapper.ServiceAccountMapper;
import com.memora.manager.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ApiKeyAccessService {
    public static final String ACCESS_MODE_READ = "READ";
    public static final String ACCESS_MODE_WRITE = "WRITE";
    public static final int API_KEY_STATUS_DISABLED = 0;
    public static final int API_KEY_STATUS_ACTIVE = 1;
    public static final int API_KEY_STATUS_REVOKED = 2;
    public static final int SERVICE_ACCOUNT_STATUS_ACTIVE = 1;
    public static final int KEY_PREFIX_LENGTH = 18;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String API_KEY_HEADER = "X-Memora-Api-Key";
    private static final String API_KEY_PREFIX = "ApiKey ";

    private final ApiKeyMapper apiKeyMapper;
    private final ApiKeyScopeMapper apiKeyScopeMapper;
    private final ServiceAccountMapper serviceAccountMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final PasswordCodec passwordCodec;
    private final AuditLogService auditLogService;

    public ApiKeyPrincipal requirePrincipal(String actionType) {
        String rawKey = resolveRawApiKey();
        if (!StringUtils.hasText(rawKey)) {
            throw new BusinessException(401, "当前请求未携带有效 API key");
        }

        String keyPrefix = buildKeyPrefix(rawKey);
        ApiKey apiKey = findApiKeyByPrefix(keyPrefix);
        if (apiKey == null) {
            throw new BusinessException(401, "当前 API key 无效");
        }
        if (!passwordCodec.matches(rawKey, apiKey.getSecretHash())) {
            recordInvalidKeyFailure(apiKey, actionType, "当前 API key 无效");
            throw new BusinessException(401, "当前 API key 无效");
        }

        ServiceAccount serviceAccount = requireActiveServiceAccount(apiKey);
        if (apiKey.getStatus() == null || apiKey.getStatus() == API_KEY_STATUS_DISABLED) {
            recordFailure(apiKey, serviceAccount, actionType, "当前 API key 已禁用");
            throw new BusinessException(403, "当前 API key 已禁用");
        }
        if (apiKey.getStatus() == API_KEY_STATUS_REVOKED || apiKey.getRevokedAt() != null) {
            recordFailure(apiKey, serviceAccount, actionType, "当前 API key 已吊销");
            throw new BusinessException(403, "当前 API key 已吊销");
        }
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            recordFailure(apiKey, serviceAccount, actionType, "当前 API key 已过期");
            throw new BusinessException(403, "当前 API key 已过期");
        }

        Map<Long, String> knowledgeBaseModes = loadKnowledgeBaseModes(apiKey.getId());
        if (knowledgeBaseModes.isEmpty()) {
            recordFailure(apiKey, serviceAccount, actionType, "当前 API key 没有任何可用作用域");
            throw new BusinessException(403, "当前 API key 没有任何可用作用域");
        }

        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.updateById(apiKey);

        return new ApiKeyPrincipal(
            apiKey.getTenantId(),
            serviceAccount.getId(),
            serviceAccount.getName(),
            serviceAccount.getCreatedByUserId(),
            apiKey.getId(),
            apiKey.getName(),
            knowledgeBaseModes
        );
    }

    public KnowledgeBase requireKnowledgeBaseRead(ApiKeyPrincipal principal, Long knowledgeBaseId, String actionType) {
        KnowledgeBase knowledgeBase = requireActiveKnowledgeBase(principal.tenantId(), knowledgeBaseId);
        String accessMode = principal.knowledgeBaseModes().get(knowledgeBaseId);
        if (!canRead(accessMode)) {
            recordFailure(principal, knowledgeBase, AuditLogConstants.OBJECT_KNOWLEDGE_BASE, knowledgeBase.getId(), knowledgeBase.getName(), actionType, "当前 API key 无权读取该知识库");
            throw new BusinessException(403, "当前 API key 无权读取该知识库");
        }
        return knowledgeBase;
    }

    public KnowledgeBase requireKnowledgeBaseWrite(ApiKeyPrincipal principal, Long knowledgeBaseId, String actionType) {
        KnowledgeBase knowledgeBase = requireKnowledgeBaseRead(principal, knowledgeBaseId, actionType);
        String accessMode = principal.knowledgeBaseModes().get(knowledgeBaseId);
        if (!ACCESS_MODE_WRITE.equals(accessMode)) {
            recordFailure(principal, knowledgeBase, AuditLogConstants.OBJECT_KNOWLEDGE_BASE, knowledgeBase.getId(), knowledgeBase.getName(), actionType, "当前 API key 无权写入该知识库");
            throw new BusinessException(403, "当前 API key 无权写入该知识库");
        }
        return knowledgeBase;
    }

    public AuditLogCommand buildAuditCommand(
        ApiKeyPrincipal principal,
        KnowledgeBase knowledgeBase,
        String objectType,
        Long objectId,
        String objectTitle,
        String actionType,
        String detail) {
        return AuditLogCommand.builder()
            .tenantId(principal.tenantId())
            .knowledgeBaseId(knowledgeBase == null ? null : knowledgeBase.getId())
            .knowledgeBaseName(knowledgeBase == null ? null : knowledgeBase.getName())
            .actorType(AuditLogConstants.ACTOR_API_KEY)
            .actorDisplayName(principal.serviceAccountName() + " / " + principal.apiKeyName())
            .objectType(objectType)
            .objectId(objectId)
            .objectTitle(objectTitle)
            .actionType(actionType)
            .detail(detail)
            .sourceType(AuditLogConstants.SOURCE_OPEN_API)
            .build();
    }

    public void recordFailure(
        ApiKeyPrincipal principal,
        KnowledgeBase knowledgeBase,
        String objectType,
        Long objectId,
        String objectTitle,
        String actionType,
        String detail) {
        auditLogService.recordFailureSafely(buildAuditCommand(
            principal,
            knowledgeBase,
            objectType,
            objectId,
            objectTitle,
            actionType,
            detail
        ));
    }

    public String normalizeAccessMode(String accessMode) {
        String normalized = accessMode == null ? "" : accessMode.trim().toUpperCase(Locale.ROOT);
        if (!ACCESS_MODE_READ.equals(normalized) && !ACCESS_MODE_WRITE.equals(normalized)) {
            throw new BusinessException(400, "当前 accessMode 仅支持 READ 或 WRITE");
        }
        return normalized;
    }

    private String resolveRawApiKey() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authorization) && authorization.startsWith(API_KEY_PREFIX)) {
            return authorization.substring(API_KEY_PREFIX.length()).trim();
        }
        String apiKeyHeader = request.getHeader(API_KEY_HEADER);
        return StringUtils.hasText(apiKeyHeader) ? apiKeyHeader.trim() : null;
    }

    private ApiKey findApiKeyByPrefix(String keyPrefix) {
        LambdaQueryWrapper<ApiKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKey::getKeyPrefix, keyPrefix).last("LIMIT 1");
        return apiKeyMapper.selectOne(queryWrapper);
    }

    private ServiceAccount requireActiveServiceAccount(ApiKey apiKey) {
        ServiceAccount serviceAccount = serviceAccountMapper.selectById(apiKey.getServiceAccountId());
        if (serviceAccount == null || !Objects.equals(serviceAccount.getTenantId(), apiKey.getTenantId())
            || serviceAccount.getStatus() == null || serviceAccount.getStatus() != SERVICE_ACCOUNT_STATUS_ACTIVE) {
            throw new BusinessException(403, "当前 API key 对应的机器主体不可用");
        }
        return serviceAccount;
    }

    private Map<Long, String> loadKnowledgeBaseModes(Long apiKeyId) {
        LambdaQueryWrapper<ApiKeyScope> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKeyScope::getApiKeyId, apiKeyId);
        List<ApiKeyScope> scopes = apiKeyScopeMapper.selectList(queryWrapper);
        Map<Long, String> result = new LinkedHashMap<>();
        for (ApiKeyScope scope : scopes) {
            result.put(scope.getKnowledgeBaseId(), normalizeAccessMode(scope.getAccessMode()));
        }
        return result;
    }

    private KnowledgeBase requireActiveKnowledgeBase(Long tenantId, Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null || knowledgeBase.getStatus() == null || knowledgeBase.getStatus() == 0) {
            throw new BusinessException(404, "知识库不存在");
        }
        if (!Objects.equals(knowledgeBase.getTenantId(), tenantId)) {
            throw new BusinessException(403, "当前 API key 无权访问该知识库");
        }
        return knowledgeBase;
    }

    private boolean canRead(String accessMode) {
        return ACCESS_MODE_READ.equals(accessMode) || ACCESS_MODE_WRITE.equals(accessMode);
    }

    private String buildKeyPrefix(String rawKey) {
        return rawKey.length() <= KEY_PREFIX_LENGTH ? rawKey : rawKey.substring(0, KEY_PREFIX_LENGTH);
    }

    private void recordFailure(ApiKey apiKey, ServiceAccount serviceAccount, String actionType, String detail) {
        auditLogService.recordFailureSafely(AuditLogCommand.builder()
            .tenantId(apiKey.getTenantId())
            .actorType(AuditLogConstants.ACTOR_API_KEY)
            .actorDisplayName(serviceAccount.getName() + " / " + apiKey.getName())
            .objectType(AuditLogConstants.OBJECT_API_KEY)
            .objectId(apiKey.getId())
            .objectTitle(apiKey.getName())
            .actionType(actionType)
            .detail(detail)
            .sourceType(AuditLogConstants.SOURCE_OPEN_API)
            .build());
    }

    private void recordInvalidKeyFailure(ApiKey apiKey, String actionType, String detail) {
        ServiceAccount serviceAccount = serviceAccountMapper.selectById(apiKey.getServiceAccountId());
        if (serviceAccount == null || !Objects.equals(serviceAccount.getTenantId(), apiKey.getTenantId())) {
            return;
        }
        recordFailure(apiKey, serviceAccount, actionType, detail);
    }

    public record ApiKeyPrincipal(
        Long tenantId,
        Long serviceAccountId,
        String serviceAccountName,
        Long serviceAccountCreatedByUserId,
        Long apiKeyId,
        String apiKeyName,
        Map<Long, String> knowledgeBaseModes) {
    }
}

package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.ApiKeyCreateDTO;
import com.memora.manager.dto.ApiKeyRotateDTO;
import com.memora.manager.dto.ApiKeyScopeAssignDTO;
import com.memora.manager.dto.ApiKeyScopeUpdateDTO;
import com.memora.manager.dto.ServiceAccountCreateDTO;
import com.memora.manager.entity.ApiKey;
import com.memora.manager.entity.ApiKeyScope;
import com.memora.manager.entity.KnowledgeBase;
import com.memora.manager.entity.ServiceAccount;
import com.memora.manager.mapper.ApiKeyMapper;
import com.memora.manager.mapper.ApiKeyScopeMapper;
import com.memora.manager.mapper.KnowledgeBaseMapper;
import com.memora.manager.mapper.ServiceAccountMapper;
import com.memora.manager.support.ApiKeyAccessService;
import com.memora.manager.support.ApiKeySecretCodec;
import com.memora.manager.support.AuditLogCommand;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.OpaqueTokenGenerator;
import com.memora.manager.support.PasswordCodec;
import com.memora.manager.support.TenantAccessService;
import com.memora.manager.vo.ApiKeyScopeVO;
import com.memora.manager.vo.ApiKeyVO;
import com.memora.manager.vo.IssuedApiKeyVO;
import com.memora.manager.vo.ServiceAccountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAccountService {
    @Value("${memora.api-key.default-expire-days:90}")
    private Integer defaultExpireDays;

    private final ServiceAccountMapper serviceAccountMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final ApiKeyScopeMapper apiKeyScopeMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final TenantAccessService tenantAccessService;
    private final CurrentAccessContext currentAccessContext;
    private final PasswordCodec passwordCodec;
    private final ApiKeySecretCodec apiKeySecretCodec;
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final AuditLogService auditLogService;
    private final ApiKeyAccessService apiKeyAccessService;

    @Transactional(rollbackFor = Exception.class)
    public IssuedApiKeyVO createServiceAccount(ServiceAccountCreateDTO dto) {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        String actorRole = tenantAccessService.requireTenantManage(tenantId).getRole();
        List<ApiKeyScopeSeed> scopeSeeds = resolveScopeSeeds(dto.getScopes());

        ServiceAccount serviceAccount = new ServiceAccount();
        serviceAccount.setTenantId(tenantId);
        serviceAccount.setName(dto.getName().trim());
        serviceAccount.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
        serviceAccount.setStatus(ApiKeyAccessService.SERVICE_ACCOUNT_STATUS_ACTIVE);
        serviceAccount.setCreatedByUserId(currentAccessContext.getCurrentUserId());
        serviceAccount.setCreatedAt(LocalDateTime.now());
        serviceAccount.setUpdatedAt(LocalDateTime.now());
        serviceAccountMapper.insert(serviceAccount);

        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(tenantId)
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_SERVICE_ACCOUNT)
            .objectId(serviceAccount.getId())
            .objectTitle(serviceAccount.getName())
            .actionType(AuditLogConstants.ACTION_CREATE_SERVICE_ACCOUNT)
            .detail("创建机器主体并准备首个访问密钥")
            .build());

        return issueApiKey(
            serviceAccount,
            dto.getKeyName(),
            resolveExpiresInDays(dto.getExpiresInDays()),
            scopeSeeds,
            null,
            actorRole,
            AuditLogConstants.ACTION_CREATE_API_KEY
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public IssuedApiKeyVO createApiKey(Long serviceAccountId, ApiKeyCreateDTO dto) {
        ServiceAccountContext context = requireManageableServiceAccount(serviceAccountId);
        return issueApiKey(
            context.serviceAccount(),
            dto.getName(),
            resolveExpiresInDays(dto.getExpiresInDays()),
            resolveScopeSeeds(dto.getScopes()),
            null,
            context.actorRole(),
            AuditLogConstants.ACTION_CREATE_API_KEY
        );
    }

    public List<ServiceAccountVO> listServiceAccounts() {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        tenantAccessService.requireTenantManage(tenantId);

        List<ServiceAccount> serviceAccounts = serviceAccountMapper.selectList(new LambdaQueryWrapper<ServiceAccount>()
            .eq(ServiceAccount::getTenantId, tenantId)
            .orderByDesc(ServiceAccount::getCreatedAt)
            .orderByDesc(ServiceAccount::getId));
        if (serviceAccounts.isEmpty()) {
            return List.of();
        }

        List<ApiKey> apiKeys = apiKeyMapper.selectList(new LambdaQueryWrapper<ApiKey>()
            .in(ApiKey::getServiceAccountId, serviceAccounts.stream().map(ServiceAccount::getId).toList())
            .ne(ApiKey::getStatus, ApiKeyAccessService.API_KEY_STATUS_REVOKED)
            .orderByDesc(ApiKey::getCreatedAt)
            .orderByDesc(ApiKey::getId));
        Map<Long, List<ApiKeyScope>> scopesByApiKeyId = listScopesByApiKeyIds(apiKeys.stream().map(ApiKey::getId).toList());
        Map<Long, List<ApiKeyVO>> apiKeysByServiceAccountId = apiKeys.stream()
            .map(item -> convertApiKey(item, scopesByApiKeyId.getOrDefault(item.getId(), List.of()), true))
            .collect(Collectors.groupingBy(ApiKeyVO::getServiceAccountId, LinkedHashMap::new, Collectors.toList()));

        return serviceAccounts.stream().map(item -> {
            ServiceAccountVO vo = new ServiceAccountVO();
            BeanUtils.copyProperties(item, vo);
            vo.setApiKeys(apiKeysByServiceAccountId.getOrDefault(item.getId(), List.of()));
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void enableServiceAccount(Long serviceAccountId) {
        ServiceAccountContext context = requireManageableServiceAccount(serviceAccountId);
        if (Objects.equals(context.serviceAccount().getStatus(), ApiKeyAccessService.SERVICE_ACCOUNT_STATUS_ACTIVE)) {
            throw new BusinessException(400, "当前机器主体已启用");
        }

        context.serviceAccount().setStatus(ApiKeyAccessService.SERVICE_ACCOUNT_STATUS_ACTIVE);
        context.serviceAccount().setRevokedAt(null);
        context.serviceAccount().setUpdatedAt(LocalDateTime.now());
        serviceAccountMapper.updateById(context.serviceAccount());
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(context.serviceAccount().getTenantId())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(context.actorRole())
            .objectType(AuditLogConstants.OBJECT_SERVICE_ACCOUNT)
            .objectId(context.serviceAccount().getId())
            .objectTitle(context.serviceAccount().getName())
            .actionType(AuditLogConstants.ACTION_ENABLE_SERVICE_ACCOUNT)
            .detail("启用机器主体，主体下未删除的密钥可继续调用")
            .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableServiceAccount(Long serviceAccountId) {
        ServiceAccountContext context = requireManageableServiceAccount(serviceAccountId);
        if (Objects.equals(context.serviceAccount().getStatus(), ApiKeyAccessService.SERVICE_ACCOUNT_STATUS_DISABLED)) {
            throw new BusinessException(400, "当前机器主体已停用");
        }

        context.serviceAccount().setStatus(ApiKeyAccessService.SERVICE_ACCOUNT_STATUS_DISABLED);
        context.serviceAccount().setRevokedAt(LocalDateTime.now());
        context.serviceAccount().setUpdatedAt(LocalDateTime.now());
        serviceAccountMapper.updateById(context.serviceAccount());
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(context.serviceAccount().getTenantId())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(context.actorRole())
            .objectType(AuditLogConstants.OBJECT_SERVICE_ACCOUNT)
            .objectId(context.serviceAccount().getId())
            .objectTitle(context.serviceAccount().getName())
            .actionType(AuditLogConstants.ACTION_DISABLE_SERVICE_ACCOUNT)
            .detail("停用机器主体，主体下密钥会立即停止外部调用")
            .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableApiKey(Long apiKeyId) {
        ApiKeyContext context = requireManageableApiKey(apiKeyId);
        if (Objects.equals(context.apiKey().getStatus(), ApiKeyAccessService.API_KEY_STATUS_REVOKED)) {
            throw new BusinessException(400, "当前密钥已删除");
        }
        if (Objects.equals(context.apiKey().getStatus(), ApiKeyAccessService.API_KEY_STATUS_DISABLED)) {
            throw new BusinessException(400, "当前密钥已暂停");
        }

        context.apiKey().setStatus(ApiKeyAccessService.API_KEY_STATUS_DISABLED);
        context.apiKey().setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.updateById(context.apiKey());
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(context.apiKey().getTenantId())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(context.actorRole())
            .objectType(AuditLogConstants.OBJECT_API_KEY)
            .objectId(context.apiKey().getId())
            .objectTitle(context.apiKey().getName())
            .actionType(AuditLogConstants.ACTION_DISABLE_API_KEY)
            .detail("暂停访问密钥")
            .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeApiKey(Long apiKeyId) {
        ApiKeyContext context = requireManageableApiKey(apiKeyId);
        if (Objects.equals(context.apiKey().getStatus(), ApiKeyAccessService.API_KEY_STATUS_REVOKED)) {
            throw new BusinessException(400, "当前密钥已删除");
        }

        context.apiKey().setStatus(ApiKeyAccessService.API_KEY_STATUS_REVOKED);
        context.apiKey().setRevokedByUserId(currentAccessContext.getCurrentUserId());
        context.apiKey().setRevokedAt(LocalDateTime.now());
        context.apiKey().setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.updateById(context.apiKey());
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(context.apiKey().getTenantId())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(context.actorRole())
            .objectType(AuditLogConstants.OBJECT_API_KEY)
            .objectId(context.apiKey().getId())
            .objectTitle(context.apiKey().getName())
            .actionType(AuditLogConstants.ACTION_REVOKE_API_KEY)
            .detail("删除访问密钥")
            .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateApiKeyScope(Long apiKeyId, ApiKeyScopeUpdateDTO dto) {
        ApiKeyContext context = requireManageableApiKey(apiKeyId);
        if (Objects.equals(context.apiKey().getStatus(), ApiKeyAccessService.API_KEY_STATUS_REVOKED)) {
            throw new BusinessException(400, "当前密钥已删除");
        }

        List<ApiKeyScopeSeed> scopeSeeds = resolveScopeSeeds(dto.getScopes());
        List<ApiKeyScope> persistedScopes = replaceApiKeyScopes(context.apiKey(), scopeSeeds);
        context.apiKey().setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.updateById(context.apiKey());
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(context.apiKey().getTenantId())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(context.actorRole())
            .objectType(AuditLogConstants.OBJECT_API_KEY)
            .objectId(context.apiKey().getId())
            .objectTitle(context.apiKey().getName())
            .actionType(AuditLogConstants.ACTION_UPDATE_API_KEY_SCOPE)
            .detail("更新密钥访问范围为 " + formatScopeDetail(persistedScopes))
            .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public IssuedApiKeyVO revealApiKey(Long apiKeyId) {
        ApiKeyContext context = requireManageableApiKey(apiKeyId);
        if (!StringUtils.hasText(context.apiKey().getSecretCiphertext())) {
            throw new BusinessException(409, "当前密钥创建较早，暂不支持再次显示，请先重新生成");
        }

        String plainTextKey;
        try {
            plainTextKey = apiKeySecretCodec.decrypt(context.apiKey().getSecretCiphertext());
        } catch (IllegalStateException ex) {
            throw new BusinessException(409, "当前密钥暂不可再次显示，请先重新生成后重试");
        }
        if (!passwordCodec.matches(plainTextKey, context.apiKey().getSecretHash())) {
            throw new BusinessException(409, "当前密钥暂不可再次显示，请先重新生成后重试");
        }

        List<ApiKeyScope> persistedScopes = apiKeyScopeMapper.selectList(new LambdaQueryWrapper<ApiKeyScope>()
            .eq(ApiKeyScope::getApiKeyId, apiKeyId)
            .orderByAsc(ApiKeyScope::getKnowledgeBaseId));
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(context.apiKey().getTenantId())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(context.actorRole())
            .objectType(AuditLogConstants.OBJECT_API_KEY)
            .objectId(context.apiKey().getId())
            .objectTitle(context.apiKey().getName())
            .actionType(AuditLogConstants.ACTION_REVEAL_API_KEY)
            .detail("重新展示访问密钥明文")
            .build());

        IssuedApiKeyVO issuedApiKey = new IssuedApiKeyVO();
        issuedApiKey.setApiKey(convertApiKey(context.apiKey(), persistedScopes, false));
        issuedApiKey.setPlainTextKey(plainTextKey);
        return issuedApiKey;
    }

    @Transactional(rollbackFor = Exception.class)
    public IssuedApiKeyVO rotateApiKey(Long apiKeyId, ApiKeyRotateDTO dto) {
        ApiKeyContext context = requireManageableApiKey(apiKeyId);
        if (Objects.equals(context.apiKey().getStatus(), ApiKeyAccessService.API_KEY_STATUS_REVOKED)) {
            throw new BusinessException(400, "当前密钥已删除");
        }

        List<ApiKeyScope> existingScopes = apiKeyScopeMapper.selectList(new LambdaQueryWrapper<ApiKeyScope>()
            .eq(ApiKeyScope::getApiKeyId, apiKeyId));
        if (existingScopes.isEmpty()) {
            throw new BusinessException(400, "当前密钥没有可继承的访问范围");
        }

        context.apiKey().setStatus(ApiKeyAccessService.API_KEY_STATUS_REVOKED);
        context.apiKey().setRevokedByUserId(currentAccessContext.getCurrentUserId());
        context.apiKey().setRevokedAt(LocalDateTime.now());
        context.apiKey().setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.updateById(context.apiKey());

        IssuedApiKeyVO issuedApiKey = issueApiKey(
            context.serviceAccount(),
            StringUtils.hasText(dto.getName()) ? dto.getName() : context.apiKey().getName(),
            resolveExpiresInDays(dto.getExpiresInDays()),
            existingScopes.stream()
                .map(item -> new ApiKeyScopeSeed(item.getKnowledgeBaseId(), item.getAccessMode()))
                .toList(),
            context.apiKey().getId(),
            context.actorRole(),
            AuditLogConstants.ACTION_ROTATE_API_KEY
        );
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(context.apiKey().getTenantId())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(context.actorRole())
            .objectType(AuditLogConstants.OBJECT_API_KEY)
            .objectId(context.apiKey().getId())
            .objectTitle(context.apiKey().getName())
            .actionType(AuditLogConstants.ACTION_ROTATE_API_KEY)
            .detail("重新生成密钥并立即失效旧密钥")
            .build());
        return issuedApiKey;
    }

    private IssuedApiKeyVO issueApiKey(
        ServiceAccount serviceAccount,
        String keyName,
        Integer expiresInDays,
        List<ApiKeyScopeSeed> scopeSeeds,
        Long rotatedFromKeyId,
        String actorRole,
        String actionType
    ) {
        String plainTextKey = opaqueTokenGenerator.generate("memora_sk_");
        String keyPrefix = plainTextKey.length() <= ApiKeyAccessService.KEY_PREFIX_LENGTH
            ? plainTextKey
            : plainTextKey.substring(0, ApiKeyAccessService.KEY_PREFIX_LENGTH);

        ApiKey apiKey = new ApiKey();
        apiKey.setServiceAccountId(serviceAccount.getId());
        apiKey.setTenantId(serviceAccount.getTenantId());
        apiKey.setName(keyName.trim());
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setSecretHash(passwordCodec.hash(plainTextKey));
        apiKey.setSecretCiphertext(apiKeySecretCodec.encrypt(plainTextKey));
        apiKey.setStatus(ApiKeyAccessService.API_KEY_STATUS_ACTIVE);
        apiKey.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        apiKey.setCreatedByUserId(currentAccessContext.getCurrentUserId());
        apiKey.setRotatedFromKeyId(rotatedFromKeyId);
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.insert(apiKey);

        List<ApiKeyScope> persistedScopes = replaceApiKeyScopes(apiKey, scopeSeeds);
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(serviceAccount.getTenantId())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_API_KEY)
            .objectId(apiKey.getId())
            .objectTitle(apiKey.getName())
            .actionType(actionType)
            .detail("签发访问密钥，访问范围为 " + formatScopeDetail(persistedScopes))
            .build());

        IssuedApiKeyVO issuedApiKey = new IssuedApiKeyVO();
        issuedApiKey.setApiKey(convertApiKey(apiKey, persistedScopes, false));
        issuedApiKey.setPlainTextKey(plainTextKey);
        return issuedApiKey;
    }

    private List<ApiKeyScopeSeed> resolveScopeSeeds(List<ApiKeyScopeAssignDTO> scopes) {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        if (scopes == null || scopes.isEmpty()) {
            throw new BusinessException(400, "至少配置一个可访问知识库");
        }

        Map<Long, String> normalizedScopes = new LinkedHashMap<>();
        for (ApiKeyScopeAssignDTO scope : scopes) {
            if (normalizedScopes.containsKey(scope.getKnowledgeBaseId())) {
                throw new BusinessException(400, "同一密钥不能重复配置知识库访问范围");
            }
            normalizedScopes.put(scope.getKnowledgeBaseId(), apiKeyAccessService.normalizeAccessMode(scope.getAccessMode()));
        }

        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBase>()
            .in(KnowledgeBase::getId, normalizedScopes.keySet())
            .eq(KnowledgeBase::getTenantId, tenantId)
            .eq(KnowledgeBase::getStatus, 1));
        if (knowledgeBases.size() != normalizedScopes.size()) {
            throw new BusinessException(404, "部分知识库不存在或不可用");
        }

        Map<Long, KnowledgeBase> knowledgeBaseMap = knowledgeBases.stream()
            .collect(Collectors.toMap(KnowledgeBase::getId, item -> item));
        return normalizedScopes.entrySet().stream().map(entry -> {
            KnowledgeBase knowledgeBase = knowledgeBaseMap.get(entry.getKey());
            if (knowledgeBase == null) {
                throw new BusinessException(404, "部分知识库不存在或不可用");
            }
            tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
            return new ApiKeyScopeSeed(entry.getKey(), entry.getValue());
        }).toList();
    }

    private List<ApiKeyScope> replaceApiKeyScopes(ApiKey apiKey, List<ApiKeyScopeSeed> scopeSeeds) {
        apiKeyScopeMapper.delete(new LambdaQueryWrapper<ApiKeyScope>()
            .eq(ApiKeyScope::getApiKeyId, apiKey.getId()));
        for (ApiKeyScopeSeed scopeSeed : scopeSeeds) {
            ApiKeyScope scope = new ApiKeyScope();
            scope.setApiKeyId(apiKey.getId());
            scope.setTenantId(apiKey.getTenantId());
            scope.setKnowledgeBaseId(scopeSeed.knowledgeBaseId());
            scope.setAccessMode(scopeSeed.accessMode());
            scope.setCreatedAt(LocalDateTime.now());
            apiKeyScopeMapper.insert(scope);
        }
        return apiKeyScopeMapper.selectList(new LambdaQueryWrapper<ApiKeyScope>()
            .eq(ApiKeyScope::getApiKeyId, apiKey.getId())
            .orderByAsc(ApiKeyScope::getKnowledgeBaseId));
    }

    private Integer resolveExpiresInDays(Integer expiresInDays) {
        return expiresInDays == null || expiresInDays <= 0 ? defaultExpireDays : expiresInDays;
    }

    private Map<Long, List<ApiKeyScope>> listScopesByApiKeyIds(List<Long> apiKeyIds) {
        if (apiKeyIds.isEmpty()) {
            return Map.of();
        }
        return apiKeyScopeMapper.selectList(new LambdaQueryWrapper<ApiKeyScope>()
                .in(ApiKeyScope::getApiKeyId, apiKeyIds)
                .orderByAsc(ApiKeyScope::getKnowledgeBaseId))
            .stream()
            .collect(Collectors.groupingBy(ApiKeyScope::getApiKeyId, LinkedHashMap::new, Collectors.toList()));
    }

    private ApiKeyVO convertApiKey(ApiKey apiKey, List<ApiKeyScope> scopes, boolean includePlainTextKey) {
        ApiKeyVO vo = new ApiKeyVO();
        BeanUtils.copyProperties(apiKey, vo);
        vo.setExpired(apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now()));
        vo.setSecretRevealAvailable(StringUtils.hasText(apiKey.getSecretCiphertext()));
        if (includePlainTextKey) {
            vo.setPlainTextKey(resolvePlainTextKeyForManagement(apiKey));
        }
        vo.setScopes(scopes.stream().map(this::convertScope).toList());
        return vo;
    }

    private String resolvePlainTextKeyForManagement(ApiKey apiKey) {
        if (!StringUtils.hasText(apiKey.getSecretCiphertext())
            || Objects.equals(apiKey.getStatus(), ApiKeyAccessService.API_KEY_STATUS_REVOKED)) {
            return null;
        }
        try {
            String plainTextKey = apiKeySecretCodec.decrypt(apiKey.getSecretCiphertext());
            if (!passwordCodec.matches(plainTextKey, apiKey.getSecretHash())) {
                return null;
            }
            return plainTextKey;
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    private ApiKeyScopeVO convertScope(ApiKeyScope scope) {
        ApiKeyScopeVO vo = new ApiKeyScopeVO();
        vo.setKnowledgeBaseId(scope.getKnowledgeBaseId());
        vo.setAccessMode(scope.getAccessMode());
        return vo;
    }

    private String formatScopeDetail(List<ApiKeyScope> scopes) {
        return scopes.stream()
            .map(scope -> "#" + scope.getKnowledgeBaseId() + "(" + scope.getAccessMode() + ")")
            .collect(Collectors.joining("、"));
    }

    private ServiceAccountContext requireManageableServiceAccount(Long serviceAccountId) {
        ServiceAccount serviceAccount = serviceAccountMapper.selectById(serviceAccountId);
        if (serviceAccount == null) {
            throw new BusinessException(404, "机器主体不存在");
        }

        Long tenantId = currentAccessContext.getCurrentTenantId();
        String actorRole = tenantAccessService.requireTenantManage(tenantId).getRole();
        if (!Objects.equals(serviceAccount.getTenantId(), tenantId)) {
            throw new BusinessException(403, "无权访问该机器主体");
        }
        return new ServiceAccountContext(serviceAccount, actorRole);
    }

    private ApiKeyContext requireManageableApiKey(Long apiKeyId) {
        ApiKey apiKey = apiKeyMapper.selectById(apiKeyId);
        if (apiKey == null) {
            throw new BusinessException(404, "密钥不存在");
        }

        ServiceAccountContext accountContext = requireManageableServiceAccount(apiKey.getServiceAccountId());
        if (!Objects.equals(apiKey.getTenantId(), accountContext.serviceAccount().getTenantId())) {
            throw new BusinessException(403, "无权访问该密钥");
        }
        return new ApiKeyContext(accountContext.serviceAccount(), apiKey, accountContext.actorRole());
    }

    private record ApiKeyScopeSeed(Long knowledgeBaseId, String accessMode) {
    }

    private record ServiceAccountContext(ServiceAccount serviceAccount, String actorRole) {
    }

    private record ApiKeyContext(ServiceAccount serviceAccount, ApiKey apiKey, String actorRole) {
    }
}

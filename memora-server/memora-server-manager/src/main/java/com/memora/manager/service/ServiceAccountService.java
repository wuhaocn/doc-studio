package com.memora.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.dto.ApiKeyRotateDTO;
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
import com.memora.manager.support.AuditLogCommand;
import com.memora.manager.support.AuditLogConstants;
import com.memora.manager.support.CurrentAccessContext;
import com.memora.manager.support.OpaqueTokenGenerator;
import com.memora.manager.support.PasswordCodec;
import com.memora.manager.support.TenantAccessService;
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
import java.util.ArrayList;
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
    private final OpaqueTokenGenerator opaqueTokenGenerator;
    private final AuditLogService auditLogService;
    private final ApiKeyAccessService apiKeyAccessService;

    @Transactional(rollbackFor = Exception.class)
    public IssuedApiKeyVO createServiceAccount(ServiceAccountCreateDTO dto) {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        String actorRole = tenantAccessService.requireTenantManage(tenantId).getRole();
        List<KnowledgeBase> knowledgeBases = requireKnowledgeBases(dto.getKnowledgeBaseIds());
        String accessMode = apiKeyAccessService.normalizeAccessMode(dto.getAccessMode());

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
            .detail("创建机器主体并准备首个 API key")
            .build());

        return issueApiKey(serviceAccount, dto.getKeyName(), resolveExpiresInDays(dto.getExpiresInDays()), buildScopeSeeds(knowledgeBases, accessMode), null, actorRole, AuditLogConstants.ACTION_CREATE_API_KEY);
    }

    public List<ServiceAccountVO> listServiceAccounts() {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        tenantAccessService.requireTenantManage(tenantId);

        LambdaQueryWrapper<ServiceAccount> serviceAccountQuery = new LambdaQueryWrapper<>();
        serviceAccountQuery.eq(ServiceAccount::getTenantId, tenantId)
            .orderByDesc(ServiceAccount::getCreatedAt)
            .orderByDesc(ServiceAccount::getId);
        List<ServiceAccount> serviceAccounts = serviceAccountMapper.selectList(serviceAccountQuery);
        if (serviceAccounts.isEmpty()) {
            return List.of();
        }

        List<Long> serviceAccountIds = serviceAccounts.stream().map(ServiceAccount::getId).toList();
        List<ApiKey> apiKeys = apiKeyMapper.selectList(new LambdaQueryWrapper<ApiKey>()
            .in(ApiKey::getServiceAccountId, serviceAccountIds)
            .orderByDesc(ApiKey::getCreatedAt)
            .orderByDesc(ApiKey::getId));
        Map<Long, List<ApiKeyScope>> scopesByApiKeyId = listScopesByApiKeyIds(apiKeys.stream().map(ApiKey::getId).toList());
        Map<Long, List<ApiKeyVO>> apiKeysByServiceAccountId = apiKeys.stream()
            .map(item -> convertApiKey(item, scopesByApiKeyId.getOrDefault(item.getId(), List.of())))
            .collect(Collectors.groupingBy(ApiKeyVO::getServiceAccountId, LinkedHashMap::new, Collectors.toList()));

        return serviceAccounts.stream().map(item -> {
            ServiceAccountVO vo = new ServiceAccountVO();
            BeanUtils.copyProperties(item, vo);
            vo.setApiKeys(apiKeysByServiceAccountId.getOrDefault(item.getId(), List.of()));
            return vo;
        }).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableApiKey(Long apiKeyId) {
        ApiKeyContext context = requireManageableApiKey(apiKeyId);
        if (context.apiKey().getStatus() == ApiKeyAccessService.API_KEY_STATUS_REVOKED) {
            throw new BusinessException(400, "当前 API key 已吊销");
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
            .detail("禁用 API key")
            .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeApiKey(Long apiKeyId) {
        ApiKeyContext context = requireManageableApiKey(apiKeyId);
        if (context.apiKey().getStatus() == ApiKeyAccessService.API_KEY_STATUS_REVOKED) {
            throw new BusinessException(400, "当前 API key 已吊销");
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
            .detail("吊销 API key")
            .build());
    }

    @Transactional(rollbackFor = Exception.class)
    public IssuedApiKeyVO rotateApiKey(Long apiKeyId, ApiKeyRotateDTO dto) {
        ApiKeyContext context = requireManageableApiKey(apiKeyId);
        if (context.apiKey().getStatus() == ApiKeyAccessService.API_KEY_STATUS_REVOKED) {
            throw new BusinessException(400, "当前 API key 已吊销");
        }

        List<ApiKeyScope> existingScopes = apiKeyScopeMapper.selectList(new LambdaQueryWrapper<ApiKeyScope>()
            .eq(ApiKeyScope::getApiKeyId, apiKeyId));
        if (existingScopes.isEmpty()) {
            throw new BusinessException(400, "当前 API key 没有可继承的作用域");
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
            existingScopes.stream().map(item -> new ApiKeyScopeSeed(item.getKnowledgeBaseId(), item.getAccessMode())).toList(),
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
            .detail("轮换 API key 并立即失效旧 key")
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
        String actionType) {
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
        apiKey.setStatus(ApiKeyAccessService.API_KEY_STATUS_ACTIVE);
        apiKey.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        apiKey.setCreatedByUserId(currentAccessContext.getCurrentUserId());
        apiKey.setRotatedFromKeyId(rotatedFromKeyId);
        apiKey.setCreatedAt(LocalDateTime.now());
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyMapper.insert(apiKey);

        for (ApiKeyScopeSeed scopeSeed : scopeSeeds) {
            ApiKeyScope scope = new ApiKeyScope();
            scope.setApiKeyId(apiKey.getId());
            scope.setTenantId(serviceAccount.getTenantId());
            scope.setKnowledgeBaseId(scopeSeed.knowledgeBaseId());
            scope.setAccessMode(scopeSeed.accessMode());
            scope.setCreatedAt(LocalDateTime.now());
            apiKeyScopeMapper.insert(scope);
        }

        List<ApiKeyScope> persistedScopes = apiKeyScopeMapper.selectList(new LambdaQueryWrapper<ApiKeyScope>()
            .eq(ApiKeyScope::getApiKeyId, apiKey.getId()));
        auditLogService.recordSuccess(AuditLogCommand.builder()
            .tenantId(serviceAccount.getTenantId())
            .actorUserId(currentAccessContext.getCurrentUserId())
            .actorRole(actorRole)
            .objectType(AuditLogConstants.OBJECT_API_KEY)
            .objectId(apiKey.getId())
            .objectTitle(apiKey.getName())
            .actionType(actionType)
            .detail("签发 API key，作用域知识库 " + persistedScopes.stream().map(item -> "#" + item.getKnowledgeBaseId()).collect(Collectors.joining("、")))
            .build());

        IssuedApiKeyVO issuedApiKey = new IssuedApiKeyVO();
        issuedApiKey.setApiKey(convertApiKey(apiKey, persistedScopes));
        issuedApiKey.setPlainTextKey(plainTextKey);
        return issuedApiKey;
    }

    private List<KnowledgeBase> requireKnowledgeBases(List<Long> knowledgeBaseIds) {
        Long tenantId = currentAccessContext.getCurrentTenantId();
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            throw new BusinessException(400, "至少选择一个知识库作用域");
        }
        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBase>()
            .in(KnowledgeBase::getId, knowledgeBaseIds)
            .eq(KnowledgeBase::getTenantId, tenantId)
            .eq(KnowledgeBase::getStatus, 1));
        if (knowledgeBases.size() != knowledgeBaseIds.size()) {
            throw new BusinessException(404, "部分知识库不存在或不可用");
        }
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            tenantAccessService.requireKnowledgeBaseManageAccess(knowledgeBase);
        }
        return knowledgeBases;
    }

    private Integer resolveExpiresInDays(Integer expiresInDays) {
        return expiresInDays == null || expiresInDays <= 0 ? defaultExpireDays : expiresInDays;
    }

    private List<ApiKeyScopeSeed> buildScopeSeeds(List<KnowledgeBase> knowledgeBases, String accessMode) {
        List<ApiKeyScopeSeed> seeds = new ArrayList<>();
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            seeds.add(new ApiKeyScopeSeed(knowledgeBase.getId(), accessMode));
        }
        return seeds;
    }

    private Map<Long, List<ApiKeyScope>> listScopesByApiKeyIds(List<Long> apiKeyIds) {
        if (apiKeyIds.isEmpty()) {
            return Map.of();
        }
        return apiKeyScopeMapper.selectList(new LambdaQueryWrapper<ApiKeyScope>().in(ApiKeyScope::getApiKeyId, apiKeyIds))
            .stream()
            .collect(Collectors.groupingBy(ApiKeyScope::getApiKeyId, LinkedHashMap::new, Collectors.toList()));
    }

    private ApiKeyVO convertApiKey(ApiKey apiKey, List<ApiKeyScope> scopes) {
        ApiKeyVO vo = new ApiKeyVO();
        BeanUtils.copyProperties(apiKey, vo);
        vo.setExpired(apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now()));
        vo.setKnowledgeBaseIds(scopes.stream().map(ApiKeyScope::getKnowledgeBaseId).toList());
        vo.setAccessModes(scopes.stream().map(ApiKeyScope::getAccessMode).distinct().toList());
        return vo;
    }

    private ApiKeyContext requireManageableApiKey(Long apiKeyId) {
        ApiKey apiKey = apiKeyMapper.selectById(apiKeyId);
        if (apiKey == null) {
            throw new BusinessException(404, "API key 不存在");
        }
        Long tenantId = currentAccessContext.getCurrentTenantId();
        String actorRole = tenantAccessService.requireTenantManage(tenantId).getRole();
        if (!Objects.equals(apiKey.getTenantId(), tenantId)) {
            throw new BusinessException(403, "无权访问该 API key");
        }
        ServiceAccount serviceAccount = serviceAccountMapper.selectById(apiKey.getServiceAccountId());
        if (serviceAccount == null || !Objects.equals(serviceAccount.getTenantId(), tenantId)) {
            throw new BusinessException(404, "service account 不存在");
        }
        return new ApiKeyContext(serviceAccount, apiKey, actorRole);
    }

    private record ApiKeyScopeSeed(Long knowledgeBaseId, String accessMode) {
    }

    private record ApiKeyContext(ServiceAccount serviceAccount, ApiKey apiKey, String actorRole) {
    }
}

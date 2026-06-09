package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.ApiKeyCreateDTO;
import com.memora.manager.dto.ApiKeyRotateDTO;
import com.memora.manager.dto.ApiKeyScopeUpdateDTO;
import com.memora.manager.dto.ServiceAccountCreateDTO;
import com.memora.manager.service.ServiceAccountService;
import com.memora.manager.vo.IssuedApiKeyVO;
import com.memora.manager.vo.ServiceAccountVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ServiceAccountController {
    private final ServiceAccountService serviceAccountService;

    @GetMapping("/api/v1/service-accounts")
    public Result<List<ServiceAccountVO>> listServiceAccounts() {
        return Result.success(serviceAccountService.listServiceAccounts());
    }

    @PostMapping("/api/v1/service-accounts")
    public Result<IssuedApiKeyVO> createServiceAccount(@Valid @RequestBody ServiceAccountCreateDTO dto) {
        return Result.success(serviceAccountService.createServiceAccount(dto));
    }

    @PostMapping("/api/v1/service-accounts/{serviceAccountId}/api-keys")
    public Result<IssuedApiKeyVO> createApiKey(
        @PathVariable Long serviceAccountId,
        @Valid @RequestBody ApiKeyCreateDTO dto
    ) {
        return Result.success(serviceAccountService.createApiKey(serviceAccountId, dto));
    }

    @PostMapping("/api/v1/service-accounts/{serviceAccountId}/disable")
    public Result<Void> disableServiceAccount(@PathVariable Long serviceAccountId) {
        serviceAccountService.disableServiceAccount(serviceAccountId);
        return Result.success();
    }

    @PostMapping("/api/v1/service-accounts/{serviceAccountId}/enable")
    public Result<Void> enableServiceAccount(@PathVariable Long serviceAccountId) {
        serviceAccountService.enableServiceAccount(serviceAccountId);
        return Result.success();
    }

    @DeleteMapping("/api/v1/service-accounts/{serviceAccountId}")
    public Result<Void> deleteServiceAccount(@PathVariable Long serviceAccountId) {
        serviceAccountService.deleteServiceAccount(serviceAccountId);
        return Result.success();
    }

    @PostMapping("/api/v1/api-keys/{apiKeyId}/disable")
    public Result<Void> disableApiKey(@PathVariable Long apiKeyId) {
        serviceAccountService.disableApiKey(apiKeyId);
        return Result.success();
    }

    @PostMapping("/api/v1/api-keys/{apiKeyId}/reveal")
    public Result<IssuedApiKeyVO> revealApiKey(@PathVariable Long apiKeyId) {
        return Result.success(serviceAccountService.revealApiKey(apiKeyId));
    }

    @PostMapping("/api/v1/api-keys/{apiKeyId}/revoke")
    public Result<Void> revokeApiKey(@PathVariable Long apiKeyId) {
        serviceAccountService.revokeApiKey(apiKeyId);
        return Result.success();
    }

    @PutMapping("/api/v1/api-keys/{apiKeyId}/scope")
    public Result<Void> updateApiKeyScope(
        @PathVariable Long apiKeyId,
        @Valid @RequestBody ApiKeyScopeUpdateDTO dto
    ) {
        serviceAccountService.updateApiKeyScope(apiKeyId, dto);
        return Result.success();
    }

    @PostMapping("/api/v1/api-keys/{apiKeyId}/rotate")
    public Result<IssuedApiKeyVO> rotateApiKey(@PathVariable Long apiKeyId, @Valid @RequestBody ApiKeyRotateDTO dto) {
        return Result.success(serviceAccountService.rotateApiKey(apiKeyId, dto));
    }
}

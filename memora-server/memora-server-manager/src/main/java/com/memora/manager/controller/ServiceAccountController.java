package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.ApiKeyRotateDTO;
import com.memora.manager.dto.ServiceAccountCreateDTO;
import com.memora.manager.service.ServiceAccountService;
import com.memora.manager.vo.IssuedApiKeyVO;
import com.memora.manager.vo.ServiceAccountVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PostMapping("/api/v1/api-keys/{apiKeyId}/disable")
    public Result<Void> disableApiKey(@PathVariable Long apiKeyId) {
        serviceAccountService.disableApiKey(apiKeyId);
        return Result.success();
    }

    @PostMapping("/api/v1/api-keys/{apiKeyId}/revoke")
    public Result<Void> revokeApiKey(@PathVariable Long apiKeyId) {
        serviceAccountService.revokeApiKey(apiKeyId);
        return Result.success();
    }

    @PostMapping("/api/v1/api-keys/{apiKeyId}/rotate")
    public Result<IssuedApiKeyVO> rotateApiKey(@PathVariable Long apiKeyId, @Valid @RequestBody ApiKeyRotateDTO dto) {
        return Result.success(serviceAccountService.rotateApiKey(apiKeyId, dto));
    }
}

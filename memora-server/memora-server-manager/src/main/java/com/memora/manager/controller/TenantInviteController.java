package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.TenantInviteAcceptDTO;
import com.memora.manager.dto.TenantInviteCreateDTO;
import com.memora.manager.service.TenantInviteService;
import com.memora.manager.support.BrowserSessionSupport;
import com.memora.manager.vo.AuthSessionVO;
import com.memora.manager.vo.TenantInviteVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TenantInviteController {
    private final TenantInviteService tenantInviteService;
    private final BrowserSessionSupport browserSessionSupport;

    @PostMapping("/tenants/current/invites")
    public Result<TenantInviteVO> createInvite(@Valid @RequestBody TenantInviteCreateDTO dto) {
        return Result.success(tenantInviteService.createInvite(dto));
    }

    @GetMapping("/tenants/current/invites")
    public Result<List<TenantInviteVO>> listInvites() {
        return Result.success(tenantInviteService.listInvites());
    }

    @GetMapping("/invites/{token}")
    public Result<TenantInviteVO> getInvite(@PathVariable("token") String token) {
        return Result.success(tenantInviteService.getInvite(token));
    }

    @PostMapping("/invites/accept")
    public Result<AuthSessionVO> acceptInvite(
        @Valid @RequestBody TenantInviteAcceptDTO dto,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        AuthSessionVO session = tenantInviteService.acceptInvite(dto);
        if (!browserSessionSupport.isWebAppClient(request)) {
            return Result.success(session);
        }
        if (StringUtils.hasText(session.getAccessToken())) {
            browserSessionSupport.writeSessionCookie(response, session.getAccessToken());
        }
        return Result.success(browserSessionSupport.sanitizeBrowserSession(session));
    }

    @PostMapping("/tenants/current/invites/{id}/revoke")
    public Result<TenantInviteVO> revokeInvite(@PathVariable("id") Long id) {
        return Result.success(tenantInviteService.revokeInvite(id));
    }
}

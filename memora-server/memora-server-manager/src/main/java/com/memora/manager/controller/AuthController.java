package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.AuthLoginDTO;
import com.memora.manager.dto.AuthRegisterOwnerDTO;
import com.memora.manager.service.AuthService;
import com.memora.manager.support.BrowserSessionSupport;
import com.memora.manager.vo.AuthSessionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final BrowserSessionSupport browserSessionSupport;

    @PostMapping("/login")
    public Result<AuthSessionVO> login(
        @Valid @RequestBody AuthLoginDTO dto,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return Result.success(prepareBrowserSession(authService.login(dto), request, response, true));
    }

    @PostMapping("/register-owner")
    public Result<AuthSessionVO> registerOwner(
        @Valid @RequestBody AuthRegisterOwnerDTO dto,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        return Result.success(prepareBrowserSession(authService.registerOwner(dto), request, response, true));
    }

    @GetMapping("/session")
    public Result<AuthSessionVO> getCurrentSession(HttpServletRequest request) {
        return Result.success(prepareBrowserSession(authService.getCurrentSession(), request, null, false));
    }

    @PostMapping("/logout")
    public Result<Boolean> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout();
        if (browserSessionSupport.shouldClearSessionCookie(request)) {
            browserSessionSupport.clearSessionCookie(response);
        }
        return Result.success(true);
    }

    @PostMapping("/refresh")
    public Result<AuthSessionVO> refresh(HttpServletRequest request, HttpServletResponse response) {
        return Result.success(prepareBrowserSession(authService.refreshSession(), request, response, true));
    }

    private AuthSessionVO prepareBrowserSession(
        AuthSessionVO session,
        HttpServletRequest request,
        HttpServletResponse response,
        boolean writeCookie
    ) {
        if (!browserSessionSupport.isWebAppClient(request)) {
            return session;
        }
        if (writeCookie && response != null && StringUtils.hasText(session == null ? null : session.getAccessToken())) {
            browserSessionSupport.writeSessionCookie(response, session.getAccessToken());
        }
        return browserSessionSupport.sanitizeBrowserSession(session);
    }
}

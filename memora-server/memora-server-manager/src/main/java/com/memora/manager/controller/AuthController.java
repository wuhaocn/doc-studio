package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.AuthLoginDTO;
import com.memora.manager.dto.AuthRegisterOwnerDTO;
import com.memora.manager.service.AuthService;
import com.memora.manager.vo.AuthSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/login")
    public Result<AuthSessionVO> login(@Valid @RequestBody AuthLoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/register-owner")
    public Result<AuthSessionVO> registerOwner(@Valid @RequestBody AuthRegisterOwnerDTO dto) {
        return Result.success(authService.registerOwner(dto));
    }

    @GetMapping("/session")
    public Result<AuthSessionVO> getCurrentSession() {
        return Result.success(authService.getCurrentSession());
    }

    @PostMapping("/logout")
    public Result<Boolean> logout() {
        authService.logout();
        return Result.success(true);
    }

    @PostMapping("/refresh")
    public Result<AuthSessionVO> refresh() {
        return Result.success(authService.refreshSession());
    }
}

package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.service.AuthService;
import com.memora.manager.service.WorkspaceService;
import com.memora.manager.vo.AuthSessionVO;
import com.memora.manager.vo.WorkspaceMembershipVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceAccessController {
    private final WorkspaceService workspaceService;
    private final AuthService authService;

    @GetMapping("/joined")
    public Result<List<WorkspaceMembershipVO>> listJoinedWorkspaces() {
        return Result.success(workspaceService.listJoinedWorkspaces());
    }

    @PostMapping("/{tenantId}/switch")
    public Result<AuthSessionVO> switchWorkspace(@PathVariable("tenantId") Long tenantId) {
        return Result.success(authService.switchWorkspace(tenantId));
    }
}

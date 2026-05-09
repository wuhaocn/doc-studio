package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.DocumentShareCreateDTO;
import com.memora.manager.service.DocumentShareService;
import com.memora.manager.vo.DocumentShareLinkVO;
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
public class DocumentShareController {
    private final DocumentShareService documentShareService;

    @PostMapping("/api/v1/document-shares")
    public Result<DocumentShareLinkVO> createShare(@Valid @RequestBody DocumentShareCreateDTO dto) {
        return Result.success(documentShareService.createShare(dto));
    }

    @GetMapping("/api/v1/documents/{documentId}/shares")
    public Result<List<DocumentShareLinkVO>> listShares(@PathVariable Long documentId) {
        return Result.success(documentShareService.listShares(documentId));
    }

    @PostMapping("/api/v1/document-shares/{shareId}/revoke")
    public Result<Void> revokeShare(@PathVariable Long shareId) {
        documentShareService.revokeShare(shareId);
        return Result.success();
    }
}

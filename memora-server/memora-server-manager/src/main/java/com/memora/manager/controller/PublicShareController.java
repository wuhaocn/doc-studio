package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.PublicShareAccessDTO;
import com.memora.manager.service.DocumentShareService;
import com.memora.manager.vo.PublicShareDocumentVO;
import com.memora.manager.vo.PublicShareInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public-shares")
@RequiredArgsConstructor
public class PublicShareController {
    private final DocumentShareService documentShareService;

    @GetMapping("/{token}")
    public Result<PublicShareInfoVO> getShareInfo(@PathVariable String token) {
        return Result.success(documentShareService.getPublicShareInfo(token));
    }

    @PostMapping("/{token}/access")
    public Result<PublicShareDocumentVO> accessShare(@PathVariable String token, @Valid @RequestBody(required = false) PublicShareAccessDTO dto) {
        return Result.success(documentShareService.accessPublicShare(token, dto));
    }
}

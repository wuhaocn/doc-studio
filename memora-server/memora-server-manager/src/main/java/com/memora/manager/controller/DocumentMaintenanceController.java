package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.dto.DocumentContentNormalizeDTO;
import com.memora.manager.service.DocumentMaintenanceService;
import com.memora.manager.vo.DocumentContentNormalizationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents/maintenance")
@RequiredArgsConstructor
public class DocumentMaintenanceController {
    private final DocumentMaintenanceService documentMaintenanceService;

    @PostMapping("/normalize-content")
    public Result<DocumentContentNormalizationVO> normalizeContent(@RequestBody(required = false) DocumentContentNormalizeDTO dto) {
        return Result.success(documentMaintenanceService.normalizeContent(dto));
    }
}

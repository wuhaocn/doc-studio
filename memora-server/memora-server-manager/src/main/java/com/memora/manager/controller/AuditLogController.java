package com.memora.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.memora.common.result.Result;
import com.memora.manager.service.AuditLogService;
import com.memora.manager.vo.AuditRetentionSummaryVO;
import com.memora.manager.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogService auditLogService;

    @GetMapping
    public Result<IPage<AuditLogVO>> list(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        @RequestParam(required = false) Long knowledgeBaseId,
        @RequestParam(required = false) String objectType,
        @RequestParam(required = false) Long objectId,
        @RequestParam(required = false) String resultType) {
        return Result.success(auditLogService.list(page, size, knowledgeBaseId, objectType, objectId, resultType));
    }

    @GetMapping("/summary")
    public Result<AuditRetentionSummaryVO> summary(
        @RequestParam(required = false) Long knowledgeBaseId,
        @RequestParam(required = false) String objectType,
        @RequestParam(required = false) Long objectId) {
        return Result.success(auditLogService.getRetentionSummary(knowledgeBaseId, objectType, objectId));
    }

    @GetMapping("/export")
    public ResponseEntity<String> export(
        @RequestParam(required = false) Long knowledgeBaseId,
        @RequestParam(required = false) String objectType,
        @RequestParam(required = false) Long objectId,
        @RequestParam(required = false) String resultType) {
        String csv = auditLogService.exportCsv(knowledgeBaseId, objectType, objectId, resultType);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"memora-audit-log.csv\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv);
    }
}

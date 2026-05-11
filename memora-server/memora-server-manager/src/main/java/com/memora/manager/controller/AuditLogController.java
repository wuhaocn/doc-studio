package com.memora.manager.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.memora.common.result.Result;
import com.memora.manager.service.AuditLogService;
import com.memora.manager.vo.AuditRetentionExecutionVO;
import com.memora.manager.vo.AuditRetentionSummaryVO;
import com.memora.manager.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

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
        @RequestParam(required = false) String resultType,
        @RequestParam(required = false) String storageScope) {
        String resolvedScope = storageScope == null ? "ACTIVE" : storageScope.trim().toUpperCase(Locale.ROOT);
        String csv = auditLogService.exportCsv(knowledgeBaseId, objectType, objectId, resultType, resolvedScope);
        String filename = switch (resolvedScope) {
            case "ARCHIVED" -> "memora-audit-log-archived.csv";
            case "ALL" -> "memora-audit-log-all.csv";
            default -> "memora-audit-log.csv";
        };
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv);
    }

    @PostMapping("/retention/run")
    public Result<AuditRetentionExecutionVO> runRetention(
        @RequestParam(required = false) Long knowledgeBaseId,
        @RequestParam(required = false) String objectType,
        @RequestParam(required = false) Long objectId) {
        return Result.success(auditLogService.runRetention(knowledgeBaseId, objectType, objectId));
    }
}

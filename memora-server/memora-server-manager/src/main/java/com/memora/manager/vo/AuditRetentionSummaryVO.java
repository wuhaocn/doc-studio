package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditRetentionSummaryVO {
    private Integer configuredRetentionDays;

    private Integer exportMaxSize;

    private Long totalCount;

    private Long successCount;

    private Long failureCount;

    private LocalDateTime earliestCreatedAt;

    private LocalDateTime latestCreatedAt;
}

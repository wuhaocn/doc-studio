package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditRetentionSummaryVO {
    private Integer configuredRetentionDays;

    private Integer archiveBatchSize;

    private Integer exportMaxSize;

    private Long totalCount;

    private Long activeCount;

    private Long archivedCount;

    private Long successCount;

    private Long failureCount;

    private Long pendingArchiveCount;

    private LocalDateTime earliestCreatedAt;

    private LocalDateTime latestCreatedAt;

    private LocalDateTime archiveBeforeCreatedAt;

    private LocalDateTime lastArchivedAt;
}

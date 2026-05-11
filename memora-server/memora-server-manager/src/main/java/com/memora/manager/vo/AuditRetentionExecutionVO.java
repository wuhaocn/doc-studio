package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditRetentionExecutionVO {
    private Integer configuredRetentionDays;

    private Integer archiveBatchSize;

    private Long eligibleCount;

    private Long archivedCount;

    private Long remainingPendingArchiveCount;

    private Long activeCount;

    private Long archivedTotalCount;

    private LocalDateTime archiveBeforeCreatedAt;

    private LocalDateTime executedAt;
}

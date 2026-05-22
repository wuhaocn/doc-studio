package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentContentNormalizationVO {
    private Long knowledgeBaseId;

    private String knowledgeBaseName;

    private Boolean dryRun;

    private Long scannedDocumentCount;

    private Long updatedDocumentCount;

    private Long documentDocTypeUpdatedCount;

    private Long documentFormatUpdatedCount;

    private Long documentContentTextUpdatedCount;

    private Long documentSummaryUpdatedCount;

    private Long scannedVersionCount;

    private Long updatedVersionCount;

    private Long versionFormatUpdatedCount;

    private Long versionContentTextUpdatedCount;

    private LocalDateTime executedAt;
}

package com.memora.manager.vo;

import lombok.Data;

import java.util.List;

@Data
public class OpenApiDocumentBatchUpsertVO {
    private Integer totalCount;

    private Integer createdCount;

    private Integer updatedCount;

    private Integer skippedCount;

    private Integer conflictedCount;

    private List<OpenApiDocumentUpsertResultVO> items;
}

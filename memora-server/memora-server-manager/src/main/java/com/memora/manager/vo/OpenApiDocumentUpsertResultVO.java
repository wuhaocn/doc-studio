package com.memora.manager.vo;

import lombok.Data;

@Data
public class OpenApiDocumentUpsertResultVO {
    private Integer itemIndex;

    private String status;

    private String message;

    private String sourceExternalId;

    private String sourceRevision;

    private DocumentVO document;
}

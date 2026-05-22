package com.memora.manager.dto;

import lombok.Data;

@Data
public class DocumentContentNormalizeDTO {
    private Long knowledgeBaseId;

    private Boolean dryRun;
}

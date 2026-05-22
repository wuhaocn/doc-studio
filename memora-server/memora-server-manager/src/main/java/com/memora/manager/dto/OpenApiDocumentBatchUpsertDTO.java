package com.memora.manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OpenApiDocumentBatchUpsertDTO {
    @Valid
    @NotEmpty(message = "至少提交一条文档")
    @Size(max = 100, message = "单次最多同步100条文档")
    private List<OpenApiDocumentUpsertDTO> documents;
}

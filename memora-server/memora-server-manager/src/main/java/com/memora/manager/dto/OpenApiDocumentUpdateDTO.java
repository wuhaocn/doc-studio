package com.memora.manager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OpenApiDocumentUpdateDTO {
    @NotNull(message = "expectedVersionNo 不能为空")
    private Integer expectedVersionNo;

    @Size(max = 200, message = "文档标题长度不能超过200个字符")
    private String title;

    private String format;

    private String content;

    @Size(max = 500, message = "摘要长度不能超过500个字符")
    private String summary;

    @Size(max = 160, message = "来源外部ID长度不能超过160个字符")
    private String sourceExternalId;

    @Size(max = 160, message = "来源版本长度不能超过160个字符")
    private String sourceRevision;
}

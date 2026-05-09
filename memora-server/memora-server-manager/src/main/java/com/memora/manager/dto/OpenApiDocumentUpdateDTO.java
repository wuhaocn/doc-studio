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

    private String contentText;

    @Size(max = 500, message = "摘要长度不能超过500个字符")
    private String summary;
}

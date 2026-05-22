package com.memora.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OpenApiDocumentCreateDTO {
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题长度不能超过200个字符")
    private String title;

    @Size(max = 160, message = "文档标识长度不能超过160个字符")
    private String slug;

    private String format;

    private String content;

    @Size(max = 500, message = "摘要长度不能超过500个字符")
    private String summary;

    @Size(max = 160, message = "来源外部ID长度不能超过160个字符")
    private String sourceExternalId;

    @Size(max = 160, message = "来源版本长度不能超过160个字符")
    private String sourceRevision;

    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;

    private Long parentId;
}

package com.memora.manager.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新文档DTO
 */
@Data
public class DocumentUpdateDTO {
    @Size(max = 200, message = "文档标题长度不能超过200个字符")
    private String title;

    @Size(max = 160, message = "文档标识长度不能超过160个字符")
    private String slug;

    private String docType;

    private String format;
    
    private String content;

    @Size(max = 500, message = "摘要长度不能超过500个字符")
    private String summary;

    private Long parentId;

    private Integer sortOrder;
}

package com.docstudio.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建文档DTO
 */
@Data
public class DocumentCreateDTO {
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题长度不能超过200个字符")
    private String title;
    
    private String content;
    
    private String contentText;
    
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;
    
    private Long parentId; // 默认为0，表示根目录
    
    private Long userId; // 临时字段，后续从鉴权中获取
}


package com.docstudio.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建知识库DTO
 */
@Data
public class KnowledgeBaseCreateDTO {
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
    
    private String cover;
    
    private Long userId; // 临时字段，后续从鉴权中获取
}


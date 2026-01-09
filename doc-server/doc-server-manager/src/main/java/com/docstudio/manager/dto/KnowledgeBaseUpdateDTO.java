package com.docstudio.manager.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新知识库DTO
 */
@Data
public class KnowledgeBaseUpdateDTO {
    @Size(max = 100, message = "知识库名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
    
    private String cover;
}


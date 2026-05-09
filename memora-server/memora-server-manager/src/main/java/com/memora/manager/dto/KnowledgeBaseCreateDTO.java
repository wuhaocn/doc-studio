package com.memora.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建知识库DTO
 */
@Data
public class KnowledgeBaseCreateDTO {
    private Long tenantId;

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称长度不能超过100个字符")
    private String name;

    @Size(max = 120, message = "知识库标识长度不能超过120个字符")
    private String slug;
    
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
    
    private String cover;
    
    private Long userId; // 兼容旧请求体，服务端会忽略并始终使用当前会话用户

}

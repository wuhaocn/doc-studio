package com.docstudio.manager.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新文档DTO
 */
@Data
public class DocumentUpdateDTO {
    @Size(max = 200, message = "文档标题长度不能超过200个字符")
    private String title;
    
    private String content;
    
    private String contentText;
}


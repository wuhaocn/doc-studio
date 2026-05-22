package com.memora.manager.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentPublishUpdateDTO {
    @Size(max = 160, message = "公开文档标识长度不能超过160个字符")
    private String publicSlug;
}


package com.memora.manager.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentShareCreateDTO {
    @NotNull(message = "文档ID不能为空")
    private Long documentId;

    @Min(value = 1, message = "分享有效期至少 1 天")
    @Max(value = 365, message = "分享有效期不能超过 365 天")
    private Integer expiresInDays;

    @Size(max = 32, message = "访问码长度不能超过32个字符")
    private String accessCode;
}

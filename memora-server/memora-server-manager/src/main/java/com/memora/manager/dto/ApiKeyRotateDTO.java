package com.memora.manager.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApiKeyRotateDTO {
    @Size(max = 120, message = "API key 名称长度不能超过120个字符")
    private String name;

    @Min(value = 1, message = "API key 有效期至少 1 天")
    @Max(value = 3650, message = "API key 有效期不能超过 3650 天")
    private Integer expiresInDays;
}

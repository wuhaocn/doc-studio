package com.memora.manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ServiceAccountCreateDTO {
    @NotBlank(message = "service account 名称不能为空")
    @Size(max = 120, message = "service account 名称长度不能超过120个字符")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @NotBlank(message = "首个 API key 名称不能为空")
    @Size(max = 120, message = "API key 名称长度不能超过120个字符")
    private String keyName;

    @Min(value = 1, message = "API key 有效期至少 1 天")
    @Max(value = 3650, message = "API key 有效期不能超过 3650 天")
    private Integer expiresInDays;

    @Valid
    @NotEmpty(message = "至少选择一个知识库作用域")
    private List<ApiKeyScopeAssignDTO> scopes;
}

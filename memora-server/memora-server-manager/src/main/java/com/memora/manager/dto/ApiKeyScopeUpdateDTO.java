package com.memora.manager.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ApiKeyScopeUpdateDTO {
    @Valid
    @NotEmpty(message = "至少配置一个知识库作用域")
    private List<ApiKeyScopeAssignDTO> scopes;
}

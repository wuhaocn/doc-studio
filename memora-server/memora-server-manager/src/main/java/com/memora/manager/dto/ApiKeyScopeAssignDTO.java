package com.memora.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApiKeyScopeAssignDTO {
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;

    @NotBlank(message = "访问模式不能为空")
    private String accessMode;
}

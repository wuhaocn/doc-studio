package com.memora.manager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantInviteCreateDTO {
    @NotBlank(message = "受邀邮箱不能为空")
    @Email(message = "受邀邮箱格式不正确")
    private String email;

    private String displayName;

    @NotBlank(message = "邀请角色不能为空")
    private String role;

    private Integer expiresInDays;
}

package com.memora.manager.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthSessionVO {
    private String username;

    private String email;

    private Long userId;

    private Long tenantId;

    private String displayName;

    private String role;

    private String tenantName;

    private String tenantSlug;

    private String industry;

    private String planName;

    private String accessToken;
}

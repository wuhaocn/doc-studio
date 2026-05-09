package com.memora.manager.vo;

import lombok.Data;

@Data
public class IssuedApiKeyVO {
    private ApiKeyVO apiKey;

    private String plainTextKey;
}

package com.memora.manager.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PublicShareInfoVO {
    private Long shareId;

    private String documentTitle;

    private String knowledgeBaseName;

    private Boolean accessCodeRequired;

    private LocalDateTime expiresAt;
}

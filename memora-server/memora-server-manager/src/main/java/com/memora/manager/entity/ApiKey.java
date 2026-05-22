package com.memora.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("api_key")
public class ApiKey {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long serviceAccountId;

    private Long tenantId;

    private String name;

    private String keyPrefix;

    private String secretHash;

    private String secretCiphertext;

    private Integer status;

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;

    private Long createdByUserId;

    private Long revokedByUserId;

    private LocalDateTime revokedAt;

    private Long rotatedFromKeyId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

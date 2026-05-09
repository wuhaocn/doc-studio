package com.memora.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("api_key_scope")
public class ApiKeyScope {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long apiKeyId;

    private Long tenantId;

    private Long knowledgeBaseId;

    private String accessMode;

    private LocalDateTime createdAt;
}

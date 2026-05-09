package com.memora.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_share_link")
public class DocumentShareLink {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long knowledgeBaseId;

    private Long documentId;

    private String shareToken;

    private String secretHash;

    private Integer status;

    private LocalDateTime expiresAt;

    private String accessCodeHash;

    private Long createdByUserId;

    private Long revokedByUserId;

    private LocalDateTime revokedAt;

    private LocalDateTime lastAccessedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

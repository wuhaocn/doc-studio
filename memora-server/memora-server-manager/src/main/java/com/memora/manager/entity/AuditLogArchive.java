package com.memora.manager.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_log_archive")
public class AuditLogArchive {
    @TableId(type = IdType.INPUT)
    private Long id;

    private Long tenantId;

    private Long knowledgeBaseId;

    private String knowledgeBaseName;

    private String actorType;

    private Long actorUserId;

    private String actorDisplayName;

    private String actorRole;

    private String objectType;

    private Long objectId;

    private String objectTitle;

    private String actionType;

    private String resultType;

    private String detail;

    private String sourceType;

    private String requestMethod;

    private String requestPath;

    private LocalDateTime createdAt;

    private LocalDateTime archivedAt;
}

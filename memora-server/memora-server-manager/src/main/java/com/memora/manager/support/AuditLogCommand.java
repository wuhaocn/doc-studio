package com.memora.manager.support;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditLogCommand {
    private final Long tenantId;

    private final Long knowledgeBaseId;

    private final String knowledgeBaseName;

    private final String actorType;

    private final Long actorUserId;

    private final String actorDisplayName;

    private final String actorRole;

    private final String objectType;

    private final Long objectId;

    private final String objectTitle;

    private final String actionType;

    private final String detail;

    private final String sourceType;

    private final String requestMethod;

    private final String requestPath;
}

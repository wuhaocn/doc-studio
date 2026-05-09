package com.memora.manager.support;

import java.util.Set;

public final class AuditLogConstants {
    public static final String ACTOR_USER = "USER";
    public static final String ACTOR_API_KEY = "API_KEY";
    public static final String ACTOR_SHARE_VISITOR = "SHARE_VISITOR";

    public static final String OBJECT_TENANT = "TENANT";
    public static final String OBJECT_USER_SESSION = "USER_SESSION";
    public static final String OBJECT_TENANT_INVITE = "TENANT_INVITE";
    public static final String OBJECT_KNOWLEDGE_BASE = "KNOWLEDGE_BASE";
    public static final String OBJECT_DOCUMENT = "DOCUMENT";
    public static final String OBJECT_DOCUMENT_SHARE_LINK = "DOCUMENT_SHARE_LINK";
    public static final String OBJECT_SERVICE_ACCOUNT = "SERVICE_ACCOUNT";
    public static final String OBJECT_API_KEY = "API_KEY";

    public static final String ACTION_REGISTER_OWNER = "REGISTER_OWNER";
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_SWITCH_WORKSPACE = "SWITCH_WORKSPACE";
    public static final String ACTION_CREATE_INVITE = "CREATE_INVITE";
    public static final String ACTION_ACCEPT_INVITE = "ACCEPT_INVITE";
    public static final String ACTION_REVOKE_INVITE = "REVOKE_INVITE";
    public static final String ACTION_CREATE_KNOWLEDGE_BASE = "CREATE_KNOWLEDGE_BASE";
    public static final String ACTION_UPDATE_KNOWLEDGE_BASE = "UPDATE_KNOWLEDGE_BASE";
    public static final String ACTION_DELETE_KNOWLEDGE_BASE = "DELETE_KNOWLEDGE_BASE";
    public static final String ACTION_RESTORE_KNOWLEDGE_BASE = "RESTORE_KNOWLEDGE_BASE";
    public static final String ACTION_UPDATE_KNOWLEDGE_BASE_MEMBERS = "UPDATE_KNOWLEDGE_BASE_MEMBERS";
    public static final String ACTION_CREATE_DOCUMENT = "CREATE_DOCUMENT";
    public static final String ACTION_UPDATE_DOCUMENT = "UPDATE_DOCUMENT";
    public static final String ACTION_MOVE_DOCUMENT = "MOVE_DOCUMENT";
    public static final String ACTION_REORDER_DOCUMENT = "REORDER_DOCUMENT";
    public static final String ACTION_DELETE_DOCUMENT = "DELETE_DOCUMENT";
    public static final String ACTION_RESTORE_DOCUMENT = "RESTORE_DOCUMENT";
    public static final String ACTION_ROLLBACK_DOCUMENT = "ROLLBACK_DOCUMENT";
    public static final String ACTION_EXPORT_AUDIT_LOG = "EXPORT_AUDIT_LOG";
    public static final String ACTION_CREATE_DOCUMENT_SHARE = "CREATE_DOCUMENT_SHARE";
    public static final String ACTION_REVOKE_DOCUMENT_SHARE = "REVOKE_DOCUMENT_SHARE";
    public static final String ACTION_ACCESS_DOCUMENT_SHARE = "ACCESS_DOCUMENT_SHARE";
    public static final String ACTION_CREATE_SERVICE_ACCOUNT = "CREATE_SERVICE_ACCOUNT";
    public static final String ACTION_CREATE_API_KEY = "CREATE_API_KEY";
    public static final String ACTION_DISABLE_API_KEY = "DISABLE_API_KEY";
    public static final String ACTION_REVOKE_API_KEY = "REVOKE_API_KEY";
    public static final String ACTION_ROTATE_API_KEY = "ROTATE_API_KEY";
    public static final String ACTION_OPEN_API_LIST_KNOWLEDGE_BASES = "OPEN_API_LIST_KNOWLEDGE_BASES";
    public static final String ACTION_OPEN_API_LIST_DOCUMENTS = "OPEN_API_LIST_DOCUMENTS";
    public static final String ACTION_OPEN_API_GET_DOCUMENT = "OPEN_API_GET_DOCUMENT";
    public static final String ACTION_OPEN_API_GET_DOCUMENT_VERSIONS = "OPEN_API_GET_DOCUMENT_VERSIONS";
    public static final String ACTION_OPEN_API_CREATE_DOCUMENT = "OPEN_API_CREATE_DOCUMENT";
    public static final String ACTION_OPEN_API_UPDATE_DOCUMENT = "OPEN_API_UPDATE_DOCUMENT";

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILURE = "FAILURE";

    public static final String SOURCE_DIRECT_API = "DIRECT_API";
    public static final String SOURCE_PUBLIC_SHARE = "PUBLIC_SHARE";
    public static final String SOURCE_OPEN_API = "OPEN_API";

    public static final Set<String> QUERYABLE_OBJECT_TYPES = Set.of(
        OBJECT_TENANT,
        OBJECT_USER_SESSION,
        OBJECT_TENANT_INVITE,
        OBJECT_KNOWLEDGE_BASE,
        OBJECT_DOCUMENT,
        OBJECT_DOCUMENT_SHARE_LINK,
        OBJECT_SERVICE_ACCOUNT,
        OBJECT_API_KEY
    );

    private AuditLogConstants() {
    }
}

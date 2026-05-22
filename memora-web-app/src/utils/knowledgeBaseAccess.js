const KNOWLEDGE_BASE_CREATE_ROLES = new Set(['OWNER', 'ADMIN', 'EDITOR'])
const KNOWLEDGE_BASE_TRASH_MANAGE_ROLES = new Set(['OWNER', 'ADMIN'])
const TENANT_INVITE_ROLES = new Set(['OWNER', 'ADMIN'])
const WORKSPACE_AUDIT_VIEW_ROLES = new Set(['OWNER', 'ADMIN'])
const SERVICE_ACCOUNT_MANAGE_ROLES = new Set(['OWNER', 'ADMIN'])
const TENANT_SESSION_MANAGE_ROLES = new Set(['OWNER', 'ADMIN'])

export const canCreateKnowledgeBaseForRole = (role) => {
  return KNOWLEDGE_BASE_CREATE_ROLES.has(role)
}

export const canManageKnowledgeBaseTrashForRole = (role) => {
  return KNOWLEDGE_BASE_TRASH_MANAGE_ROLES.has(role)
}

export const canInviteTenantMembersForRole = (role) => {
  return TENANT_INVITE_ROLES.has(role)
}

export const canViewWorkspaceAuditForRole = (role) => {
  return WORKSPACE_AUDIT_VIEW_ROLES.has(role)
}

export const canManageServiceAccountsForRole = (role) => {
  return SERVICE_ACCOUNT_MANAGE_ROLES.has(role)
}

export const canManageTenantSessionsForRole = (role) => {
  return TENANT_SESSION_MANAGE_ROLES.has(role)
}

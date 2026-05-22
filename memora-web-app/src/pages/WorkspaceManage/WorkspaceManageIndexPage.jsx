import { Navigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import {
  canInviteTenantMembersForRole,
  canManageKnowledgeBaseTrashForRole,
  canManageServiceAccountsForRole,
  canViewWorkspaceAuditForRole,
} from '../../utils/knowledgeBaseAccess'

const WorkspaceManageIndexPage = () => {
  const { currentUser } = useAuth()
  const role = currentUser?.role

  if (canInviteTenantMembersForRole(role)) {
    return <Navigate to="members" replace />
  }
  if (canManageKnowledgeBaseTrashForRole(role)) {
    return <Navigate to="trash" replace />
  }
  if (canManageServiceAccountsForRole(role)) {
    return <Navigate to="access" replace />
  }
  if (canViewWorkspaceAuditForRole(role)) {
    return <Navigate to="audit" replace />
  }
  return <Navigate to="security" replace />
}

export default WorkspaceManageIndexPage

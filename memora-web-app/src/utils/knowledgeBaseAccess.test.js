import assert from 'node:assert/strict'
import test from 'node:test'
import {
  canCreateKnowledgeBaseForRole,
  canInviteTenantMembersForRole,
  canManageServiceAccountsForRole,
  canManageKnowledgeBaseTrashForRole,
  canViewWorkspaceAuditForRole,
  shouldAutoOpenKnowledgeBaseTrash,
} from './knowledgeBaseAccess.js'

test('canCreateKnowledgeBaseForRole only allows workspace write roles', () => {
  assert.equal(canCreateKnowledgeBaseForRole('OWNER'), true)
  assert.equal(canCreateKnowledgeBaseForRole('ADMIN'), true)
  assert.equal(canCreateKnowledgeBaseForRole('EDITOR'), true)
  assert.equal(canCreateKnowledgeBaseForRole('REVIEWER'), false)
  assert.equal(canCreateKnowledgeBaseForRole('VIEWER'), false)
  assert.equal(canCreateKnowledgeBaseForRole(undefined), false)
})

test('canManageKnowledgeBaseTrashForRole only allows workspace manage roles', () => {
  assert.equal(canManageKnowledgeBaseTrashForRole('OWNER'), true)
  assert.equal(canManageKnowledgeBaseTrashForRole('ADMIN'), true)
  assert.equal(canManageKnowledgeBaseTrashForRole('EDITOR'), false)
  assert.equal(canManageKnowledgeBaseTrashForRole('VIEWER'), false)
  assert.equal(canManageKnowledgeBaseTrashForRole(null), false)
})

test('canInviteTenantMembersForRole only allows workspace manage roles', () => {
  assert.equal(canInviteTenantMembersForRole('OWNER'), true)
  assert.equal(canInviteTenantMembersForRole('ADMIN'), true)
  assert.equal(canInviteTenantMembersForRole('EDITOR'), false)
  assert.equal(canInviteTenantMembersForRole('VIEWER'), false)
})

test('canViewWorkspaceAuditForRole only allows workspace manage roles', () => {
  assert.equal(canViewWorkspaceAuditForRole('OWNER'), true)
  assert.equal(canViewWorkspaceAuditForRole('ADMIN'), true)
  assert.equal(canViewWorkspaceAuditForRole('EDITOR'), false)
  assert.equal(canViewWorkspaceAuditForRole('VIEWER'), false)
})

test('canManageServiceAccountsForRole only allows workspace manage roles', () => {
  assert.equal(canManageServiceAccountsForRole('OWNER'), true)
  assert.equal(canManageServiceAccountsForRole('ADMIN'), true)
  assert.equal(canManageServiceAccountsForRole('EDITOR'), false)
  assert.equal(canManageServiceAccountsForRole('VIEWER'), false)
})

test('shouldAutoOpenKnowledgeBaseTrash requires both route intent and manage permission', () => {
  assert.equal(shouldAutoOpenKnowledgeBaseTrash({ openKnowledgeBaseTrash: true }, 'OWNER'), true)
  assert.equal(shouldAutoOpenKnowledgeBaseTrash({ openKnowledgeBaseTrash: true }, 'EDITOR'), false)
  assert.equal(shouldAutoOpenKnowledgeBaseTrash({ openKnowledgeBaseTrash: false }, 'OWNER'), false)
  assert.equal(shouldAutoOpenKnowledgeBaseTrash(null, 'OWNER'), false)
})

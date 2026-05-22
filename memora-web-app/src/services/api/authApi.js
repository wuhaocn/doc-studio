import httpClient from '../http/axios'

export const authApi = {
  login: async (payload) => {
    return httpClient.post('/api/v1/auth/login', payload, { _skipAuthRefresh: true })
  },

  registerOwner: async (payload) => {
    return httpClient.post('/api/v1/auth/register-owner', payload, { _skipAuthRefresh: true })
  },

  getCurrentSession: async () => {
    return httpClient.get('/api/v1/auth/session')
  },

  logout: async () => {
    return httpClient.post('/api/v1/auth/logout')
  },

  refreshSession: async () => {
    return httpClient.post('/api/v1/auth/refresh', {}, { _skipAuthRefresh: true })
  },

  listSessions: async () => {
    return httpClient.get('/api/v1/auth/sessions')
  },

  listTenantSessions: async () => {
    return httpClient.get('/api/v1/auth/tenant-sessions')
  },

  revokeSession: async (sessionId) => {
    return httpClient.post(`/api/v1/auth/sessions/${sessionId}/revoke`)
  },

  revokeTenantSession: async (sessionId) => {
    return httpClient.post(`/api/v1/auth/tenant-sessions/${sessionId}/revoke`)
  },

  revokeTenantUserSessions: async (userId) => {
    return httpClient.post(`/api/v1/auth/tenant-sessions/users/${userId}/revoke`)
  },

  revokeOtherSessions: async () => {
    return httpClient.post('/api/v1/auth/sessions/revoke-others')
  },
}

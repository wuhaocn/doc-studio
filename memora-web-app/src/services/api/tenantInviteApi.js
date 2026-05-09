import httpClient from '../http/axios'

export const tenantInviteApi = {
  createInvite: async (payload) => {
    return httpClient.post('/api/v1/tenants/current/invites', payload)
  },

  listInvites: async () => {
    return httpClient.get('/api/v1/tenants/current/invites')
  },

  getInvite: async (token) => {
    return httpClient.get(`/api/v1/invites/${token}`, { _skipAuthRefresh: true })
  },

  acceptInvite: async (payload) => {
    return httpClient.post('/api/v1/invites/accept', payload, { _skipAuthRefresh: true })
  },

  revokeInvite: async (id) => {
    return httpClient.post(`/api/v1/tenants/current/invites/${id}/revoke`)
  },
}

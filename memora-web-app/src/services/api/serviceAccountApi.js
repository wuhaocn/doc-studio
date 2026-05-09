import httpClient from '../http/axios'

export const serviceAccountApi = {
  listServiceAccounts: async () => {
    return httpClient.get('/api/v1/service-accounts')
  },

  createServiceAccount: async (payload) => {
    return httpClient.post('/api/v1/service-accounts', payload)
  },

  disableApiKey: async (apiKeyId) => {
    return httpClient.post(`/api/v1/api-keys/${apiKeyId}/disable`)
  },

  revokeApiKey: async (apiKeyId) => {
    return httpClient.post(`/api/v1/api-keys/${apiKeyId}/revoke`)
  },

  rotateApiKey: async (apiKeyId, payload = {}) => {
    return httpClient.post(`/api/v1/api-keys/${apiKeyId}/rotate`, payload)
  },
}

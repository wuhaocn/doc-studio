import httpClient from '../http/axios'

export const serviceAccountApi = {
  listServiceAccounts: async () => {
    return httpClient.get('/api/v1/service-accounts')
  },

  createServiceAccount: async (payload) => {
    return httpClient.post('/api/v1/service-accounts', payload)
  },

  createApiKey: async (serviceAccountId, payload) => {
    return httpClient.post(`/api/v1/service-accounts/${serviceAccountId}/api-keys`, payload)
  },

  disableServiceAccount: async (serviceAccountId) => {
    return httpClient.post(`/api/v1/service-accounts/${serviceAccountId}/disable`)
  },

  enableServiceAccount: async (serviceAccountId) => {
    return httpClient.post(`/api/v1/service-accounts/${serviceAccountId}/enable`)
  },

  disableApiKey: async (apiKeyId) => {
    return httpClient.post(`/api/v1/api-keys/${apiKeyId}/disable`)
  },

  revealApiKey: async (apiKeyId) => {
    return httpClient.post(`/api/v1/api-keys/${apiKeyId}/reveal`)
  },

  revokeApiKey: async (apiKeyId) => {
    return httpClient.post(`/api/v1/api-keys/${apiKeyId}/revoke`)
  },

  updateApiKeyScope: async (apiKeyId, payload) => {
    return httpClient.put(`/api/v1/api-keys/${apiKeyId}/scope`, payload)
  },

  rotateApiKey: async (apiKeyId, payload = {}) => {
    return httpClient.post(`/api/v1/api-keys/${apiKeyId}/rotate`, payload)
  },
}

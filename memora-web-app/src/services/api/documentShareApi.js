import httpClient from '../http/axios'

export const documentShareApi = {
  createShare: async (payload) => {
    return httpClient.post('/api/v1/document-shares', payload)
  },

  listShares: async (documentId) => {
    return httpClient.get(`/api/v1/documents/${documentId}/shares`)
  },

  revokeShare: async (shareId) => {
    return httpClient.post(`/api/v1/document-shares/${shareId}/revoke`)
  },
}

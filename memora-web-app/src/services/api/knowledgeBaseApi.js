import httpClient from '../http/axios'

export const knowledgeBaseApi = {
  getKnowledgeBases: async (params = {}) => {
    const { page = 1, size = 20, keyword, tenantId, userId } = params
    return httpClient.get('/api/v1/knowledge-bases', {
      params: { page, size, keyword, tenantId, userId },
    })
  },

  getKnowledgeBasesByTenantId: async (tenantId) => {
    return httpClient.get(`/api/v1/knowledge-bases/tenant/${tenantId}`)
  },

  getKnowledgeBasesByUserId: async (userId) => {
    return httpClient.get(`/api/v1/knowledge-bases/user/${userId}`)
  },

  getKnowledgeBaseById: async (id) => {
    return httpClient.get(`/api/v1/knowledge-bases/${id}`)
  },

  getKnowledgeBaseMembers: async (id) => {
    return httpClient.get(`/api/v1/knowledge-bases/${id}/members`)
  },

  updateKnowledgeBaseMembers: async (id, members = []) => {
    return httpClient.put(`/api/v1/knowledge-bases/${id}/members`, { members })
  },

  createKnowledgeBase: async (data) => {
    return httpClient.post('/api/v1/knowledge-bases', data)
  },

  updateKnowledgeBase: async (id, data) => {
    return httpClient.put(`/api/v1/knowledge-bases/${id}`, data)
  },

  deleteKnowledgeBase: async (id) => {
    return httpClient.delete(`/api/v1/knowledge-bases/${id}`)
  },

  getDeletedKnowledgeBases: async () => {
    return httpClient.get('/api/v1/knowledge-bases/trash')
  },

  restoreKnowledgeBase: async (id) => {
    return httpClient.post(`/api/v1/knowledge-bases/${id}/restore`)
  },

  getDocumentsByKnowledgeBaseId: async (id, params = {}) => {
    const { parentId, page, size, keyword } = params
    return httpClient.get(`/api/v1/knowledge-bases/${id}/documents`, {
      params: { parentId, page, size, keyword },
    })
  },

  getDocumentTree: async (id) => {
    return httpClient.get(`/api/v1/knowledge-bases/${id}/document-tree`)
  },
}

import httpClient from '../http/axios'

// 文档API服务
export const documentApi = {
  getDocuments: async (params = {}) => {
    const { page = 1, size = 20, keyword, knowledgeBaseId, parentId, userId } = params
    return httpClient.get('/api/v1/documents', {
      params: { page, size, keyword, knowledgeBaseId, parentId, userId },
    })
  },

  searchDocuments: async (keyword, options = {}) => {
    const { page = 1, size = 50 } = options
    return httpClient.get('/api/v1/documents', {
      params: { page, size, keyword },
    })
  },

  getDocumentsByKnowledgeBaseId: async (kbId, parentId = null) => {
    return httpClient.get(`/api/v1/documents/knowledge-base/${kbId}`, {
      params: { parentId },
    })
  },

  getDocumentTreeByKnowledgeBaseId: async (kbId) => {
    return httpClient.get(`/api/v1/documents/knowledge-base/${kbId}/tree`)
  },

  getDocumentById: async (id) => {
    return httpClient.get(`/api/v1/documents/${id}`)
  },

  createDocument: async (data) => {
    return httpClient.post('/api/v1/documents', {
      ...data,
      parentId: data.parentId || 0,
    })
  },

  updateDocument: async (id, data) => {
    return httpClient.put(`/api/v1/documents/${id}`, data)
  },

  deleteDocument: async (id) => {
    return httpClient.delete(`/api/v1/documents/${id}`)
  },

  getDeletedDocuments: async (params = {}) => {
    const { knowledgeBaseId } = params
    return httpClient.get('/api/v1/documents/trash', {
      params: { knowledgeBaseId },
    })
  },

  restoreDocument: async (id) => {
    return httpClient.post(`/api/v1/documents/${id}/restore`)
  },

  batchMoveDocuments: async (documentIds = [], parentId = 0) => {
    return httpClient.post('/api/v1/documents/batch-move', {
      documentIds,
      parentId,
    })
  },

  batchDeleteDocuments: async (documentIds = []) => {
    return httpClient.post('/api/v1/documents/batch-delete', {
      documentIds,
    })
  },

  updateSortOrder: async (sortList) => {
    return httpClient.put('/api/v1/documents/sort', sortList)
  },

  getVersions: async (id) => {
    return httpClient.get(`/api/v1/documents/${id}/versions`)
  },

  getVersionById: async (versionId) => {
    return httpClient.get(`/api/v1/documents/versions/${versionId}`)
  },

  rollbackToVersion: async (id, versionId) => {
    return httpClient.post(`/api/v1/documents/${id}/rollback/${versionId}`)
  },
}

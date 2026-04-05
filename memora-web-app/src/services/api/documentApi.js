import httpClient from '../http/axios'

// 文档API服务
export const documentApi = {
  // 获取文档列表（分页）
  getDocuments: async (params = {}) => {
    const { page = 1, size = 20, keyword, knowledgeBaseId, parentId, userId } = params
    const response = await httpClient.get('/api/v1/documents', {
      params: { page, size, keyword, knowledgeBaseId, parentId, userId },
    })
    return response
  },

  // 获取知识库下的文档列表（不分页）
  getDocumentsByKnowledgeBaseId: async (kbId, parentId = null) => {
    const response = await httpClient.get(`/api/v1/documents/knowledge-base/${kbId}`, {
      params: { parentId },
    })
    return response
  },

  // 获取文档详情
  getDocumentById: async (id) => {
    const response = await httpClient.get(`/api/v1/documents/${id}`)
    return response
  },

  // 创建文档
  createDocument: async (data) => {
    // TODO: 后续从鉴权中获取userId，暂时使用固定值
    const requestData = {
      ...data,
      userId: data.userId || 1,
      parentId: data.parentId || 0,
    }
    const response = await httpClient.post('/api/v1/documents', requestData)
    return response
  },

  // 更新文档
  updateDocument: async (id, data) => {
    const response = await httpClient.put(`/api/v1/documents/${id}`, data)
    return response
  },

  // 删除文档
  deleteDocument: async (id) => {
    const response = await httpClient.delete(`/api/v1/documents/${id}`)
    return response
  },

  // 更新文档排序
  updateSortOrder: async (sortList) => {
    const response = await httpClient.put('/api/v1/documents/sort', sortList)
    return response
  },

  // 获取文档版本列表
  getVersions: async (id) => {
    const response = await httpClient.get(`/api/v1/documents/${id}/versions`)
    return response
  },

  // 获取版本详情
  getVersionById: async (versionId) => {
    const response = await httpClient.get(`/api/v1/documents/versions/${versionId}`)
    return response
  },

  // 回滚到指定版本
  rollbackToVersion: async (id, versionId) => {
    const response = await httpClient.post(`/api/v1/documents/${id}/rollback/${versionId}`)
    return response
  },

  // 高级搜索文档
  advancedSearch: async (params = {}, searchRequest) => {
    const { page = 1, size = 20 } = params
    const response = await httpClient.post(`/api/v1/documents/search`, searchRequest, {
      params: { page, size },
    })
    return response
  },
}


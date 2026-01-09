import httpClient from '../http/axios'

// 文档API服务
export const documentApi = {
  // 获取文档列表（分页）
  getDocuments: async (params = {}) => {
    const { page = 1, size = 20, keyword, knowledgeBaseId, parentId, userId } = params
    const response = await httpClient.get('/documents', {
      params: { page, size, keyword, knowledgeBaseId, parentId, userId },
    })
    return response
  },

  // 获取知识库下的文档列表（不分页）
  getDocumentsByKnowledgeBaseId: async (kbId, parentId = null) => {
    const response = await httpClient.get(`/documents/knowledge-base/${kbId}`, {
      params: { parentId },
    })
    return response
  },

  // 获取文档详情
  getDocumentById: async (id) => {
    const response = await httpClient.get(`/documents/${id}`)
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
    const response = await httpClient.post('/documents', requestData)
    return response
  },

  // 更新文档
  updateDocument: async (id, data) => {
    const response = await httpClient.put(`/documents/${id}`, data)
    return response
  },

  // 删除文档
  deleteDocument: async (id) => {
    const response = await httpClient.delete(`/documents/${id}`)
    return response
  },
}


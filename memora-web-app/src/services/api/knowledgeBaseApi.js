import httpClient from '../http/axios'

// 知识库API服务
export const knowledgeBaseApi = {
  // 获取知识库列表
  getKnowledgeBases: async (params = {}) => {
    const { page = 1, size = 20, keyword, userId } = params
    const response = await httpClient.get('/api/v1/knowledge-bases', {
      params: { page, size, keyword, userId },
    })
    return response
  },

  // 获取用户的知识库列表（不分页）
  getKnowledgeBasesByUserId: async (userId) => {
    const response = await httpClient.get(`/api/v1/knowledge-bases/user/${userId}`)
    return response
  },

  // 获取知识库详情
  getKnowledgeBaseById: async (id) => {
    const response = await httpClient.get(`/api/v1/knowledge-bases/${id}`)
    return response
  },

  // 创建知识库
  createKnowledgeBase: async (data) => {
    // TODO: 后续从鉴权中获取userId，暂时使用固定值
    const requestData = {
      ...data,
      userId: data.userId || 1,
    }
    const response = await httpClient.post('/api/v1/knowledge-bases', requestData)
    return response
  },

  // 更新知识库
  updateKnowledgeBase: async (id, data) => {
    const response = await httpClient.put(`/api/v1/knowledge-bases/${id}`, data)
    return response
  },

  // 删除知识库
  deleteKnowledgeBase: async (id) => {
    const response = await httpClient.delete(`/api/v1/knowledge-bases/${id}`)
    return response
  },

  // 获取知识库下的文档列表
  getDocumentsByKnowledgeBaseId: async (id, params = {}) => {
    const { parentId, page, size, keyword } = params
    const response = await httpClient.get(`/api/v1/knowledge-bases/${id}/documents`, {
      params: { parentId, page, size, keyword },
    })
    return response
  },

  // 获取知识库统计信息
  getStats: async (userId) => {
    const response = await httpClient.get('/api/v1/knowledge-bases/stats', {
      params: { userId },
    })
    return response
  },

  // 生成知识库分享链接
  generateShareLink: async (id, expireDays = 7) => {
    const response = await httpClient.post(`/api/v1/knowledge-bases/${id}/share`, null, {
      params: { expireDays },
    })
    return response
  },

  // 根据分享token获取知识库
  getByShareToken: async (token) => {
    const response = await httpClient.get('/api/v1/knowledge-bases/shared', {
      params: { token },
    })
    return response
  },

  // 设置知识库公开状态
  setPublicStatus: async (id, isPublic) => {
    const response = await httpClient.put(`/api/v1/knowledge-bases/${id}/public-status`, null, {
      params: { isPublic },
    })
    return response
  },
}


import httpClient from '../http/axios'

// 资源API服务
export const resourceApi = {
  // 获取资源列表（分页）
  getResources: async (params = {}) => {
    const { page = 1, size = 20, keyword, type, tagId } = params
    const response = await httpClient.get('/api/v1/resources', {
      params: { page, size, keyword, type, tagId },
    })
    return response
  },

  // 获取资源详情
  getResourceById: async (id) => {
    const response = await httpClient.get(`/api/v1/resources/${id}`)
    return response
  },

  // 创建资源
  createResource: async (data) => {
    // TODO: 后续从鉴权中获取userId，暂时使用固定值
    const requestData = {
      ...data,
      userId: data.userId || 1,
    }
    const response = await httpClient.post('/api/v1/resources', requestData)
    return response
  },

  // 更新资源
  updateResource: async (id, data) => {
    const response = await httpClient.put(`/api/v1/resources/${id}`, data)
    return response
  },

  // 删除资源
  deleteResource: async (id) => {
    const response = await httpClient.delete(`/api/v1/resources/${id}`)
    return response
  },

  // 按类型获取资源
  getResourcesByType: async (type) => {
    const response = await httpClient.get(`/api/v1/resources/type/${type}`)
    return response
  },

  // 搜索资源
  searchResources: async (params = {}) => {
    const { page = 1, size = 20, keyword, type, tagId } = params
    const response = await httpClient.get('/api/v1/resources/search', {
      params: { page, size, keyword, type, tagId },
    })
    return response
  },

  // 获取资源标签
  getResourceTags: async (resourceId) => {
    const response = await httpClient.get(`/api/v1/tags/resource/${resourceId}`)
    return response
  },

  // 关联资源到文档
  associateWithDocument: async (resourceId, documentId) => {
    const response = await httpClient.post(`/api/v1/resources/${resourceId}/associate/${documentId}`)
    return response
  },
}
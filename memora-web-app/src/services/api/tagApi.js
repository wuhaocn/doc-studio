import httpClient from '../http/axios'

// 标签API服务
export const tagApi = {
  // 获取标签列表
  getTags: async (keyword = '') => {
    const response = await httpClient.get('/api/v1/tags', {
      params: { keyword },
    })
    return response
  },

  // 获取标签详情
  getTagById: async (id) => {
    const response = await httpClient.get(`/api/v1/tags/${id}`)
    return response
  },

  // 创建标签
  createTag: async (data) => {
    // TODO: 后续从鉴权中获取userId，暂时使用固定值
    const requestData = {
      ...data,
      userId: data.userId || 1,
    }
    const response = await httpClient.post('/api/v1/tags', requestData)
    return response
  },

  // 更新标签
  updateTag: async (id, data) => {
    const response = await httpClient.put(`/api/v1/tags/${id}`, data)
    return response
  },

  // 删除标签
  deleteTag: async (id) => {
    const response = await httpClient.delete(`/api/v1/tags/${id}`)
    return response
  },

  // 获取资源的标签
  getTagsByResourceId: async (resourceId) => {
    const response = await httpClient.get(`/api/v1/tags/resource/${resourceId}`)
    return response
  },

  // 为资源添加标签
  addTagToResource: async (resourceId, tagId) => {
    const response = await httpClient.post(`/api/v1/tags/resource/${resourceId}/add/${tagId}`)
    return response
  },

  // 从资源移除标签
  removeTagFromResource: async (resourceId, tagId) => {
    const response = await httpClient.delete(`/api/v1/tags/resource/${resourceId}/remove/${tagId}`)
    return response
  },
}
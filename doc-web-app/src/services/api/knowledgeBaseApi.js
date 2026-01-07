import { mockApi } from '../mock/mockData'

// 知识库API服务（使用mock数据）
export const knowledgeBaseApi = {
  // 获取知识库列表
  getKnowledgeBases: async () => {
    return await mockApi.getKnowledgeBases()
  },

  // 获取知识库详情
  getKnowledgeBaseById: async (id) => {
    return await mockApi.getKnowledgeBaseById(id)
  },

  // 创建知识库
  createKnowledgeBase: async (data) => {
    return await mockApi.createKnowledgeBase(data)
  },

  // 更新知识库
  updateKnowledgeBase: async (id, data) => {
    // TODO: 实现更新逻辑
    return Promise.resolve({ code: 200, data: null, message: 'success' })
  },

  // 删除知识库
  deleteKnowledgeBase: async (id) => {
    // TODO: 实现删除逻辑
    return Promise.resolve({ code: 200, data: null, message: 'success' })
  },
}


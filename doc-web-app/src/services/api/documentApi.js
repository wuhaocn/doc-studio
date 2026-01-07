import { mockApi } from '../mock/mockData'

// 文档API服务（使用mock数据）
export const documentApi = {
  // 获取知识库下的文档列表
  getDocumentsByKnowledgeBaseId: async (kbId) => {
    return await mockApi.getDocumentsByKnowledgeBaseId(kbId)
  },

  // 获取文档详情
  getDocumentById: async (id) => {
    return await mockApi.getDocumentById(id)
  },

  // 创建文档
  createDocument: async (data) => {
    return await mockApi.createDocument(data)
  },

  // 更新文档
  updateDocument: async (id, data) => {
    return await mockApi.updateDocument(id, data)
  },

  // 删除文档
  deleteDocument: async (id) => {
    // TODO: 实现删除逻辑
    return Promise.resolve({ code: 200, data: null, message: 'success' })
  },
}


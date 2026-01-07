// Mock数据服务

// 模拟知识库数据
export const mockKnowledgeBases = [
  {
    id: '1',
    name: '产品文档',
    description: '产品相关的所有文档和资料',
    cover: null,
    userId: '1',
    status: 1,
    isPublic: false,
    documentCount: 5,
    viewCount: 120,
    sortOrder: 0,
    createdAt: '2024-01-15T10:00:00Z',
    updatedAt: '2024-01-20T15:30:00Z',
  },
  {
    id: '2',
    name: '技术文档',
    description: '技术开发相关的文档和规范',
    cover: null,
    userId: '1',
    status: 1,
    isPublic: false,
    documentCount: 8,
    viewCount: 256,
    sortOrder: 1,
    createdAt: '2024-01-10T09:00:00Z',
    updatedAt: '2024-01-22T11:20:00Z',
  },
  {
    id: '3',
    name: '团队协作',
    description: '团队协作和会议记录',
    cover: null,
    userId: '1',
    status: 1,
    isPublic: true,
    documentCount: 3,
    viewCount: 89,
    sortOrder: 2,
    createdAt: '2024-01-08T14:00:00Z',
    updatedAt: '2024-01-18T16:45:00Z',
  },
]

// 模拟文档数据
export const mockDocuments = {
  '1': [
    {
      id: '1-1',
      title: '产品需求文档',
      content: '<h1>产品需求文档</h1><p>这是产品需求文档的内容...</p>',
      contentText: '产品需求文档 这是产品需求文档的内容...',
      knowledgeBaseId: '1',
      userId: '1',
      parentId: '0',
      status: 1,
      isPublic: false,
      viewCount: 45,
      sortOrder: 0,
      createdAt: '2024-01-15T10:30:00Z',
      updatedAt: '2024-01-20T15:30:00Z',
    },
    {
      id: '1-2',
      title: '用户调研报告',
      content: '<h1>用户调研报告</h1><p>用户调研的详细内容...</p>',
      contentText: '用户调研报告 用户调研的详细内容...',
      knowledgeBaseId: '1',
      userId: '1',
      parentId: '0',
      status: 1,
      isPublic: false,
      viewCount: 32,
      sortOrder: 1,
      createdAt: '2024-01-16T11:00:00Z',
      updatedAt: '2024-01-19T14:20:00Z',
    },
    {
      id: '1-3',
      title: '产品规划',
      content: '<h1>产品规划</h1><p>产品规划相关内容...</p>',
      contentText: '产品规划 产品规划相关内容...',
      knowledgeBaseId: '1',
      userId: '1',
      parentId: '0',
      status: 1,
      isPublic: false,
      viewCount: 28,
      sortOrder: 2,
      createdAt: '2024-01-17T09:15:00Z',
      updatedAt: '2024-01-18T10:30:00Z',
    },
  ],
  '2': [
    {
      id: '2-1',
      title: 'API接口文档',
      content: '<h1>API接口文档</h1><p>API接口的详细说明...</p>',
      contentText: 'API接口文档 API接口的详细说明...',
      knowledgeBaseId: '2',
      userId: '1',
      parentId: '0',
      status: 1,
      isPublic: false,
      viewCount: 156,
      sortOrder: 0,
      createdAt: '2024-01-10T09:30:00Z',
      updatedAt: '2024-01-22T11:20:00Z',
    },
    {
      id: '2-2',
      title: '开发规范',
      content: '<h1>开发规范</h1><p>代码规范和开发流程...</p>',
      contentText: '开发规范 代码规范和开发流程...',
      knowledgeBaseId: '2',
      userId: '1',
      parentId: '0',
      status: 1,
      isPublic: false,
      viewCount: 98,
      sortOrder: 1,
      createdAt: '2024-01-11T10:00:00Z',
      updatedAt: '2024-01-21T09:15:00Z',
    },
  ],
  '3': [
    {
      id: '3-1',
      title: '周会记录',
      content: '<h1>周会记录</h1><p>本周会议内容...</p>',
      contentText: '周会记录 本周会议内容...',
      knowledgeBaseId: '3',
      userId: '1',
      parentId: '0',
      status: 1,
      isPublic: true,
      viewCount: 45,
      sortOrder: 0,
      createdAt: '2024-01-08T14:30:00Z',
      updatedAt: '2024-01-18T16:45:00Z',
    },
  ],
}

// Mock API服务
export const mockApi = {
  // 获取知识库列表
  getKnowledgeBases: () => {
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve({
          code: 200,
          data: mockKnowledgeBases,
          message: 'success',
        })
      }, 300)
    })
  },

  // 获取知识库详情
  getKnowledgeBaseById: (id) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const kb = mockKnowledgeBases.find((item) => item.id === id)
        resolve({
          code: 200,
          data: kb,
          message: 'success',
        })
      }, 200)
    })
  },

  // 获取知识库下的文档列表
  getDocumentsByKnowledgeBaseId: (kbId) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const documents = mockDocuments[kbId] || []
        resolve({
          code: 200,
          data: documents,
          message: 'success',
        })
      }, 200)
    })
  },

  // 获取文档详情
  getDocumentById: (id) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        // 在所有知识库中查找文档
        let document = null
        for (const kbId in mockDocuments) {
          const doc = mockDocuments[kbId].find((item) => item.id === id)
          if (doc) {
            document = doc
            break
          }
        }
        resolve({
          code: 200,
          data: document,
          message: 'success',
        })
      }, 200)
    })
  },

  // 创建文档
  createDocument: (data) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const newDoc = {
          id: `${data.knowledgeBaseId}-${Date.now()}`,
          title: data.title || '未命名文档',
          content: data.content || '<p></p>',
          contentText: data.contentText || '',
          knowledgeBaseId: data.knowledgeBaseId,
          userId: '1',
          parentId: data.parentId || '0',
          status: 1,
          isPublic: false,
          viewCount: 0,
          sortOrder: 0,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
        if (!mockDocuments[data.knowledgeBaseId]) {
          mockDocuments[data.knowledgeBaseId] = []
        }
        mockDocuments[data.knowledgeBaseId].push(newDoc)
        resolve({
          code: 200,
          data: newDoc,
          message: 'success',
        })
      }, 300)
    })
  },

  // 更新文档
  updateDocument: (id, data) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        let document = null
        for (const kbId in mockDocuments) {
          const index = mockDocuments[kbId].findIndex((item) => item.id === id)
          if (index !== -1) {
            document = {
              ...mockDocuments[kbId][index],
              ...data,
              updatedAt: new Date().toISOString(),
            }
            mockDocuments[kbId][index] = document
            break
          }
        }
        resolve({
          code: 200,
          data: document,
          message: 'success',
        })
      }, 300)
    })
  },

  // 创建知识库
  createKnowledgeBase: (data) => {
    return new Promise((resolve) => {
      setTimeout(() => {
        const newKb = {
          id: String(Date.now()),
          name: data.name || '未命名知识库',
          description: data.description || '',
          cover: data.cover || null,
          userId: '1',
          status: 1,
          isPublic: false,
          documentCount: 0,
          viewCount: 0,
          sortOrder: mockKnowledgeBases.length,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
        }
        mockKnowledgeBases.push(newKb)
        mockDocuments[newKb.id] = []
        resolve({
          code: 200,
          data: newKb,
          message: 'success',
        })
      }, 300)
    })
  },
}


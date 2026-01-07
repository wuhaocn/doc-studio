// Mock AI Chat API
// 在实际项目中，这里应该调用真实的大模型API

const mockAIResponse = (message, documentContent) => {
  // 模拟AI响应延迟
  const delay = Math.random() * 1000 + 500

  return new Promise((resolve) => {
    setTimeout(() => {
      let response = ''

      // 简单的关键词匹配，模拟AI回复
      const lowerMessage = message.toLowerCase()

      if (lowerMessage.includes('你好') || lowerMessage.includes('hello')) {
        response = '你好！我是AI助手，很高兴为你服务。我可以帮助你：\n• 回答关于文档的问题\n• 生成内容建议\n• 优化文档结构\n• 翻译和改写文本\n\n请告诉我你需要什么帮助？'
      } else if (lowerMessage.includes('总结') || lowerMessage.includes('摘要')) {
        if (documentContent) {
          const preview = documentContent.substring(0, 200)
          response = `根据文档内容，我为你总结如下：\n\n${preview}...\n\n这是一个简短的摘要。如果需要更详细的总结，请提供更多文档内容。`
        } else {
          response = '请先提供需要总结的文档内容。'
        }
      } else if (lowerMessage.includes('优化') || lowerMessage.includes('改进')) {
        response = '以下是一些文档优化建议：\n\n• 确保标题清晰明确\n• 使用段落分隔不同主题\n• 添加适当的标题层级（H1, H2, H3）\n• 使用列表来组织信息\n• 添加图表和示例来说明复杂概念\n• 保持语言简洁明了\n\n需要我帮你优化文档的某个部分吗？'
      } else if (lowerMessage.includes('翻译') || lowerMessage.includes('translate')) {
        response = '我可以帮你翻译文本。请告诉我：\n• 需要翻译的内容\n• 目标语言（如：英文、日文等）\n\n例如："请将这段文字翻译成英文：..."'
      } else if (lowerMessage.includes('生成') || lowerMessage.includes('写')) {
        if (lowerMessage.includes('标题')) {
          response = '以下是一些标题建议：\n\n• 如何开始使用文档系统\n• 文档编辑最佳实践\n• 团队协作指南\n• 常见问题解答\n\n需要我根据你的主题生成更具体的标题吗？'
        } else if (lowerMessage.includes('大纲') || lowerMessage.includes('目录')) {
          response = '以下是一个文档大纲示例：\n\n1. 引言\n   1.1 背景\n   1.2 目标\n2. 主要内容\n   2.1 核心概念\n   2.2 实施步骤\n3. 总结\n   3.1 要点回顾\n   3.2 下一步行动\n\n需要我根据你的主题生成更具体的大纲吗？'
        } else {
          response = '我可以帮你生成内容。请告诉我：\n• 需要生成的内容类型（如：段落、列表、代码示例等）\n• 主题或关键词\n\n例如："请生成一段关于React的介绍"'
        }
      } else if (lowerMessage.includes('代码') || lowerMessage.includes('code')) {
        response = '我可以帮你生成代码示例。请告诉我：\n• 编程语言\n• 功能需求\n\n例如："请生成一个React组件的示例代码"'
      } else {
        // 默认回复
        response = `我理解你想了解"${message}"。\n\n作为AI助手，我可以帮助你：\n• 回答关于文档的问题\n• 生成和优化内容\n• 提供写作建议\n• 翻译文本\n• 生成代码示例\n\n请告诉我更具体的需求，我会尽力帮助你！`
      }

      resolve({
        code: 200,
        data: {
          content: response,
        },
        message: 'success',
      })
    }, delay)
  })
}

export const aiChatApi = {
  /**
   * 发送聊天消息
   * @param {Object} params
   * @param {string} params.message - 用户消息
   * @param {string} params.documentContent - 当前文档内容（可选）
   * @returns {Promise}
   */
  chat: async ({ message, documentContent = '' }) => {
    // 在实际项目中，这里应该调用真实的大模型API
    // 例如：OpenAI API, Claude API, 或其他大模型服务
    
    try {
      // Mock实现
      const response = await mockAIResponse(message, documentContent)
      return response
    } catch (error) {
      console.error('AI Chat API Error:', error)
      throw error
    }
  },

  /**
   * 流式聊天（用于实时响应）
   * @param {Object} params
   * @param {string} params.message - 用户消息
   * @param {string} params.documentContent - 当前文档内容（可选）
   * @param {Function} params.onChunk - 接收数据块的回调函数
   * @returns {Promise}
   */
  chatStream: async ({ message, documentContent = '', onChunk }) => {
    // 在实际项目中，这里应该调用支持流式响应的大模型API
    // 例如：OpenAI Stream API
    
    try {
      const response = await mockAIResponse(message, documentContent)
      const content = response.data?.content || ''
      
      // 模拟流式输出
      const words = content.split('')
      for (let i = 0; i < words.length; i++) {
        await new Promise((resolve) => setTimeout(resolve, 50))
        if (onChunk) {
          onChunk(words[i])
        }
      }
      
      return response
    } catch (error) {
      console.error('AI Chat Stream API Error:', error)
      throw error
    }
  },
}


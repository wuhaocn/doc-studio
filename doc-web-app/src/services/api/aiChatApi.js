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
        if (lowerMessage.includes('流程图') || lowerMessage.includes('flowchart') || lowerMessage.includes('mermaid')) {
          // 生成 Mermaid 流程图
          let mermaidCode = ''
          if (lowerMessage.includes('登录') || lowerMessage.includes('登录流程')) {
            mermaidCode = `graph TD
    A[用户访问] --> B{是否已登录?}
    B -->|否| C[显示登录页面]
    B -->|是| D[进入主页]
    C --> E[输入用户名密码]
    E --> F{验证成功?}
    F -->|是| G[保存登录状态]
    F -->|否| H[显示错误信息]
    G --> D
    H --> C`
          } else if (lowerMessage.includes('注册') || lowerMessage.includes('注册流程')) {
            mermaidCode = `graph TD
    A[用户点击注册] --> B[填写注册信息]
    B --> C{信息是否完整?}
    C -->|否| D[提示补充信息]
    C -->|是| E[验证邮箱]
    D --> B
    E --> F{邮箱验证成功?}
    F -->|是| G[创建账户]
    F -->|否| H[提示验证失败]
    G --> I[注册成功]
    H --> E`
          } else if (lowerMessage.includes('订单') || lowerMessage.includes('下单')) {
            mermaidCode = `graph TD
    A[用户选择商品] --> B[加入购物车]
    B --> C[进入结算页面]
    C --> D[填写收货信息]
    D --> E[选择支付方式]
    E --> F[提交订单]
    F --> G{支付成功?}
    G -->|是| H[订单确认]
    G -->|否| I[支付失败]
    H --> J[开始发货]
    I --> E`
          } else {
            // 默认流程图
            mermaidCode = `graph TD
    A[开始] --> B{判断条件}
    B -->|条件1| C[处理A]
    B -->|条件2| D[处理B]
    C --> E[结果A]
    D --> F[结果B]
    E --> G[结束]
    F --> G`
          }
          response = `我为你生成了一个流程图，请使用以下 Mermaid 代码：\n\n\`\`\`mermaid\n${mermaidCode}\n\`\`\`\n\n你可以点击"插入流程图"按钮将这段代码插入到文档中。`
        } else if (lowerMessage.includes('标题')) {
          response = '以下是一些标题建议：\n\n• 如何开始使用文档系统\n• 文档编辑最佳实践\n• 团队协作指南\n• 常见问题解答\n\n需要我根据你的主题生成更具体的标题吗？'
        } else if (lowerMessage.includes('大纲') || lowerMessage.includes('目录')) {
          response = '以下是一个文档大纲示例：\n\n1. 引言\n   1.1 背景\n   1.2 目标\n2. 主要内容\n   2.1 核心概念\n   2.2 实施步骤\n3. 总结\n   3.1 要点回顾\n   3.2 下一步行动\n\n需要我根据你的主题生成更具体的大纲吗？'
        } else {
          response = '我可以帮你生成内容。请告诉我：\n• 需要生成的内容类型（如：段落、列表、代码示例、流程图等）\n• 主题或关键词\n\n例如："请生成一个登录流程的流程图"或"请生成一段关于React的介绍"'
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


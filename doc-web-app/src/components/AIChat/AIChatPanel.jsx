import { useState, useRef, useEffect } from 'react'
import { Input, Button, Avatar, Spin, Message } from '@arco-design/web-react'
import { IconSend, IconClose, IconRobot, IconUser } from '@arco-design/web-react/icon'
import { aiChatApi } from '../../services/api/aiChatApi'
import styles from './AIChatPanel.module.css'

const AIChatPanel = ({ visible, onClose, onInsertText, documentContent }) => {
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      role: 'assistant',
      content: '你好！我是AI助手，可以帮助你：\n• 回答关于文档的问题\n• 生成内容建议\n• 优化文档结构\n• 翻译和改写文本\n\n请告诉我你需要什么帮助？',
      timestamp: new Date(),
    },
  ])
  const [inputValue, setInputValue] = useState('')
  const [loading, setLoading] = useState(false)
  const messagesEndRef = useRef(null)
  const inputRef = useRef(null)

  // 滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  useEffect(() => {
    if (visible && inputRef.current) {
      setTimeout(() => {
        inputRef.current?.focus()
      }, 100)
    }
  }, [visible])

  const handleSend = async () => {
    if (!inputValue.trim() || loading) return

    const userMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: inputValue.trim(),
      timestamp: new Date(),
    }

    setMessages((prev) => [...prev, userMessage])
    setInputValue('')
    setLoading(true)

    try {
      const response = await aiChatApi.chat({
        message: userMessage.content,
        documentContent: documentContent || '',
      })

      const assistantMessage = {
        id: `assistant-${Date.now()}`,
        role: 'assistant',
        content: response.data?.content || response.content || '抱歉，我暂时无法回答这个问题。',
        timestamp: new Date(),
      }

      setMessages((prev) => [...prev, assistantMessage])
    } catch (error) {
      console.error('AI对话失败:', error)
      Message.error('AI对话失败，请稍后重试')
      
      const errorMessage = {
        id: `error-${Date.now()}`,
        role: 'assistant',
        content: '抱歉，发生了错误，请稍后重试。',
        timestamp: new Date(),
      }
      setMessages((prev) => [...prev, errorMessage])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleInsertToDocument = (content) => {
    if (onInsertText) {
      onInsertText(content)
      Message.success('内容已插入到文档')
    }
  }

  const formatMessage = (content) => {
    // 简单的markdown格式化
    return content
      .split('\n')
      .map((line, index) => {
        if (line.trim().startsWith('•') || line.trim().startsWith('-')) {
          return <div key={index} className={styles.listItem}>{line}</div>
        }
        return <div key={index}>{line || <br />}</div>
      })
  }

  return (
    <div className={`${styles.panel} ${visible ? styles.visible : ''}`}>
      <div className={styles.header}>
        <div className={styles.headerLeft}>
          <Avatar size={24} style={{ backgroundColor: 'var(--color-primary)' }}>
            <IconRobot />
          </Avatar>
          <span className={styles.headerTitle}>AI 助手</span>
        </div>
        <Button
          type="text"
          size="small"
          icon={<IconClose />}
          onClick={onClose}
          className={styles.closeButton}
        />
      </div>

      <div className={styles.messages}>
        {messages.map((message) => (
          <div
            key={message.id}
            className={`${styles.message} ${
              message.role === 'user' ? styles.userMessage : styles.assistantMessage
            }`}
          >
            <div className={styles.messageAvatar}>
              {message.role === 'user' ? (
                <Avatar size={32} style={{ backgroundColor: 'var(--color-primary-light-1)' }}>
                  <IconUser />
                </Avatar>
              ) : (
                <Avatar size={32} style={{ backgroundColor: 'var(--color-primary)' }}>
                  <IconRobot />
                </Avatar>
              )}
            </div>
            <div className={styles.messageContent}>
              <div className={styles.messageText}>{formatMessage(message.content)}</div>
              {message.role === 'assistant' && (
                <Button
                  type="text"
                  size="mini"
                  className={styles.insertButton}
                  onClick={() => handleInsertToDocument(message.content)}
                >
                  插入到文档
                </Button>
              )}
            </div>
          </div>
        ))}
        {loading && (
          <div className={`${styles.message} ${styles.assistantMessage}`}>
            <div className={styles.messageAvatar}>
              <Avatar size={32} style={{ backgroundColor: 'var(--color-primary)' }}>
                <IconRobot />
              </Avatar>
            </div>
            <div className={styles.messageContent}>
              <Spin size="small" />
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className={styles.inputArea}>
        <Input.TextArea
          ref={inputRef}
          value={inputValue}
          onChange={setInputValue}
          onKeyPress={handleKeyPress}
          placeholder="输入消息... (Shift+Enter 换行)"
          autoSize={{ minRows: 1, maxRows: 4 }}
          className={styles.input}
          disabled={loading}
        />
        <Button
          type="primary"
          icon={<IconSend />}
          onClick={handleSend}
          loading={loading}
          disabled={!inputValue.trim()}
          className={styles.sendButton}
        >
          发送
        </Button>
      </div>
    </div>
  )
}

export default AIChatPanel


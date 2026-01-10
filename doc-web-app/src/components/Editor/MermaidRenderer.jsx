import { useEffect, useRef, useState } from 'react'
import mermaid from 'mermaid'
import styles from './MermaidRenderer.module.css'

// 全局初始化 Mermaid（只初始化一次）
let mermaidInitialized = false

const initializeMermaid = async () => {
  if (!mermaidInitialized) {
    try {
      mermaid.initialize({
        startOnLoad: false,
        theme: 'default',
        securityLevel: 'loose',
        flowchart: {
          useMaxWidth: true,
          htmlLabels: true,
          curve: 'basis',
        },
        themeVariables: {
          primaryColor: '#4a90e2',
          primaryTextColor: '#333',
          primaryBorderColor: '#4a90e2',
          lineColor: '#333',
          secondaryColor: '#f0f0f0',
          tertiaryColor: '#fff',
        },
      })
      mermaidInitialized = true
      console.log('Mermaid 初始化成功')
    } catch (error) {
      console.error('Mermaid 初始化失败:', error)
      throw error
    }
  }
}

const MermaidRenderer = ({ content }) => {
  const containerRef = useRef(null)
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(false)
  const renderedContentRef = useRef('')
  const renderKeyRef = useRef(0)
  const isRenderingRef = useRef(false)
  const initPromiseRef = useRef(null)

  // 初始化 Mermaid
  useEffect(() => {
    if (!initPromiseRef.current) {
      initPromiseRef.current = initializeMermaid()
    }
  }, [])

  useEffect(() => {
    const renderDiagram = async () => {
      // 等待初始化完成
      if (initPromiseRef.current) {
        try {
          await initPromiseRef.current
        } catch (err) {
          console.error('等待 Mermaid 初始化失败:', err)
          setError('Mermaid 初始化失败')
          setIsLoading(false)
          return
        }
      }

      // 防止并发渲染
      if (isRenderingRef.current) {
        console.log('正在渲染中，跳过本次')
        // 如果已经在渲染，等待完成后重试
        const checkInterval = setInterval(() => {
          if (!isRenderingRef.current && containerRef.current) {
            clearInterval(checkInterval)
            // 重新触发渲染
            const trimmedContent = content.trim()
            if (trimmedContent !== renderedContentRef.current) {
              renderDiagram()
            }
          }
        }, 200)
        // 5秒后清除检查
        setTimeout(() => clearInterval(checkInterval), 5000)
        return
      }

      // 检查内容是否有效
      if (!content || typeof content !== 'string' || content.trim() === '') {
        console.log('内容为空，跳过渲染')
        setIsLoading(false)
        setError(null)
        renderedContentRef.current = ''
        if (containerRef.current) {
          containerRef.current.innerHTML = ''
        }
        return
      }

      if (!containerRef.current) {
        console.warn('容器不存在')
        setIsLoading(false)
        isRenderingRef.current = false
        return
      }

      const trimmedContent = content.trim()
      
      // 如果内容没有变化，跳过重新渲染
      if (trimmedContent === renderedContentRef.current) {
        console.log('内容未变化，跳过渲染')
        setIsLoading(false)
        isRenderingRef.current = false
        return
      }

      isRenderingRef.current = true

      try {
        setIsLoading(true)
        setError(null)

        // 清空容器
        if (containerRef.current) {
          containerRef.current.innerHTML = ''
        }

        // 生成唯一ID
        renderKeyRef.current += 1
        const id = `mermaid-${Date.now()}-${renderKeyRef.current}`

        console.log('开始渲染 Mermaid 图表:', {
          id,
          contentLength: trimmedContent.length,
          contentPreview: trimmedContent.substring(0, 50),
        })

        // 使用 Mermaid 10.x API - render 方法
        const result = await mermaid.render(id, trimmedContent)
        const svg = result?.svg || result
        
        if (!svg || typeof svg !== 'string') {
          throw new Error('Mermaid 返回的 SVG 无效')
        }
        
        // 检查容器是否还存在
        if (!containerRef.current) {
          console.warn('渲染过程中容器被移除')
          setIsLoading(false)
          isRenderingRef.current = false
          return
        }

        // 插入 SVG
        containerRef.current.innerHTML = svg
        renderedContentRef.current = trimmedContent
        console.log('Mermaid 图表渲染成功，SVG 长度:', svg.length)
        setIsLoading(false)
      } catch (err) {
        console.error('Mermaid 渲染错误:', err)
        console.error('错误详情:', {
          message: err.message,
          name: err.name,
          stack: err.stack,
          content: trimmedContent.substring(0, 200),
        })
        const errorMessage = err.message || err.toString() || '图表渲染失败'
        setError(errorMessage)
        setIsLoading(false)
        renderedContentRef.current = ''
        if (containerRef.current) {
          containerRef.current.innerHTML = ''
        }
      } finally {
        isRenderingRef.current = false
      }
    }

    renderDiagram()
  }, [content])

  if (isLoading) {
    return (
      <div className={styles.container}>
        <div className={styles.loading}>正在渲染图表...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className={styles.container}>
        <div className={styles.error}>
          <div className={styles.errorTitle}>图表渲染失败</div>
          <div className={styles.errorMessage}>{error}</div>
          <div className={styles.errorCode}>
            <pre>{content}</pre>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.container} ref={containerRef}>
      {/* Mermaid SVG 会在这里渲染 */}
    </div>
  )
}

export default MermaidRenderer


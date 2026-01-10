import { useEffect, useRef, useState } from 'react'
import mermaid from 'mermaid'
import styles from './MermaidRenderer.module.css'

const MermaidRenderer = ({ content }) => {
  const containerRef = useRef(null)
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const diagramIdRef = useRef(null)

  useEffect(() => {
    // 初始化 Mermaid（只初始化一次）
    mermaid.initialize({
      startOnLoad: false,
      theme: 'default',
      securityLevel: 'loose',
      flowchart: {
        useMaxWidth: true,
        htmlLabels: true,
        curve: 'basis',
      },
    })
  }, [])

  useEffect(() => {
    if (!content || content.trim() === '' || !containerRef.current) {
      setIsLoading(false)
      if (containerRef.current) {
        containerRef.current.innerHTML = ''
      }
      return
    }

    const renderDiagram = async () => {
      try {
        setIsLoading(true)
        setError(null)

        // 清空容器
        containerRef.current.innerHTML = ''

        // 生成唯一ID
        const id = `mermaid-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
        diagramIdRef.current = id

        // 渲染图表
        const { svg } = await mermaid.render(id, content.trim())
        if (containerRef.current) {
          containerRef.current.innerHTML = svg
        }

        setIsLoading(false)
      } catch (err) {
        console.error('Mermaid 渲染错误:', err)
        setError(err.message || '图表渲染失败')
        setIsLoading(false)
        if (containerRef.current) {
          containerRef.current.innerHTML = ''
        }
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


import { useEffect, useRef, useState } from 'react'
import styles from './MermaidRenderer.module.css'

let mermaidInstance = null
let mermaidInitialized = false

const initializeMermaid = async () => {
  if (!mermaidInitialized) {
    try {
      const mermaidModule = await import('mermaid')
      mermaidInstance = mermaidModule.default
      mermaidInstance.initialize({
        startOnLoad: false,
        theme: 'default',
        securityLevel: 'strict',
        flowchart: {
          useMaxWidth: true,
          htmlLabels: false,
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
  const isMountedRef = useRef(true)
  const cleanupRef = useRef([])

  useEffect(() => {
    if (!initPromiseRef.current) {
      initPromiseRef.current = initializeMermaid()
    }
  }, [])

  useEffect(() => {
    return () => {
      isMountedRef.current = false
      cleanupRef.current.forEach((timerId) => {
        try {
          clearInterval(timerId)
          clearTimeout(timerId)
        } catch {
          // ignore cleanup errors
        }
      })
      cleanupRef.current = []
    }
  }, [])

  useEffect(() => {
    const renderDiagram = async () => {
      if (!isMountedRef.current) {
        return
      }

      if (initPromiseRef.current) {
        try {
          await initPromiseRef.current
        } catch (err) {
          console.error('等待 Mermaid 初始化失败:', err)
          if (isMountedRef.current) {
            setError('Mermaid 初始化失败')
            setIsLoading(false)
          }
          return
        }
      }

      if (isRenderingRef.current) {
        const checkInterval = setInterval(() => {
          if (!isRenderingRef.current && containerRef.current && isMountedRef.current) {
            clearInterval(checkInterval)
            cleanupRef.current = cleanupRef.current.filter((id) => id !== checkInterval)
            const trimmedContent = content.trim()
            if (trimmedContent !== renderedContentRef.current) {
              renderDiagram()
            }
          }
        }, 200)

        cleanupRef.current.push(checkInterval)
        const timeoutId = setTimeout(() => {
          clearInterval(checkInterval)
          cleanupRef.current = cleanupRef.current.filter((id) => id !== checkInterval)
        }, 5000)

        cleanupRef.current.push(timeoutId)
        return
      }

      if (!content || typeof content !== 'string' || content.trim() === '') {
        if (isMountedRef.current) {
          setIsLoading(false)
          setError(null)
        }
        renderedContentRef.current = ''
        if (containerRef.current) {
          containerRef.current.innerHTML = ''
        }
        return
      }

      if (!containerRef.current) {
        if (isMountedRef.current) {
          setIsLoading(false)
        }
        isRenderingRef.current = false
        return
      }

      const trimmedContent = content.trim()
      if (trimmedContent === renderedContentRef.current) {
        if (isMountedRef.current) {
          setIsLoading(false)
        }
        isRenderingRef.current = false
        return
      }

      isRenderingRef.current = true

      try {
        if (isMountedRef.current) {
          setIsLoading(true)
          setError(null)
        }

        if (containerRef.current) {
          containerRef.current.innerHTML = ''
        }

        renderKeyRef.current += 1
        const id = `mermaid-${Date.now()}-${renderKeyRef.current}`

        if (!mermaidInstance) {
          throw new Error('Mermaid 未初始化')
        }

        const result = await mermaidInstance.render(id, trimmedContent)

        if (!isMountedRef.current) {
          isRenderingRef.current = false
          return
        }

        const svg = result?.svg || result

        if (!svg || typeof svg !== 'string') {
          throw new Error('Mermaid 返回的 SVG 无效')
        }

        if (!containerRef.current) {
          if (isMountedRef.current) {
            setIsLoading(false)
          }
          isRenderingRef.current = false
          return
        }

        containerRef.current.innerHTML = svg
        renderedContentRef.current = trimmedContent
        if (isMountedRef.current) {
          setIsLoading(false)
        }
      } catch (err) {
        console.error('Mermaid 渲染错误:', err)
        const errorMessage = err.message || err.toString() || '图表渲染失败'
        if (isMountedRef.current) {
          setError(errorMessage)
          setIsLoading(false)
        }
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

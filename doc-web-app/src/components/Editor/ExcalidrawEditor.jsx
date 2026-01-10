import { useState, useEffect, useRef, forwardRef, useImperativeHandle, useCallback } from 'react'
import { Button, Modal, Message } from '@arco-design/web-react'
import { IconEdit, IconDownload } from '@arco-design/web-react/icon'
import { Excalidraw } from '@excalidraw/excalidraw'
import { exportToSvg, exportToPng } from '@excalidraw/excalidraw'
import styles from './ExcalidrawEditor.module.css'

const ExcalidrawEditor = forwardRef(({ content, onUpdate, showEditor: externalShowEditor, onEditorClose }, ref) => {
  const [showEditor, setShowEditor] = useState(externalShowEditor || false)
  const elementsRef = useRef(null)
  const appStateRef = useRef(null)
  const [initialData, setInitialData] = useState(null)
  const [previewSvg, setPreviewSvg] = useState(null)
  const previewContainerRef = useRef(null)

  // 同步外部 showEditor 状态
  useEffect(() => {
    if (externalShowEditor !== undefined && externalShowEditor !== showEditor) {
      setShowEditor(externalShowEditor)
    }
  }, [externalShowEditor, showEditor])

  useEffect(() => {
    // 如果有内容，尝试解析并生成预览
    if (content) {
      try {
        const data = JSON.parse(content)
        setInitialData(data)
        
        // 生成 SVG 预览
        if (data.elements && data.elements.length > 0) {
          exportToSvg({
            elements: data.elements,
            appState: data.appState || {},
            files: null,
            getDimensions: (width, height) => ({ width, height }),
          }).then((svg) => {
            setPreviewSvg(svg.outerHTML)
          }).catch((err) => {
            console.error('生成预览失败:', err)
            setPreviewSvg(null)
          })
        } else {
          setPreviewSvg(null)
        }
      } catch (error) {
        console.error('解析 Excalidraw 数据失败:', error)
        setInitialData(null)
        setPreviewSvg(null)
      }
    } else {
      setInitialData(null)
      setPreviewSvg(null)
    }
  }, [content])

  const handleOpenEditor = useCallback(() => {
    console.log('handleOpenEditor 被调用')
    setShowEditor(true)
  }, [])

  // 暴露方法给父组件
  useImperativeHandle(ref, () => ({
    handleOpenEditor,
    openEditor: handleOpenEditor, // 别名，确保兼容性
  }), [handleOpenEditor])

  const handleCloseEditor = () => {
    setShowEditor(false)
    if (onEditorClose) {
      onEditorClose()
    }
  }

  const handleSave = async () => {
    if (!elementsRef.current || elementsRef.current.length === 0) {
      Message.warning('请先创建一些内容')
      return
    }

    try {
      const data = {
        type: 'excalidraw',
        version: 2,
        source: 'DocStudio',
        elements: elementsRef.current,
        appState: appStateRef.current ? {
          viewBackgroundColor: appStateRef.current.viewBackgroundColor,
          gridSize: appStateRef.current.gridSize,
        } : {},
      }

      const dataString = JSON.stringify(data)
      if (onUpdate) {
        onUpdate(dataString)
      }
      
      // 更新预览
      const svg = await exportToSvg({
        elements: elementsRef.current,
        appState: appStateRef.current || {},
        files: null,
        getDimensions: (width, height) => ({ width, height }),
      })
      setPreviewSvg(svg.outerHTML)
      
      setShowEditor(false)
      if (onEditorClose) {
        onEditorClose()
      }
      Message.success('图表已保存')
    } catch (error) {
      console.error('保存 Excalidraw 图表失败:', error)
      Message.error('保存图表失败')
    }
  }

  const handleExportSVG = async () => {
    if (!elementsRef.current || elementsRef.current.length === 0) {
      Message.warning('没有可导出的内容')
      return
    }

    try {
      const svg = await exportToSvg({
        elements: elementsRef.current,
        appState: appStateRef.current || {},
        files: null,
        getDimensions: (width, height) => ({ width, height }),
      })

      const svgString = svg.outerHTML
      const blob = new Blob([svgString], { type: 'image/svg+xml' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'diagram.svg'
      link.click()
      URL.revokeObjectURL(url)
      Message.success('SVG 导出成功')
    } catch (error) {
      console.error('导出 SVG 失败:', error)
      Message.error('导出失败')
    }
  }

  const handleExportPNG = async () => {
    if (!elementsRef.current || elementsRef.current.length === 0) {
      Message.warning('没有可导出的内容')
      return
    }

    try {
      const png = await exportToPng({
        elements: elementsRef.current,
        appState: appStateRef.current || {},
        files: null,
        getDimensions: (width, height) => ({ width, height }),
      })

      const link = document.createElement('a')
      link.href = png
      link.download = 'diagram.png'
      link.click()
      Message.success('PNG 导出成功')
    } catch (error) {
      console.error('导出 PNG 失败:', error)
      Message.error('导出失败')
    }
  }

  return (
    <>
      <div className={styles.container}>
        {content && initialData && previewSvg ? (
          <div className={styles.preview}>
            <div className={styles.previewHeader}>
              <div className={styles.previewText}>Excalidraw 图表</div>
              <div className={styles.previewInfo}>
                {initialData.elements?.length || 0} 个元素
              </div>
            </div>
            <div 
              className={styles.previewSvg}
              ref={previewContainerRef}
              dangerouslySetInnerHTML={{ __html: previewSvg }}
              onClick={(e) => {
                e.stopPropagation()
                handleOpenEditor()
              }}
            />
            <div className={styles.previewActions}>
              <Button
                type="primary"
                size="small"
                icon={<IconEdit />}
                onClick={(e) => {
                  e.stopPropagation()
                  e.preventDefault()
                  handleOpenEditor()
                }}
              >
                编辑图表
              </Button>
              <Button
                type="outline"
                size="small"
                icon={<IconDownload />}
                onClick={(e) => {
                  e.stopPropagation()
                  e.preventDefault()
                  handleExportSVG()
                }}
              >
                导出 SVG
              </Button>
            </div>
          </div>
        ) : content && initialData ? (
          <div className={styles.preview}>
            <div className={styles.previewIcon}>✏️</div>
            <div className={styles.previewText}>Excalidraw 图表</div>
            <div className={styles.previewInfo}>
              {initialData.elements?.length || 0} 个元素
            </div>
            <div className={styles.previewActions}>
              <Button
                type="primary"
                size="small"
                icon={<IconEdit />}
                onClick={(e) => {
                  e.stopPropagation()
                  e.preventDefault()
                  handleOpenEditor()
                }}
              >
                编辑图表
              </Button>
            </div>
          </div>
        ) : (
          <div className={styles.empty}>
            <div className={styles.emptyIcon}>✏️</div>
            <div className={styles.emptyText}>Excalidraw 手绘图表</div>
            <div className={styles.emptyDescription}>
              创建手绘风格的流程图、架构图和示意图
            </div>
            <Button
              type="primary"
              icon={<IconEdit />}
              onClick={(e) => {
                e.stopPropagation()
                e.preventDefault()
                handleOpenEditor()
              }}
            >
              创建图表
            </Button>
          </div>
        )}
      </div>

      <Modal
        title="Excalidraw 图表编辑器"
        visible={showEditor}
        onCancel={handleCloseEditor}
        onOk={handleSave}
        okText="保存"
        cancelText="取消"
        style={{ width: '95%', maxWidth: '1400px', height: '90vh' }}
        className={styles.modal}
        footer={
          <div className={styles.modalFooter}>
            <div className={styles.footerLeft}>
              <Button onClick={handleExportSVG} type="outline" size="small">
                导出 SVG
              </Button>
              <Button onClick={handleExportPNG} type="outline" size="small">
                导出 PNG
              </Button>
            </div>
            <div className={styles.footerRight}>
              <Button onClick={handleCloseEditor}>取消</Button>
              <Button type="primary" onClick={handleSave}>
                保存
              </Button>
            </div>
          </div>
        }
      >
        <div className={styles.modalBody}>
          {showEditor && (
            <Excalidraw
              initialData={initialData}
              onChange={(elements, appState, files) => {
                // 使用 ref 存储，避免频繁的状态更新导致无限循环
                elementsRef.current = elements
                appStateRef.current = appState
              }}
              UIOptions={{
                canvasActions: {
                  saveToActiveFile: false,
                  loadScene: false,
                  export: false,
                },
              }}
              theme="light"
            />
          )}
        </div>
      </Modal>
    </>
  )
})

ExcalidrawEditor.displayName = 'ExcalidrawEditor'

export default ExcalidrawEditor


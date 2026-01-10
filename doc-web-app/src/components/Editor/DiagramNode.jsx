import { NodeViewWrapper } from '@tiptap/react'
import { useState } from 'react'
import { Button, Modal, Select, Input, Message } from '@arco-design/web-react'
import { IconEdit, IconDelete } from '@arco-design/web-react/icon'
import MermaidRenderer from './MermaidRenderer'
import ExcalidrawEditor from './ExcalidrawEditor'
import styles from './DiagramNode.module.css'

const DiagramNode = ({ node, updateAttributes, deleteNode, editor, getPos }) => {
  const { diagramType, content, width, height } = node.attrs
  const [isEditing, setIsEditing] = useState(false)
  const [editContent, setEditContent] = useState(content || '')
  const [previewContent, setPreviewContent] = useState(content || '') // 预览内容
  const [editType, setEditType] = useState(diagramType || 'mermaid')
  const [editWidth, setEditWidth] = useState(width || '100%')
  const [editHeight, setEditHeight] = useState(height || '400px')
  const [showExcalidrawEditor, setShowExcalidrawEditor] = useState(false)

  const handleEdit = (e) => {
    if (e) {
      e.stopPropagation()
      e.preventDefault()
    }
    
    // 如果是 Excalidraw 类型，直接打开编辑器
    if (diagramType === 'excalidraw') {
      setShowExcalidrawEditor(true)
      return
    }
    // Mermaid 类型使用文本编辑
    setIsEditing(true)
    setEditContent(content || '')
    setPreviewContent(content || '') // 初始化预览内容
    setEditType(diagramType || 'mermaid')
  }

  // 实时更新预览内容
  const handleContentChange = (newContent) => {
    setEditContent(newContent)
    // 使用防抖更新预览
    clearTimeout(handleContentChange.timer)
    handleContentChange.timer = setTimeout(() => {
      setPreviewContent(newContent)
    }, 500) // 500ms 延迟更新预览
  }

  const handleSave = () => {
    if (!editContent.trim()) {
      Message.warning('请输入图表内容')
      return
    }
    updateAttributes({
      content: editContent,
      diagramType: editType,
      width: editWidth,
      height: editHeight,
    })
    setIsEditing(false)
    Message.success('图表已更新')
  }

  const handleDelete = (e) => {
    if (e) {
      e.stopPropagation()
      e.preventDefault()
    }
    
    if (!editor) {
      if (deleteNode) {
        deleteNode()
      }
      return
    }

    // 使用 getPos 获取节点在文档中的位置并删除
    if (typeof getPos === 'function') {
      try {
        const pos = getPos()
        if (pos !== undefined && pos !== null && pos >= 0) {
          // 计算节点的起始和结束位置
          const from = pos
          const to = pos + node.nodeSize
          
          // 使用 deleteRange 删除节点
          editor.chain()
            .focus()
            .deleteRange({ from, to })
            .run()
          return
        }
      } catch (error) {
        console.error('删除节点时出错:', error)
      }
    }

    // 备用方案：如果 getPos 不可用，尝试使用 deleteNode 命令
    // 但这需要节点已经被选中
    try {
      editor.chain().focus().deleteNode('diagram').run()
    } catch (error) {
      console.error('删除节点失败:', error)
      Message.error('删除图表失败，请尝试选中后删除')
    }
  }

  return (
    <NodeViewWrapper className={styles.diagramWrapper} data-type="diagram">
      <div className={styles.diagramContainer} style={{ width, height }}>
        <div className={styles.diagramToolbar}>
          <span className={styles.diagramLabel}>
            {diagramType === 'mermaid' ? 'Mermaid 图表' : 'Excalidraw 图表'}
          </span>
          <div className={styles.diagramActions}>
            <Button
              type="text"
              size="small"
              icon={<IconEdit />}
              onClick={(e) => {
                e.stopPropagation()
                handleEdit(e)
              }}
            >
              编辑
            </Button>
            <Button
              type="text"
              size="small"
              icon={<IconDelete />}
              onClick={(e) => {
                e.stopPropagation()
                e.preventDefault()
                handleDelete(e)
              }}
            >
              删除
            </Button>
          </div>
        </div>
        <div className={styles.diagramContent}>
          {diagramType === 'mermaid' ? (
            <MermaidRenderer content={content || ''} />
          ) : (
            <ExcalidrawEditor 
              content={content || ''} 
              showEditor={showExcalidrawEditor}
              onEditorClose={() => setShowExcalidrawEditor(false)}
              onUpdate={(newContent) => {
                updateAttributes({ content: newContent })
                setShowExcalidrawEditor(false)
              }} 
            />
          )}
        </div>
      </div>

      <Modal
        title="编辑图表"
        visible={isEditing}
        onOk={handleSave}
        onCancel={() => {
          setIsEditing(false)
          setPreviewContent(content || '') // 重置预览内容
        }}
        style={{ width: '95%', maxWidth: '1400px' }}
        okText="保存"
        cancelText="取消"
        className={styles.editModal}
      >
        <div className={styles.editForm}>
          <div className={styles.formItem}>
            <label>图表类型</label>
            <Select
              value={editType}
              onChange={(value) => {
                setEditType(value)
                setPreviewContent(editContent) // 切换类型时更新预览
              }}
              style={{ width: '100%' }}
            >
              <Select.Option value="mermaid">Mermaid (流程图/架构图)</Select.Option>
              <Select.Option value="excalidraw">Excalidraw (手绘图表)</Select.Option>
            </Select>
          </div>

          {editType === 'mermaid' ? (
            <div className={styles.editContainer}>
              <div className={styles.editPanel}>
                <div className={styles.panelHeader}>
                  <label>Mermaid 代码</label>
                  <a
                    href="https://mermaid.js.org/intro/"
                    target="_blank"
                    rel="noopener noreferrer"
                    className={styles.helpLink}
                  >
                    查看语法文档
                  </a>
                </div>
                <Input.TextArea
                  value={editContent}
                  onChange={handleContentChange}
                  placeholder="输入 Mermaid 代码，例如：&#10;graph TD&#10;  A[开始] --> B[处理]&#10;  B --> C[结束]"
                  rows={20}
                  style={{ fontFamily: 'monospace', fontSize: '14px' }}
                  className={styles.codeEditor}
                />
              </div>
              <div className={styles.previewPanel}>
                <div className={styles.panelHeader}>
                  <label>实时预览</label>
                </div>
                <div className={styles.previewContent}>
                  <MermaidRenderer content={previewContent || editContent} />
                </div>
              </div>
            </div>
          ) : (
            <div className={styles.formItem}>
              <label>Excalidraw JSON</label>
              <Input.TextArea
                value={editContent}
                onChange={setEditContent}
                placeholder="Excalidraw 图表 JSON 数据"
                rows={10}
                style={{ fontFamily: 'monospace' }}
              />
              <div className={styles.helpText}>
                使用 Excalidraw 编辑器创建图表，数据会自动保存为 JSON 格式
              </div>
            </div>
          )}

          <div className={styles.formRow}>
            <div className={styles.formItem}>
              <label>宽度</label>
              <Input
                value={editWidth}
                onChange={setEditWidth}
                placeholder="例如: 100% 或 800px"
              />
            </div>
            <div className={styles.formItem}>
              <label>高度</label>
              <Input
                value={editHeight}
                onChange={setEditHeight}
                placeholder="例如: 400px"
              />
            </div>
          </div>
        </div>
      </Modal>
    </NodeViewWrapper>
  )
}

export default DiagramNode


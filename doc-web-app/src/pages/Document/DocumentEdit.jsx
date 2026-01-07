import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import Image from '@tiptap/extension-image'
import { Button, Input, Message, Spin } from '@arco-design/web-react'
import { IconSave, IconArrowLeft } from '@arco-design/web-react/icon'
import { documentApi } from '../../services/api/documentApi'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import DiagramExtension from '../../extensions/DiagramExtension'
import ContextMenu from '../../components/Editor/ContextMenu'
import styles from './DocumentEdit.module.css'

const DocumentEdit = () => {
  const { kbId, id } = useParams()
  const navigate = useNavigate()
  const isNew = id === 'new'
  const [title, setTitle] = useState('')
  const [knowledgeBase, setKnowledgeBase] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [contextMenu, setContextMenu] = useState(null)
  const editorRef = useRef(null)

  const editor = useEditor({
    extensions: [
      StarterKit,
      Image,
      Placeholder.configure({
        placeholder: '开始输入内容...',
      }),
      DiagramExtension,
    ],
    content: '',
    editorProps: {
      handleDOMEvents: {
        contextmenu: (view, event) => {
          event.preventDefault()
          const { clientX, clientY } = event
          setContextMenu({ x: clientX, y: clientY })
          return true
        },
      },
    },
  })

  useEffect(() => {
    if (editor) {
      editorRef.current = editor
    }
  }, [editor])

  useEffect(() => {
    loadData()
  }, [kbId, id])

  const loadData = async () => {
    try {
      setLoading(true)
      const kbResponse = await knowledgeBaseApi.getKnowledgeBaseById(kbId)
      if (kbResponse.code === 200) {
        setKnowledgeBase(kbResponse.data)
      }

      if (!isNew) {
        const docResponse = await documentApi.getDocumentById(id)
        if (docResponse.code === 200 && docResponse.data) {
          const doc = docResponse.data
          setTitle(doc.title)
          if (editor && doc.content) {
            editor.commands.setContent(doc.content)
          }
        }
      }
    } catch (error) {
      console.error('加载数据失败:', error)
      Message.error('加载文档失败')
    } finally {
      setLoading(false)
    }
  }

  const handleSave = async () => {
    if (!title.trim()) {
      Message.warning('请输入文档标题')
      return
    }

    try {
      setSaving(true)
      const content = editor.getHTML()
      const contentText = editor.getText()

      if (isNew) {
        const response = await documentApi.createDocument({
          title: title.trim(),
          content,
          contentText,
          knowledgeBaseId: kbId,
        })
        if (response.code === 200) {
          Message.success('文档创建成功')
          navigate(`/kb/${kbId}/doc/${response.data.id}`)
        }
      } else {
        const response = await documentApi.updateDocument(id, {
          title: title.trim(),
          content,
          contentText,
        })
        if (response.code === 200) {
          Message.success('文档保存成功')
        }
      }
    } catch (error) {
      console.error('保存文档失败:', error)
      Message.error('保存文档失败')
    } finally {
      setSaving(false)
    }
  }

  const handleBack = () => {
    navigate(`/kb/${kbId}`)
  }

  const handleInsertDiagram = (type) => {
    if (!editor) return

    const defaultContent = type === 'mermaid' 
      ? `graph TD
    A[开始] --> B{判断}
    B -->|是| C[处理1]
    B -->|否| D[处理2]
    C --> E[结束]
    D --> E`
      : type === 'excalidraw'
      ? JSON.stringify({
          type: 'excalidraw',
          version: 2,
          source: 'DocStudio',
          elements: [],
          appState: {
            viewBackgroundColor: '#ffffff',
            gridSize: null,
          },
        })
      : ''

    editor.chain().focus().insertDiagram({
      diagramType: type,
      content: defaultContent,
      width: '100%',
      height: '400px',
    }).run()
  }

  const handleInsertImage = () => {
    if (!editor) return
    const url = window.prompt('请输入图片URL:')
    if (url) {
      editor.chain().focus().setImage({ src: url }).run()
    }
  }

  const handleInsertCode = () => {
    if (!editor) return
    editor.chain().focus().toggleCodeBlock().run()
  }

  const handleCloseContextMenu = () => {
    setContextMenu(null)
  }

  if (loading) {
    return (
      <div className={styles.loading}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <div className={styles.edit}>
      <div className={styles.toolbar}>
        <div className={styles.toolbarLeft}>
          <Button icon={<IconArrowLeft />} onClick={handleBack}>
            返回
          </Button>
          {knowledgeBase && (
            <span className={styles.kbName}>{knowledgeBase.name}</span>
          )}
        </div>
        <div className={styles.toolbarRight}>
          <Button
            type="primary"
            icon={<IconSave />}
            loading={saving}
            onClick={handleSave}
          >
            保存
          </Button>
        </div>
      </div>

      <div className={styles.editor}>
        <Input
          className={styles.titleInput}
          placeholder="请输入文档标题..."
          value={title}
          onChange={(value) => setTitle(value)}
          size="large"
        />
        <div className={styles.editorContent}>
          {editor && <EditorContent editor={editor} />}
        </div>
      </div>

      {contextMenu && (
        <ContextMenu
          position={contextMenu}
          onClose={handleCloseContextMenu}
          onInsertDiagram={handleInsertDiagram}
          onInsertImage={handleInsertImage}
          onInsertCode={handleInsertCode}
        />
      )}
    </div>
  )
}

export default DocumentEdit


import React, { useState } from 'react'
import { useParams } from 'react-router-dom'
import './DocumentEdit.module.css'

const DocumentEdit = () => {
  const { kbId, id } = useParams()
  const [title, setTitle] = useState(id ? '编辑文档' : '新建文档')
  const [content, setContent] = useState('')
  const [saving, setSaving] = useState(false)

  const handleSave = async () => {
    setSaving(true)
    try {
      // 这里将实现文档保存逻辑
      console.log('Saving document:', { title, content })
      // 模拟保存延迟
      await new Promise(resolve => setTimeout(resolve, 1000))
      alert('文档保存成功！')
    } catch (error) {
      console.error('Error saving document:', error)
      alert('保存失败，请重试')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="document-edit">
      <div className="document-edit-header">
        <input
          type="text"
          className="document-title"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="输入文档标题"
        />
        <div className="document-actions">
          <button onClick={handleSave} disabled={saving}>
            {saving ? '保存中...' : '保存'}
          </button>
          <button>导出</button>
        </div>
      </div>
      <div className="document-edit-content">
        <textarea
          className="document-textarea"
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="开始编辑文档内容..."
        />
      </div>
      <div className="document-edit-footer">
        <span className="document-stats">{content.length} 字符</span>
        <span className="document-status">已自动保存</span>
      </div>
    </div>
  )
}

export default DocumentEdit
import React from 'react'
import { useParams } from 'react-router-dom'
import './KnowledgeBaseDetail.module.css'

const KnowledgeBaseDetail = () => {
  const { id } = useParams()

  // 模拟知识库数据
  const knowledgeBase = {
    id,
    name: '示例知识库',
    description: '这是一个示例知识库，用于测试功能',
    documents: [
      { id: 1, title: '文档1', updatedAt: '2026-03-03' },
      { id: 2, title: '文档2', updatedAt: '2026-03-02' },
      { id: 3, title: '文档3', updatedAt: '2026-03-01' }
    ]
  }

  return (
    <div className="knowledge-base-detail">
      <div className="knowledge-base-header">
        <h2>{knowledgeBase.name}</h2>
        <p>{knowledgeBase.description}</p>
        <div className="knowledge-base-actions">
          <button>新建文档</button>
          <button>编辑知识库</button>
        </div>
      </div>
      <div className="knowledge-base-content">
        <h3>文档列表</h3>
        <div className="document-list">
          {knowledgeBase.documents.map((doc) => (
            <div key={doc.id} className="document-item">
              <div className="document-info">
                <h4>{doc.title}</h4>
                <span className="document-date">{doc.updatedAt}</span>
              </div>
              <div className="document-actions">
                <button>编辑</button>
                <button>删除</button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

export default KnowledgeBaseDetail
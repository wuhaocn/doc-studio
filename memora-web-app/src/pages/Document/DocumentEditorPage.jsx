import { Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import dayjs from 'dayjs'
import DocumentShareDrawer from '../../components/Document/DocumentShareDrawer'
import { documentApi } from '../../services/api/documentApi'
import { buildLineDiff } from '../../utils/documentDiff'
import { summarizePlainText } from '../../utils/documentContent'
import styles from './DocumentEditorPage.module.css'

const DocumentRichEditor = lazy(() => import('../../components/Document/DocumentRichEditor'))

const PAGE_STATUS = {
  LOADING: 'loading',
  READY: 'ready',
  FORBIDDEN: 'forbidden',
  NOT_FOUND: 'not_found',
  ERROR: 'error',
}

const DocumentEditorPage = () => {
  const { documentId } = useParams()
  const navigate = useNavigate()
  const [pageStatus, setPageStatus] = useState(PAGE_STATUS.LOADING)
  const [pageErrorMessage, setPageErrorMessage] = useState('')
  const [document, setDocument] = useState(null)
  const [versions, setVersions] = useState([])
  const [versionsLoading, setVersionsLoading] = useState(false)
  const [versionsOpen, setVersionsOpen] = useState(false)
  const [shareOpen, setShareOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [feedback, setFeedback] = useState(null)
  const [rollingBackVersionId, setRollingBackVersionId] = useState(null)
  const [comparingVersionId, setComparingVersionId] = useState(null)

  const loadData = useCallback(async ({ resetState = false } = {}) => {
    try {
      setPageErrorMessage('')
      if (resetState) {
        setPageStatus(PAGE_STATUS.LOADING)
        setDocument(null)
        setVersions([])
        setComparingVersionId(null)
      }

      const documentResponse = await documentApi.getDocumentById(documentId)
      const documentData = documentResponse?.data

      if (!documentData || documentData.docType !== 'DOC') {
        setPageStatus(PAGE_STATUS.ERROR)
        setPageErrorMessage('当前节点不是可编辑文档')
        return
      }

      setDocument(documentData)
      setPageStatus(PAGE_STATUS.READY)
    } catch (error) {
      console.error('加载文档编辑页失败', error)
      setDocument(null)
      setVersions([])
      setComparingVersionId(null)

      if (error?.code === 403) {
        setPageStatus(PAGE_STATUS.FORBIDDEN)
        setPageErrorMessage(error?.message || '当前会话没有访问该文档的权限')
        return
      }

      if (error?.code === 404) {
        setPageStatus(PAGE_STATUS.NOT_FOUND)
        setPageErrorMessage(error?.message || '当前文档不存在或已删除')
        return
      }

      setPageStatus(PAGE_STATUS.ERROR)
      setPageErrorMessage(error?.message || '加载文档编辑页失败，请稍后重试')
    }
  }, [documentId])

  const loadVersions = useCallback(async () => {
    try {
      setVersionsLoading(true)
      const response = await documentApi.getVersions(documentId)
      if (response.code === 200) {
        setVersions(response.data || [])
      } else {
        setVersions([])
      }
    } catch (error) {
      console.error('加载文档版本失败', error)
      setVersions([])
    } finally {
      setVersionsLoading(false)
    }
  }, [documentId])

  useEffect(() => {
    loadData({ resetState: true })
  }, [loadData])

  useEffect(() => {
    if (pageStatus === PAGE_STATUS.READY) {
      loadVersions()
    }
  }, [loadVersions, pageStatus])

  const comparingVersion = useMemo(() => {
    return versions.find((version) => version.id === comparingVersionId) || null
  }, [comparingVersionId, versions])

  const versionDiffRows = useMemo(() => {
    if (!comparingVersion || !document) {
      return []
    }

    return buildLineDiff(document.contentText || '', comparingVersion.contentText || comparingVersion.content || '')
  }, [comparingVersion, document])

  const backToKnowledgeBase = () => {
    if (!document?.knowledgeBaseId) {
      navigate('/')
      return
    }

    navigate(`/kb/${document.knowledgeBaseId}`, {
      state: { selectedDocumentId: document.id },
    })
  }

  const handleSave = async ({ content, contentText }) => {
    if (!document) {
      return
    }

    try {
      setSaving(true)
      await documentApi.updateDocument(document.id, {
        content,
        contentText,
        summary: summarizePlainText(contentText),
      })
      await loadData()
      await loadVersions()
      setFeedback({ type: 'success', message: '文档已保存，已生成新版本' })
    } catch (error) {
      console.error('保存文档失败', error)
      setFeedback({ type: 'error', message: error?.message || '保存文档失败，请稍后重试' })
    } finally {
      setSaving(false)
    }
  }

  const handleRollbackVersion = async (versionId) => {
    if (!document) {
      return
    }

    if (!window.confirm(`确认将“${document.title}”回滚到该版本吗？`)) {
      return
    }

    try {
      setRollingBackVersionId(versionId)
      await documentApi.rollbackToVersion(document.id, versionId)
      await loadData()
      await loadVersions()
      setFeedback({ type: 'success', message: `文档“${document.title}”已回滚到历史版本` })
    } catch (error) {
      console.error('回滚文档失败', error)
      setFeedback({ type: 'error', message: error?.message || '回滚文档失败，请稍后重试' })
    } finally {
      setRollingBackVersionId(null)
    }
  }

  if (pageStatus === PAGE_STATUS.LOADING) {
    return <div className={styles.state}>正在加载文档编辑页...</div>
  }

  if (pageStatus !== PAGE_STATUS.READY || !document) {
    const stateTitleMap = {
      [PAGE_STATUS.FORBIDDEN]: '当前会话无权编辑该文档',
      [PAGE_STATUS.NOT_FOUND]: '当前文档不存在',
      [PAGE_STATUS.ERROR]: '文档编辑页暂时不可用',
    }

    return (
      <div className={styles.stateCard}>
        <h1>{stateTitleMap[pageStatus] || '文档编辑页暂时不可用'}</h1>
        <p>{pageErrorMessage || '请返回知识库继续操作。'}</p>
        <div className={styles.stateActions}>
          <button type="button" className={styles.primaryButton} onClick={() => navigate('/')}>
            返回工作台
          </button>
          <button type="button" className={styles.secondaryButton} onClick={() => loadData({ resetState: true })}>
            重新加载
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <header className={styles.topbar}>
        <div className={styles.topbarMain}>
          <button type="button" className={styles.backButton} onClick={backToKnowledgeBase}>
            返回文档列表
          </button>
          <div className={styles.documentIdentity}>
            <div className={styles.eyebrow}>文档编辑</div>
            <h1 className={styles.title}>{document.title}</h1>
            <div className={styles.meta}>
              <span className={styles.metaPill}>v{document.versionNo}</span>
              <span className={styles.metaPill}>{saving ? '保存中…' : '已进入编辑'}</span>
              <span className={styles.metaPill}>更新于 {dayjs(document.updatedAt).format('MM-DD HH:mm')}</span>
            </div>
          </div>
        </div>
        <div className={styles.topbarActions}>
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={() => navigate(`/docs/${document.id}`)}
          >
            阅读文档
          </button>
          <button
            type="button"
            className={styles.secondaryButton}
            onClick={() => setShareOpen(true)}
          >
            分享文档
          </button>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={() => setVersionsOpen((current) => !current)}
          >
            {versionsOpen ? '收起版本' : '查看版本'}
          </button>
        </div>
      </header>

      {feedback && (
        <div className={`${styles.feedback} ${feedback.type === 'error' ? styles.feedbackError : styles.feedbackSuccess}`}>
          {feedback.message}
        </div>
      )}

      <section className={`${styles.workspace} ${versionsOpen ? styles.workspaceWithDrawer : ''}`}>
        <div className={styles.editorPanel}>
          <Suspense fallback={<div className={styles.editorLoading}>正在加载编辑器...</div>}>
            <DocumentRichEditor
              focusMode
              initialContent={document.content || document.contentText || ''}
              placeholder="开始编写文档正文..."
              saving={saving}
              onCancel={backToKnowledgeBase}
              onSave={handleSave}
            />
          </Suspense>
        </div>

        {versionsOpen && (
          <aside className={styles.versionDrawer}>
            <div className={styles.versionHeader}>
              <div>
                <h2>版本记录</h2>
                <span>{versions.length} 条</span>
              </div>
              <button type="button" className={styles.ghostButton} onClick={() => setVersionsOpen(false)}>
                关闭
              </button>
            </div>
            <div className={styles.versionList}>
              {versionsLoading ? (
                <div className={styles.emptyVersion}>正在加载版本记录...</div>
              ) : versions.length > 0 ? (
                versions.map((version) => (
                  <article key={version.id} className={styles.versionItem}>
                    <div className={styles.versionTop}>
                      <div>
                        <strong>v{version.version}</strong>
                        <span className={styles.versionTime}>{dayjs(version.createdAt).format('MM-DD HH:mm')}</span>
                      </div>
                      <div className={styles.versionActions}>
                        <button
                          type="button"
                          className={styles.ghostButton}
                          onClick={() => setComparingVersionId((current) => (current === version.id ? null : version.id))}
                        >
                          {comparingVersionId === version.id ? '关闭对比' : '查看对比'}
                        </button>
                        <button
                          type="button"
                          className={styles.ghostButton}
                          disabled={rollingBackVersionId === version.id}
                          onClick={() => handleRollbackVersion(version.id)}
                        >
                          {rollingBackVersionId === version.id ? '回滚中...' : '回滚'}
                        </button>
                      </div>
                    </div>
                    <p className={styles.versionRemark}>{version.remark || '自动快照'}</p>
                    <div className={styles.versionPreview}>
                      {version.contentText || version.content || '当前版本未记录正文摘要。'}
                    </div>
                  </article>
                ))
              ) : (
                <div className={styles.emptyVersion}>当前文档还没有可展示的历史版本。</div>
              )}
            </div>

            {comparingVersion && (
              <div className={styles.diffPanel}>
                <div className={styles.versionHeader}>
                  <div>
                    <h2>版本对比</h2>
                    <span>当前版本 v{document.versionNo} vs 历史版本 v{comparingVersion.version}</span>
                  </div>
                </div>
                <div className={styles.diffTable}>
                  <div className={styles.diffHeader}>
                    <span>当前版本</span>
                    <span>历史版本</span>
                  </div>
                  {versionDiffRows.length > 0 ? (
                    versionDiffRows.map((row, index) => (
                      <div key={`${row.type}-${index}`} className={styles.diffRow}>
                        <div className={`${styles.diffCell} ${styles[`diff${row.type}`]}`}>
                          {row.currentLine || <span className={styles.emptyLine}>∅</span>}
                        </div>
                        <div className={`${styles.diffCell} ${styles[`diff${row.type}`]}`}>
                          {row.targetLine || <span className={styles.emptyLine}>∅</span>}
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className={styles.emptyVersion}>当前版本和历史版本内容一致。</div>
                  )}
                </div>
              </div>
            )}
          </aside>
        )}
      </section>

      <DocumentShareDrawer
        open={shareOpen}
        documentId={document.id}
        title={document.title}
        onClose={() => setShareOpen(false)}
      />
    </div>
  )
}

export default DocumentEditorPage

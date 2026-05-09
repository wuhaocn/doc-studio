import { Suspense, lazy, useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import dayjs from 'dayjs'
import PageState from '../../components/Feedback/PageState'
import Header from '../../components/Layout/Header'
import DocumentReadLinkDrawer from '../../components/Document/DocumentReadLinkDrawer'
import DocumentShareDrawer from '../../components/Document/DocumentShareDrawer'
import DocumentVersionDiff from '../../components/Document/DocumentVersionDiff'
import DocumentVersionList from '../../components/Document/DocumentVersionList'
import { documentApi } from '../../services/api/documentApi'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import { buildLineDiff } from '../../utils/documentDiff'
import { summarizePlainText } from '../../utils/documentContent'
import styles from './DocumentEditorPage.module.css'

const DocumentRichEditor = lazy(() => import('../../components/Document/DocumentRichEditor'))

const PAGE_STATUS = {
  LOADING: 'loading',
  READY: 'ready',
  READ_ONLY: 'read_only',
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
  const [knowledgeBaseAccess, setKnowledgeBaseAccess] = useState(null)
  const [versions, setVersions] = useState([])
  const [versionsLoading, setVersionsLoading] = useState(false)
  const [versionsOpen, setVersionsOpen] = useState(false)
  const [readLinkOpen, setReadLinkOpen] = useState(false)
  const [shareDrawerOpen, setShareDrawerOpen] = useState(false)
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
        setKnowledgeBaseAccess(null)
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

      const knowledgeBaseResponse = await knowledgeBaseApi.getKnowledgeBaseById(documentData.knowledgeBaseId)
      const knowledgeBaseData = knowledgeBaseResponse?.data
      setDocument(documentData)
      setKnowledgeBaseAccess(knowledgeBaseData || null)
      setPageStatus(knowledgeBaseData?.canWrite ? PAGE_STATUS.READY : PAGE_STATUS.READ_ONLY)
    } catch (error) {
      console.error('加载文档编辑页失败', error)
      setDocument(null)
      setKnowledgeBaseAccess(null)
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
    return (
      <PageState
        eyebrow={pageStatus === PAGE_STATUS.READ_ONLY ? '只读角色' : pageStatus === PAGE_STATUS.FORBIDDEN ? '无权访问' : pageStatus === PAGE_STATUS.NOT_FOUND ? '内容不存在' : '编辑页不可用'}
        title={pageStatus === PAGE_STATUS.READ_ONLY ? '当前角色只能阅读，不能进入编辑页' : pageStatus === PAGE_STATUS.FORBIDDEN ? '当前会话无权编辑该文档' : pageStatus === PAGE_STATUS.NOT_FOUND ? '当前文档不存在' : '文档编辑页暂时不可用'}
        description={pageStatus === PAGE_STATUS.READ_ONLY
          ? `当前工作区角色为 ${knowledgeBaseAccess?.currentRole || '只读'}。请返回阅读页继续查看正文。`
          : pageErrorMessage || '请返回知识库继续操作。'}
        primaryAction={{
          label: pageStatus === PAGE_STATUS.READ_ONLY ? '进入阅读页' : '返回工作台',
          onClick: () => {
            if (pageStatus === PAGE_STATUS.READ_ONLY) {
              navigate(`/docs/${documentId}`)
              return
            }
            navigate('/')
          },
        }}
        secondaryAction={{ label: '重新加载', onClick: () => loadData({ resetState: true }) }}
      />
    )
  }

  return (
    <div className={styles.page}>
      <Header onToggleSidebar={() => {}} showMenuButton={false} />
      <div className={`${styles.pageShell} ${versionsOpen ? styles.pageShellWide : ''}`}>
        {feedback && (
          <div className={`${styles.feedback} ${feedback.type === 'error' ? styles.feedbackError : styles.feedbackSuccess}`}>
            {feedback.message}
          </div>
        )}

        <section className={`${styles.workspace} ${versionsOpen ? styles.workspaceWithDrawer : ''}`}>
          <div className={styles.editorPanel}>
            <header className={styles.topbar}>
              <div className={styles.topbarMain}>
                <button type="button" className={styles.backButton} onClick={backToKnowledgeBase}>
                  返回知识库
                </button>
                <div className={styles.documentIdentity}>
                  <div className={styles.eyebrow}>文档编辑</div>
                  <h1 className={styles.title}>{document.title}</h1>
                  <div className={styles.meta}>
                    <span className={styles.metaPill}>v{document.versionNo}</span>
                    <span className={styles.metaPill}>{saving ? '保存中…' : '可编辑'}</span>
                    <span className={styles.metaPill}>{knowledgeBaseAccess?.currentRole || '未知角色'}</span>
                    <span className={styles.metaPill}>最近更新 {dayjs(document.updatedAt).format('MM-DD HH:mm')}</span>
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
                  onClick={() => setReadLinkOpen(true)}
                >
                  复制阅读链接
                </button>
                {knowledgeBaseAccess?.canManage ? (
                  <button
                    type="button"
                    className={styles.secondaryButton}
                    onClick={() => setShareDrawerOpen(true)}
                  >
                    受控分享
                  </button>
                ) : null}
                <button
                  type="button"
                  className={styles.ghostButton}
                  onClick={() => setVersionsOpen(true)}
                >
                  查看版本
                </button>
              </div>
            </header>

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
                  <div className={styles.versionEyebrow}>右侧抽屉</div>
                  <h2>查看版本</h2>
                  <span>{versions.length} 个版本</span>
                </div>
                <button type="button" className={styles.versionCloseButton} onClick={() => setVersionsOpen(false)}>
                  收起
                </button>
              </div>
              <DocumentVersionList
                versions={versions}
                loading={versionsLoading}
                comparingVersionId={comparingVersionId}
                rollingBackVersionId={rollingBackVersionId}
                onToggleCompare={(versionId) => setComparingVersionId((current) => (current === versionId ? null : versionId))}
                onRollback={handleRollbackVersion}
              />

              {comparingVersion && (
                <DocumentVersionDiff
                  title="查看差异"
                  subtitle={`当前版本 v${document.versionNo} vs 历史版本 v${comparingVersion.version}`}
                  rows={versionDiffRows}
                />
              )}
            </aside>
          )}
        </section>
      </div>

      <DocumentReadLinkDrawer
        open={readLinkOpen}
        documentId={document.id}
        title={document.title}
        onClose={() => setReadLinkOpen(false)}
      />
      <DocumentShareDrawer
        open={shareDrawerOpen}
        documentId={document.id}
        title={document.title}
        onClose={() => setShareDrawerOpen(false)}
      />
    </div>
  )
}

export default DocumentEditorPage

import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import dayjs from 'dayjs'
import Header from '../../components/Layout/Header'
import DocumentShareDrawer from '../../components/Document/DocumentShareDrawer'
import { documentApi } from '../../services/api/documentApi'
import styles from './DocumentReaderPage.module.css'

const PAGE_STATUS = {
  LOADING: 'loading',
  READY: 'ready',
  FORBIDDEN: 'forbidden',
  NOT_FOUND: 'not_found',
  ERROR: 'error',
}

const DocumentReaderPage = () => {
  const { documentId } = useParams()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [scrolled, setScrolled] = useState(false)
  const [pageStatus, setPageStatus] = useState(PAGE_STATUS.LOADING)
  const [pageErrorMessage, setPageErrorMessage] = useState('')
  const [document, setDocument] = useState(null)
  const [shareOpen, setShareOpen] = useState(false)
  const isShareMode = searchParams.get('share') === '1'

  const loadDocument = useCallback(async () => {
    try {
      setPageErrorMessage('')
      setPageStatus(PAGE_STATUS.LOADING)
      const response = await documentApi.getDocumentById(documentId)
      const documentData = response?.data

      if (!documentData) {
        setPageStatus(PAGE_STATUS.ERROR)
        setPageErrorMessage('当前文档不可用')
        return
      }

      setDocument(documentData)
      setPageStatus(PAGE_STATUS.READY)
    } catch (error) {
      console.error('加载文档阅读页失败', error)
      setDocument(null)

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
      setPageErrorMessage(error?.message || '加载文档阅读页失败，请稍后重试')
    }
  }, [documentId])

  useEffect(() => {
    loadDocument()
  }, [loadDocument])

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 24)
    }

    handleScroll()
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const backToKnowledgeBase = () => {
    if (!document?.knowledgeBaseId) {
      navigate('/')
      return
    }

    navigate(`/kb/${document.knowledgeBaseId}`, {
      state: { selectedDocumentId: document.id },
    })
  }

  if (pageStatus === PAGE_STATUS.LOADING) {
    return <div className={styles.state}>正在加载文档...</div>
  }

  if (pageStatus !== PAGE_STATUS.READY || !document) {
    const stateTitleMap = {
      [PAGE_STATUS.FORBIDDEN]: '当前会话无权访问该文档',
      [PAGE_STATUS.NOT_FOUND]: '当前文档不存在',
      [PAGE_STATUS.ERROR]: '文档阅读页暂时不可用',
    }

    return (
      <div className={styles.stateCard}>
        <h1>{stateTitleMap[pageStatus] || '文档阅读页暂时不可用'}</h1>
        <p>{pageErrorMessage || '请返回知识库继续操作。'}</p>
        <div className={styles.stateActions}>
          <button type="button" className={styles.primaryButton} onClick={() => navigate('/')}>
            返回工作台
          </button>
          <button type="button" className={styles.secondaryButton} onClick={loadDocument}>
            重新加载
          </button>
        </div>
      </div>
    )
  }

  const shouldRenderRichPreview = document.docType === 'DOC' && !!document.content

  return (
    <div className={`${styles.page} ${scrolled ? styles.pageScrolled : ''}`}>
      <Header onToggleSidebar={() => {}} showMenuButton={false} />
      <div className={styles.pageShell}>
        <header className={`${styles.topbar} ${scrolled ? styles.topbarScrolled : ''}`}>
          <div className={styles.topbarMain}>
            <button
              type="button"
              className={styles.backButton}
              onClick={isShareMode ? () => navigate('/') : backToKnowledgeBase}
            >
              {isShareMode ? '返回工作台' : '返回知识库'}
            </button>
            <div className={styles.documentIdentity}>
              <div className={styles.eyebrow}>{isShareMode ? '分享阅读' : '文档阅读'}</div>
              <h1 className={styles.title}>{document.title}</h1>
              <div className={styles.documentSubline}>
                <span className={styles.metaPill}>v{document.versionNo}</span>
                <span className={styles.metaPill}>{dayjs(document.updatedAt).format('MM-DD HH:mm')}</span>
                <span className={styles.metaPill}>{isShareMode ? '只读访问' : '阅读模式'}</span>
              </div>
            </div>
          </div>
          <div className={styles.documentToolbar}>
            <button type="button" className={styles.secondaryButton} onClick={() => setShareOpen(true)}>
              分享文档
            </button>
            {!isShareMode && (
              <button type="button" className={styles.primaryButton} onClick={() => navigate(`/docs/${document.id}/edit`)}>
                继续编辑
              </button>
            )}
          </div>
        </header>

        <main className={styles.content}>
          <section className={styles.readerCard}>
            <div className={styles.documentStage}>
              {shouldRenderRichPreview ? (
                <article className={styles.richPreview}>
                  <div className={styles.richPreviewBody}>
                    <div className={styles.documentPaperHeader}>
                      <h2 className={styles.paperTitle}>{document.title}</h2>
                      {document.summary ? <p className={styles.documentSummary}>{document.summary}</p> : null}
                    </div>
                    <div className={styles.richPreviewContent} dangerouslySetInnerHTML={{ __html: document.content }} />
                  </div>
                </article>
              ) : (
                <div className={styles.preview}>
                  <div className={styles.documentPaperHeader}>
                    <h2 className={styles.paperTitle}>{document.title}</h2>
                    {document.summary ? <p className={styles.documentSummary}>{document.summary}</p> : null}
                  </div>
                  <div className={styles.previewContent}>
                    {document.contentText || document.content || '当前文档暂无正文。'}
                  </div>
                </div>
              )}
            </div>
          </section>
        </main>
      </div>

      <DocumentShareDrawer
        open={shareOpen}
        documentId={document.id}
        title={document.title}
        onClose={() => setShareOpen(false)}
      />
    </div>
  )
}

export default DocumentReaderPage

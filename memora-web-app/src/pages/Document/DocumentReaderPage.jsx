import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import dayjs from 'dayjs'
import PageState from '../../components/Feedback/PageState'
import Header from '../../components/Layout/Header'
import DocumentRenderedContent from '../../components/Document/DocumentRenderedContent'
import DocumentReadLinkDrawer from '../../components/Document/DocumentReadLinkDrawer'
import DocumentShareDrawer from '../../components/Document/DocumentShareDrawer'
import { documentApi } from '../../services/api/documentApi'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { getDocumentRenderState } from '../../utils/documentContent'
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
  const [scrolled, setScrolled] = useState(false)
  const [pageStatus, setPageStatus] = useState(PAGE_STATUS.LOADING)
  const [pageErrorMessage, setPageErrorMessage] = useState('')
  const [document, setDocument] = useState(null)
  const [knowledgeBaseAccess, setKnowledgeBaseAccess] = useState(null)
  const [readLinkOpen, setReadLinkOpen] = useState(false)
  const [shareDrawerOpen, setShareDrawerOpen] = useState(false)
  useDocumentTitle(document ? document.title : '文档阅读')

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

      const knowledgeBaseResponse = await knowledgeBaseApi.getKnowledgeBaseById(documentData.knowledgeBaseId)
      setDocument(documentData)
      setKnowledgeBaseAccess(knowledgeBaseResponse?.data || null)
      setPageStatus(PAGE_STATUS.READY)
    } catch (error) {
      console.error('加载文档阅读页失败', error)
      setDocument(null)
      setKnowledgeBaseAccess(null)

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

  const documentRenderState = useMemo(() => {
    return getDocumentRenderState({
      format: document?.format,
      content: document?.content,
      contentText: document?.contentText,
      renderedHtml: document?.renderedHtml,
    })
  }, [document?.format, document?.content, document?.contentText, document?.renderedHtml])

  if (pageStatus === PAGE_STATUS.LOADING) {
    return <div className={styles.state}>正在加载文档...</div>
  }

  if (pageStatus !== PAGE_STATUS.READY || !document) {
    return (
      <PageState
        eyebrow={pageStatus === PAGE_STATUS.FORBIDDEN ? '无权访问' : pageStatus === PAGE_STATUS.NOT_FOUND ? '内容不存在' : '阅读页不可用'}
        title={pageStatus === PAGE_STATUS.FORBIDDEN ? '当前会话无权访问该文档' : pageStatus === PAGE_STATUS.NOT_FOUND ? '当前文档不存在' : '文档阅读页暂时不可用'}
        description={pageErrorMessage || '请返回工作台或知识库继续操作。'}
        primaryAction={{ label: '返回工作台', onClick: () => navigate('/') }}
        secondaryAction={{ label: '重新加载', onClick: loadDocument }}
      />
    )
  }

  const shouldRenderRichPreview = document.docType === 'DOC' && documentRenderState.hasRenderedContent
  const canWriteKnowledgeBase = !!knowledgeBaseAccess?.canWrite
  const canManageKnowledgeBase = !!knowledgeBaseAccess?.canManage
  const publicUrl = document.publicUrl
    || (knowledgeBaseAccess?.siteUrl && document.publicSlug ? `${knowledgeBaseAccess.siteUrl}/${document.publicSlug}` : '')

  return (
    <div className={`${styles.page} ${scrolled ? styles.pageScrolled : ''}`}>
      <Header onToggleSidebar={() => {}} showMenuButton={false} />
      <div className={styles.pageShell}>
        <header className={`${styles.topbar} ${scrolled ? styles.topbarScrolled : ''}`}>
          <div className={styles.topbarMain}>
            <button
              type="button"
              className={styles.backButton}
              onClick={backToKnowledgeBase}
            >
              返回知识库
            </button>
            <div className={styles.documentIdentity}>
              <div className={styles.eyebrow}>文档阅读</div>
              <h1 className={styles.title}>{document.title}</h1>
              <div className={styles.documentSubline}>
                <span className={styles.metaPill}>v{document.versionNo}</span>
                <span className={styles.metaPill}>{dayjs(document.updatedAt).format('MM-DD HH:mm')}</span>
                <span className={styles.metaPill}>{canWriteKnowledgeBase ? '可继续编辑' : '当前角色只读'}</span>
                {document.publishStatus === 'PUBLISHED' ? <span className={styles.metaPill}>已正式发布</span> : null}
              </div>
            </div>
          </div>
          <div className={styles.documentToolbar}>
            {publicUrl ? (
              <button type="button" className={styles.secondaryButton} onClick={() => window.open(publicUrl, '_blank', 'noopener,noreferrer')}>
                打开公开页
              </button>
            ) : null}
            <button type="button" className={styles.secondaryButton} onClick={() => setReadLinkOpen(true)}>
              复制阅读链接
            </button>
            {canManageKnowledgeBase ? (
              <button type="button" className={styles.secondaryButton} onClick={() => setShareDrawerOpen(true)}>
                受控分享
              </button>
            ) : null}
            {canWriteKnowledgeBase ? (
              <button type="button" className={styles.primaryButton} onClick={() => navigate(`/docs/${document.id}/edit`)}>
                继续编辑
              </button>
            ) : null}
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
                    <DocumentRenderedContent
                      className={styles.richPreviewContent}
                      html={documentRenderState.html}
                      variant="article"
                    />
                  </div>
                </article>
              ) : (
                <div className={styles.preview}>
                  <div className={styles.documentPaperHeader}>
                    <h2 className={styles.paperTitle}>{document.title}</h2>
                    {document.summary ? <p className={styles.documentSummary}>{document.summary}</p> : null}
                  </div>
                  <div className={styles.previewContent}>
                    {documentRenderState.plainText || '当前文档暂无正文。'}
                  </div>
                </div>
              )}
            </div>
          </section>
        </main>
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

export default DocumentReaderPage

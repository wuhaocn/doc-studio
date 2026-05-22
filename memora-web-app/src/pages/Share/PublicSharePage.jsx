import { useCallback, useEffect, useMemo, useState } from 'react'
import dayjs from 'dayjs'
import { useParams } from 'react-router-dom'
import DocumentRenderedContent from '../../components/Document/DocumentRenderedContent'
import PageState from '../../components/Feedback/PageState'
import { publicShareApi } from '../../services/api/publicShareApi'
import { getDocumentRenderState } from '../../utils/documentContent'
import styles from './PublicSharePage.module.css'

const PAGE_STATUS = {
  LOADING: 'loading',
  NEED_ACCESS_CODE: 'need_access_code',
  READY: 'ready',
  BLOCKED: 'blocked',
  ERROR: 'error',
}

const PublicSharePage = () => {
  const { token } = useParams()
  const [pageStatus, setPageStatus] = useState(PAGE_STATUS.LOADING)
  const [shareInfo, setShareInfo] = useState(null)
  const [document, setDocument] = useState(null)
  const [accessCode, setAccessCode] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const renderState = useMemo(() => {
    return getDocumentRenderState({
      format: document?.format,
      content: document?.content,
      contentText: document?.contentText,
      renderedHtml: document?.renderedHtml,
    })
  }, [document?.format, document?.content, document?.contentText, document?.renderedHtml])
  const shouldRenderRichContent = renderState.hasRenderedContent

  const handleTerminalError = useCallback((error) => {
    console.error('加载受控分享失败', error)
    setDocument(null)
    setErrorMessage(error?.message || '当前分享暂时不可用')
    if (error?.code === 404 || error?.code === 410) {
      setPageStatus(PAGE_STATUS.BLOCKED)
      return
    }
    setPageStatus(PAGE_STATUS.ERROR)
  }, [])

  const loadSharedDocument = useCallback(async (nextAccessCode) => {
    try {
      setSubmitting(true)
      setErrorMessage('')
      const response = await publicShareApi.accessShare(token, nextAccessCode ? { accessCode: nextAccessCode } : {})
      setDocument(response?.data || null)
      setPageStatus(PAGE_STATUS.READY)
    } catch (error) {
      if (error?.code === 403) {
        setPageStatus(PAGE_STATUS.NEED_ACCESS_CODE)
        setErrorMessage(error?.message || '访问码错误，请重新输入')
        return
      }
      handleTerminalError(error)
    } finally {
      setSubmitting(false)
    }
  }, [handleTerminalError, token])

  useEffect(() => {
    let cancelled = false

    const bootstrap = async () => {
      try {
        setPageStatus(PAGE_STATUS.LOADING)
        setErrorMessage('')
        const response = await publicShareApi.getShareInfo(token)
        const info = response?.data || null

        if (cancelled) {
          return
        }

        setShareInfo(info)
        if (info?.accessCodeRequired) {
          setPageStatus(PAGE_STATUS.NEED_ACCESS_CODE)
          return
        }

        await loadSharedDocument('')
      } catch (error) {
        if (cancelled) {
          return
        }
        handleTerminalError(error)
      }
    }

    bootstrap()
    return () => {
      cancelled = true
    }
  }, [handleTerminalError, loadSharedDocument, token])

  const handleSubmitAccessCode = async (event) => {
    event.preventDefault()
    await loadSharedDocument(accessCode.trim())
  }

  if (pageStatus === PAGE_STATUS.LOADING) {
    return <div className={styles.state}>正在验证分享链接...</div>
  }

  if (pageStatus === PAGE_STATUS.BLOCKED || pageStatus === PAGE_STATUS.ERROR) {
    return (
      <PageState
        eyebrow={pageStatus === PAGE_STATUS.BLOCKED ? '分享不可用' : '访问失败'}
        title={pageStatus === PAGE_STATUS.BLOCKED ? '当前受控分享已失效' : '当前分享暂时无法打开'}
        description={errorMessage || '请联系文档提供方重新生成可用分享链接。'}
        primaryAction={{ label: '重新加载', onClick: () => window.location.reload() }}
      />
    )
  }

  return (
    <div className={styles.page}>
      <header className={styles.hero}>
        <div>
          <p className={styles.eyebrow}>Memora 受控分享</p>
          <h1 className={styles.title}>{document?.title || shareInfo?.documentTitle || '外部只读文档'}</h1>
          <div className={styles.meta}>
            {shareInfo?.knowledgeBaseName ? <span>{shareInfo.knowledgeBaseName}</span> : null}
            {document?.versionNo ? <span>v{document.versionNo}</span> : null}
            {document?.updatedAt ? <span>更新于 {dayjs(document.updatedAt).format('YYYY-MM-DD HH:mm')}</span> : null}
            {shareInfo?.expiresAt ? <span>分享截止 {dayjs(shareInfo.expiresAt).format('YYYY-MM-DD HH:mm')}</span> : null}
          </div>
        </div>
        <div className={styles.badge}>只读访问</div>
      </header>

      {pageStatus === PAGE_STATUS.NEED_ACCESS_CODE ? (
        <section className={styles.accessCard}>
          <div>
            <h2>输入访问码继续查看</h2>
            <p>文档提供方为这条受控分享设置了访问码，输入正确后即可只读浏览正文。</p>
          </div>
          <form className={styles.accessForm} onSubmit={handleSubmitAccessCode}>
            <input
              type="password"
              value={accessCode}
              onChange={(event) => setAccessCode(event.target.value)}
              placeholder="请输入访问码"
              maxLength={32}
            />
            <button type="submit" className={styles.primaryButton} disabled={submitting}>
              {submitting ? '验证中...' : '验证并打开'}
            </button>
          </form>
          {errorMessage ? <div className={styles.errorMessage}>{errorMessage}</div> : null}
        </section>
      ) : null}

      {pageStatus === PAGE_STATUS.READY && document ? (
        <main className={styles.readerCard}>
          <div className={styles.paperHeader}>
            <h2>{document.title}</h2>
            {document.summary ? <p>{document.summary}</p> : null}
          </div>
          {shouldRenderRichContent ? (
            <DocumentRenderedContent
              className={styles.richContent}
              html={renderState.html}
              variant="article"
            />
          ) : (
            <div className={styles.plainContent}>{renderState.plainText || '当前文档暂无正文。'}</div>
          )}
        </main>
      ) : null}
    </div>
  )
}

export default PublicSharePage

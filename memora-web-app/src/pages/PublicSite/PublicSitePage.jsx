import { useEffect, useMemo, useState } from 'react'
import dayjs from 'dayjs'
import { Link, useParams } from 'react-router-dom'
import DocumentEntryCard from '../../components/Document/DocumentEntryCard'
import DocumentRenderedContent from '../../components/Document/DocumentRenderedContent'
import PageState from '../../components/Feedback/PageState'
import { publicSiteApi } from '../../services/api/publicSiteApi'
import { getDocumentRenderState } from '../../utils/documentContent'
import styles from './PublicSitePage.module.css'

const PAGE_STATUS = {
  LOADING: 'loading',
  READY: 'ready',
  NOT_FOUND: 'not_found',
  ERROR: 'error',
}

const PublicSitePage = () => {
  const { siteSlug, publicSlug } = useParams()
  const [pageStatus, setPageStatus] = useState(PAGE_STATUS.LOADING)
  const [errorMessage, setErrorMessage] = useState('')
  const [site, setSite] = useState(null)
  const [document, setDocument] = useState(null)

  useEffect(() => {
    let cancelled = false

    const load = async () => {
      try {
        setPageStatus(PAGE_STATUS.LOADING)
        setErrorMessage('')
        const siteResponse = await publicSiteApi.getSite(siteSlug)
        if (cancelled) {
          return
        }
        const siteData = siteResponse?.data || null
        setSite(siteData)

        if (!publicSlug) {
          setDocument(null)
          setPageStatus(PAGE_STATUS.READY)
          return
        }

        const documentResponse = await publicSiteApi.getDocument(siteSlug, publicSlug)
        if (cancelled) {
          return
        }
        setDocument(documentResponse?.data || null)
        setPageStatus(PAGE_STATUS.READY)
      } catch (error) {
        if (cancelled) {
          return
        }
        console.error('加载公开站点失败', error)
        setSite(null)
        setDocument(null)
        setErrorMessage(error?.message || '公开站点暂时不可用')
        setPageStatus(error?.code === 404 ? PAGE_STATUS.NOT_FOUND : PAGE_STATUS.ERROR)
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [publicSlug, siteSlug])

  const navigation = useMemo(() => document?.navigation || site?.documents || [], [document?.navigation, site?.documents])
  const renderState = useMemo(() => {
    return getDocumentRenderState({
      format: document?.format,
      content: '',
      contentText: document?.contentText,
      renderedHtml: document?.renderedHtml,
    })
  }, [document?.contentText, document?.format, document?.renderedHtml])

  if (pageStatus === PAGE_STATUS.LOADING) {
    return <div className={styles.state}>正在加载公开站点...</div>
  }

  if (pageStatus !== PAGE_STATUS.READY || !site) {
    return (
      <PageState
        eyebrow={pageStatus === PAGE_STATUS.NOT_FOUND ? '站点不存在' : '公开页不可用'}
        title={pageStatus === PAGE_STATUS.NOT_FOUND ? '当前公开站点不存在' : '当前公开站点暂时无法打开'}
        description={errorMessage || '请联系内容提供方确认公开链接是否有效。'}
        primaryAction={{ label: '重新加载', onClick: () => window.location.reload() }}
      />
    )
  }

  return (
    <div className={styles.page}>
      <header className={styles.hero}>
        <div className={styles.heroMain}>
          <p className={styles.eyebrow}>公开站点</p>
          <h1 className={styles.title}>{site.siteTitle}</h1>
          {site.siteDescription ? <p className={styles.description}>{site.siteDescription}</p> : null}
          <div className={styles.meta}>
            <span>{site.knowledgeBaseName}</span>
            <span>{navigation.length} 篇已发布文档</span>
            {document?.updatedAt ? <span>最近更新 {dayjs(document.updatedAt).format('YYYY-MM-DD HH:mm')}</span> : null}
          </div>
        </div>
      </header>

      <div className={styles.layout}>
        <aside className={styles.sidebar}>
          <div className={styles.sidebarHeader}>
            <h2>目录</h2>
            <span>{navigation.length} 篇</span>
          </div>
          <nav className={styles.navList}>
            {navigation.map((item) => (
              <Link
                key={item.documentId}
                className={`${styles.navItem} ${item.publicSlug === publicSlug ? styles.navItemActive : ''}`}
                to={`/site/${site.siteSlug}/${item.publicSlug}`}
                style={{ paddingLeft: `${16 + (item.depth || 0) * 16}px` }}
              >
                <span className={styles.navTitle}>{item.title}</span>
                {item.summary ? <span className={styles.navSummary}>{item.summary}</span> : null}
              </Link>
            ))}
          </nav>
        </aside>

        <main className={styles.content}>
          {document ? (
            <article className={styles.article}>
              <div className={styles.articleHeader}>
                <h2>{document.title}</h2>
                <div className={styles.articleMeta}>
                  <span>v{document.versionNo}</span>
                  {document.publishedAt ? <span>发布于 {dayjs(document.publishedAt).format('YYYY-MM-DD HH:mm')}</span> : null}
                  {document.updatedAt ? <span>更新于 {dayjs(document.updatedAt).format('YYYY-MM-DD HH:mm')}</span> : null}
                </div>
                {document.summary ? <p className={styles.articleSummary}>{document.summary}</p> : null}
              </div>
              {renderState.hasRenderedContent ? (
                <DocumentRenderedContent
                  className={styles.articleBody}
                  html={renderState.html}
                  variant="article"
                />
              ) : (
                <div className={styles.emptyContent}>当前公开文档暂无正文。</div>
              )}
            </article>
          ) : (
            <section className={styles.homePanel}>
              <div className={styles.homeCard}>
                <h2>已发布文档</h2>
                <p>这里展示当前知识库已经正式公开的内容入口。选择任一条目即可直接阅读。</p>
              </div>
              <div className={styles.cardGrid}>
                {navigation.length > 0 ? navigation.map((item) => (
                  <DocumentEntryCard
                    key={item.documentId}
                    className={styles.siteCard}
                    to={`/site/${site.siteSlug}/${item.publicSlug}`}
                    eyebrow="公开文档"
                    title={item.title}
                    summary={item.summary || '查看这篇公开文档的正文内容。'}
                    badges={[{ key: 'published', label: '已发布', tone: 'success' }]}
                  />
                )) : (
                  <div className={styles.homeCard}>
                    当前站点还没有已发布文档。
                  </div>
                )}
              </div>
            </section>
          )}
        </main>
      </div>
    </div>
  )
}

export default PublicSitePage

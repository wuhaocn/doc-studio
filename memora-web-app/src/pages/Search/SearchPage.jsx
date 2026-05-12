import { startTransition, useDeferredValue, useEffect, useMemo, useState } from 'react'
import dayjs from 'dayjs'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import PageState from '../../components/Feedback/PageState'
import { useAuth } from '../../contexts/AuthContext'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { useKnowledgeBaseNavigation } from '../../hooks/useKnowledgeBaseNavigation'
import { documentApi } from '../../services/api/documentApi'
import { addRecentSearch, clearRecentSearches, getRecentSearches } from '../../utils/searchHistory'
import { groupSearchResultsByKnowledgeBase } from '../../utils/workspaceSearch'
import styles from './SearchPage.module.css'

const SEARCH_STATUS = {
  IDLE: 'idle',
  LOADING: 'loading',
  READY: 'ready',
  EMPTY: 'empty',
  ERROR: 'error',
}

const SearchPage = () => {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const { currentUser } = useAuth()
  useDocumentTitle('搜索')
  const { knowledgeBases } = useKnowledgeBaseNavigation(currentUser.tenantId, {
    errorMessage: '加载搜索知识库上下文失败',
  })
  const [keyword, setKeyword] = useState(searchParams.get('keyword') || '')
  const deferredKeyword = useDeferredValue(keyword.trim())
  const activeKeyword = searchParams.get('keyword')?.trim() || ''
  const [status, setStatus] = useState(searchParams.get('keyword') ? SEARCH_STATUS.LOADING : SEARCH_STATUS.IDLE)
  const [errorMessage, setErrorMessage] = useState('')
  const [records, setRecords] = useState([])
  const [reloadVersion, setReloadVersion] = useState(0)

  useEffect(() => {
    setKeyword(searchParams.get('keyword') || '')
  }, [searchParams])

  useEffect(() => {
    let cancelled = false

    const loadResults = async () => {
      if (!activeKeyword) {
        setStatus(SEARCH_STATUS.IDLE)
        setRecords([])
        setErrorMessage('')
        return
      }

      try {
        setStatus(SEARCH_STATUS.LOADING)
        setErrorMessage('')
        const response = await documentApi.searchDocuments(activeKeyword, { size: 50 })
        if (cancelled) {
          return
        }
        const nextRecords = (response?.data?.records || []).filter((item) => item?.docType === 'DOC')
        setRecords(nextRecords)
        setStatus(nextRecords.length > 0 ? SEARCH_STATUS.READY : SEARCH_STATUS.EMPTY)
      } catch (error) {
        if (cancelled) {
          return
        }
        console.error('加载统一搜索结果失败', error)
        setRecords([])
        setStatus(SEARCH_STATUS.ERROR)
        setErrorMessage(error?.message || '搜索失败，请稍后重试')
      }
    }

    loadResults()

    return () => {
      cancelled = true
    }
  }, [activeKeyword, reloadVersion])

  const groupedResults = useMemo(() => {
    return groupSearchResultsByKnowledgeBase(records, knowledgeBases, deferredKeyword)
  }, [deferredKeyword, knowledgeBases, records])

  const totalHits = groupedResults.reduce((sum, group) => sum + group.hitCount, 0)

  const handleSubmit = (event) => {
    event.preventDefault()
    const nextKeyword = keyword.trim()
    startTransition(() => {
      if (!nextKeyword) {
        setSearchParams({})
        return
      }
      addRecentSearch(nextKeyword)
      setRecentSearches(getRecentSearches())
      setSearchParams({ keyword: nextKeyword })
    })
  }

  const [recentSearches, setRecentSearches] = useState(() => getRecentSearches())

  if (status === SEARCH_STATUS.IDLE) {
    return (
      <div className={styles.page}>
        <section className={styles.hero}>
          <div>
            <p className={styles.eyebrow}>统一搜索</p>
            <h1 className={styles.title}>从整个工作区里找文档</h1>
            <p className={styles.description}>
              输入标题或正文关键词后，会按知识库分组返回当前工作区里你有权阅读的结果。
            </p>
          </div>
        </section>
        <form className={styles.searchForm} onSubmit={handleSubmit}>
          <input
            className={styles.searchInput}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="输入关键词搜索..."
            autoFocus
          />
          <button type="submit" className={styles.searchButton}>搜索</button>
        </form>
        {recentSearches.length > 0 && (
          <section className={styles.recentSection}>
            <div className={styles.recentHeader}>
              <span className={styles.recentLabel}>最近搜索</span>
              <button type="button" className={styles.recentClear} onClick={() => { clearRecentSearches(); setRecentSearches([]) }}>
                清除
              </button>
            </div>
            <div className={styles.recentList}>
              {recentSearches.map((item) => (
                <button
                  key={item}
                  type="button"
                  className={styles.recentItem}
                  onClick={() => { setKeyword(item); setSearchParams({ keyword: item }) }}
                >
                  {item}
                </button>
              ))}
            </div>
          </section>
        )}
      </div>
    )
  }

  if (status === SEARCH_STATUS.LOADING) {
    return <div className={styles.state}>正在搜索工作区内容...</div>
  }

  if (status === SEARCH_STATUS.ERROR) {
    return (
      <PageState
        eyebrow="搜索失败"
        title="统一搜索暂时不可用"
        description={errorMessage || '请稍后重试，或先返回工作台继续浏览知识库。'}
        primaryAction={{ label: '重新搜索', onClick: () => setReloadVersion((current) => current + 1) }}
        secondaryAction={{ label: '返回工作台', onClick: () => navigate('/') }}
      />
    )
  }

  if (status === SEARCH_STATUS.EMPTY) {
    return (
      <PageState
        eyebrow="没有命中"
        title={`没有找到“${deferredKeyword}”`}
        description="可以尝试缩短关键词、换用正文里的自然语言，或者先回到知识库继续浏览。"
        primaryAction={{ label: '返回工作台', onClick: () => navigate('/') }}
      />
    )
  }

  return (
    <div className={styles.page}>
      <section className={styles.hero}>
        <div>
          <p className={styles.eyebrow}>统一搜索</p>
          <h1 className={styles.title}>在当前工作区里找文档</h1>
          <p className={styles.description}>
            已按知识库边界过滤，只返回当前会话在工作区内可访问的结果。
          </p>
        </div>
        <form className={styles.searchForm} onSubmit={handleSubmit}>
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="搜索标题或正文，例如：交接、升级、巡检"
          />
          <button type="submit">更新搜索</button>
        </form>
      </section>

      <section className={styles.summaryBar}>
        <span>{totalHits} 条命中</span>
        <span>{groupedResults.length} 个知识库</span>
        <span>关键词：{deferredKeyword}</span>
      </section>

      <div className={styles.groupList}>
        {groupedResults.map((group) => (
          <section key={group.knowledgeBaseId} className={styles.groupCard}>
            <div className={styles.groupHeader}>
              <div>
                <div className={styles.groupTitleRow}>
                  <h2>{group.knowledgeBaseName}</h2>
                  <span className={styles.roleBadge}>{group.currentRole}</span>
                  {group.permissionRestricted ? <span className={styles.scopeBadge}>独立权限</span> : null}
                </div>
                <p className={styles.groupMeta}>
                  {group.hitCount} 条结果 · {group.canWrite ? '可直接编辑命中文档' : '当前角色只读，结果仅可阅读'}
                </p>
              </div>
              <Link to={`/kb/${group.knowledgeBaseId}`} className={styles.groupLink}>
                进入知识库
              </Link>
            </div>

            <div className={styles.resultList}>
              {group.documents.map((document) => (
                <article key={document.id} className={styles.resultItem}>
                  <div className={styles.resultMain}>
                    <div className={styles.resultTopline}>
                      <strong>{document.title}</strong>
                      <span>v{document.versionNo}</span>
                    </div>
                    <p className={styles.resultExcerpt}>{document.excerpt}</p>
                    <div className={styles.resultMeta}>
                      <span>{dayjs(document.updatedAt).format('MM-DD HH:mm')}</span>
                      <span>{document.summary || '暂无摘要'}</span>
                    </div>
                  </div>
                  <div className={styles.resultActions}>
                    <Link to={`/docs/${document.id}`} className={styles.secondaryButton}>
                      阅读
                    </Link>
                    {group.canWrite ? (
                      <Link to={`/docs/${document.id}/edit`} className={styles.primaryButton}>
                        继续编辑
                      </Link>
                    ) : null}
                  </div>
                </article>
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  )
}

export default SearchPage

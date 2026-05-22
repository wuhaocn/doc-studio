import { startTransition, useDeferredValue, useEffect, useMemo, useState } from 'react'
import dayjs from 'dayjs'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import DocumentEntryCard from '../../components/Document/DocumentEntryCard'
import PageState from '../../components/Feedback/PageState'
import { useAuth } from '../../contexts/AuthContext'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { useKnowledgeBaseNavigation } from '../../hooks/useKnowledgeBaseNavigation'
import { documentApi } from '../../services/api/documentApi'
import { addRecentSearch, clearRecentSearches, getRecentSearches } from '../../utils/searchHistory'
import { buildKeywordHighlightParts, groupSearchResultsByKnowledgeBase } from '../../utils/workspaceSearch'
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
  const [knowledgeBaseScope, setKnowledgeBaseScope] = useState(searchParams.get('knowledgeBaseId') || '')
  const deferredKeyword = useDeferredValue(keyword.trim())
  const activeKeyword = searchParams.get('keyword')?.trim() || ''
  const activeKnowledgeBaseScope = searchParams.get('knowledgeBaseId') || ''
  const activeKnowledgeBaseId = Number(activeKnowledgeBaseScope || 0)
  const [status, setStatus] = useState(searchParams.get('keyword') ? SEARCH_STATUS.LOADING : SEARCH_STATUS.IDLE)
  const [errorMessage, setErrorMessage] = useState('')
  const [records, setRecords] = useState([])
  const [reloadVersion, setReloadVersion] = useState(0)

  useEffect(() => {
    setKeyword(searchParams.get('keyword') || '')
    setKnowledgeBaseScope(searchParams.get('knowledgeBaseId') || '')
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
        const response = await documentApi.searchDocuments(activeKeyword, {
          size: 50,
          knowledgeBaseId: activeKnowledgeBaseId > 0 ? activeKnowledgeBaseId : undefined,
        })
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
  }, [activeKeyword, activeKnowledgeBaseId, reloadVersion])

  const groupedResults = useMemo(() => {
    return groupSearchResultsByKnowledgeBase(records, knowledgeBases, deferredKeyword)
  }, [deferredKeyword, knowledgeBases, records])

  const totalHits = groupedResults.reduce((sum, group) => sum + group.hitCount, 0)
  const activeKnowledgeBase = knowledgeBases.find((item) => item.id === activeKnowledgeBaseId) || null

  const renderHighlightedText = (text) => {
    const parts = buildKeywordHighlightParts(text, deferredKeyword)
    if (parts.length === 0) {
      return text
    }
    return parts.map((part, index) => (
      part.matched ? <mark key={`${part.text}-${index}`} className={styles.highlight}>{part.text}</mark> : <span key={`${part.text}-${index}`}>{part.text}</span>
    ))
  }

  const updateSearchParams = (nextKeyword, nextKnowledgeBaseScope) => {
    const params = {}
    if (nextKeyword) {
      params.keyword = nextKeyword
    }
    if (nextKnowledgeBaseScope) {
      params.knowledgeBaseId = nextKnowledgeBaseScope
    }
    setSearchParams(params)
  }

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
      updateSearchParams(nextKeyword, knowledgeBaseScope)
    })
  }

  const [recentSearches, setRecentSearches] = useState(() => getRecentSearches())
  const pageTitle = activeKeyword ? '搜索结果' : '搜索'
  const pageDescription = activeKeyword
    ? `关键词：${activeKeyword}${activeKnowledgeBase ? ` · 范围：${activeKnowledgeBase.name}` : ' · 范围：全部知识库'}`
    : ''
  const renderSearchToolbar = () => (
    <section className={styles.toolbar}>
      <div className={styles.toolbarHeader}>
        <p className={styles.eyebrow}>统一搜索</p>
        <h1 className={styles.title}>{pageTitle}</h1>
        {pageDescription ? <p className={styles.description}>{pageDescription}</p> : null}
      </div>
      <form className={styles.searchForm} onSubmit={handleSubmit}>
        <input
          className={styles.searchInput}
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="搜索标题或正文"
          autoFocus={status === SEARCH_STATUS.IDLE}
        />
        <select
          className={styles.searchSelect}
          value={knowledgeBaseScope}
          onChange={(event) => setKnowledgeBaseScope(event.target.value)}
        >
          <option value="">全部知识库</option>
          {knowledgeBases.map((knowledgeBase) => (
            <option key={knowledgeBase.id} value={String(knowledgeBase.id)}>
              {knowledgeBase.name}
            </option>
          ))}
        </select>
        <button type="submit" className={styles.searchButton}>搜索</button>
      </form>
    </section>
  )

  if (status === SEARCH_STATUS.IDLE) {
    return (
      <div className={styles.page}>
        {renderSearchToolbar()}
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
                  onClick={() => { setKeyword(item); updateSearchParams(item, knowledgeBaseScope) }}
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
    return (
      <div className={styles.page}>
        {renderSearchToolbar()}
        <div className={styles.surfaceState}>搜索中...</div>
      </div>
    )
  }

  if (status === SEARCH_STATUS.ERROR) {
    return (
      <div className={styles.page}>
        {renderSearchToolbar()}
        <PageState
          title="搜索暂时不可用"
          description={errorMessage || '请稍后重试'}
          primaryAction={{ label: '重试', onClick: () => setReloadVersion((current) => current + 1) }}
          secondaryAction={{ label: '返回', onClick: () => navigate('/') }}
        />
      </div>
    )
  }

  if (status === SEARCH_STATUS.EMPTY) {
    return (
      <div className={styles.page}>
        {renderSearchToolbar()}
        <section className={styles.summaryBar}>
          <span>0 条命中</span>
          <span>关键词：{deferredKeyword}</span>
          <span>范围：{activeKnowledgeBase ? activeKnowledgeBase.name : '全部知识库'}</span>
        </section>
        <section className={styles.emptyCard}>
          <strong>{`没有找到“${deferredKeyword}”`}</strong>
          <p>{activeKnowledgeBase ? `当前范围：${activeKnowledgeBase.name}。尝试其他关键词或切换知识库范围。` : '尝试其他关键词或切换知识库范围。'}</p>
          <button type="button" className={styles.secondaryButton} onClick={() => navigate('/')}>
            返回工作台
          </button>
        </section>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      {renderSearchToolbar()}

      <section className={styles.summaryBar}>
        <span>{totalHits} 条命中</span>
        <span>{groupedResults.length} 个知识库</span>
        <span>关键词：{deferredKeyword}</span>
        <span>范围：{activeKnowledgeBase ? activeKnowledgeBase.name : '全部知识库'}</span>
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
                  {group.hitCount} 条结果 · {group.canWrite ? '可编辑' : '只读'}
                </p>
              </div>
              <Link to={`/kb/${group.knowledgeBaseId}`} className={styles.groupLink}>
                进入知识库
              </Link>
            </div>

            <div className={styles.resultList}>
              {group.documents.map((document) => (
                <DocumentEntryCard
                  key={document.id}
                  className={styles.resultCard}
                  title={renderHighlightedText(document.title)}
                  summary={renderHighlightedText(document.excerpt)}
                  badges={[
                    { key: `version-${document.id}`, label: `v${document.versionNo}` },
                    ...(document.matchLabel ? [{ key: `match-${document.id}`, label: document.matchLabel, tone: 'primary' }] : []),
                  ]}
                  details={(
                    <>
                      <div className={styles.resultMeta}>
                        <span>{dayjs(document.updatedAt).format('MM-DD HH:mm')}</span>
                        <span>{group.canWrite ? '可编辑' : '只读'}</span>
                      </div>
                      {document.summary ? <p className={styles.resultSummary}>{document.summary}</p> : null}
                    </>
                  )}
                  actions={(
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
                  )}
                />
              ))}
            </div>
          </section>
        ))}
      </div>
    </div>
  )
}

export default SearchPage

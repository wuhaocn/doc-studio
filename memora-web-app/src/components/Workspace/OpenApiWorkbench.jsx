import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import dayjs from 'dayjs'
import DocumentRenderedContent from '../Document/DocumentRenderedContent'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import { openDocumentApi } from '../../services/api/openDocumentApi'
import { API_BASE_URL } from '../../services/http/axios'
import { copyText } from '../../utils/copyText'
import { getDocumentRenderState } from '../../utils/documentContent'
import { buildFolderOptions } from '../../utils/knowledgeBaseTree'
import {
  buildCaptureBookmarkletSnippet,
  buildDialogCaptureSnippet,
  buildDefaultSourceExternalId,
  buildDefaultSourceRevision,
  buildOpenApiConsumeCurlSnippet,
  buildOpenApiCurlSnippet,
  buildOpenApiFetchSnippet,
  buildOpenReaderFetchSnippet,
  buildOpenApiUpsertPayload,
  resolveAbsoluteWorkbenchUrl,
} from '../../utils/openApiWorkbench'
import styles from './OpenApiWorkbench.module.css'

const UPSERT_STATUS_LABELS = {
  CREATED: '已创建',
  UPDATED: '已更新',
  SKIPPED: '已跳过',
  CONFLICTED: '发生冲突',
}

const CONSUME_VIEWS = [
  { value: 'metadata', label: 'metadata' },
  { value: 'rendered', label: 'rendered' },
  { value: 'source', label: 'source' },
  { value: 'plain', label: 'plain' },
  { value: 'summary', label: 'summary' },
]

const EMPTY_INITIAL_VALUES = Object.freeze({})

const buildInitialForm = (knowledgeBases = [], initialValues = {}) => {
  const preferredKnowledgeBaseId = Number(initialValues.knowledgeBaseId || knowledgeBases[0]?.id || 0)
  const normalizedSourceUrl = `${initialValues.sourceUrl || ''}`.trim()
  return {
    apiKey: '',
    knowledgeBaseId: preferredKnowledgeBaseId,
    parentId: Number(initialValues.parentId || 0),
    title: `${initialValues.title || ''}`,
    format: `${initialValues.format || 'MARKDOWN'}`.toUpperCase(),
    content: `${initialValues.content || ''}`,
    sourceUrl: normalizedSourceUrl,
    sourceExternalId: `${initialValues.sourceExternalId || ''}`.trim()
      || buildDefaultSourceExternalId(preferredKnowledgeBaseId, normalizedSourceUrl),
    sourceRevision: `${initialValues.sourceRevision || ''}`.trim() || buildDefaultSourceRevision(),
  }
}

const isUpsertReadable = (status) => ['CREATED', 'UPDATED', 'SKIPPED'].includes(status)

const OpenApiWorkbench = ({
  mode = 'workspace',
  knowledgeBases = [],
  initialValues = EMPTY_INITIAL_VALUES,
  resetKey = 'default',
}) => {
  const lastResetKeyRef = useRef(null)
  const [form, setForm] = useState(() => buildInitialForm(knowledgeBases, initialValues))
  const [folderOptions, setFolderOptions] = useState([{ id: 0, label: '根目录' }])
  const [folderLoading, setFolderLoading] = useState(false)
  const [consumeView, setConsumeView] = useState('metadata')
  const [submitting, setSubmitting] = useState(false)
  const [reading, setReading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [feedback, setFeedback] = useState('')
  const [upsertResult, setUpsertResult] = useState(null)
  const [consumeResult, setConsumeResult] = useState(null)

  const workspaceMode = mode === 'workspace'
  const hasManagedKnowledgeBases = knowledgeBases.length > 0
  const webOrigin = typeof window !== 'undefined'
    ? window.location.origin.replace(/\/$/, '')
    : 'http://localhost:8800'
  const payload = useMemo(() => buildOpenApiUpsertPayload(form), [form])
  const renderState = useMemo(() => getDocumentRenderState({
    format: form.format,
    content: form.content,
    contentText: '',
  }), [form.content, form.format])
  const selectedKnowledgeBase = useMemo(
    () => knowledgeBases.find((item) => item.id === Number(form.knowledgeBaseId)) || null,
    [form.knowledgeBaseId, knowledgeBases],
  )
  const canSubmit = !!form.apiKey.trim()
    && payload.knowledgeBaseId > 0
    && !!payload.title
    && !!payload.sourceExternalId
    && !!payload.content.trim()

  useEffect(() => {
    if (lastResetKeyRef.current === resetKey) {
      return
    }
    lastResetKeyRef.current = resetKey
    setForm(buildInitialForm(knowledgeBases, initialValues))
    setFolderOptions([{ id: 0, label: '根目录' }])
    setConsumeView('metadata')
    setErrorMessage('')
    setFeedback('')
    setUpsertResult(null)
    setConsumeResult(null)
  }, [initialValues, knowledgeBases, resetKey])

  useEffect(() => {
    if (!hasManagedKnowledgeBases) {
      return
    }

    const hasCurrentKnowledgeBase = knowledgeBases.some((item) => item.id === Number(form.knowledgeBaseId))
    if (hasCurrentKnowledgeBase || !knowledgeBases[0]?.id) {
      return
    }

    setForm((current) => ({
      ...current,
      knowledgeBaseId: knowledgeBases[0].id,
      sourceExternalId: current.sourceExternalId || buildDefaultSourceExternalId(knowledgeBases[0].id, current.sourceUrl),
    }))
  }, [form.knowledgeBaseId, hasManagedKnowledgeBases, knowledgeBases])

  useEffect(() => {
    if (!hasManagedKnowledgeBases || !Number(form.knowledgeBaseId)) {
      setFolderOptions([{ id: 0, label: '根目录' }])
      return
    }

    let cancelled = false
    const loadFolderOptions = async () => {
      try {
        setFolderLoading(true)
        const response = await knowledgeBaseApi.getDocumentTree(form.knowledgeBaseId)
        if (cancelled) {
          return
        }
        const nextOptions = buildFolderOptions(response?.data || [])
        setFolderOptions(nextOptions)
        setForm((current) => (
          nextOptions.some((item) => item.id === Number(current.parentId))
            ? current
            : {
              ...current,
              parentId: 0,
            }
        ))
      } catch (error) {
        if (cancelled) {
          return
        }
        console.error('加载开放接入父目录选项失败', error)
        setFolderOptions([{ id: 0, label: '根目录' }])
      } finally {
        if (!cancelled) {
          setFolderLoading(false)
        }
      }
    }

    void loadFolderOptions()
    return () => {
      cancelled = true
    }
  }, [form.knowledgeBaseId, hasManagedKnowledgeBases])

  const copySnippet = async (label, text) => {
    try {
      await copyText(text)
      setFeedback(`${label} 已复制`)
      setErrorMessage('')
    } catch (error) {
      console.error('复制开放接入片段失败', error)
      setErrorMessage(`${label} 复制失败，请手动复制`)
    }
  }

  const loadBySource = useCallback(async (view, options = {}) => {
    const apiKey = `${options.apiKey ?? form.apiKey}`.trim()
    const knowledgeBaseId = Number(options.knowledgeBaseId ?? payload.knowledgeBaseId)
    const sourceExternalId = `${options.sourceExternalId ?? payload.sourceExternalId}`.trim()

    if (!apiKey || !knowledgeBaseId || !sourceExternalId) {
      setErrorMessage('请先填写访问密钥、知识库和来源标识')
      return null
    }

    try {
      setReading(true)
      setErrorMessage('')
      if (!options.silent) {
        setFeedback('')
      }
      const response = await openDocumentApi.getDocumentBySource(
        apiKey,
        knowledgeBaseId,
        sourceExternalId,
        view,
      )
      const nextConsumeResult = response?.data || null
      setConsumeResult(nextConsumeResult)
      setConsumeView(view)
      if (!options.silent) {
        setFeedback(`已按 ${view} 视图读取当前文档`)
      }
      return nextConsumeResult
    } catch (error) {
      console.error('按来源读取文档失败', error)
      setConsumeResult(null)
      setErrorMessage(error?.message || '按来源读取文档失败，请稍后重试')
      return null
    } finally {
      setReading(false)
    }
  }, [form.apiKey, payload.knowledgeBaseId, payload.sourceExternalId])

  const handleUpsert = async () => {
    const nextSourceRevision = payload.sourceRevision || buildDefaultSourceRevision()
    const nextPayload = {
      ...payload,
      sourceRevision: nextSourceRevision,
    }

    try {
      setSubmitting(true)
      setErrorMessage('')
      setFeedback('')
      if (!form.sourceRevision) {
        setForm((current) => ({
          ...current,
          sourceRevision: nextSourceRevision,
        }))
      }
      const response = await openDocumentApi.upsertDocument(form.apiKey, nextPayload)
      const nextUpsertResult = response?.data || null
      setUpsertResult(nextUpsertResult)
      setFeedback(`已完成保存/更新：${UPSERT_STATUS_LABELS[nextUpsertResult?.status] || '已处理'}`)
      if (isUpsertReadable(nextUpsertResult?.status)) {
        await loadBySource('metadata', {
          apiKey: form.apiKey,
          knowledgeBaseId: nextPayload.knowledgeBaseId,
          sourceExternalId: nextPayload.sourceExternalId,
          silent: true,
        })
      }
    } catch (error) {
      console.error('执行开放写入失败', error)
      setUpsertResult(null)
      setConsumeResult(null)
      setErrorMessage(error?.message || '执行开放写入失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  const handleRegenerateIdentity = () => {
    setForm((current) => ({
      ...current,
      sourceExternalId: buildDefaultSourceExternalId(current.knowledgeBaseId, current.sourceUrl),
      sourceRevision: buildDefaultSourceRevision(),
    }))
  }

  const openExternalLink = (path) => {
    const targetUrl = resolveAbsoluteWorkbenchUrl(path, webOrigin)
    if (!targetUrl) {
      return
    }
    window.open(targetUrl, '_blank', 'noopener,noreferrer')
  }

  const readerUrl = resolveAbsoluteWorkbenchUrl(
    consumeResult?.readerUrl || (upsertResult?.document?.id ? `/docs/${upsertResult.document.id}` : ''),
    webOrigin,
  )
  const siteUrl = resolveAbsoluteWorkbenchUrl(consumeResult?.siteUrl, webOrigin)
  const publicUrl = resolveAbsoluteWorkbenchUrl(
    consumeResult?.publicUrl || upsertResult?.document?.publicUrl,
    webOrigin,
  )

  const snippets = useMemo(() => {
    if (!workspaceMode) {
      return []
    }

    return [
      {
        id: 'curl',
        title: 'cURL 写入',
        description: '用于外部脚本或流水线先做单文档联调。',
        snippet: buildOpenApiCurlSnippet({
          apiBaseUrl: API_BASE_URL,
          payload,
        }),
      },
      {
        id: 'fetch',
        title: 'fetch 写入',
        description: '用于对话框插件、浏览器侧工具或 Node / Edge 轻脚本。',
        snippet: buildOpenApiFetchSnippet({
          apiBaseUrl: API_BASE_URL,
          payload,
        }),
      },
      {
        id: 'consume',
        title: '按来源读取',
        description: '用来源标识稳定定位文档，再按视图读取。',
        snippet: buildOpenApiConsumeCurlSnippet({
          apiBaseUrl: API_BASE_URL,
          knowledgeBaseId: payload.knowledgeBaseId,
          sourceExternalId: payload.sourceExternalId,
          view: consumeView,
        }),
      },
      {
        id: 'bookmarklet',
        title: '浏览器书签脚本',
        description: '从当前网页打开 Memora 捕获页，不直接跨站调用后端 API。',
        snippet: buildCaptureBookmarkletSnippet({
          webOrigin,
          knowledgeBaseId: payload.knowledgeBaseId,
          parentId: payload.parentId,
        }),
      },
      {
        id: 'dialog-save',
        title: '对话框保存入口',
        description: '把预生成的 Markdown / HTML 草稿打包进 capture/save，再由 Memora 页内完成保存或更新。',
        snippet: buildDialogCaptureSnippet({
          webOrigin,
          draft: {
            knowledgeBaseId: payload.knowledgeBaseId,
            parentId: payload.parentId,
            title: payload.title,
            format: payload.format,
            content: payload.content,
            sourceUrl: form.sourceUrl,
            sourceExternalId: payload.sourceExternalId,
            sourceRevision: payload.sourceRevision,
          },
        }),
      },
      {
        id: 'dialog-open',
        title: '按来源打开阅读页',
        description: '先按来源标识解析文档，再打开当前登录态下的只读阅读页。',
        snippet: buildOpenReaderFetchSnippet({
          apiBaseUrl: API_BASE_URL,
          knowledgeBaseId: payload.knowledgeBaseId,
          sourceExternalId: payload.sourceExternalId,
          webOrigin,
        }),
      },
    ]
  }, [consumeView, form.sourceUrl, payload, webOrigin, workspaceMode])

  return (
    <section className={`${styles.panel} ${workspaceMode ? '' : styles.panelStandalone}`}>
      <div className={styles.header}>
        <div>
          <p className={styles.eyebrow}>{workspaceMode ? '导入验证工具' : '保存到 Memora'}</p>
          <h2 className={styles.title}>{workspaceMode ? '先验证保存与读取闭环' : '把当前内容保存为文档草稿'}</h2>
          <p className={styles.description}>
            {workspaceMode
              ? '先用单文档把 Markdown / HTML 的保存、回读和打开链路跑通，再扩展到批量同步或对话框插件。'
              : '这个页面可由浏览器书签脚本或外部工具打开。临时粘贴一把具备写入权限的访问密钥后，即可把当前内容保存到指定知识库。'}
          </p>
        </div>
        <div className={styles.headerPills}>
          <span className={styles.metaPill}>保存或更新</span>
          <span className={styles.metaPill}>来源标识</span>
          <span className={styles.metaPill}>Markdown / HTML</span>
        </div>
      </div>

      <div className={styles.workspace}>
        <div className={styles.editorCard}>
          <div className={styles.cardHeader}>
            <strong>保存参数</strong>
            <span>使用访问密钥</span>
          </div>

          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>临时访问密钥</span>
              <input
                type="password"
                autoComplete="off"
                value={form.apiKey}
                onChange={(event) => setForm((current) => ({ ...current, apiKey: event.target.value }))}
                placeholder="memora_sk_xxx"
              />
            </label>

            {hasManagedKnowledgeBases ? (
              <label className={styles.field}>
                <span>目标知识库</span>
                <select
                  value={form.knowledgeBaseId}
                  onChange={(event) => setForm((current) => ({ ...current, knowledgeBaseId: Number(event.target.value) }))}
                >
                  {knowledgeBases.map((item) => (
                    <option key={item.id} value={item.id}>{item.name}</option>
                  ))}
                </select>
              </label>
            ) : (
              <label className={styles.field}>
                <span>目标知识库编号</span>
                <input
                  type="number"
                  min="1"
                  value={form.knowledgeBaseId}
                  onChange={(event) => setForm((current) => ({ ...current, knowledgeBaseId: Number(event.target.value) }))}
                  placeholder="例如：1"
                />
              </label>
            )}
          </div>

          <div className={styles.formGrid}>
            {hasManagedKnowledgeBases ? (
              <label className={styles.field}>
                <span>父目录</span>
                <select
                  value={form.parentId}
                  onChange={(event) => setForm((current) => ({ ...current, parentId: Number(event.target.value) }))}
                >
                  {folderOptions.map((item) => (
                    <option key={item.id} value={item.id}>{item.label}</option>
                  ))}
                </select>
              </label>
            ) : (
              <label className={styles.field}>
                <span>父目录编号</span>
                <input
                  type="number"
                  min="0"
                  value={form.parentId}
                  onChange={(event) => setForm((current) => ({ ...current, parentId: Number(event.target.value) }))}
                  placeholder="根目录填 0"
                />
              </label>
            )}

            <label className={styles.field}>
              <span>格式</span>
              <select
                value={form.format}
                onChange={(event) => setForm((current) => ({ ...current, format: event.target.value }))}
              >
                <option value="MARKDOWN">MARKDOWN</option>
                <option value="HTML">HTML</option>
              </select>
            </label>
          </div>

          <label className={styles.field}>
            <span>标题</span>
            <input
              value={form.title}
              onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
              placeholder="例如：发布说明 / 网页剪藏"
            />
          </label>

          {form.sourceUrl ? (
            <label className={styles.field}>
              <span>来源 URL</span>
              <input value={form.sourceUrl} readOnly />
            </label>
          ) : null}

          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>来源标识</span>
              <input
                value={form.sourceExternalId}
                onChange={(event) => setForm((current) => ({ ...current, sourceExternalId: event.target.value }))}
                placeholder="建议长期稳定，不要每次随机生成"
              />
            </label>
            <label className={styles.field}>
              <span>来源版本</span>
              <input
                value={form.sourceRevision}
                onChange={(event) => setForm((current) => ({ ...current, sourceRevision: event.target.value }))}
                placeholder="建议使用时间戳或上游版本号"
              />
            </label>
          </div>

          <div className={styles.inlineActions}>
            <button type="button" className={styles.secondaryButton} onClick={handleRegenerateIdentity}>
              重新生成来源标识
            </button>
            <span className={styles.inlineHint}>
              {folderLoading ? '正在加载父目录选项...' : '来源标识负责定位文档，来源版本负责避免覆盖更新。'}
            </span>
          </div>

          <label className={styles.field}>
            <span>正文</span>
            <textarea
              className={styles.contentInput}
              value={form.content}
              onChange={(event) => setForm((current) => ({ ...current, content: event.target.value }))}
              spellCheck={form.format !== 'HTML'}
              placeholder={form.format === 'HTML' ? '<article><h1>标题</h1><p>正文</p></article>' : '# 标题\n\n这里写正文'}
            />
          </label>

          {(errorMessage || feedback) ? (
            <div className={`${styles.message} ${errorMessage ? styles.messageError : styles.messageSuccess}`}>
              {errorMessage || feedback}
            </div>
          ) : null}

          <div className={styles.actionRow}>
            <div className={styles.consumeControl}>
              <select value={consumeView} onChange={(event) => setConsumeView(event.target.value)}>
                {CONSUME_VIEWS.map((item) => (
                  <option key={item.value} value={item.value}>{item.label}</option>
                ))}
              </select>
              <button
                type="button"
                className={styles.secondaryButton}
                disabled={reading || !form.apiKey.trim() || !payload.sourceExternalId}
                onClick={() => void loadBySource(consumeView)}
              >
                {reading ? '读取中...' : '按来源读取'}
              </button>
            </div>

            <button
              type="button"
              className={styles.primaryButton}
              disabled={!canSubmit || submitting}
              onClick={() => void handleUpsert()}
            >
              {submitting ? '保存中...' : '保存或更新'}
            </button>
          </div>

          <div className={styles.subtleText}>
            访问密钥只保存在当前页面内存中。这个入口只负责生成或更新草稿，正式公开仍通过文档发布和公开站点完成。
          </div>
        </div>

        <div className={styles.previewColumn}>
          <section className={styles.previewCard}>
            <div className={styles.cardHeader}>
              <strong>本地预览</strong>
              <span>{renderState.plainText.length} 字符</span>
            </div>
            {renderState.hasRenderedContent ? (
              <DocumentRenderedContent
                className={styles.previewContent}
                html={renderState.html}
                variant="compact"
              />
            ) : (
              <div className={styles.emptyState}>当前正文为空，执行保存后会生成空白文档。</div>
            )}
          </section>

          {(upsertResult || consumeResult) ? (
            <section className={styles.resultCard}>
              <div className={styles.cardHeader}>
                <strong>保存与读取结果</strong>
                <span>{selectedKnowledgeBase?.name || consumeResult?.knowledgeBaseName || `知识库 #${payload.knowledgeBaseId}`}</span>
              </div>

              {upsertResult ? (
                <div className={styles.resultBlock}>
                  <div className={styles.resultTopline}>
                    <strong>{UPSERT_STATUS_LABELS[upsertResult.status] || upsertResult.status || '已处理'}</strong>
                    {upsertResult.document?.id ? <span className={styles.statusChip}>文档 #{upsertResult.document.id}</span> : null}
                  </div>
                  <div className={styles.resultMeta}>
                    <span>{upsertResult.message || '请求已完成'}</span>
                    {upsertResult.sourceRevision ? <span>来源版本 {upsertResult.sourceRevision}</span> : null}
                  </div>
                </div>
              ) : null}

              {consumeResult ? (
                <>
                  <div className={styles.resultBlock}>
                    <div className={styles.resultTopline}>
                      <strong>{consumeResult.title || '已读取文档'}</strong>
                      <span className={styles.statusChip}>{consumeResult.representation || 'metadata'}</span>
                    </div>
                    <div className={styles.resultMeta}>
                      <span>文档 #{consumeResult.documentId}</span>
                      <span>{consumeResult.format}</span>
                      <span>v{consumeResult.versionNo}</span>
                      {consumeResult.updatedAt ? <span>更新于 {dayjs(consumeResult.updatedAt).format('MM-DD HH:mm')}</span> : null}
                    </div>
                    {consumeResult.summary ? <p className={styles.resultSummary}>{consumeResult.summary}</p> : null}
                  </div>

                  <div className={styles.linkActions}>
                    {readerUrl ? (
                      <button type="button" className={styles.secondaryButton} onClick={() => openExternalLink(readerUrl)}>
                        打开阅读页
                      </button>
                    ) : null}
                    {publicUrl ? (
                      <button type="button" className={styles.secondaryButton} onClick={() => openExternalLink(publicUrl)}>
                        打开公开文档
                      </button>
                    ) : null}
                    {!publicUrl && siteUrl ? (
                      <button type="button" className={styles.secondaryButton} onClick={() => openExternalLink(siteUrl)}>
                        打开公开站点
                      </button>
                    ) : null}
                  </div>

                  {consumeResult.payload ? (
                    consumeResult.representation === 'rendered' ? (
                      <DocumentRenderedContent
                        className={styles.remotePreview}
                        html={consumeResult.payload}
                        variant="compact"
                      />
                    ) : (
                      <pre className={styles.codeBlock}>{consumeResult.payload}</pre>
                    )
                  ) : (
                    <div className={styles.subtleText}>
                      当前读取视图主要返回元数据和打开链接，没有额外正文载荷。
                    </div>
                  )}
                </>
              ) : null}
            </section>
          ) : null}
        </div>
      </div>

      {workspaceMode ? (
        <div className={styles.snippetGrid}>
          {snippets.map((item) => (
            <section key={item.id} className={styles.snippetCard}>
              <div className={styles.cardHeader}>
                <div>
                  <strong>{item.title}</strong>
                  <p className={styles.cardDescription}>{item.description}</p>
                </div>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => void copySnippet(item.title, item.snippet)}
                >
                  复制
                </button>
              </div>
              <pre className={styles.codeBlock}>{item.snippet}</pre>
            </section>
          ))}
        </div>
      ) : null}
    </section>
  )
}

export default OpenApiWorkbench

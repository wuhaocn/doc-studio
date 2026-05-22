const DEFAULT_WEB_ORIGIN = 'http://localhost:8800'
const DEFAULT_API_BASE_URL = 'http://localhost:8080'

const normalizeBaseUrl = (value, fallback) => {
  const normalized = `${value || fallback || ''}`.trim()
  return normalized.replace(/\/$/, '')
}

const normalizeInteger = (value, fallback = 0) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback
}

const padNumber = (value) => `${value}`.padStart(2, '0')

const escapeHtml = (value) =>
  `${value || ''}`
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')

export const buildDefaultSourceRevision = (date = new Date()) => date.toISOString()

export const buildDefaultSourceExternalId = (knowledgeBaseId, sourceUrl = '', date = new Date()) => {
  const normalizedSourceUrl = `${sourceUrl || ''}`.trim()
  if (normalizedSourceUrl) {
    return `capture:${normalizedSourceUrl}`.slice(0, 220)
  }

  const kbId = normalizeInteger(knowledgeBaseId, 0) || 'x'
  const stamp = [
    date.getFullYear(),
    padNumber(date.getMonth() + 1),
    padNumber(date.getDate()),
    padNumber(date.getHours()),
    padNumber(date.getMinutes()),
    padNumber(date.getSeconds()),
  ].join('')
  return `manual-kb-${kbId}-${stamp}`
}

export const buildCaptureMarkdownContent = ({ title = '', sourceUrl = '', selection = '' }) => {
  const normalizedTitle = `${title || ''}`.trim() || '网页剪藏'
  const normalizedSourceUrl = `${sourceUrl || ''}`.trim()
  const normalizedSelection = `${selection || ''}`.trim()
  const lines = [`# ${normalizedTitle}`]

  if (normalizedSourceUrl) {
    lines.push('', `来源：${normalizedSourceUrl}`)
  }

  lines.push('', normalizedSelection || '（未捕获选中文本，可在保存前继续补充正文。）')
  return lines.join('\n')
}

export const normalizeCaptureDraft = (draft = {}) => {
  const sourceUrl = `${draft.sourceUrl || draft.url || ''}`.trim()
  const format = `${draft.format || 'MARKDOWN'}`.trim().toUpperCase()
  return {
    knowledgeBaseId: normalizeInteger(draft.knowledgeBaseId, 0),
    parentId: normalizeInteger(draft.parentId, 0),
    title: `${draft.title || ''}`.trim() || '网页剪藏',
    format: ['HTML', 'MARKDOWN'].includes(format) ? format : 'MARKDOWN',
    content: `${draft.content || ''}`,
    sourceUrl,
    sourceExternalId: `${draft.sourceExternalId || ''}`.trim(),
    sourceRevision: `${draft.sourceRevision || ''}`.trim(),
    selection: `${draft.selection || ''}`.trim(),
  }
}

export const buildCaptureInitialValues = (draft = {}) => {
  const normalized = normalizeCaptureDraft(draft)
  return {
    knowledgeBaseId: normalized.knowledgeBaseId,
    parentId: normalized.parentId,
    title: normalized.title,
    format: normalized.format,
    sourceUrl: normalized.sourceUrl,
    sourceExternalId: normalized.sourceExternalId
      || buildDefaultSourceExternalId(normalized.knowledgeBaseId, normalized.sourceUrl),
    sourceRevision: normalized.sourceRevision || buildDefaultSourceRevision(),
    content: normalized.content || buildCaptureMarkdownContent({
      title: normalized.title,
      sourceUrl: normalized.sourceUrl,
      selection: normalized.selection,
    }),
  }
}

export const buildOpenApiUpsertPayload = (form = {}) => {
  return {
    knowledgeBaseId: normalizeInteger(form.knowledgeBaseId, 0),
    parentId: normalizeInteger(form.parentId, 0),
    title: `${form.title || ''}`.trim(),
    format: `${form.format || 'MARKDOWN'}`.trim().toUpperCase(),
    content: `${form.content || ''}`,
    sourceExternalId: `${form.sourceExternalId || ''}`.trim(),
    sourceRevision: `${form.sourceRevision || ''}`.trim(),
  }
}

const buildPrettyJson = (value) => JSON.stringify(value, null, 2)

export const buildOpenApiCurlSnippet = ({
  apiBaseUrl = DEFAULT_API_BASE_URL,
  apiKeyLabel = 'memora_sk_xxx',
  payload,
}) => {
  const normalizedBaseUrl = normalizeBaseUrl(apiBaseUrl, DEFAULT_API_BASE_URL)
  return `curl -X POST \\
  -H "Authorization: ApiKey ${apiKeyLabel}" \\
  -H "Content-Type: application/json" \\
  ${normalizedBaseUrl}/api/v1/open/documents/upsert \\
  -d @- <<'JSON'
${buildPrettyJson(payload)}
JSON`
}

export const buildOpenApiFetchSnippet = ({
  apiBaseUrl = DEFAULT_API_BASE_URL,
  payload,
}) => {
  const normalizedBaseUrl = normalizeBaseUrl(apiBaseUrl, DEFAULT_API_BASE_URL)
  return `const payload = ${buildPrettyJson(payload)}

const response = await fetch('${normalizedBaseUrl}/api/v1/open/documents/upsert', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    Authorization: 'ApiKey ' + process.env.MEMORA_API_KEY,
  },
  body: JSON.stringify(payload),
})

const result = await response.json()
if (!response.ok || result.code !== 200) {
  throw new Error(result.message || 'Memora upsert failed')
}

console.log(result.data)`
}

export const buildOpenApiConsumeCurlSnippet = ({
  apiBaseUrl = DEFAULT_API_BASE_URL,
  apiKeyLabel = 'memora_sk_xxx',
  knowledgeBaseId,
  sourceExternalId,
  view = 'metadata',
}) => {
  const normalizedBaseUrl = normalizeBaseUrl(apiBaseUrl, DEFAULT_API_BASE_URL)
  const params = new URLSearchParams({
    sourceExternalId: `${sourceExternalId || ''}`.trim(),
    view: `${view || 'metadata'}`.trim(),
  })
  return `curl -H "Authorization: ApiKey ${apiKeyLabel}" \\
  "${normalizedBaseUrl}/api/v1/open/knowledge-bases/${normalizeInteger(knowledgeBaseId, 0)}/documents/by-source?${params.toString()}"`
}

export const buildOpenReaderFetchSnippet = ({
  apiBaseUrl = DEFAULT_API_BASE_URL,
  knowledgeBaseId,
  sourceExternalId,
  webOrigin = DEFAULT_WEB_ORIGIN,
}) => {
  const normalizedBaseUrl = normalizeBaseUrl(apiBaseUrl, DEFAULT_API_BASE_URL)
  const normalizedWebOrigin = normalizeBaseUrl(webOrigin, DEFAULT_WEB_ORIGIN)
  return `const params = new URLSearchParams({
  sourceExternalId: ${JSON.stringify(`${sourceExternalId || ''}`.trim())},
  view: 'metadata',
})

const response = await fetch(
  '${normalizedBaseUrl}/api/v1/open/knowledge-bases/${normalizeInteger(knowledgeBaseId, 0)}/documents/by-source?' + params.toString(),
  {
    headers: {
      Authorization: 'ApiKey ' + process.env.MEMORA_API_KEY,
    },
  },
)

const result = await response.json()
if (!response.ok || result.code !== 200 || !result.data?.readerUrl) {
  throw new Error(result.message || 'Memora reader open failed')
}

window.open(new URL(result.data.readerUrl, '${normalizedWebOrigin}').toString(), '_blank', 'noopener,noreferrer')`
}

export const buildCaptureBookmarkletSnippet = ({
  webOrigin = DEFAULT_WEB_ORIGIN,
  knowledgeBaseId,
  parentId,
}) => {
  const normalizedWebOrigin = normalizeBaseUrl(webOrigin, DEFAULT_WEB_ORIGIN)
  const normalizedKnowledgeBaseId = normalizeInteger(knowledgeBaseId, 0)
  const normalizedParentId = normalizeInteger(parentId, 0)
  const script = `(function(){const base=${JSON.stringify(normalizedWebOrigin)};const params=new URLSearchParams();params.set('knowledgeBaseId',${JSON.stringify(String(normalizedKnowledgeBaseId))});params.set('parentId',${JSON.stringify(String(normalizedParentId))});params.set('title',(document.title||'网页剪藏').trim().slice(0,120));params.set('url',window.location.href);const selection=(window.getSelection&&window.getSelection().toString()||'').trim().slice(0,6000);if(selection){params.set('selection',selection)}window.open(base+'/capture/save?'+params.toString(),'_blank','noopener,noreferrer')})();`
  return `javascript:${script}`
}

export const buildCaptureDraftHash = (draft = {}) => {
  return `draft=${encodeURIComponent(JSON.stringify(normalizeCaptureDraft(draft)))}`
}

export const parseCaptureDraftHash = (hash = '') => {
  const normalizedHash = `${hash || ''}`.trim().replace(/^#/, '')
  if (!normalizedHash) {
    return null
  }

  const params = new URLSearchParams(normalizedHash)
  const encodedDraft = params.get('draft')
  if (!encodedDraft) {
    return null
  }

  try {
    return normalizeCaptureDraft(JSON.parse(encodedDraft))
  } catch (error) {
    console.error('解析 capture draft 失败', error)
    return null
  }
}

export const buildDialogCaptureSnippet = ({
  webOrigin = DEFAULT_WEB_ORIGIN,
  draft = {},
}) => {
  const normalizedWebOrigin = normalizeBaseUrl(webOrigin, DEFAULT_WEB_ORIGIN)
  const normalizedDraft = normalizeCaptureDraft(draft)
  return `const draft = ${buildPrettyJson(normalizedDraft)}

const target = new URL('${normalizedWebOrigin}/capture/save')
target.hash = 'draft=' + encodeURIComponent(JSON.stringify(draft))
window.open(target.toString(), '_blank', 'noopener,noreferrer')`
}

export const resolveAbsoluteWorkbenchUrl = (path, webOrigin = DEFAULT_WEB_ORIGIN) => {
  const normalizedPath = `${path || ''}`.trim()
  if (!normalizedPath) {
    return ''
  }
  if (/^https?:\/\//i.test(normalizedPath)) {
    return normalizedPath
  }
  const normalizedWebOrigin = normalizeBaseUrl(webOrigin, DEFAULT_WEB_ORIGIN)
  if (normalizedPath.startsWith('/')) {
    return `${normalizedWebOrigin}${normalizedPath}`
  }
  return `${normalizedWebOrigin}/${normalizedPath}`
}

export const buildCapturePreviewHtml = ({ title = '', sourceUrl = '', selection = '' }) => {
  const normalizedTitle = escapeHtml(title || '网页剪藏')
  const normalizedSourceUrl = `${sourceUrl || ''}`.trim()
  const normalizedSelection = escapeHtml(selection || '（未捕获选中文本，可在保存前继续补充正文。）')
  const sourceLine = normalizedSourceUrl
    ? `<p><strong>来源：</strong><a href="${escapeHtml(normalizedSourceUrl)}" target="_blank" rel="noreferrer">${escapeHtml(normalizedSourceUrl)}</a></p>`
    : ''

  return `<article><h1>${normalizedTitle}</h1>${sourceLine}<p>${normalizedSelection.replaceAll('\n', '<br>')}</p></article>`
}

import { marked } from 'marked'

export const DOCUMENT_FORMATS = {
  RICH_TEXT: 'RICH_TEXT',
  MARKDOWN: 'MARKDOWN',
  HTML: 'HTML',
}

export const DEFAULT_DOCUMENT_FORMAT = DOCUMENT_FORMATS.RICH_TEXT

const TEXT_NODE = 3
const ELEMENT_NODE = 1
const BLOCKED_TAGS = new Set(['script', 'style', 'iframe', 'object', 'embed', 'form', 'input', 'button', 'link', 'meta'])
const ALLOWED_TAGS = new Set([
  'a',
  'article',
  'blockquote',
  'br',
  'code',
  'del',
  'div',
  'em',
  'figure',
  'figcaption',
  'h1',
  'h2',
  'h3',
  'h4',
  'h5',
  'h6',
  'hr',
  'img',
  'li',
  'ol',
  'p',
  'pre',
  'section',
  'span',
  'strong',
  'sub',
  'sup',
  'table',
  'tbody',
  'td',
  'th',
  'thead',
  'tr',
  'ul',
])
const GLOBAL_ALLOWED_ATTRS = new Set(['class'])
const TAG_ALLOWED_ATTRS = {
  a: new Set(['href', 'title', 'target', 'rel']),
  div: new Set(['data-type', 'data-diagram-type', 'data-content', 'data-width', 'data-height']),
  img: new Set(['src', 'alt', 'title']),
}
const FORMAT_SET = new Set(Object.values(DOCUMENT_FORMATS))

marked.setOptions({
  gfm: true,
  breaks: true,
})

const stripUnsafeMarkupFallback = (value) =>
  value
    .replace(/<\s*(script|style|iframe|object|embed|form|input|button|link|meta)\b[^>]*>[\s\S]*?<\s*\/\s*\1\s*>/gi, '')
    .replace(/\son[a-z]+\s*=\s*(".*?"|'.*?'|[^\s>]+)/gi, '')
    .replace(/\s(href|src)\s*=\s*("javascript:[^"]*"|'javascript:[^']*'|javascript:[^\s>]+)/gi, '')
    .replace(/\s(href|src)\s*=\s*("vbscript:[^"]*"|'vbscript:[^']*'|vbscript:[^\s>]+)/gi, '')

const isSafeUrl = (value, { allowDataImage = false } = {}) => {
  const normalized = value.trim()
  if (!normalized) {
    return false
  }

  const lowerValue = normalized.toLowerCase()
  if (lowerValue.startsWith('javascript:') || lowerValue.startsWith('vbscript:')) {
    return false
  }
  if (lowerValue.startsWith('data:')) {
    return allowDataImage && /^data:image\//i.test(normalized)
  }

  return true
}

const sanitizeNode = (node, ownerDocument) => {
  if (node.nodeType === TEXT_NODE) {
    return ownerDocument.createTextNode(node.textContent || '')
  }

  if (node.nodeType !== ELEMENT_NODE) {
    return ownerDocument.createDocumentFragment()
  }

  const tagName = node.tagName.toLowerCase()
  if (BLOCKED_TAGS.has(tagName)) {
    return ownerDocument.createDocumentFragment()
  }

  if (!ALLOWED_TAGS.has(tagName)) {
    const fragment = ownerDocument.createDocumentFragment()
    Array.from(node.childNodes).forEach((childNode) => {
      fragment.appendChild(sanitizeNode(childNode, ownerDocument))
    })
    return fragment
  }

  const sanitizedElement = ownerDocument.createElement(tagName)
  const allowedAttrs = TAG_ALLOWED_ATTRS[tagName] || new Set()

  Array.from(node.attributes).forEach((attribute) => {
    const attrName = attribute.name.toLowerCase()
    if (attrName.startsWith('on')) {
      return
    }
    if (!GLOBAL_ALLOWED_ATTRS.has(attrName) && !allowedAttrs.has(attrName)) {
      return
    }

    const attrValue = attribute.value || ''
    if ((attrName === 'src' || attrName === 'href') && !isSafeUrl(attrValue, { allowDataImage: attrName === 'src' })) {
      return
    }

    sanitizedElement.setAttribute(attribute.name, attrValue)
  })

  Array.from(node.childNodes).forEach((childNode) => {
    sanitizedElement.appendChild(sanitizeNode(childNode, ownerDocument))
  })
  return sanitizedElement
}

export const normalizeDocumentFormat = (value) => {
  const normalized = (value || '').trim().toUpperCase()
  if (!normalized) {
    return DEFAULT_DOCUMENT_FORMAT
  }
  return FORMAT_SET.has(normalized) ? normalized : DEFAULT_DOCUMENT_FORMAT
}

export const sanitizeDocumentHtml = (value) => {
  const normalized = (value || '').trim()
  if (!normalized) {
    return ''
  }

  if (typeof DOMParser === 'undefined' || typeof document === 'undefined') {
    return stripUnsafeMarkupFallback(normalized)
  }

  const parser = new DOMParser()
  const parsedDocument = parser.parseFromString(`<body>${normalized}</body>`, 'text/html')
  const safeDocument = document.implementation.createHTMLDocument('')
  const container = safeDocument.createElement('div')

  Array.from(parsedDocument.body.childNodes).forEach((node) => {
    container.appendChild(sanitizeNode(node, safeDocument))
  })

  return container.innerHTML
}

export const normalizeRichTextEditorContent = (value) => sanitizeDocumentHtml(value) || '<p></p>'

export const renderMarkdownToHtml = (value) => {
  if (!value || !value.trim()) {
    return ''
  }
  return sanitizeDocumentHtml(marked.parse(value))
}

export const renderDocumentHtml = (format, content) => {
  const normalizedFormat = normalizeDocumentFormat(format)
  if (!content || !content.trim()) {
    return ''
  }

  if (normalizedFormat === DOCUMENT_FORMATS.MARKDOWN) {
    return renderMarkdownToHtml(content)
  }
  return sanitizeDocumentHtml(content)
}

export const extractPlainTextFromHtml = (value) => {
  const normalized = (value || '').trim()
  if (!normalized) {
    return ''
  }

  if (typeof DOMParser === 'undefined') {
    return normalized.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  }

  const parser = new DOMParser()
  const doc = parser.parseFromString(`<body>${normalized}</body>`, 'text/html')
  return doc.body.textContent?.replace(/\s+/g, ' ').trim() || ''
}

export const getDocumentRenderState = ({ format, content, contentText, renderedHtml }) => {
  const normalizedFormat = normalizeDocumentFormat(format)
  const html = renderedHtml
    ? sanitizeDocumentHtml(renderedHtml)
    : renderDocumentHtml(normalizedFormat, content || '')
  const plainText = summarizePlainText(contentText || extractPlainTextFromHtml(html) || '')

  return {
    format: normalizedFormat,
    html,
    plainText,
    hasRenderedContent: Boolean(html),
  }
}

export const summarizePlainText = (value, maxLength = 180) => {
  const normalized = (value || '').replace(/\s+/g, ' ').trim()
  if (!normalized) {
    return ''
  }
  return normalized.length > maxLength ? normalized.slice(0, maxLength) : normalized
}

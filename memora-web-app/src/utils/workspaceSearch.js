const normalizeKeyword = (keyword = '') => keyword.trim().toLowerCase()
const normalizeText = (value = '') => value.replace(/\s+/g, ' ').trim()
const escapeRegExp = (value = '') => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

const tokenizeKeyword = (keyword = '') => {
  const normalizedKeyword = normalizeKeyword(keyword)
  if (!normalizedKeyword) {
    return []
  }

  const tokens = new Set([normalizedKeyword])
  normalizedKeyword.split(/\s+/).forEach((part) => {
    if (part) {
      tokens.add(part)
    }
  })
  return Array.from(tokens)
}

const toTimestamp = (value) => {
  if (!value) {
    return 0
  }
  const timestamp = new Date(value).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

const findKeywordMatchIndex = (sourceText = '', keyword = '') => {
  const normalizedSource = sourceText.toLowerCase()
  const tokens = tokenizeKeyword(keyword)
  let matchedIndex = -1
  let matchedTokenLength = 0

  tokens.forEach((token) => {
    const tokenIndex = normalizedSource.indexOf(token)
    if (tokenIndex >= 0 && (matchedIndex < 0 || tokenIndex < matchedIndex)) {
      matchedIndex = tokenIndex
      matchedTokenLength = token.length
    }
  })

  return { matchedIndex, matchedTokenLength }
}

export const buildSearchExcerpt = (document, keyword) => {
  const sourceText = normalizeText(document?.contentText || document?.summary || document?.title || '')
  if (!sourceText) {
    return ''
  }

  const { matchedIndex, matchedTokenLength } = findKeywordMatchIndex(sourceText, keyword)
  if (matchedIndex < 0) {
    return sourceText.length > 160 ? `${sourceText.slice(0, 160)}...` : sourceText
  }

  const start = Math.max(matchedIndex - 44, 0)
  const end = Math.min(matchedIndex + matchedTokenLength + 72, sourceText.length)
  const prefix = start > 0 ? '...' : ''
  const suffix = end < sourceText.length ? '...' : ''
  return `${prefix}${sourceText.slice(start, end)}${suffix}`
}

export const buildKeywordHighlightParts = (text = '', keyword = '') => {
  const normalizedText = `${text || ''}`
  const tokens = tokenizeKeyword(keyword).sort((left, right) => right.length - left.length)
  if (!normalizedText) {
    return []
  }
  if (tokens.length === 0) {
    return [{ text: normalizedText, matched: false }]
  }

  const regex = new RegExp(`(${tokens.map(escapeRegExp).join('|')})`, 'ig')
  const parts = []
  let cursor = 0
  let match = regex.exec(normalizedText)

  while (match) {
    const matchIndex = match.index
    if (matchIndex > cursor) {
      parts.push({ text: normalizedText.slice(cursor, matchIndex), matched: false })
    }
    parts.push({ text: normalizedText.slice(matchIndex, matchIndex + match[0].length), matched: true })
    cursor = matchIndex + match[0].length
    match = regex.exec(normalizedText)
  }

  if (cursor < normalizedText.length) {
    parts.push({ text: normalizedText.slice(cursor), matched: false })
  }

  return parts
}

const calculateSearchScore = (document, keyword) => {
  const tokens = tokenizeKeyword(keyword)
  const normalizedKeyword = tokens[0] || ''
  const normalizedTitle = normalizeKeyword(document?.title || '')
  const normalizedSummary = normalizeKeyword(document?.summary || '')
  const normalizedContent = normalizeKeyword(document?.contentText || document?.summary || '')
  const normalizedPath = normalizeKeyword(document?.path || '')

  if (!normalizedKeyword) {
    return toTimestamp(document?.updatedAt)
  }

  let score = 0
  if (normalizedTitle === normalizedKeyword) {
    score += 240
  } else if (normalizedTitle.startsWith(normalizedKeyword)) {
    score += 180
  } else if (normalizedTitle.includes(normalizedKeyword)) {
    score += 120
  }

  if (normalizedContent.includes(normalizedKeyword)) {
    score += 44
  }
  if (normalizedSummary.includes(normalizedKeyword)) {
    score += 24
  }
  if (normalizedPath.includes(normalizedKeyword)) {
    score += 18
  }

  let tokenHits = 0
  tokens.forEach((token) => {
    let tokenMatched = false
    if (normalizedTitle === token) {
      score += 90
      tokenMatched = true
    } else if (normalizedTitle.startsWith(token)) {
      score += 56
      tokenMatched = true
    } else if (normalizedTitle.includes(token)) {
      score += 28
      tokenMatched = true
    }

    if (normalizedSummary.includes(token)) {
      score += 12
      tokenMatched = true
    }
    if (normalizedContent.includes(token)) {
      score += 10
      tokenMatched = true
    }
    if (normalizedPath.includes(token)) {
      score += 6
      tokenMatched = true
    }
    if (tokenMatched) {
      tokenHits += 1
    }
  })

  if (tokenHits === tokens.length) {
    score += 20
  }

  return score
}

const resolveMatchLabel = (document, keyword) => {
  const tokens = tokenizeKeyword(keyword)
  if (tokens.length === 0) {
    return ''
  }

  const normalizedTitle = normalizeKeyword(document?.title || '')
  if (tokens.some((token) => normalizedTitle.includes(token))) {
    return '标题命中'
  }

  const normalizedSummary = normalizeKeyword(document?.summary || '')
  if (tokens.some((token) => normalizedSummary.includes(token))) {
    return '摘要命中'
  }

  return '正文命中'
}

export const groupSearchResultsByKnowledgeBase = (documents = [], knowledgeBases = [], keyword = '') => {
  const knowledgeBaseMap = new Map(knowledgeBases.map((item) => [item.id, item]))
  const groupMap = new Map()

  documents.forEach((document) => {
    if (document?.docType && document.docType !== 'DOC') {
      return
    }

    const knowledgeBase = knowledgeBaseMap.get(document.knowledgeBaseId)
    if (!knowledgeBase) {
      return
    }

    if (!groupMap.has(knowledgeBase.id)) {
      groupMap.set(knowledgeBase.id, {
        knowledgeBaseId: knowledgeBase.id,
        knowledgeBaseName: knowledgeBase.name,
        currentRole: knowledgeBase.currentRole,
        canWrite: !!knowledgeBase.canWrite,
        permissionRestricted: !!knowledgeBase.permissionRestricted,
        documents: [],
      })
    }

    groupMap.get(knowledgeBase.id).documents.push({
      ...document,
      excerpt: buildSearchExcerpt(document, keyword),
      score: calculateSearchScore(document, keyword),
      matchLabel: resolveMatchLabel(document, keyword),
    })
  })

  return Array.from(groupMap.values())
    .map((group) => ({
      ...group,
      documents: group.documents
        .slice()
        .sort((left, right) => {
          if (right.score !== left.score) {
            return right.score - left.score
          }
          return toTimestamp(right.updatedAt) - toTimestamp(left.updatedAt)
        }),
      hitCount: group.documents.length,
      topScore: group.documents.reduce((max, item) => Math.max(max, item.score || 0), 0),
    }))
    .sort((left, right) => {
      if (right.topScore !== left.topScore) {
        return right.topScore - left.topScore
      }
      if (right.hitCount !== left.hitCount) {
        return right.hitCount - left.hitCount
      }
      return left.knowledgeBaseName.localeCompare(right.knowledgeBaseName, 'zh-Hans-CN')
    })
}

const normalizeKeyword = (keyword = '') => keyword.trim().toLowerCase()

export const buildSearchExcerpt = (document, keyword) => {
  const sourceText = (document?.contentText || document?.summary || document?.title || '').replace(/\s+/g, ' ').trim()
  if (!sourceText) {
    return ''
  }

  const normalizedKeyword = normalizeKeyword(keyword)
  if (!normalizedKeyword) {
    return sourceText.length > 160 ? `${sourceText.slice(0, 160)}...` : sourceText
  }

  const matchIndex = sourceText.toLowerCase().indexOf(normalizedKeyword)
  if (matchIndex < 0) {
    return sourceText.length > 160 ? `${sourceText.slice(0, 160)}...` : sourceText
  }

  const start = Math.max(matchIndex - 44, 0)
  const end = Math.min(matchIndex + normalizedKeyword.length + 72, sourceText.length)
  const prefix = start > 0 ? '...' : ''
  const suffix = end < sourceText.length ? '...' : ''
  return `${prefix}${sourceText.slice(start, end)}${suffix}`
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
    })
  })

  return Array.from(groupMap.values())
    .map((group) => ({
      ...group,
      hitCount: group.documents.length,
    }))
    .sort((left, right) => right.hitCount - left.hitCount)
}

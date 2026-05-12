const DRAFT_PREFIX = 'memora_draft_'

export const saveDraft = (documentId, content) => {
  if (!documentId || !content) return
  try {
    localStorage.setItem(`${DRAFT_PREFIX}${documentId}`, JSON.stringify({
      content,
      savedAt: Date.now(),
    }))
  } catch { /* quota exceeded, ignore */ }
}

export const getDraft = (documentId) => {
  if (!documentId) return null
  try {
    const raw = localStorage.getItem(`${DRAFT_PREFIX}${documentId}`)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    // Discard drafts older than 24 hours
    if (Date.now() - parsed.savedAt > 24 * 60 * 60 * 1000) {
      removeDraft(documentId)
      return null
    }
    return parsed
  } catch {
    return null
  }
}

export const removeDraft = (documentId) => {
  if (!documentId) return
  localStorage.removeItem(`${DRAFT_PREFIX}${documentId}`)
}

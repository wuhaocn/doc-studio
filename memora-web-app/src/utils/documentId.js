export const normalizeDocumentId = (id) => {
  const value = typeof id === 'string' ? id.trim() : id
  const normalizedId = Number(value)

  if (!Number.isInteger(normalizedId) || normalizedId <= 0) {
    const error = new Error('文档ID不合法')
    error.code = 400
    throw error
  }

  return normalizedId
}

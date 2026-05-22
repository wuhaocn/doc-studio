import httpClient, { API_BASE_URL } from '../http/axios'
import { getRuntimeWebClientHeaders } from '../runtime/runtimeConfig'

export const auditApi = {
  listAuditLogs: async (params = {}) => {
    const {
      page = 1,
      size = 20,
      knowledgeBaseId,
      objectType,
      objectId,
      resultType,
    } = params

    return httpClient.get('/api/v1/audit-logs', {
      params: { page, size, knowledgeBaseId, objectType, objectId, resultType },
    })
  },

  getAuditSummary: async (params = {}) => {
    const { knowledgeBaseId, objectType, objectId } = params
    return httpClient.get('/api/v1/audit-logs/summary', {
      params: { knowledgeBaseId, objectType, objectId },
    })
  },

  runAuditRetention: async (params = {}) => {
    const { knowledgeBaseId, objectType, objectId } = params
    return httpClient.post('/api/v1/audit-logs/retention/run', null, {
      params: { knowledgeBaseId, objectType, objectId },
    })
  },

  exportAuditLogs: async (params = {}) => {
    const searchParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        searchParams.set(key, value)
      }
    })

    const response = await fetch(`${API_BASE_URL}/api/v1/audit-logs/export?${searchParams.toString()}`, {
      method: 'GET',
      credentials: 'include',
      headers: {
        Accept: 'text/csv,application/json',
        ...getRuntimeWebClientHeaders(),
      },
    })

    const contentType = response.headers.get('content-type') || ''
    if (contentType.includes('application/json')) {
      const payload = await response.json()
      throw payload
    }

    const blob = await response.blob()
    const downloadUrl = window.URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    const contentDisposition = response.headers.get('content-disposition') || ''
    const filenameMatch = contentDisposition.match(/filename="?([^"]+)"?/i)
    anchor.href = downloadUrl
    anchor.download = filenameMatch?.[1] || 'memora-audit-log.csv'
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    window.URL.revokeObjectURL(downloadUrl)
    return true
  },
}

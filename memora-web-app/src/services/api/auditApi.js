import { getCurrentUser } from '../../utils/user'
import httpClient, { API_BASE_URL } from '../http/axios'

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

  exportAuditLogs: async (params = {}) => {
    const currentUser = getCurrentUser()
    const searchParams = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        searchParams.set(key, value)
      }
    })

    const response = await fetch(`${API_BASE_URL}/api/v1/audit-logs/export?${searchParams.toString()}`, {
      method: 'GET',
      headers: {
        Accept: 'text/csv,application/json',
        Authorization: currentUser?.accessToken ? `Bearer ${currentUser.accessToken}` : '',
        'X-Memora-Client': 'memora-web-app',
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
    anchor.href = downloadUrl
    anchor.download = 'memora-audit-log.csv'
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    window.URL.revokeObjectURL(downloadUrl)
    return true
  },
}

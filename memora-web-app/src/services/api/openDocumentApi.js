import axios from 'axios'
import { API_BASE_URL } from '../http/axios'
import { getRuntimeWebClientHeaders } from '../runtime/runtimeConfig'

const openApiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
    ...getRuntimeWebClientHeaders(),
  },
})

const buildAuthHeaders = (apiKey) => ({
  Authorization: `ApiKey ${`${apiKey || ''}`.trim()}`,
})

const normalizeError = (error) => {
  if (error?.response?.data) {
    return error.response.data
  }
  if (error?.request) {
    return { code: 500, message: '网络错误，请检查开放接口地址和网络连通性' }
  }
  return { code: 500, message: error?.message || '开放接口请求失败' }
}

const request = async (config) => {
  try {
    const response = await openApiClient(config)
    const payload = response?.data
    if (payload?.code === 200) {
      return payload
    }
    throw payload || { code: response?.status || 500, message: '开放接口请求失败' }
  } catch (error) {
    throw normalizeError(error)
  }
}

export const openDocumentApi = {
  upsertDocument: async (apiKey, payload) => {
    return request({
      method: 'POST',
      url: '/api/v1/open/documents/upsert',
      data: payload,
      headers: buildAuthHeaders(apiKey),
    })
  },

  getDocumentBySource: async (apiKey, knowledgeBaseId, sourceExternalId, view = 'metadata') => {
    return request({
      method: 'GET',
      url: `/api/v1/open/knowledge-bases/${knowledgeBaseId}/documents/by-source`,
      params: {
        sourceExternalId,
        view,
      },
      headers: buildAuthHeaders(apiKey),
    })
  },
}

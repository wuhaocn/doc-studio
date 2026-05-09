import axios from 'axios'
import { clearCurrentUser, getCurrentUser, hydrateCurrentUser } from '../../utils/user'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 创建axios实例
const httpClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'X-Memora-Client': 'memora-web-app',
  },
})

const refreshClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'X-Memora-Client': 'memora-web-app',
  },
})

let refreshPromise = null

const shouldAttemptRefresh = (config, code) => {
  return code === 401
    && !config?._skipAuthRefresh
    && !config?._retriedAuthRefresh
    && !!getCurrentUser()?.accessToken
}

const refreshAccessToken = async () => {
  if (refreshPromise) {
    return refreshPromise
  }

  const currentUser = getCurrentUser()
  refreshPromise = refreshClient.post('/api/v1/auth/refresh', {}, {
    headers: currentUser?.accessToken
      ? { Authorization: `Bearer ${currentUser.accessToken}` }
      : {},
  })
    .then((response) => {
      const payload = response.data
      if (payload?.code !== 200) {
        throw payload
      }

      const nextUser = hydrateCurrentUser(payload.data)
      if (!nextUser?.accessToken) {
        throw { code: 401, message: '会话刷新失败' }
      }
      return nextUser.accessToken
    })
    .finally(() => {
      refreshPromise = null
    })

  return refreshPromise
}

const retryWithRefreshedSession = async (config, errorPayload) => {
  const code = errorPayload?.code || errorPayload?.status
  if (!shouldAttemptRefresh(config, code)) {
    if (code === 401) {
      clearCurrentUser()
    }
    return Promise.reject(errorPayload)
  }

  try {
    const nextAccessToken = await refreshAccessToken()
    return httpClient({
      ...config,
      _retriedAuthRefresh: true,
      headers: {
        ...(config?.headers || {}),
        Authorization: `Bearer ${nextAccessToken}`,
      },
    })
  } catch (refreshError) {
    clearCurrentUser()
    return Promise.reject(errorPayload)
  }
}

// 请求拦截器
httpClient.interceptors.request.use(
  (config) => {
    const currentUser = getCurrentUser()
    if (currentUser?.accessToken) {
      config.headers.Authorization = `Bearer ${currentUser.accessToken}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
httpClient.interceptors.response.use(
  async (response) => {
    const payload = response.data
    if (payload?.code === 200) {
      return payload
    }
    return retryWithRefreshedSession(response.config, payload)
  },
  async (error) => {
    // 处理错误响应
    if (error.response) {
      const { status, data } = error.response
      if (status === 404) {
        console.error('请求的资源不存在')
      } else if (status === 500) {
        console.error('服务器错误')
      }
      return retryWithRefreshedSession(error.config, data || { code: status, message: error.message })
    } else if (error.request) {
      console.error('网络错误，请检查网络连接')
      return Promise.reject({ code: 500, message: '网络错误，请检查网络连接' })
    } else {
      console.error('请求配置错误', error.message)
      return Promise.reject({ code: 500, message: error.message })
    }
  }
)

export default httpClient

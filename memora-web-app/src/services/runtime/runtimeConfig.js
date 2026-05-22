const DEFAULT_RUNTIME_CONFIG = Object.freeze({
  app: {
    name: 'Memora',
    title: 'Memora - 在线文档工作区',
  },
  auth: {
    browserClientHeaderName: 'X-Memora-Client',
    browserClientHeaderValue: 'memora-web-app',
    seedAccountLoginEnabled: false,
    sessionTransport: 'HTTP_ONLY_COOKIE',
    sessionCookieSameSite: 'Lax',
    sessionCookieSecure: false,
    sessionCookieMaxAgeSeconds: 2592000,
    refreshEnabled: true,
  },
  features: {
    ownerRegistrationEnabled: true,
    inviteAcceptEnabled: true,
    workspaceSwitchEnabled: true,
    knowledgeBasePermissionEnabled: true,
    publicShareEnabled: true,
    publicSiteEnabled: true,
    openApiEnabled: true,
    auditExportEnabled: true,
  },
})

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
const RUNTIME_CONFIG_URL = `${API_BASE_URL}/services/config`
let runtimeConfig = DEFAULT_RUNTIME_CONFIG

const normalizeRuntimeConfig = (rawConfig) => {
  if (!rawConfig || typeof rawConfig !== 'object') {
    return DEFAULT_RUNTIME_CONFIG
  }

  return {
    app: {
      ...DEFAULT_RUNTIME_CONFIG.app,
      ...(rawConfig.app || {}),
    },
    auth: {
      ...DEFAULT_RUNTIME_CONFIG.auth,
      ...(rawConfig.auth || {}),
    },
    features: {
      ...DEFAULT_RUNTIME_CONFIG.features,
      ...(rawConfig.features || {}),
    },
  }
}

const applyRuntimeConfig = (nextConfig) => {
  runtimeConfig = normalizeRuntimeConfig(nextConfig)
  if (typeof window !== 'undefined') {
    window.__MEMORA_RUNTIME_CONFIG__ = runtimeConfig
  }
  if (typeof document !== 'undefined' && runtimeConfig.app?.title) {
    document.title = runtimeConfig.app.title
  }
  return runtimeConfig
}

export const getRuntimeConfig = () => runtimeConfig

export const getRuntimeWebClientHeaders = () => {
  const config = getRuntimeConfig()
  const headerName = config.auth?.browserClientHeaderName || DEFAULT_RUNTIME_CONFIG.auth.browserClientHeaderName
  const headerValue = config.auth?.browserClientHeaderValue || DEFAULT_RUNTIME_CONFIG.auth.browserClientHeaderValue
  return {
    [headerName]: headerValue,
  }
}

export const bootstrapRuntimeConfig = async () => {
  try {
    const response = await fetch(RUNTIME_CONFIG_URL, {
      method: 'GET',
      credentials: 'include',
      headers: {
        ...getRuntimeWebClientHeaders(),
      },
    })
    if (!response.ok) {
      throw new Error(`runtime config request failed with ${response.status}`)
    }

    const payload = await response.json()
    if (payload?.code !== 200 || !payload?.data) {
      throw new Error(payload?.message || 'runtime config payload invalid')
    }

    return applyRuntimeConfig(payload.data)
  } catch (error) {
    console.error('加载运行时配置失败，已回退到默认配置', error)
    return applyRuntimeConfig(DEFAULT_RUNTIME_CONFIG)
  }
}

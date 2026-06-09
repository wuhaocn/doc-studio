const STORAGE_KEY = 'memora-auth-session'
export const AUTH_SESSION_CHANGED_EVENT = 'memora:auth-session-changed'
export const AUTH_SESSION_BROADCAST_CHANNEL = 'memora-auth-session-broadcast'
const AUTH_SESSION_BROADCAST_ACTIONS = {
  SYNC: 'SYNC',
  CLEAR: 'CLEAR',
}
const primaryStorage = () => window.sessionStorage

const normalizeSessionUser = (session) => {
  if (!session?.userId || !session?.tenantId) {
    return null
  }

  return {
    id: session.userId,
    username: session.username || `user-${session.userId}`,
    nickname: session.displayName || session.username || `用户${session.userId}`,
    email: session.email || `${session.userId}@memora.local`,
    avatar: '',
    status: 1,
    tenantId: session.tenantId,
    tenantName: session.tenantName,
    tenantSlug: session.tenantSlug,
    industry: session.industry,
    planName: session.planName,
    role: session.role,
  }
}

const normalizeStoredUser = (storedUser) => {
  if (!storedUser?.id || !storedUser?.tenantId) {
    return null
  }

  return {
    id: storedUser.id,
    username: storedUser.username || `user-${storedUser.id}`,
    nickname: storedUser.nickname || storedUser.username || `用户${storedUser.id}`,
    email: storedUser.email || `${storedUser.id}@memora.local`,
    avatar: storedUser.avatar || '',
    status: storedUser.status ?? 1,
    tenantId: storedUser.tenantId,
    tenantName: storedUser.tenantName,
    tenantSlug: storedUser.tenantSlug,
    industry: storedUser.industry,
    planName: storedUser.planName,
    role: storedUser.role,
  }
}

const readStoredUser = () => {
  try {
    const rawValue = primaryStorage().getItem(STORAGE_KEY)
    if (!rawValue) {
      return null
    }

    const parsed = JSON.parse(rawValue)
    const normalized = normalizeStoredUser(parsed)
    if (!normalized) {
      return null
    }

    return normalized
  } catch (error) {
    console.error('读取本地会话失败', error)
    return null
  }
}

const dispatchSessionChanged = (detail) => {
  window.dispatchEvent(new CustomEvent(AUTH_SESSION_CHANGED_EVENT, { detail }))
}

const writeStoredUser = (user) => {
  if (!user) {
    return null
  }

  const normalizedUser = normalizeStoredUser(user)
  if (!normalizedUser) {
    return null
  }

  const nextValue = JSON.stringify(normalizedUser)
  const currentValue = primaryStorage().getItem(STORAGE_KEY)
  if (currentValue === nextValue) {
    return normalizedUser
  }

  primaryStorage().setItem(STORAGE_KEY, nextValue)
  return normalizedUser
}

const broadcastSessionChange = (action, user = null) => {
  if (typeof BroadcastChannel === 'undefined') {
    return
  }

  const channel = new BroadcastChannel(AUTH_SESSION_BROADCAST_CHANNEL)
  channel.postMessage({
    action,
    user,
    emittedAt: Date.now(),
  })
  channel.close()
}

const persistUser = (user, options = {}) => {
  const normalizedUser = writeStoredUser(user)
  if (!normalizedUser) {
    return null
  }

  if (options.broadcast !== false) {
    broadcastSessionChange(AUTH_SESSION_BROADCAST_ACTIONS.SYNC, normalizedUser)
  }
  if (options.dispatch !== false) {
    dispatchSessionChanged(normalizedUser)
  }
  return normalizedUser
}

export const hydrateCurrentUser = (session) => {
  const sessionUser = normalizeSessionUser(session)
  if (!sessionUser) {
    return null
  }

  return persistUser(sessionUser)
}

export const clearCurrentUser = (options = {}) => {
  primaryStorage().removeItem(STORAGE_KEY)
  if (options.broadcast !== false) {
    broadcastSessionChange(AUTH_SESSION_BROADCAST_ACTIONS.CLEAR)
  }
  if (options.dispatch !== false) {
    dispatchSessionChanged(null)
  }
}

export const parseAuthSessionBroadcastMessage = (message) => {
  try {
    const payload = typeof message === 'string' ? JSON.parse(message) : message
    if (!payload?.action || !Object.values(AUTH_SESSION_BROADCAST_ACTIONS).includes(payload.action)) {
      return null
    }
    return payload
  } catch (error) {
    console.error('解析跨标签页会话广播失败', error)
    return null
  }
}

export const subscribeAuthSessionBroadcast = (handler) => {
  if (typeof BroadcastChannel === 'undefined') {
    return () => {}
  }

  const channel = new BroadcastChannel(AUTH_SESSION_BROADCAST_CHANNEL)
  channel.onmessage = (event) => {
    const payload = parseAuthSessionBroadcastMessage(event.data)
    if (payload) {
      handler(payload)
    }
  }
  return () => channel.close()
}

export const applyAuthSessionBroadcastPayload = (payload) => {
  if (!payload?.action) {
    return getCurrentUser()
  }

  if (payload.action === AUTH_SESSION_BROADCAST_ACTIONS.CLEAR) {
    clearCurrentUser({ broadcast: false })
    return null
  }

  if (payload.action === AUTH_SESSION_BROADCAST_ACTIONS.SYNC) {
    return persistUser(payload.user, { broadcast: false })
  }

  return getCurrentUser()
}

export const isLoggedIn = () => {
  return !!readStoredUser()
}

export const getCurrentUser = () => {
  return readStoredUser()
}

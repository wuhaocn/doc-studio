const STORAGE_KEY = 'memora-auth-session'
export const AUTH_SESSION_CHANGED_EVENT = 'memora:auth-session-changed'
const primaryStorage = () => window.sessionStorage
const legacyStorage = () => window.localStorage

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
    const rawValue = primaryStorage().getItem(STORAGE_KEY) || legacyStorage().getItem(STORAGE_KEY)
    if (!rawValue) {
      return null
    }

    const parsed = JSON.parse(rawValue)
    const normalized = normalizeStoredUser(parsed)
    if (!normalized) {
      return null
    }

    const normalizedValue = JSON.stringify(normalized)
    if (!primaryStorage().getItem(STORAGE_KEY) || primaryStorage().getItem(STORAGE_KEY) !== normalizedValue) {
      primaryStorage().setItem(STORAGE_KEY, normalizedValue)
      legacyStorage().removeItem(STORAGE_KEY)
    }

    return normalized
  } catch (error) {
    console.error('读取本地会话失败', error)
    return null
  }
}

const persistUser = (user) => {
  if (!user) {
    return null
  }

  const nextValue = JSON.stringify(user)
  const currentValue = primaryStorage().getItem(STORAGE_KEY)
  if (currentValue === nextValue) {
    return user
  }

  primaryStorage().setItem(STORAGE_KEY, nextValue)
  legacyStorage().removeItem(STORAGE_KEY)
  window.dispatchEvent(new CustomEvent(AUTH_SESSION_CHANGED_EVENT, { detail: user }))
  return user
}

export const hydrateCurrentUser = (session) => {
  const sessionUser = normalizeSessionUser(session)
  if (!sessionUser) {
    return null
  }

  return persistUser(sessionUser)
}

export const clearCurrentUser = () => {
  primaryStorage().removeItem(STORAGE_KEY)
  legacyStorage().removeItem(STORAGE_KEY)
  window.dispatchEvent(new CustomEvent(AUTH_SESSION_CHANGED_EVENT))
}

export const isLoggedIn = () => {
  return !!readStoredUser()
}

export const getCurrentUser = () => {
  return readStoredUser()
}

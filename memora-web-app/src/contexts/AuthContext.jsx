import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { authApi } from '../services/api/authApi'
import { tenantInviteApi } from '../services/api/tenantInviteApi'
import { workspaceApi } from '../services/api/workspaceApi'
import { clearRememberedKnowledgeBase } from '../utils/knowledgeBaseRoute'
import { AUTH_SESSION_CHANGED_EVENT, clearCurrentUser, getCurrentUser, hydrateCurrentUser, isLoggedIn } from '../utils/user'

const AuthContext = createContext(null)

export const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(getCurrentUser())
  const [sessionLoading, setSessionLoading] = useState(true)
  const [joinedWorkspaces, setJoinedWorkspaces] = useState([])
  const [workspaceSwitching, setWorkspaceSwitching] = useState(false)

  const refreshCurrentSession = useCallback(async () => {
    if (!isLoggedIn()) {
      setCurrentUser(null)
      return null
    }

    try {
      const response = await authApi.getCurrentSession()
      if (response.code !== 200) {
        return null
      }

      return hydrateCurrentUser(response.data)
    } catch (error) {
      console.error('同步当前会话失败', error)
      clearCurrentUser()
      return null
    }
  }, [])

  const loadJoinedWorkspaces = useCallback(async () => {
    if (!isLoggedIn()) {
      setJoinedWorkspaces([])
      return []
    }

    try {
      const response = await workspaceApi.getJoinedWorkspaces()
      const nextWorkspaces = response.code === 200 ? (response.data || []) : []
      setJoinedWorkspaces(nextWorkspaces)
      return nextWorkspaces
    } catch (error) {
      console.error('加载已加入工作区失败', error)
      setJoinedWorkspaces([])
      return []
    }
  }, [])

  useEffect(() => {
    const handleSessionChanged = () => {
      setCurrentUser(getCurrentUser())
    }

    window.addEventListener(AUTH_SESSION_CHANGED_EVENT, handleSessionChanged)
    return () => {
      window.removeEventListener(AUTH_SESSION_CHANGED_EVENT, handleSessionChanged)
    }
  }, [])

  useEffect(() => {
    let cancelled = false

    const bootstrapSession = async () => {
      if (!isLoggedIn()) {
        if (!cancelled) {
          setSessionLoading(false)
        }
        return
      }

      await refreshCurrentSession()
      if (!cancelled) {
        setSessionLoading(false)
      }
    }

    bootstrapSession()

    return () => {
      cancelled = true
    }
  }, [refreshCurrentSession])

  useEffect(() => {
    if (!currentUser?.id || !currentUser?.tenantId) {
      setJoinedWorkspaces([])
      return
    }

    loadJoinedWorkspaces()
  }, [currentUser?.id, currentUser?.tenantId, loadJoinedWorkspaces])

  const login = useCallback(async ({ username, password, tenantSlug }) => {
    const response = await authApi.login({ username, password, tenantSlug })
    if (response.code !== 200) {
      throw new Error(response.message || '登录失败')
    }

    const nextUser = hydrateCurrentUser(response.data)
    setCurrentUser(nextUser)
    await loadJoinedWorkspaces()
    return nextUser
  }, [loadJoinedWorkspaces])

  const registerOwner = useCallback(async (payload) => {
    const response = await authApi.registerOwner(payload)
    if (response.code !== 200) {
      throw new Error(response.message || '注册失败')
    }

    const nextUser = hydrateCurrentUser(response.data)
    setCurrentUser(nextUser)
    await loadJoinedWorkspaces()
    return nextUser
  }, [loadJoinedWorkspaces])

  const acceptInvite = useCallback(async (payload) => {
    const response = await tenantInviteApi.acceptInvite(payload)
    if (response.code !== 200) {
      throw new Error(response.message || '接受邀请失败')
    }

    const nextUser = hydrateCurrentUser(response.data)
    setCurrentUser(nextUser)
    await loadJoinedWorkspaces()
    return nextUser
  }, [loadJoinedWorkspaces])

  const switchWorkspace = useCallback(async (tenantId) => {
    try {
      setWorkspaceSwitching(true)
      const response = await workspaceApi.switchWorkspace(tenantId)
      if (response.code !== 200) {
        throw new Error(response.message || '切换工作区失败')
      }

      clearRememberedKnowledgeBase()
      const nextUser = hydrateCurrentUser(response.data)
      setCurrentUser(nextUser)
      await loadJoinedWorkspaces()
      return nextUser
    } finally {
      setWorkspaceSwitching(false)
    }
  }, [loadJoinedWorkspaces])

  const logout = useCallback(async () => {
    try {
      if (isLoggedIn()) {
        await authApi.logout()
      }
    } catch (error) {
      console.error('退出登录失败', error)
    }

    clearRememberedKnowledgeBase()
    clearCurrentUser()
    setCurrentUser(null)
    setJoinedWorkspaces([])
  }, [])

  const contextValue = useMemo(() => ({
    currentUser,
    sessionLoading,
    joinedWorkspaces,
    workspaceSwitching,
    isAuthenticated: !!currentUser?.accessToken,
    login,
    registerOwner,
    acceptInvite,
    switchWorkspace,
    logout,
    refreshCurrentSession,
    loadJoinedWorkspaces,
  }), [
    acceptInvite,
    currentUser,
    joinedWorkspaces,
    loadJoinedWorkspaces,
    login,
    logout,
    refreshCurrentSession,
    registerOwner,
    sessionLoading,
    switchWorkspace,
    workspaceSwitching,
  ])

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

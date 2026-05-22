import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { prefetchHome } from '../../router/prefetch'
import { getRuntimeConfig } from '../../services/runtime/runtimeConfig'
import styles from './LoginPage.module.css'

const LoginPage = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, login, sessionLoading } = useAuth()
  useDocumentTitle('登录')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [tenantSlug, setTenantSlug] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const runtimeConfig = getRuntimeConfig()
  const seedAccountLoginEnabled = Boolean(runtimeConfig?.auth?.seedAccountLoginEnabled)

  const from = location.state?.from || '/'

  if (!sessionLoading && isAuthenticated) {
    return <Navigate to={from} replace />
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const trimmedUsername = username.trim()
    if (!trimmedUsername) {
      setErrorMessage('请输入用户名')
      return
    }
    if (!password) {
      setErrorMessage('请输入密码')
      return
    }
    try {
      setSubmitting(true)
      setErrorMessage('')
      await login({ username: trimmedUsername, password, tenantSlug: tenantSlug.trim() || undefined })
      prefetchHome()
      navigate(from, { replace: true })
    } catch (error) {
      setErrorMessage(error?.message || '登录失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.page}>
      <section className={styles.panel}>
        <div className={styles.hero}>
          <p className={styles.eyebrow}>Memora</p>
          <h1 className={styles.title}>登录到工作区</h1>
          <p className={styles.description}>使用账号进入工作区。</p>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <label className={styles.field}>
            <span>用户名</span>
            <input value={username} onChange={(event) => setUsername(event.target.value)} placeholder="请输入用户名" />
          </label>
          <label className={styles.field}>
            <span>密码</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="请输入密码"
            />
          </label>
          <label className={styles.field}>
            <span>工作区地址（可选）</span>
            <input
              value={tenantSlug}
              onChange={(event) => setTenantSlug(event.target.value)}
              placeholder="如 my-team"
            />
          </label>
          {errorMessage && <div className={styles.error}>{errorMessage}</div>}
          <button type="submit" className={styles.submitButton} disabled={submitting || sessionLoading}>
            {submitting ? '登录中...' : '进入工作区'}
          </button>
          <div className={styles.linkRow}>
            <Link to="/register" className={styles.linkAction}>创建工作区</Link>
            <Link to="/accept-invite" className={styles.linkAction}>接受邀请</Link>
          </div>
          <div className={styles.inlineNote}>
            {seedAccountLoginEnabled ? (
              <>
                <strong>本地联调：admin / 123456</strong>
                <span>默认主类支持该账号登录。</span>
              </>
            ) : (
              <>
                <strong>当前环境无预置管理员</strong>
                <span>请先创建工作区，或用 `./start-backend-dev.sh` 启动后端。</span>
              </>
            )}
          </div>
        </form>
      </section>
    </div>
  )
}

export default LoginPage

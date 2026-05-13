import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { prefetchHome } from '../../router/prefetch'
import styles from './LoginPage.module.css'

const LoginPage = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, login, sessionLoading } = useAuth()
  useDocumentTitle('登录')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [tenantSlug, setTenantSlug] = useState('')
  const [showWorkspaceField, setShowWorkspaceField] = useState(false)
  const [showDevHint, setShowDevHint] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

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
          <h1 className={styles.title}>进入工作区</h1>
          <p className={styles.description}>
            输入账号登录，或创建新工作区开始使用。
          </p>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.formHeader}>
            <h2 className={styles.formTitle}>登录</h2>
          </div>
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
          <button
            type="button"
            className={styles.textButton}
            onClick={() => setShowWorkspaceField((current) => !current)}
          >
            {showWorkspaceField ? '收起' : '指定工作区'}
          </button>
          {showWorkspaceField ? (
            <div className={styles.advancedPanel}>
              <label className={styles.field}>
                <span>工作区地址</span>
                <input
                  value={tenantSlug}
                  onChange={(event) => setTenantSlug(event.target.value)}
                  placeholder="可选，如 my-team"
                />
              </label>
            </div>
          ) : null}
          {errorMessage && <div className={styles.error}>{errorMessage}</div>}
          <button type="submit" className={styles.submitButton} disabled={submitting || sessionLoading}>
            {submitting ? '登录中...' : '进入工作区'}
          </button>
          <div className={styles.secondaryActions}>
            <Link to="/register" className={styles.secondaryActionCard}>
              <strong>创建工作区</strong>
              <span>注册并成为管理员</span>
            </Link>
            <Link to="/accept-invite" className={styles.secondaryActionCard}>
              <strong>接受邀请</strong>
              <span>通过邀请链接加入</span>
            </Link>
          </div>
          {import.meta.env.DEV ? (
            <>
              <button
                type="button"
                className={styles.textButton}
                onClick={() => setShowDevHint((current) => !current)}
              >
                {showDevHint ? '收起本地联调说明' : '查看本地联调账号说明'}
              </button>
              {showDevHint ? (
                <div className={styles.inlineNote}>
                  <strong>admin / 123456</strong>
                  <span>仅当后端通过 ./start-backend-dev.sh 以 dev profile 启动时可用。</span>
                </div>
              ) : null}
            </>
          ) : null}
        </form>
      </section>
    </div>
  )
}

export default LoginPage

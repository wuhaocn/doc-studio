import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import styles from './LoginPage.module.css'

const LoginPage = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, login, sessionLoading } = useAuth()
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
    try {
      setSubmitting(true)
      setErrorMessage('')
      await login({ username: username.trim(), password, tenantSlug: tenantSlug.trim() || undefined })
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
          <p className={styles.eyebrow}>Memora 登录</p>
          <h1 className={styles.title}>先进入工作区，再继续写作</h1>
          <p className={styles.description}>
            已有账号默认只需要用户名和密码。首次进入请创建工作区，收到邀请则通过邀请入口加入现有团队。
          </p>
          <div className={styles.heroChecklist}>
            <div className={styles.heroChecklistItem}>
              <strong>已有账号</strong>
              <span>登录后会直接返回你刚才要访问的页面。</span>
            </div>
            <div className={styles.heroChecklistItem}>
              <strong>首次使用</strong>
              <span>创建工作区后自动成为管理员，再邀请成员加入。</span>
            </div>
            <div className={styles.heroChecklistItem}>
              <strong>收到邀请</strong>
              <span>使用邀请链接加入现有工作区，不需要重复创建团队空间。</span>
            </div>
          </div>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.formHeader}>
            <p className={styles.formEyebrow}>登录</p>
            <h2 className={styles.formTitle}>继续进入工作区</h2>
            <p className={styles.formDescription}>默认使用用户名和密码登录；只有多工作区账号才需要展开高级选项。</p>
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
            {showWorkspaceField ? '收起高级选项' : '多工作区账号？指定工作区地址'}
          </button>
          {showWorkspaceField ? (
            <div className={styles.advancedPanel}>
              <label className={styles.field}>
                <span>工作区地址</span>
                <input
                  value={tenantSlug}
                  onChange={(event) => setTenantSlug(event.target.value)}
                  placeholder="可选，例如 east-manufacturing-docs"
                />
              </label>
              <p className={styles.fieldHint}>仅当同一账号加入多个工作区，且需要显式指定进入哪一个时使用。</p>
            </div>
          ) : null}
          {errorMessage && <div className={styles.error}>{errorMessage}</div>}
          <button type="submit" className={styles.submitButton} disabled={submitting || sessionLoading}>
            {submitting ? '登录中...' : '进入工作区'}
          </button>
          <div className={styles.secondaryActions}>
            <Link to="/register" className={styles.secondaryActionCard}>
              <strong>首次使用</strong>
              <span>创建工作区并成为当前管理员</span>
            </Link>
            <Link to="/accept-invite" className={styles.secondaryActionCard}>
              <strong>收到邀请</strong>
              <span>通过邀请加入现有工作区</span>
            </Link>
          </div>
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
        </form>
      </section>
    </div>
  )
}

export default LoginPage

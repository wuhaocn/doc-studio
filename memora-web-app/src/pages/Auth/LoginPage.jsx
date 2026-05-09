import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import styles from './LoginPage.module.css'

const LoginPage = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated, login, sessionLoading } = useAuth()
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('123456')
  const [tenantSlug, setTenantSlug] = useState('')
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
          <p className={styles.eyebrow}>Memora 文档工作区</p>
          <h1 className={styles.title}>进入知识与文档工作台</h1>
          <p className={styles.description}>
            在同一个工作区里继续管理知识库、撰写文档、查看版本并复制阅读链接。当前登录页只保留真实进入产品所需的最小会话信息。
          </p>
          <div className={styles.featureList}>
            <div className={styles.featureItem}>
              <strong>知识库</strong>
              <span>继续进入最近使用的知识库</span>
            </div>
            <div className={styles.featureItem}>
              <strong>文档</strong>
              <span>从新建到编辑、阅读、复制阅读链接一条线完成</span>
            </div>
            <div className={styles.featureItem}>
              <strong>版本</strong>
              <span>按需查看历史，不干扰当前写作流程</span>
            </div>
          </div>
          <div className={styles.demoTip}>
            <span className={styles.demoLabel}>本地种子账号</span>
            <strong>admin / 123456</strong>
            <span>用于本地联调；生产化入口应优先走 Owner 注册和成员邀请。</span>
          </div>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.formHeader}>
            <p className={styles.formEyebrow}>登录</p>
            <h2 className={styles.formTitle}>继续进入工作区</h2>
            <p className={styles.formDescription}>使用当前租户账号进入知识库与文档页面。</p>
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
          <label className={styles.field}>
            <span>工作区标识</span>
            <input
              value={tenantSlug}
              onChange={(event) => setTenantSlug(event.target.value)}
              placeholder="可选，多工作区账号可显式指定"
            />
          </label>
          {errorMessage && <div className={styles.error}>{errorMessage}</div>}
          <button type="submit" className={styles.submitButton} disabled={submitting || sessionLoading}>
            {submitting ? '登录中...' : '进入工作区'}
          </button>
          <p className={styles.submitHint}>当前已支持 Owner 注册、成员邀请接受和真实 session。</p>
          <div className={styles.helperLinks}>
            <Link to="/register" className={styles.helperLink}>注册工作区</Link>
            <Link to="/accept-invite" className={styles.helperLink}>接受邀请</Link>
          </div>
        </form>
      </section>
    </div>
  )
}

export default LoginPage

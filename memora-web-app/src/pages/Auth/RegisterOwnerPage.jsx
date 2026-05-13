import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import styles from './LoginPage.module.css'

const RegisterOwnerPage = () => {
  const navigate = useNavigate()
  const { isAuthenticated, registerOwner, sessionLoading } = useAuth()
  const [showCustomSlug, setShowCustomSlug] = useState(false)
  const [form, setForm] = useState({
    tenantName: '',
    tenantSlug: '',
    displayName: '',
    username: '',
    email: '',
    password: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  if (!sessionLoading && isAuthenticated) {
    return <Navigate to="/" replace />
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const trimmedTenantName = form.tenantName.trim()
    const trimmedUsername = form.username.trim()
    const trimmedDisplayName = form.displayName.trim()
    if (!trimmedTenantName) {
      setErrorMessage('请输入工作区名称')
      return
    }
    if (!trimmedDisplayName) {
      setErrorMessage('请输入显示名称')
      return
    }
    if (!trimmedUsername) {
      setErrorMessage('请输入用户名')
      return
    }
    if (!form.password || form.password.length < 6) {
      setErrorMessage('密码长度不能少于 6 位')
      return
    }
    try {
      setSubmitting(true)
      setErrorMessage('')
      await registerOwner({
        tenantName: trimmedTenantName,
        tenantSlug: form.tenantSlug.trim() || undefined,
        displayName: trimmedDisplayName,
        username: trimmedUsername,
        email: form.email.trim(),
        password: form.password,
      })
      navigate('/', { replace: true })
    } catch (error) {
      setErrorMessage(error?.message || '注册失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.page}>
      <section className={styles.panel}>
        <div className={styles.hero}>
          <p className={styles.eyebrow}>Memora</p>
          <h1 className={styles.title}>创建工作区</h1>
          <p className={styles.description}>
            注册后直接进入工作台，你将成为工作区管理员。
          </p>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.formHeader}>
            <h2 className={styles.formTitle}>注册</h2>
          </div>
          <label className={styles.field}>
            <span>工作区名称</span>
            <input
              value={form.tenantName}
              onChange={(event) => setForm((current) => ({ ...current, tenantName: event.target.value }))}
              placeholder="例如：华北质检中心"
              required
            />
          </label>
          <div className={styles.splitFields}>
            <label className={styles.field}>
              <span>你的姓名</span>
              <input
                value={form.displayName}
                onChange={(event) => setForm((current) => ({ ...current, displayName: event.target.value }))}
                placeholder="请输入姓名"
                required
              />
            </label>
            <label className={styles.field}>
              <span>邮箱</span>
              <input
                type="email"
                value={form.email}
                onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
                placeholder="请输入邮箱"
                required
              />
            </label>
          </div>
          <div className={styles.splitFields}>
            <label className={styles.field}>
              <span>用户名</span>
              <input
                value={form.username}
                onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))}
                placeholder="请输入用户名"
                required
              />
            </label>
            <label className={styles.field}>
              <span>密码</span>
              <input
                type="password"
                value={form.password}
                onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
                placeholder="至少 6 位密码"
                required
              />
            </label>
          </div>
          <button
            type="button"
            className={styles.textButton}
            onClick={() => setShowCustomSlug((current) => !current)}
          >
            {showCustomSlug ? '收起自定义地址' : '自定义工作区地址（可选）'}
          </button>
          {showCustomSlug ? (
            <div className={styles.advancedPanel}>
              <label className={styles.field}>
                <span>工作区地址</span>
                <input
                  value={form.tenantSlug}
                  onChange={(event) => setForm((current) => ({ ...current, tenantSlug: event.target.value }))}
                  placeholder="不填则自动生成"
                />
              </label>
            </div>
          ) : null}
          {errorMessage && <div className={styles.error}>{errorMessage}</div>}
          <button type="submit" className={styles.submitButton} disabled={submitting || sessionLoading}>
            {submitting ? '创建中...' : '创建工作区并进入'}
          </button>
          <div className={styles.helperLinks}>
            <Link to="/login" className={styles.helperLink}>已有账号，去登录</Link>
          </div>
        </form>
      </section>
    </div>
  )
}

export default RegisterOwnerPage

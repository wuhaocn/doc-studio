import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import styles from './LoginPage.module.css'

const RegisterOwnerPage = () => {
  const navigate = useNavigate()
  const { isAuthenticated, registerOwner, sessionLoading } = useAuth()
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
    try {
      setSubmitting(true)
      setErrorMessage('')
      await registerOwner({
        tenantName: form.tenantName.trim(),
        tenantSlug: form.tenantSlug.trim() || undefined,
        displayName: form.displayName.trim(),
        username: form.username.trim(),
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
          <p className={styles.eyebrow}>Owner Signup</p>
          <h1 className={styles.title}>先创建工作区，再开始知识生产</h1>
          <p className={styles.description}>
            当前注册只面向工作区管理员。注册完成后会立即创建用户、工作区、Owner 成员关系和可用会话。
          </p>
          <div className={styles.featureList}>
            <div className={styles.featureItem}>
              <strong>工作区</strong>
              <span>生成独立 tenant，并以它作为当前主权限边界。</span>
            </div>
            <div className={styles.featureItem}>
              <strong>成员</strong>
              <span>注册者默认成为 Owner，后续通过邀请成员加入。</span>
            </div>
            <div className={styles.featureItem}>
              <strong>主链路</strong>
              <span>注册成功后直接进入工作台继续创建知识库和文档。</span>
            </div>
          </div>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.formHeader}>
            <p className={styles.formEyebrow}>注册工作区</p>
            <h2 className={styles.formTitle}>创建 Owner 账号</h2>
            <p className={styles.formDescription}>只收集进入产品闭环所需的最小信息。</p>
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
          <label className={styles.field}>
            <span>工作区标识</span>
            <input
              value={form.tenantSlug}
              onChange={(event) => setForm((current) => ({ ...current, tenantSlug: event.target.value }))}
              placeholder="可选，不填则按名称自动生成"
            />
          </label>
          <div className={styles.splitFields}>
            <label className={styles.field}>
              <span>姓名</span>
              <input
                value={form.displayName}
                onChange={(event) => setForm((current) => ({ ...current, displayName: event.target.value }))}
                placeholder="请输入姓名"
                required
              />
            </label>
            <label className={styles.field}>
              <span>用户名</span>
              <input
                value={form.username}
                onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))}
                placeholder="请输入用户名"
                required
              />
            </label>
          </div>
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

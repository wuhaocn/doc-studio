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
          <p className={styles.eyebrow}>创建工作区</p>
          <h1 className={styles.title}>先开一个团队空间，再开始沉淀知识</h1>
          <p className={styles.description}>
            注册完成后会直接进入工作台。你会成为当前工作区管理员，后续再邀请成员加入即可。
          </p>
          <div className={styles.heroChecklist}>
            <div className={styles.heroChecklistItem}>
              <strong>一步进入</strong>
              <span>创建完成后直接建立会话，不需要再单独登录一次。</span>
            </div>
            <div className={styles.heroChecklistItem}>
              <strong>管理员身份</strong>
              <span>当前注册者会自动成为工作区管理员。</span>
            </div>
            <div className={styles.heroChecklistItem}>
              <strong>后续扩展</strong>
              <span>成员通过邀请加入，知识库和文档在进入后继续创建。</span>
            </div>
          </div>
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.formHeader}>
            <p className={styles.formEyebrow}>注册工作区</p>
            <h2 className={styles.formTitle}>创建 Owner 账号</h2>
            <p className={styles.formDescription}>先填写工作区名称和你的账号信息；自定义工作区地址属于可选项。</p>
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
                  placeholder="不填则按名称自动生成"
                />
              </label>
              <p className={styles.fieldHint}>仅当你希望工作区使用固定访问地址时再填写，例如 north-quality-center。</p>
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

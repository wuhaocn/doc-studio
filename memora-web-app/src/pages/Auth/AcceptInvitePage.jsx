import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { tenantInviteApi } from '../../services/api/tenantInviteApi'
import styles from './LoginPage.module.css'

const AcceptInvitePage = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { isAuthenticated, acceptInvite, sessionLoading } = useAuth()
  const [token, setToken] = useState(searchParams.get('token') || '')
  const [inviteLoading, setInviteLoading] = useState(Boolean(searchParams.get('token')))
  const [inviteInfo, setInviteInfo] = useState(null)
  const [form, setForm] = useState({
    displayName: '',
    username: '',
    email: '',
    password: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const loadInvite = useCallback(async (rawToken = token) => {
    const normalizedToken = rawToken.trim()
    if (!normalizedToken) {
      setInviteInfo(null)
      setInviteLoading(false)
      setErrorMessage('请先输入邀请令牌')
      return
    }

    try {
      setInviteLoading(true)
      setErrorMessage('')
      const response = await tenantInviteApi.getInvite(normalizedToken)
      if (response.code === 200) {
        setInviteInfo(response.data)
        setForm((current) => ({
          ...current,
          displayName: current.displayName || response.data.inviteeDisplayName || '',
          email: response.data.inviteeEmail || current.email,
        }))
      }
    } catch (error) {
      setInviteInfo(null)
      setErrorMessage(error?.message || '读取邀请失败，请确认链接是否有效')
    } finally {
      setInviteLoading(false)
    }
  }, [token])

  useEffect(() => {
    const initialToken = searchParams.get('token')
    if (!initialToken) {
      setInviteLoading(false)
      return
    }

    setToken(initialToken)
    void loadInvite(initialToken)
  }, [loadInvite, searchParams])

  if (!sessionLoading && isAuthenticated) {
    return <Navigate to="/" replace />
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    try {
      setSubmitting(true)
      setErrorMessage('')
      await acceptInvite({
        token: token.trim(),
        displayName: form.displayName.trim(),
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
      })
      navigate('/', { replace: true })
    } catch (error) {
      setErrorMessage(error?.message || '接受邀请失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.page}>
      <section className={styles.panel}>
        <div className={styles.hero}>
          <p className={styles.eyebrow}>Invite Accept</p>
          <h1 className={styles.title}>通过邀请加入现有工作区</h1>
          <p className={styles.description}>
            普通成员不开放公共注册。接受邀请时会创建账号或复用已有账号，并直接进入对应工作区。
          </p>
          {inviteInfo ? (
            <div className={styles.infoCard}>
              <strong>{inviteInfo.tenantName}</strong>
              <span>{inviteInfo.role} · 受邀邮箱 {inviteInfo.inviteeEmail}</span>
              <span>邀请人 {inviteInfo.inviterDisplayName}</span>
            </div>
          ) : (
            <div className={styles.infoCard}>
              <strong>邀请链接</strong>
              <span>{inviteLoading ? '正在读取邀请信息...' : '如果链接无效，可手动粘贴邀请令牌继续。'}</span>
            </div>
          )}
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.formHeader}>
            <p className={styles.formEyebrow}>接受邀请</p>
            <h2 className={styles.formTitle}>完成账号并进入工作区</h2>
            <p className={styles.formDescription}>接受成功后会直接建立真实 session。</p>
          </div>
          <label className={styles.field}>
            <span>邀请令牌</span>
            <div className={styles.fieldWithAction}>
              <input
                value={token}
                onChange={(event) => {
                  const nextToken = event.target.value
                  setToken(nextToken)
                  if (inviteInfo?.inviteToken !== nextToken.trim()) {
                    setInviteInfo(null)
                  }
                }}
                placeholder="请粘贴邀请令牌"
                required
              />
              <button
                type="button"
                className={styles.fieldActionButton}
                onClick={() => loadInvite()}
                disabled={inviteLoading || !token.trim()}
              >
                {inviteLoading ? '读取中...' : '读取邀请'}
              </button>
            </div>
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
              placeholder="首次加入请设置密码；已有账号请填写原密码"
              required
            />
          </label>
          {errorMessage && <div className={styles.error}>{errorMessage}</div>}
          <button type="submit" className={styles.submitButton} disabled={submitting || sessionLoading}>
            {submitting ? '加入中...' : '接受邀请并进入'}
          </button>
          <div className={styles.helperLinks}>
            <Link to="/login" className={styles.helperLink}>已有会话入口，返回登录</Link>
          </div>
        </form>
      </section>
    </div>
  )
}

export default AcceptInvitePage

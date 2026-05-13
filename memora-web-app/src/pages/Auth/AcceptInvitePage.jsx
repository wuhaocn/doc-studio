import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { tenantInviteApi } from '../../services/api/tenantInviteApi'
import styles from './LoginPage.module.css'

const AcceptInvitePage = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const tokenFromLink = searchParams.get('token') || ''
  const { isAuthenticated, acceptInvite, sessionLoading } = useAuth()
  const [token, setToken] = useState(tokenFromLink)
  const [inviteLoading, setInviteLoading] = useState(Boolean(tokenFromLink))
  const [inviteInfo, setInviteInfo] = useState(null)
  const [manualTokenOpen, setManualTokenOpen] = useState(!tokenFromLink)
  const [accountMode, setAccountMode] = useState('new')
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

  const isExistingAccountMode = accountMode === 'existing'
  const inviteLoaded = Boolean(inviteInfo)

  return (
    <div className={styles.page}>
      <section className={styles.panel}>
        <div className={styles.hero}>
          <p className={styles.eyebrow}>Memora</p>
          <h1 className={styles.title}>接受邀请</h1>
          <p className={styles.description}>
            加入现有工作区，首次加入会创建账号。
          </p>
          {inviteInfo ? (
            <div className={styles.infoCard}>
              <strong>{inviteInfo.tenantName}</strong>
              <span>{inviteInfo.role} · {inviteInfo.inviteeEmail}</span>
            </div>
          ) : (
            <div className={styles.infoCard}>
              <span>{inviteLoading ? '正在读取邀请...' : '请通过邀请链接进入或手动输入令牌'}</span>
            </div>
          )}
        </div>

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.formHeader}>
            <h2 className={styles.formTitle}>完成注册</h2>
          </div>
          <div className={styles.modeSwitch}>
            <button
              type="button"
              className={`${styles.modeButton} ${!isExistingAccountMode ? styles.modeButtonActive : ''}`}
              onClick={() => setAccountMode('new')}
              aria-pressed={!isExistingAccountMode}
            >
              首次加入
            </button>
            <button
              type="button"
              className={`${styles.modeButton} ${isExistingAccountMode ? styles.modeButtonActive : ''}`}
              onClick={() => setAccountMode('existing')}
              aria-pressed={isExistingAccountMode}
            >
              已有账号
            </button>
          </div>
          <button
            type="button"
            className={styles.textButton}
            onClick={() => setManualTokenOpen((current) => !current)}
          >
            {manualTokenOpen ? '收起' : '手动输入令牌'}
          </button>
          {manualTokenOpen || !inviteLoaded ? (
            <div className={styles.advancedPanel}>
              <label className={styles.field}>
                <span>邀请令牌</span>
                <div className={styles.fieldWithAction}>
                  <input
                    value={token}
                    onChange={(event) => {
                      const nextToken = event.target.value
                      setToken(nextToken)
                      if (inviteInfo) {
                        setInviteInfo(null)
                      }
                    }}
                    placeholder="粘贴邀请令牌"
                    required
                  />
                  <button
                    type="button"
                    className={styles.fieldActionButton}
                    onClick={() => loadInvite()}
                    disabled={inviteLoading || !token.trim()}
                  >
                    {inviteLoading ? '读取中...' : '验证'}
                  </button>
                </div>
              </label>
            </div>
          ) : null}
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
                placeholder={isExistingAccountMode ? '请输入已有账号用户名' : '请设置登录用户名'}
                required
              />
            </label>
          </div>
          <label className={styles.field}>
            <span>受邀邮箱</span>
            <input
              type="email"
              value={form.email}
              onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
              placeholder="请输入受邀邮箱"
              readOnly={inviteLoaded}
              className={inviteLoaded ? styles.readonlyInput : ''}
              required
            />
          </label>
          {inviteLoaded ? (
            <p className={styles.fieldHint}>邮箱已锁定为邀请指定地址</p>
          ) : null}
          <label className={styles.field}>
            <span>密码</span>
            <input
              type="password"
              value={form.password}
              onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
              placeholder={isExistingAccountMode ? '请输入已有账号密码' : '请设置登录密码，至少 6 位'}
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

import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { IconMenuFold, IconMenuUnfold } from '@arco-design/web-react/icon'
import { Avatar } from '@arco-design/web-react'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../Feedback/Toast'
import styles from './Header.module.css'

const Header = ({ onToggleSidebar, showMenuButton = true }) => {
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { currentUser, joinedWorkspaces, workspaceSwitching, switchWorkspace, logout } = useAuth()
  const toast = useToast()
  const [scrolled, setScrolled] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState(searchParams.get('keyword') || '')

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 8)
    }

    handleScroll()
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  useEffect(() => {
    setSearchKeyword(searchParams.get('keyword') || '')
  }, [location.pathname, searchParams])

  const handleSearchSubmit = (event) => {
    event.preventDefault()
    const normalizedKeyword = searchKeyword.trim()
    if (!normalizedKeyword) {
      navigate('/search')
      return
    }
    navigate(`/search?keyword=${encodeURIComponent(normalizedKeyword)}`)
  }

  const handleWorkspaceChange = async (event) => {
    const nextTenantId = Number(event.target.value)
    if (!nextTenantId || nextTenantId === currentUser.tenantId) {
      return
    }

    try {
      await switchWorkspace(nextTenantId)
      navigate('/', { replace: true })
    } catch (error) {
      console.error('切换工作区失败', error)
      toast.error(error?.message || '切换工作区失败，请稍后重试')
    }
  }

  return (
    <header className={`${styles.header} ${scrolled ? styles.scrolled : ''}`}>
      <div className={styles.left}>
        {showMenuButton ? (
          <button className={styles.menuButton} onClick={onToggleSidebar} type="button" aria-label="切换侧边栏">
            {location.pathname.startsWith('/kb/') ? <IconMenuFold /> : <IconMenuUnfold />}
          </button>
        ) : null}
        <Link to="/" className={styles.brand}>
          <span className={styles.brandMark}>M</span>
          <div className={styles.brandText}>
            <div className={styles.brandName}>Memora</div>
            <div className={styles.brandWorkspace}>{currentUser.tenantName}</div>
          </div>
        </Link>
      </div>

      <form className={styles.searchForm} onSubmit={handleSearchSubmit}>
        <input
          className={styles.searchInput}
          value={searchKeyword}
          onChange={(event) => setSearchKeyword(event.target.value)}
          placeholder="搜索标题或正文"
        />
        <button type="submit" className={styles.searchButton}>
          搜索
        </button>
      </form>

      <div className={styles.right}>
        {joinedWorkspaces.length > 1 ? (
          <div className={styles.workspaceSwitcher}>
            <select
              className={styles.workspaceSelect}
              value={currentUser.tenantId}
              onChange={handleWorkspaceChange}
              disabled={workspaceSwitching}
            >
              {joinedWorkspaces.map((workspace) => (
                <option key={workspace.tenantId} value={workspace.tenantId}>
                  {workspace.tenantName} · {workspace.role}
                </option>
              ))}
            </select>
          </div>
        ) : null}
        <Link
          to="/workspace/manage"
          className={`${styles.manageButton} ${location.pathname.startsWith('/workspace/manage') ? styles.manageButtonActive : ''}`}
        >
          <span className={styles.manageLabelFull}>工作区管理</span>
          <span className={styles.manageLabelShort}>管理</span>
        </Link>
        <div className={styles.userCard}>
          <Avatar size={30} className={styles.avatar}>
            {currentUser.nickname.charAt(0)}
          </Avatar>
          <div className={styles.userName}>{currentUser.nickname}</div>
        </div>
        <button type="button" className={styles.logoutButton} onClick={logout}>
          退出
        </button>
      </div>
    </header>
  )
}

export default Header

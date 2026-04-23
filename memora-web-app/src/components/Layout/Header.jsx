import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { IconMenuFold, IconMenuUnfold, IconHome, IconFolder } from '@arco-design/web-react/icon'
import { Avatar } from '@arco-design/web-react'
import { useAuth } from '../../contexts/AuthContext'
import { useKnowledgeBaseNavigation } from '../../hooks/useKnowledgeBaseNavigation'
import { getRememberedKnowledgeBaseId } from '../../utils/knowledgeBaseRoute'
import styles from './Header.module.css'

const Header = ({ onToggleSidebar, showMenuButton = true }) => {
  const location = useLocation()
  const { currentUser, logout } = useAuth()
  const [scrolled, setScrolled] = useState(false)
  const { knowledgeBases } = useKnowledgeBaseNavigation(currentUser.tenantId, {
    errorMessage: '加载头部知识库导航失败',
  })

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 8)
    }

    handleScroll()
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  const currentKnowledgeBaseRoute = useMemo(() => {
    if (location.pathname.startsWith('/kb/')) {
      return location.pathname
    }

    const rememberedKnowledgeBaseId = getRememberedKnowledgeBaseId()
    const rememberedKnowledgeBase = knowledgeBases.find((item) => item.id === rememberedKnowledgeBaseId)
    if (rememberedKnowledgeBase) {
      return `/kb/${rememberedKnowledgeBase.id}`
    }

    if (knowledgeBases[0]?.id) {
      return `/kb/${knowledgeBases[0].id}`
    }

    return '/'
  }, [knowledgeBases, location.pathname])

  const currentKnowledgeBase = useMemo(() => {
    return knowledgeBases.find((item) => currentKnowledgeBaseRoute === `/kb/${item.id}`) || null
  }, [currentKnowledgeBaseRoute, knowledgeBases])

  const contextItems = useMemo(() => {
    const items = [{
      to: '/',
      label: currentUser.tenantName,
      icon: <IconHome />,
      active: location.pathname === '/',
    }]

    if (location.pathname.startsWith('/kb/')) {
      items.push({
        to: currentKnowledgeBaseRoute,
        label: currentKnowledgeBase?.name || '知识库工作区',
        icon: <IconFolder />,
        active: true,
      })
      return items
    }

    if (location.pathname.startsWith('/docs/')) {
      items.push({
        to: currentKnowledgeBaseRoute,
        label: currentKnowledgeBase?.name || '知识库工作区',
        icon: <IconFolder />,
        active: false,
      })
      items.push({
        label: location.pathname.endsWith('/edit') ? '文档编辑' : '文档阅读',
        active: true,
      })
    }

    return items
  }, [currentKnowledgeBase, currentKnowledgeBaseRoute, currentUser.tenantName, location.pathname])

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

      <nav className={styles.nav}>
        {contextItems.map((item, index) => (
          item.to ? (
            <Link key={`${item.to}-${index}`} to={item.to} className={`${styles.navLink} ${item.active ? styles.active : ''}`}>
              {item.icon || null}
              <span>{item.label}</span>
            </Link>
          ) : (
            <span key={`${item.label}-${index}`} className={`${styles.navLabel} ${item.active ? styles.active : ''}`}>
              <span>{item.label}</span>
            </span>
          )
        ))}
      </nav>

      <div className={styles.right}>
        <div className={styles.userCard}>
          <Avatar size={30} className={styles.avatar}>
            {currentUser.nickname.charAt(0)}
          </Avatar>
          <div>
            <div className={styles.userName}>{currentUser.nickname}</div>
            <div className={styles.userRole}>{currentUser.role}</div>
          </div>
        </div>
        <button type="button" className={styles.logoutButton} onClick={logout}>
          退出
        </button>
      </div>
    </header>
  )
}

export default Header

import { Link, useLocation } from 'react-router-dom'
import { IconMenuFold, IconSearch, IconHome, IconFolder, IconRobot } from '@arco-design/web-react/icon'
import { Input, Avatar, Dropdown, Menu, Space } from '@arco-design/web-react'
import { getCurrentUser } from '../../utils/user'
import styles from './Header.module.css'

const Header = ({ onToggleSidebar }) => {
  const location = useLocation()
  const user = getCurrentUser()

  const userMenu = (
    <Menu>
      <Menu.Item key="profile">个人资料</Menu.Item>
      <Menu.Item key="settings">设置</Menu.Item>
      <Menu.Item key="logout">退出登录</Menu.Item>
    </Menu>
  )

  const isActive = (path) => {
    return location.pathname.startsWith(path)
  }

  return (
    <header className={styles.header}>
      <div className={styles.left}>
        <button className={styles.menuBtn} onClick={onToggleSidebar}>
          <IconMenuFold />
        </button>
        <Link to="/" className={styles.logo}>
          <span className={styles.logoText}>DocStudio</span>
        </Link>
        <div className={styles.nav}>
          <Space size="large">
            <Link to="/" className={`${styles.navLink} ${isActive('/') ? styles.active : ''}`}>
              <Space size="mini">
                <IconHome />
                <span>首页</span>
              </Space>
            </Link>
            <Link to="/resource" className={`${styles.navLink} ${isActive('/resource') ? styles.active : ''}`}>
              <Space size="mini">
                <IconFolder />
                <span>资源库</span>
              </Space>
            </Link>
            <Link to="/memora-ai" className={`${styles.navLink} ${isActive('/memora-ai') ? styles.active : ''}`}>
              <Space size="mini">
                <IconRobot />
                <span>Memora AI</span>
              </Space>
            </Link>
          </Space>
        </div>
      </div>
      <div className={styles.center}>
        <Input
          prefix={<IconSearch />}
          placeholder="搜索知识库和文档..."
          className={styles.search}
        />
      </div>
      <div className={styles.right}>
        <Dropdown trigger="click" droplist={userMenu}>
          <div className={styles.userInfo}>
            <Avatar size={32} className={styles.avatar}>
              {user.nickname.charAt(0)}
            </Avatar>
            <span className={styles.username}>{user.nickname}</span>
          </div>
        </Dropdown>
      </div>
    </header>
  )
}

export default Header

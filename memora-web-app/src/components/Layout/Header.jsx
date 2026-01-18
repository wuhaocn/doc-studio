import { Link } from 'react-router-dom'
import { IconMenuFold, IconSearch } from '@arco-design/web-react/icon'
import { Input, Avatar, Dropdown, Menu } from '@arco-design/web-react'
import { getCurrentUser } from '../../utils/user'
import styles from './Header.module.css'

const Header = ({ onToggleSidebar }) => {
  const user = getCurrentUser()

  const userMenu = (
    <Menu>
      <Menu.Item key="profile">个人资料</Menu.Item>
      <Menu.Item key="settings">设置</Menu.Item>
      <Menu.Item key="logout">退出登录</Menu.Item>
    </Menu>
  )

  return (
    <header className={styles.header}>
      <div className={styles.left}>
        <button className={styles.menuBtn} onClick={onToggleSidebar}>
          <IconMenuFold />
        </button>
        <Link to="/" className={styles.logo}>
          <span className={styles.logoText}>DocStudio</span>
        </Link>
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


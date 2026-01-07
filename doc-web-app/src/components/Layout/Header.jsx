import { Link } from 'react-router-dom'
import { IconMenuFold, IconSearch } from '@arco-design/web-react/icon'
import { Input } from '@arco-design/web-react'
import styles from './Header.module.css'

const Header = ({ onToggleSidebar }) => {
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
        {/* 用户信息区域，暂时留空 */}
      </div>
    </header>
  )
}

export default Header


import { useState } from 'react'
import Header from './Header'
import Sidebar from './Sidebar'
import styles from './Layout.module.css'

const Layout = ({ children }) => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)

  return (
    <div className={styles.layout}>
      <Header onToggleSidebar={() => setSidebarCollapsed(!sidebarCollapsed)} />
      <div className={styles.content}>
        <Sidebar collapsed={sidebarCollapsed} />
        <main className={styles.main}>{children}</main>
      </div>
    </div>
  )
}

export default Layout


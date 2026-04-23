import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Header from './Header'
import Sidebar from './Sidebar'
import styles from './Layout.module.css'

const Layout = () => {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const showGlobalSidebar = false

  return (
    <div className={styles.layout}>
      <Header
        onToggleSidebar={() => setSidebarCollapsed((value) => !value)}
        showMenuButton={showGlobalSidebar}
      />
      <div className={`${styles.shell} ${!showGlobalSidebar ? styles.shellFull : ''}`}>
        {showGlobalSidebar ? <Sidebar collapsed={sidebarCollapsed} /> : null}
        <main className={`${styles.main} ${!showGlobalSidebar ? styles.mainFull : ''}`}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default Layout

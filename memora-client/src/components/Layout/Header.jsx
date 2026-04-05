import React from 'react'
import './Layout.module.css'

const Header = () => {
  return (
    <header className="header">
      <div className="header-left">
        <h1 className="logo">Memora Client</h1>
      </div>
      <div className="header-right">
        <button>设置</button>
        <button className="primary">同步</button>
      </div>
    </header>
  )
}

export default Header
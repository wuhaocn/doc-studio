import React from 'react'
import './Home.module.css'

const Home = () => {
  return (
    <div className="home">
      <div className="home-header">
        <h1>欢迎使用 Memora Client</h1>
        <p>基于 Tauri + Rust 的在线文档管理系统桌面客户端</p>
      </div>
      <div className="home-content">
        <div className="home-cards">
          <div className="home-card">
            <div className="home-card-icon">📁</div>
            <h3>文件管理</h3>
            <p>浏览、编辑、组织本地文件和文件夹</p>
          </div>
          <div className="home-card">
            <div className="home-card-icon">📄</div>
            <h3>文档管理</h3>
            <p>创建、编辑、存储本地文档</p>
          </div>
          <div className="home-card">
            <div className="home-card-icon">☁️</div>
            <h3>云端同步</h3>
            <p>与 Memora Java 后端服务同步数据</p>
          </div>
          <div className="home-card">
            <div className="home-card-icon">🔍</div>
            <h3>本地搜索</h3>
            <p>快速搜索本地文件和文档</p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Home
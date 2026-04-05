import React, { useState, useEffect } from 'react'
import './FileExplorer.module.css'

const FileExplorer = () => {
  const [currentPath, setCurrentPath] = useState('/')
  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(false)

  const readDir = async (path) => {
    setLoading(true)
    try {
      const { invoke } = window.__TAURI__
      const result = await invoke('read_dir', { path })
      setFiles(result)
      setCurrentPath(path)
    } catch (error) {
      console.error('Error reading directory:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    readDir(currentPath)
  }, [])

  const handleFileClick = (file) => {
    if (file.is_dir) {
      readDir(file.path)
    }
  }

  const handleBackClick = () => {
    const parentPath = currentPath.split('/').slice(0, -1).join('/') || '/' 
    readDir(parentPath)
  }

  return (
    <div className="file-explorer">
      <div className="file-explorer-header">
        <div className="path-bar">
          <button onClick={handleBackClick} className="back-button">←</button>
          <span className="current-path">{currentPath}</span>
        </div>
      </div>
      <div className="file-explorer-content">
        {loading ? (
          <div className="loading">加载中...</div>
        ) : (
          <div className="file-list">
            {files.map((file, index) => (
              <div
                key={index}
                className={`file-item ${file.is_dir ? 'is-dir' : ''}`}
                onClick={() => handleFileClick(file)}
              >
                <span className="icon">{file.is_dir ? '📁' : '📄'}</span>
                <span className="name">{file.name}</span>
                {!file.is_dir && (
                  <>
                    <span className="size">{file.size} bytes</span>
                    <span className="modified">
                      {new Date(file.modified * 1000).toLocaleString()}
                    </span>
                  </>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default FileExplorer
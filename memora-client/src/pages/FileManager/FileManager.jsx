import React from 'react'
import FileExplorer from '../../components/FileExplorer/FileExplorer'
import './FileManager.module.css'

const FileManager = () => {
  return (
    <div className="file-manager">
      <div className="file-manager-header">
        <h2>文件管理</h2>
        <div className="file-manager-actions">
          <button>新建文件夹</button>
          <button>上传文件</button>
        </div>
      </div>
      <div className="file-manager-content">
        <FileExplorer />
      </div>
    </div>
  )
}

export default FileManager
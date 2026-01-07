import { useState, useEffect, useRef } from 'react'
import { Menu } from '@arco-design/web-react'
import {
  IconApps,
  IconSettings,
  IconImage,
  IconCode,
} from '@arco-design/web-react/icon'
import styles from './ContextMenu.module.css'

const ContextMenu = ({ position, onClose, onInsertDiagram, onInsertImage, onInsertCode }) => {
  const menuRef = useRef(null)

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        onClose()
      }
    }

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleEscape)

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [onClose])

  if (!position) return null

  const diagramItems = [
    {
      key: 'mermaid',
      icon: <IconApps />,
      label: '插入 Mermaid 流程图',
      onClick: () => {
        onInsertDiagram('mermaid')
        onClose()
      },
    },
    {
      key: 'excalidraw',
      icon: <IconSettings />,
      label: '插入 Excalidraw 图表',
      onClick: () => {
        onInsertDiagram('excalidraw')
        onClose()
      },
    },
  ]

  const otherItems = [
    {
      key: 'image',
      icon: <IconImage />,
      label: '插入图片',
      onClick: () => {
        onInsertImage()
        onClose()
      },
    },
    {
      key: 'code',
      icon: <IconCode />,
      label: '插入代码块',
      onClick: () => {
        onInsertCode()
        onClose()
      },
    },
  ]

  return (
    <div
      ref={menuRef}
      className={styles.contextMenu}
      style={{
        left: `${position.x}px`,
        top: `${position.y}px`,
      }}
    >
      <Menu
        mode="pop"
        selectedKeys={[]}
        onClickMenuItem={(key) => {
          const allItems = [...diagramItems, ...otherItems]
          const item = allItems.find((i) => i.key === key)
          if (item && item.onClick) {
            item.onClick()
          }
        }}
      >
        {diagramItems.map((item) => (
          <Menu.Item key={item.key} icon={item.icon}>
            {item.label}
          </Menu.Item>
        ))}
        <Menu.Item key="divider" disabled className={styles.dividerItem}>
          <div className={styles.divider} />
        </Menu.Item>
        {otherItems.map((item) => (
          <Menu.Item key={item.key} icon={item.icon}>
            {item.label}
          </Menu.Item>
        ))}
      </Menu>
    </div>
  )
}

export default ContextMenu


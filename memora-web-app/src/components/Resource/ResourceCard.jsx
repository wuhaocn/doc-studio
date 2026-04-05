import React from 'react'
import { Card, Tag, Space, Button, Tooltip, Avatar } from '@arco-design/web-react'
import { IconEdit, IconDelete, IconEye, IconDownload } from '@arco-design/web-react/icon'
import styles from './ResourceCard.module.css'

/**
 * 资源卡片组件
 * @param {Object} props
 * @param {Object} props.resource - 资源数据
 * @param {string} props.viewMode - 视图模式（card/list）
 * @param {Function} props.onClick - 点击回调
 * @param {Function} props.onEdit - 编辑回调
 * @param {Function} props.onDelete - 删除回调
 */
const ResourceCard = ({
  resource,
  viewMode = 'card',
  onClick,
  onEdit,
  onDelete,
}) => {
  if (!resource) return null

  const getTypeIcon = (type) => {
    switch (type) {
      case 'material':
        return '📁'
      case 'copywriting':
        return '📝'
      case 'prompt':
        return '💡'
      default:
        return '📄'
    }
  }

  const getTypeColor = (type) => {
    switch (type) {
      case 'material':
        return 'arcoblue'
      case 'copywriting':
        return 'purple'
      case 'prompt':
        return 'orange'
      default:
        return 'gray'
    }
  }

  const getTypeText = (type) => {
    switch (type) {
      case 'material':
        return '素材'
      case 'copywriting':
        return '文案'
      case 'prompt':
        return '提示词'
      default:
        return '其他'
    }
  }

  return viewMode === 'card' ? (
    <Card
      className={styles.card}
      cover={
        <div className={styles.cardCover}>
          <Avatar
            style={{
              fontSize: '24px',
              backgroundColor: '#f0f2f5',
            }}
          >
            {getTypeIcon(resource.type)}
          </Avatar>
        </div>
      }
      title={
        <div className={styles.cardTitle}>
          <span>{resource.name}</span>
          <Tag color={getTypeColor(resource.type)} size="small">
            {getTypeText(resource.type)}
          </Tag>
        </div>
      }
      extra={
        <Space size="mini">
          <Tooltip content="编辑">
            <Button
              icon={<IconEdit />}
              size="small"
              type="text"
              onClick={(e) => {
                e.stopPropagation()
                onEdit && onEdit()
              }}
            />
          </Tooltip>
          <Tooltip content="删除">
            <Button
              icon={<IconDelete />}
              size="small"
              type="text"
              status="danger"
              onClick={(e) => {
                e.stopPropagation()
                onDelete && onDelete()
              }}
            />
          </Tooltip>
        </Space>
      }
      onClick={onClick}
    >
      <div className={styles.cardContent}>
        <p className={styles.description}>
          {resource.description || '无描述'}
        </p>
        {resource.tags && resource.tags.length > 0 && (
          <div className={styles.tags}>
            {resource.tags.slice(0, 3).map((tag) => (
              <Tag key={tag.id} size="small" style={{ marginRight: 4, marginBottom: 4 }}>
                {tag.name}
              </Tag>
            ))}
            {resource.tags.length > 3 && (
              <Tag size="small" style={{ marginRight: 4, marginBottom: 4 }}>
                +{resource.tags.length - 3}
              </Tag>
            )}
          </div>
        )}
      </div>
      <div className={styles.cardFooter}>
        <Space size="mini">
          <Tooltip content="查看次数">
            <Space size="mini">
              <IconEye />
              <span>{resource.viewCount || 0}</span>
            </Space>
          </Tooltip>
          <Tooltip content="下载次数">
            <Space size="mini">
              <IconDownload />
              <span>{resource.downloadCount || 0}</span>
            </Space>
          </Tooltip>
        </Space>
        <span className={styles.time}>
          {new Date(resource.createdAt).toLocaleDateString()}
        </span>
      </div>
    </Card>
  ) : (
    <Card
      className={styles.listCard}
      hoverable
      bodyStyle={{ padding: 16 }}
      onClick={onClick}
    >
      <div className={styles.listContent}>
        <div className={styles.listLeft}>
          <div className={styles.listTitle}>
            <Avatar
              style={{
                fontSize: '16px',
                backgroundColor: '#f0f2f5',
                marginRight: 12,
              }}
            >
              {getTypeIcon(resource.type)}
            </Avatar>
            <div>
              <h4 style={{ margin: 0, marginBottom: 4 }}>{resource.name}</h4>
              <p style={{ margin: 0, fontSize: 12, color: '#86909C' }}>
                {resource.description || '无描述'}
              </p>
            </div>
          </div>
          {resource.tags && resource.tags.length > 0 && (
            <div className={styles.listTags}>
              {resource.tags.slice(0, 3).map((tag) => (
                <Tag key={tag.id} size="small" style={{ marginRight: 8, marginBottom: 4 }}>
                  {tag.name}
                </Tag>
              ))}
              {resource.tags.length > 3 && (
                <Tag size="small" style={{ marginRight: 8, marginBottom: 4 }}>
                  +{resource.tags.length - 3}
                </Tag>
              )}
            </div>
          )}
        </div>
        <div className={styles.listRight}>
          <div className={styles.listMeta}>
            <Tag color={getTypeColor(resource.type)} size="small" style={{ marginBottom: 8 }}>
              {getTypeText(resource.type)}
            </Tag>
            <Space size="mini" direction="vertical">
              <Space size="mini">
                <IconEye />
                <span>{resource.viewCount || 0}</span>
              </Space>
              <Space size="mini">
                <IconDownload />
                <span>{resource.downloadCount || 0}</span>
              </Space>
            </Space>
          </div>
          <Space size="mini">
            <Button
              icon={<IconEdit />}
              size="small"
              type="text"
              onClick={(e) => {
                e.stopPropagation()
                onEdit && onEdit()
              }}
            />
            <Button
              icon={<IconDelete />}
              size="small"
              type="text"
              status="danger"
              onClick={(e) => {
                e.stopPropagation()
                onDelete && onDelete()
              }}
            />
          </Space>
        </div>
      </div>
    </Card>
  )
}

export default ResourceCard

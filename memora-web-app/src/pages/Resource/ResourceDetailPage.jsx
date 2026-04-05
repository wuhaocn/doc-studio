import React, { useState, useEffect } from 'react'
import { Card, Typography, Tag, Space, Button, Avatar, Message, Empty, Divider } from '@arco-design/web-react'
import { IconEdit, IconDelete, IconEye, IconDownload, IconArrowLeft } from '@arco-design/web-react/icon'
import { useNavigate, useParams } from 'react-router-dom'
import { resourceApi } from '../../services/api/resourceApi'
import styles from './ResourceDetailPage.module.css'

/**
 * 资源详情页面
 */
const ResourceDetailPage = () => {
  const navigate = useNavigate()
  const { id } = useParams()

  // 状态管理
  const [resource, setResource] = useState(null)
  const [loading, setLoading] = useState(true)

  // 获取资源详情
  const fetchResourceDetail = async () => {
    try {
      setLoading(true)
      const response = await resourceApi.getResourceById(id)
      setResource(response.data.data)
    } catch (error) {
      console.error('获取资源详情失败:', error)
      Message.error('获取资源详情失败')
      navigate('/resource')
    } finally {
      setLoading(false)
    }
  }

  // 初始化数据
  useEffect(() => {
    fetchResourceDetail()
  }, [id])

  // 处理编辑资源
  const handleEditResource = () => {
    navigate(`/resource/edit/${id}`)
  }

  // 处理删除资源
  const handleDeleteResource = async () => {
    try {
      await resourceApi.deleteResource(id)
      Message.success('删除成功')
      navigate('/resource')
    } catch (error) {
      console.error('删除资源失败:', error)
      Message.error('删除资源失败')
    }
  }

  // 处理返回
  const handleReturn = () => {
    navigate('/resource')
  }

  if (loading) {
    return (
      <div className={styles.loadingContainer}>
        <Typography.Title heading={4}>加载中...</Typography.Title>
      </div>
    )
  }

  if (!resource) {
    return (
      <div className={styles.emptyContainer}>
        <Empty description="资源不存在" />
      </div>
    )
  }

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

  return (
    <div className={styles.container}>
      {/* 返回按钮 */}
      <Button
        icon={<IconArrowLeft />}
        onClick={handleReturn}
        className={styles.returnButton}
      >
        返回资源库
      </Button>

      {/* 资源详情卡片 */}
      <Card className={styles.detailCard}>
        <div className={styles.header}>
          <div className={styles.titleSection}>
            <Avatar
              style={{
                fontSize: '32px',
                backgroundColor: '#f0f2f5',
                marginRight: 16,
              }}
            >
              {getTypeIcon(resource.type)}
            </Avatar>
            <div>
              <Typography.Title heading={3}>
                {resource.name}
              </Typography.Title>
              <Space size="medium">
                <Tag color={getTypeColor(resource.type)}>
                  {getTypeText(resource.type)}
                </Tag>
                <Tag color={resource.isPublic ? 'green' : 'gray'}>
                  {resource.isPublic ? '公开' : '私有'}
                </Tag>
                <Space size="mini">
                  <IconEye />
                  <span>{resource.viewCount || 0} 次查看</span>
                </Space>
                <Space size="mini">
                  <IconDownload />
                  <span>{resource.downloadCount || 0} 次下载</span>
                </Space>
              </Space>
            </div>
          </div>
          <Space size="medium">
            <Button
              type="primary"
              icon={<IconEdit />}
              onClick={handleEditResource}
            >
              编辑
            </Button>
            <Button
              icon={<IconDelete />}
              status="danger"
              onClick={handleDeleteResource}
            >
              删除
            </Button>
          </Space>
        </div>

        <Divider />

        {/* 资源描述 */}
        <div className={styles.section}>
          <Typography.Title heading={5}>
            资源描述
          </Typography.Title>
          <Typography.Paragraph>
            {resource.description || '无描述'}
          </Typography.Paragraph>
        </div>

        {/* 资源内容 */}
        <div className={styles.section}>
          <Typography.Title heading={5}>
            资源内容
          </Typography.Title>
          {resource.content ? (
            <div className={styles.content}>
              <Typography.Paragraph>
                {resource.content}
              </Typography.Paragraph>
            </div>
          ) : (
            <Typography.Paragraph>
              无内容
            </Typography.Paragraph>
          )}
        </div>

        {/* 资源文件信息 */}
        {resource.contentUrl && (
          <div className={styles.section}>
            <Typography.Title heading={5}>
              文件信息
            </Typography.Title>
            <Space size="medium">
              <Typography.Link href={resource.contentUrl} target="_blank">
                {resource.contentUrl}
              </Typography.Link>
              {resource.contentType && (
                <Tag>
                  {resource.contentType}
                </Tag>
              )}
            </Space>
          </div>
        )}

        {/* 标签 */}
        {resource.tags && resource.tags.length > 0 && (
          <div className={styles.section}>
            <Typography.Title heading={5}>
              标签
            </Typography.Title>
            <Space wrap>
              {resource.tags.map((tag) => (
                <Tag key={tag.id}>{tag.name}</Tag>
              ))}
            </Space>
          </div>
        )}

        {/* 元数据 */}
        <div className={styles.section}>
          <Typography.Title heading={5}>
            元数据
          </Typography.Title>
          <Space direction="vertical" size="large">
            <Space size="medium">
              <span>创建时间：</span>
              <span>{new Date(resource.createdAt).toLocaleString()}</span>
            </Space>
            <Space size="medium">
              <span>更新时间：</span>
              <span>{new Date(resource.updatedAt).toLocaleString()}</span>
            </Space>
            <Space size="medium">
              <span>创建者：</span>
              <span>用户 ID {resource.userId}</span>
            </Space>
          </Space>
        </div>
      </Card>
    </div>
  )
}

export default ResourceDetailPage

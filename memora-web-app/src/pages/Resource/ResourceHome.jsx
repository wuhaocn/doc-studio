import React, { useState, useEffect } from 'react'
import { Button, Input, Select, Space, Card, Typography, Switch, Message, Empty } from '@arco-design/web-react'
import { IconSearch, IconList, IconLayout, IconPlus, IconRefresh } from '@arco-design/web-react/icon'
import { useNavigate } from 'react-router-dom'
import ResourceList from '../../components/Resource/ResourceList'
import { resourceApi } from '../../services/api/resourceApi'
import { tagApi } from '../../services/api/tagApi'
import styles from './ResourceHome.module.css'

/**
 * 资源库首页
 */
const ResourceHome = () => {
  const navigate = useNavigate()
  
  // 状态管理
  const [resources, setResources] = useState([])
  const [allTags, setAllTags] = useState([])
  const [loading, setLoading] = useState(true)
  const [viewMode, setViewMode] = useState('card') // card 或 list
  
  // 搜索和筛选条件
  const [keyword, setKeyword] = useState('')
  const [type, setType] = useState('')
  const [tagId, setTagId] = useState('')
  
  // 分页参数
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(20)
  const [total, setTotal] = useState(0)

  // 获取资源列表
  const fetchResources = async () => {
    try {
      setLoading(true)
      const response = await resourceApi.getResources({
        page,
        size,
        keyword,
        type,
        tagId: tagId || undefined,
      })
      setResources(response.data.data.records || [])
      setTotal(response.data.data.total || 0)
    } catch (error) {
      console.error('获取资源列表失败:', error)
      Message.error('获取资源列表失败')
    } finally {
      setLoading(false)
    }
  }

  // 获取所有标签
  const fetchAllTags = async () => {
    try {
      const response = await tagApi.getTags()
      setAllTags(response.data.data || [])
    } catch (error) {
      console.error('获取标签列表失败:', error)
    }
  }

  // 初始化数据
  useEffect(() => {
    fetchAllTags()
    fetchResources()
  }, [page, size, keyword, type, tagId])

  // 处理搜索
  const handleSearch = () => {
    setPage(1)
    fetchResources()
  }

  // 处理筛选条件变更
  const handleFilterChange = () => {
    setPage(1)
    fetchResources()
  }

  // 处理刷新
  const handleRefresh = () => {
    fetchResources()
  }

  // 处理创建资源
  const handleCreateResource = () => {
    navigate('/resource/new')
  }

  // 处理编辑资源
  const handleEditResource = (resource) => {
    navigate(`/resource/edit/${resource.id}`)
  }

  // 处理删除资源
  const handleDeleteResource = async (resource) => {
    try {
      await resourceApi.deleteResource(resource.id)
      Message.success('删除成功')
      fetchResources()
    } catch (error) {
      console.error('删除资源失败:', error)
      Message.error('删除资源失败')
    }
  }

  // 处理查看资源详情
  const handleViewResource = (resource) => {
    navigate(`/resource/${resource.id}`)
  }

  return (
    <div className={styles.container}>
      {/* 页面标题 */}
      <div className={styles.header}>
        <Typography.Title heading={4}>
          资源库
        </Typography.Title>
        <Button
          type="primary"
          icon={<IconPlus />}
          onClick={handleCreateResource}
        >
          新建资源
        </Button>
      </div>

      {/* 搜索和筛选 */}
      <Card className={styles.filterCard}>
        <Space size="medium" className={styles.filterForm}>
          <Input
            placeholder="搜索资源名称或描述"
            prefix={<IconSearch />}
            value={keyword}
            onChange={setKeyword}
            onPressEnter={handleSearch}
            style={{ width: 300 }}
          />
          <Button
            icon={<IconSearch />}
            onClick={handleSearch}
          >
            搜索
          </Button>
          <Button
            icon={<IconRefresh />}
            onClick={handleRefresh}
          >
            刷新
          </Button>
          <Select
            placeholder="资源类型"
            value={type}
            onChange={(value) => {
              setType(value)
              handleFilterChange()
            }}
            style={{ width: 150 }}
          >
            <Select.Option value="">全部类型</Select.Option>
            <Select.Option value="material">素材</Select.Option>
            <Select.Option value="copywriting">文案</Select.Option>
            <Select.Option value="prompt">提示词</Select.Option>
          </Select>
          <Select
            placeholder="标签"
            value={tagId}
            onChange={(value) => {
              setTagId(value)
              handleFilterChange()
            }}
            style={{ width: 150 }}
          >
            <Select.Option value="">全部标签</Select.Option>
            {allTags.map((tag) => (
              <Select.Option key={tag.id} value={tag.id}>
                {tag.name}
              </Select.Option>
            ))}
          </Select>
          <div className={styles.viewMode}>
            <Space size="mini">
              <Button
                icon={<IconLayout />}
                type={viewMode === 'card' ? 'primary' : 'default'}
                size="small"
                onClick={() => setViewMode('card')}
              >
                卡片
              </Button>
              <Button
                icon={<IconList />}
                type={viewMode === 'list' ? 'primary' : 'default'}
                size="small"
                onClick={() => setViewMode('list')}
              >
                列表
              </Button>
            </Space>
          </div>
        </Space>
      </Card>

      {/* 资源列表 */}
      <div className={styles.content}>
        <ResourceList
          resources={resources}
          viewMode={viewMode}
          loading={loading}
          onEdit={handleEditResource}
          onDelete={handleDeleteResource}
          onSelect={handleViewResource}
        />
      </div>

      {/* 分页 */}
      {total > 0 && (
        <div className={styles.pagination}>
          {/* 这里可以添加分页组件 */}
          <Typography.Text>
            共 {total} 条记录，当前第 {page} 页
          </Typography.Text>
        </div>
      )}
    </div>
  )
}

export default ResourceHome

import React, { useState, useEffect } from 'react'
import { Card, Typography, Message } from '@arco-design/web-react'
import { useNavigate, useParams } from 'react-router-dom'
import ResourceForm from '../../components/Resource/ResourceForm'
import { resourceApi } from '../../services/api/resourceApi'
import { tagApi } from '../../services/api/tagApi'
import styles from './ResourceEdit.module.css'

/**
 * 资源编辑页面
 */
const ResourceEdit = () => {
  const navigate = useNavigate()
  const { id } = useParams()
  const isEditMode = !!id

  // 状态管理
  const [resource, setResource] = useState(null)
  const [allTags, setAllTags] = useState([])
  const [loading, setLoading] = useState(true)

  // 获取所有标签
  const fetchAllTags = async () => {
    try {
      const response = await tagApi.getTags()
      setAllTags(response.data.data || [])
    } catch (error) {
      console.error('获取标签列表失败:', error)
      Message.error('获取标签列表失败')
    }
  }

  // 获取资源详情（编辑模式）
  const fetchResourceDetail = async () => {
    try {
      const response = await resourceApi.getResourceById(id)
      setResource(response.data.data)
    } catch (error) {
      console.error('获取资源详情失败:', error)
      Message.error('获取资源详情失败')
      navigate('/resource')
    }
  }

  // 初始化数据
  useEffect(() => {
    fetchAllTags()
    if (isEditMode) {
      fetchResourceDetail()
    } else {
      setLoading(false)
    }
  }, [isEditMode, id])

  // 处理表单提交
  const handleSubmit = async (values) => {
    try {
      if (isEditMode) {
        // 更新资源
        await resourceApi.updateResource(id, values)
        Message.success('资源更新成功')
      } else {
        // 创建资源
        await resourceApi.createResource(values)
        Message.success('资源创建成功')
      }
      navigate('/resource')
    } catch (error) {
      console.error('保存资源失败:', error)
      Message.error('保存资源失败')
    }
  }

  // 处理取消
  const handleCancel = () => {
    navigate('/resource')
  }

  return (
    <div className={styles.container}>
      <Card className={styles.card}>
        <Typography.Title heading={4}>
          {isEditMode ? '编辑资源' : '创建资源'}
        </Typography.Title>
        <ResourceForm
          resource={resource}
          onSubmit={handleSubmit}
          onCancel={handleCancel}
          allTags={allTags}
        />
      </Card>
    </div>
  )
}

export default ResourceEdit

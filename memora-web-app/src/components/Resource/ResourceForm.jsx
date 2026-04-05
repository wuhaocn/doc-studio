import React, { useState, useEffect } from 'react'
import { Form, Input, Select, Switch, Button, Space, Message, Card, Tabs, Typography } from '@arco-design/web-react'
import TagManager from './TagManager'
import styles from './ResourceForm.module.css'

/**
 * 资源表单组件
 * @param {Object} props
 * @param {Object} props.resource - 资源数据（编辑模式）
 * @param {Function} props.onSubmit - 提交回调
 * @param {Function} props.onCancel - 取消回调
 * @param {Array} props.allTags - 所有可用标签
 */
const ResourceForm = ({
  resource = null,
  onSubmit,
  onCancel,
  allTags = [],
}) => {
  const [form] = Form.useForm()
  const [activeTab, setActiveTab] = useState('basic')

  // 初始化表单数据
  useEffect(() => {
    if (resource) {
      form.setFieldsValue({
        name: resource.name,
        description: resource.description,
        type: resource.type,
        isPublic: resource.isPublic,
        content: resource.content,
        contentUrl: resource.contentUrl,
        contentType: resource.contentType,
        tags: resource.tags?.map((tag) => tag.id) || [],
      })
    } else {
      form.resetFields()
    }
  }, [resource, form])

  const handleSubmit = async () => {
    try {
      const values = await form.validate()
      onSubmit && onSubmit({
        ...values,
        tags: values.tags || [],
      })
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }

  const handleCancel = () => {
    onCancel && onCancel()
  }

  return (
    <div className={styles.container}>
      <Tabs activeTab={activeTab} onChange={setActiveTab}>
        <Tabs.TabPane key="basic" title="基本信息">
          <Form
            form={form}
            layout="vertical"
            className={styles.form}
          >
            <Form.Item
              label="资源名称"
              field="name"
              rules={[{ required: true, message: '请输入资源名称' }]}
            >
              <Input placeholder="请输入资源名称" />
            </Form.Item>

            <Form.Item
              label="资源类型"
              field="type"
              rules={[{ required: true, message: '请选择资源类型' }]}
            >
              <Select placeholder="请选择资源类型">
                <Select.Option value="material">素材</Select.Option>
                <Select.Option value="copywriting">文案</Select.Option>
                <Select.Option value="prompt">提示词</Select.Option>
              </Select>
            </Form.Item>

            <Form.Item
              label="资源描述"
              field="description"
            >
              <Input.TextArea
                placeholder="请输入资源描述"
                rows={3}
                maxLength={500}
                showCount
              />
            </Form.Item>

            <Form.Item
              label="是否公开"
              field="isPublic"
            >
              <Switch />
            </Form.Item>

            <Form.Item
              label="标签"
              field="tags"
            >
              <TagManager
                selectedTags={form.getFieldValue('tags') || []}
                allTags={allTags}
                onTagsChange={(tags) => {
                  form.setFieldValue('tags', tags)
                }}
              />
            </Form.Item>
          </Form>
        </Tabs.TabPane>

        <Tabs.TabPane key="content" title="内容设置">
          <Form
            form={form}
            layout="vertical"
            className={styles.form}
          >
            <Form.Item
              label="资源内容"
              field="content"
            >
              <Input.TextArea
                placeholder="请输入资源内容"
                rows={6}
              />
            </Form.Item>

            <Form.Item
              label="资源文件URL"
              field="contentUrl"
            >
              <Input placeholder="请输入资源文件URL（用于素材类型）" />
            </Form.Item>

            <Form.Item
              label="内容类型"
              field="contentType"
            >
              <Input placeholder="请输入内容类型（MIME类型）" />
            </Form.Item>
          </Form>
        </Tabs.TabPane>
      </Tabs>

      <div className={styles.actions}>
        <Space size="medium">
          <Button
            type="primary"
            onClick={handleSubmit}
          >
            保存
          </Button>
          <Button
            onClick={handleCancel}
          >
            取消
          </Button>
        </Space>
      </div>
    </div>
  )
}

export default ResourceForm

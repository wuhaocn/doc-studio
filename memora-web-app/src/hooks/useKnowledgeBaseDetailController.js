import { useCallback, useDeferredValue, useEffect, useMemo, useState } from 'react'
import { useConfirm } from '../components/Feedback/ConfirmDialog'
import { useToast } from '../components/Feedback/Toast'
import { documentApi } from '../services/api/documentApi'
import { knowledgeBaseApi } from '../services/api/knowledgeBaseApi'
import { workspaceApi } from '../services/api/workspaceApi'
import { DOCUMENT_FORMATS, getDocumentRenderState } from '../utils/documentContent'
import { emitKnowledgeBasesChanged } from '../utils/knowledgeBaseEvents'
import { rememberKnowledgeBase } from '../utils/knowledgeBaseRoute'
import {
  TYPE_LABELS,
  buildBatchFolderOptions,
  buildFolderOptions,
  buildInitialExpandedFolderIds,
  getDocumentByIdMap,
  getSiblingDocuments,
  getTopLevelSelectedDocuments,
  isTreeItemVisible,
  reorderSiblingDocuments,
  resolveBatchMoveInitialParentId,
  resolveDefaultParentId,
  validateBatchDeleteSelection,
} from '../utils/knowledgeBaseTree'

const escapeHtml = (value) =>
  value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')

const buildInitialDocumentContent = (title, format) => {
  if (format === DOCUMENT_FORMATS.MARKDOWN) {
    return `# ${title}\n\n`
  }
  if (format === DOCUMENT_FORMATS.HTML) {
    return `<article>\n  <h1>${escapeHtml(title)}</h1>\n  <p></p>\n</article>`
  }
  return `<h1>${escapeHtml(title)}</h1><p></p>`
}

export const PAGE_STATUS = {
  LOADING: 'loading',
  READY: 'ready',
  FORBIDDEN: 'forbidden',
  NOT_FOUND: 'not_found',
  ERROR: 'error',
}

export const useKnowledgeBaseDetailController = ({ id, currentUser, navigate, locationState }) => {
  const confirm = useConfirm()
  const toast = useToast()
  const [pageStatus, setPageStatus] = useState(PAGE_STATUS.LOADING)
  const [pageErrorMessage, setPageErrorMessage] = useState('')
  const [knowledgeBase, setKnowledgeBase] = useState(null)
  const [documents, setDocuments] = useState([])
  const [selectedDocumentId, setSelectedDocumentId] = useState(null)
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [modalError, setModalError] = useState('')
  const [documentModalOpen, setDocumentModalOpen] = useState(false)
  const [documentModalMode, setDocumentModalMode] = useState('create')
  const [documentModalType, setDocumentModalType] = useState('DOC')
  const [documentModalInitialValues, setDocumentModalInitialValues] = useState(null)
  const [documentModalError, setDocumentModalError] = useState('')
  const [documentSubmitting, setDocumentSubmitting] = useState(false)
  const [batchMode, setBatchMode] = useState(false)
  const [selectedDocumentIds, setSelectedDocumentIds] = useState([])
  const [batchMoveOpen, setBatchMoveOpen] = useState(false)
  const [batchMoveSubmitting, setBatchMoveSubmitting] = useState(false)
  const [batchMoveError, setBatchMoveError] = useState('')
  const [batchDeleting, setBatchDeleting] = useState(false)
  const [draggingDocumentId, setDraggingDocumentId] = useState(null)
  const [dragOverDocumentId, setDragOverDocumentId] = useState(null)
  const [dragOverPosition, setDragOverPosition] = useState('before')
  const [dragSorting, setDragSorting] = useState(false)
  const [expandedFolderIds, setExpandedFolderIds] = useState([])
  const [treePanelCollapsed, setTreePanelCollapsed] = useState(false)
  const [focusMode, setFocusMode] = useState(false)
  const [knowledgeBaseInfoVisible, setKnowledgeBaseInfoVisible] = useState(false)
  const [siteSubmitting, setSiteSubmitting] = useState(false)
  const [siteError, setSiteError] = useState('')
  const [documentPublishingSubmitting, setDocumentPublishingSubmitting] = useState(false)
  const [documentPublishingError, setDocumentPublishingError] = useState('')
  const [readLinkOpen, setReadLinkOpen] = useState(false)
  const [permissionModalOpen, setPermissionModalOpen] = useState(false)
  const [permissionMembers, setPermissionMembers] = useState([])
  const [permissionAssignments, setPermissionAssignments] = useState([])
  const [permissionLoading, setPermissionLoading] = useState(false)
  const [permissionSubmitting, setPermissionSubmitting] = useState(false)
  const [permissionError, setPermissionError] = useState('')
  const [documentTrashOpen, setDocumentTrashOpen] = useState(false)
  const [deletedDocuments, setDeletedDocuments] = useState([])
  const [documentTrashLoading, setDocumentTrashLoading] = useState(false)
  const [documentTrashError, setDocumentTrashError] = useState('')
  const [restoringDocumentId, setRestoringDocumentId] = useState(null)
  const deferredSearch = useDeferredValue(search)

  const loadData = useCallback(async ({ resetState = false } = {}) => {
    try {
      setPageErrorMessage('')
      if (resetState) {
        setPageStatus(PAGE_STATUS.LOADING)
        setKnowledgeBase(null)
        setDocuments([])
        setSelectedDocumentId(null)
      }

      const knowledgeBaseResponse = await knowledgeBaseApi.getKnowledgeBaseById(id)

      if (knowledgeBaseResponse.code === 200) {
        setKnowledgeBase(knowledgeBaseResponse.data)
      }

      const [documentTreeResult] = await Promise.allSettled([
        documentApi.getDocumentTreeByKnowledgeBaseId(id),
      ])

      if (documentTreeResult.status === 'fulfilled' && documentTreeResult.value.code === 200) {
        setDocuments(documentTreeResult.value.data || [])
      }

      setPageStatus(PAGE_STATUS.READY)
    } catch (error) {
      console.error('加载知识库详情失败', error)
      setKnowledgeBase(null)
      setDocuments([])
      setSelectedDocumentId(null)

      if (error?.code === 403) {
        setPageStatus(PAGE_STATUS.FORBIDDEN)
        setPageErrorMessage(error?.message || '当前会话没有访问该知识库的权限')
        return
      }

      if (error?.code === 404) {
        setPageStatus(PAGE_STATUS.NOT_FOUND)
        setPageErrorMessage(error?.message || '当前知识库不存在或已删除')
        return
      }

      setPageStatus(PAGE_STATUS.ERROR)
      setPageErrorMessage(error?.message || '加载知识库详情失败，请稍后重试')
    }
  }, [id])

  useEffect(() => {
    loadData({ resetState: true })
  }, [loadData, currentUser.id, currentUser.tenantId])

  useEffect(() => {
    rememberKnowledgeBase(id)
  }, [id])

  useEffect(() => {
    if (documents.length === 0) {
      setSelectedDocumentId(null)
      return
    }

    const preferredDocumentId = Number(locationState?.selectedDocumentId || 0)
    if (preferredDocumentId && documents.some((item) => item.id === preferredDocumentId)) {
      setSelectedDocumentId(preferredDocumentId)
      return
    }

    setSelectedDocumentId((current) => {
      if (current && documents.some((item) => item.id === current)) {
        return current
      }
      const firstDoc = documents.find((item) => item.docType === 'DOC') || documents[0]
      return firstDoc?.id || null
    })
  }, [documents, locationState])

  useEffect(() => {
    const folderIds = new Set(documents.filter((item) => item.docType === 'FOLDER').map((item) => item.id))
    const initialExpandedIds = buildInitialExpandedFolderIds(documents, selectedDocumentId)

    setExpandedFolderIds((current) => {
      const nextIds = new Set(initialExpandedIds)
      current.forEach((folderId) => {
        if (folderIds.has(folderId)) {
          nextIds.add(folderId)
        }
      })
      return Array.from(nextIds)
    })
  }, [documents, selectedDocumentId])

  const documentMap = getDocumentByIdMap(documents)
  const expandedFolderIdSet = new Set(expandedFolderIds)
  const visibleDocuments = documents.filter((item) => {
    if (!deferredSearch.trim()) {
      return isTreeItemVisible(item, documentMap, expandedFolderIdSet)
    }
    const searchValue = deferredSearch.toLowerCase()
    return item.title.toLowerCase().includes(searchValue)
  })

  const selectedDocument = documents.find((item) => item.id === selectedDocumentId) || visibleDocuments[0] || documents[0]
  const selectedDocumentRenderState = useMemo(() => {
    return getDocumentRenderState({
      format: selectedDocument?.format,
      content: selectedDocument?.content,
      contentText: selectedDocument?.contentText,
      renderedHtml: selectedDocument?.renderedHtml,
    })
  }, [selectedDocument?.format, selectedDocument?.content, selectedDocument?.contentText, selectedDocument?.renderedHtml])
  const safeSelectedDocumentContent = selectedDocumentRenderState.html
  const siblingDocuments = getSiblingDocuments(documents, selectedDocument)
  const selectedSiblingIndex = siblingDocuments.findIndex((item) => item.id === selectedDocument?.id)
  const canMoveUp = selectedSiblingIndex > 0
  const canMoveDown = selectedSiblingIndex >= 0 && selectedSiblingIndex < siblingDocuments.length - 1
  const documentModalFolderOptions = buildFolderOptions(documents, documentModalMode === 'edit' ? selectedDocument : null)
  const selectedDocumentIdSet = new Set(selectedDocumentIds)
  const topLevelSelectedDocuments = getTopLevelSelectedDocuments(documents, selectedDocumentIds)
  const batchFolderOptions = buildBatchFolderOptions(documents, topLevelSelectedDocuments)
  const batchMoveInitialParentId = resolveBatchMoveInitialParentId(topLevelSelectedDocuments)
  const canWriteKnowledgeBase = !!knowledgeBase?.canWrite
  const canManageKnowledgeBase = !!knowledgeBase?.canManage
  const dragSortEnabled = canWriteKnowledgeBase && !batchMode && !search.trim()
  const shouldRenderRichPreview = selectedDocument?.docType === 'DOC' && selectedDocumentRenderState.hasRenderedContent
  const compactKnowledgeBaseDescription = knowledgeBase?.description?.trim()
  const hasActiveSearch = !!deferredSearch.trim()
  const treePanelStatusText = hasActiveSearch
    ? `找到 ${visibleDocuments.length} 项`
    : batchMode
      ? `已选 ${selectedDocumentIds.length} 项`
      : `${documents.length} 个节点`
  const treeHintText = batchMode
    ? '选择文档或目录后，再移动或删除。'
    : !canWriteKnowledgeBase
      ? '当前角色只能阅读和搜索，不能新建、移动或删除节点。'
    : dragSortEnabled
      ? '支持右键操作；同级可直接拖拽排序，拖到目录中部可移入该目录。'
      : hasActiveSearch
        ? '按标题过滤，层级关系保持不变。'
        : '继续像文档目录一样浏览即可。'
  const hasFolderNodes = documents.some((item) => item.docType === 'FOLDER')
  const selectedFolderChildren = selectedDocument
    ? documents.filter((item) => (item.parentId ?? 0) === selectedDocument.id)
    : []
  const selectedFolderDocumentCount = selectedFolderChildren.filter((item) => item.docType === 'DOC').length
  const selectedFolderDirectoryCount = selectedFolderChildren.filter((item) => item.docType === 'FOLDER').length

  useEffect(() => {
    if (selectedDocument?.docType !== 'DOC') {
      setFocusMode(false)
      setReadLinkOpen(false)
    }
    setDocumentPublishingError('')
  }, [selectedDocument?.docType])

  useEffect(() => {
    if (selectedDocumentIds.length === 0) {
      return
    }

    const documentIds = new Set(documents.map((item) => item.id))
    setSelectedDocumentIds((current) => current.filter((itemId) => documentIds.has(itemId)))
  }, [documents, selectedDocumentIds.length])

  const handleSaveKnowledgeBase = async (formData) => {
    if (!canManageKnowledgeBase) {
      return
    }

    try {
      setSubmitting(true)
      setModalError('')
      await knowledgeBaseApi.updateKnowledgeBase(id, formData)
      setEditing(false)
      await loadData()
      emitKnowledgeBasesChanged()
      toast.success(`知识库”${formData.name}”已更新`)
    } catch (error) {
      console.error('更新知识库失败', error)
      setModalError(error?.message || '更新知识库失败，请稍后重试')
    } finally {
      setSubmitting(false)
    }
  }

  const handleSaveSiteSettings = async (siteForm) => {
    if (!canManageKnowledgeBase) {
      return
    }

    try {
      setSiteSubmitting(true)
      setSiteError('')
      await knowledgeBaseApi.updateKnowledgeBaseSite(id, siteForm)
      await loadData()
      emitKnowledgeBasesChanged()
      toast.success(siteForm.siteEnabled ? '公开站点设置已保存' : '公开站点已停用')
    } catch (error) {
      console.error('保存公开站点设置失败', error)
      setSiteError(error?.message || '保存公开站点设置失败，请稍后重试')
    } finally {
      setSiteSubmitting(false)
    }
  }

  const handlePublishDocument = async ({ publicSlug } = {}) => {
    if (!canManageKnowledgeBase || selectedDocument?.docType !== 'DOC') {
      return
    }

    try {
      setDocumentPublishingSubmitting(true)
      setDocumentPublishingError('')
      await documentApi.publishDocument(selectedDocument.id, {
        publicSlug: publicSlug || undefined,
      })
      await loadData()
      setSelectedDocumentId(selectedDocument.id)
      toast.success('文档已进入正式发布')
    } catch (error) {
      console.error('发布文档失败', error)
      setDocumentPublishingError(error?.message || '发布文档失败，请稍后重试')
    } finally {
      setDocumentPublishingSubmitting(false)
    }
  }

  const handleUnpublishDocument = async () => {
    if (!canManageKnowledgeBase || selectedDocument?.docType !== 'DOC') {
      return
    }

    try {
      setDocumentPublishingSubmitting(true)
      setDocumentPublishingError('')
      await documentApi.unpublishDocument(selectedDocument.id)
      await loadData()
      setSelectedDocumentId(selectedDocument.id)
      toast.success('文档已取消正式发布')
    } catch (error) {
      console.error('取消发布文档失败', error)
      setDocumentPublishingError(error?.message || '取消发布失败，请稍后重试')
    } finally {
      setDocumentPublishingSubmitting(false)
    }
  }

  const openPermissionModal = async () => {
    if (!canManageKnowledgeBase) {
      return
    }

    try {
      setPermissionModalOpen(true)
      setPermissionLoading(true)
      setPermissionError('')
      const [dashboardResponse, memberResponse] = await Promise.all([
        workspaceApi.getCurrentDashboard(),
        knowledgeBaseApi.getKnowledgeBaseMembers(id),
      ])

      setPermissionMembers(dashboardResponse.code === 200 ? (dashboardResponse.data?.members || []) : [])
      setPermissionAssignments(memberResponse.code === 200 ? (memberResponse.data || []) : [])
    } catch (error) {
      console.error('加载知识库权限配置失败', error)
      setPermissionError(error?.message || '加载知识库权限配置失败，请稍后重试')
    } finally {
      setPermissionLoading(false)
    }
  }

  const loadDeletedDocuments = useCallback(async () => {
    try {
      setDocumentTrashLoading(true)
      setDocumentTrashError('')
      const response = await documentApi.getDeletedDocuments({
        knowledgeBaseId: Number(id),
      })
      if (response.code === 200) {
        setDeletedDocuments(response.data || [])
      }
    } catch (error) {
      console.error('加载文档回收站失败', error)
      setDocumentTrashError(error?.message || '加载文档回收站失败，请稍后重试')
    } finally {
      setDocumentTrashLoading(false)
    }
  }, [id])

  const openDocumentTrash = async () => {
    if (!canWriteKnowledgeBase) {
      return
    }

    setDocumentTrashOpen(true)
    await loadDeletedDocuments()
  }

  const handleRestoreDocument = async (documentId) => {
    try {
      setRestoringDocumentId(documentId)
      setDocumentTrashError('')
      await documentApi.restoreDocument(documentId)
      await Promise.all([
        loadData(),
        loadDeletedDocuments(),
      ])
      setSelectedDocumentId(documentId)
      toast.success('文档已从回收站恢复')
    } catch (error) {
      console.error('恢复文档失败', error)
      setDocumentTrashError(error?.message || '恢复文档失败，请稍后重试')
    } finally {
      setRestoringDocumentId(null)
    }
  }

  const handleSubmitPermissions = async (members) => {
    try {
      setPermissionSubmitting(true)
      setPermissionError('')
      const response = await knowledgeBaseApi.updateKnowledgeBaseMembers(id, members)
      if (response.code === 200) {
        setPermissionAssignments(response.data || [])
      }
      setPermissionModalOpen(false)
      await loadData()
      emitKnowledgeBasesChanged()
      toast.success(members.length > 0 ? '知识库独立权限已更新' : '知识库已恢复继承租户权限')
    } catch (error) {
      console.error('保存知识库权限配置失败', error)
      setPermissionError(error?.message || '保存知识库权限配置失败，请稍后重试')
    } finally {
      setPermissionSubmitting(false)
    }
  }

  const toggleDocumentSelection = (documentId) => {
    setSelectedDocumentIds((current) =>
      current.includes(documentId)
        ? current.filter((item) => item !== documentId)
        : [...current, documentId]
    )
  }

  const clearBatchSelection = () => {
    setSelectedDocumentIds([])
    setBatchMode(false)
    setBatchMoveOpen(false)
    setBatchMoveError('')
  }

  const handleToggleBatchMode = () => {
    if (batchMode) {
      clearBatchSelection()
      return
    }

    setBatchMode(true)
    setBatchMoveError('')
  }

  const clearDragState = () => {
    setDraggingDocumentId(null)
    setDragOverDocumentId(null)
    setDragOverPosition('before')
  }

  const toggleFolderExpanded = (folderId, event) => {
    event.stopPropagation()
    setExpandedFolderIds((current) =>
      current.includes(folderId) ? current.filter((itemId) => itemId !== folderId) : [...current, folderId]
    )
  }

  const expandAllFolders = () => {
    setExpandedFolderIds(documents.filter((item) => item.docType === 'FOLDER').map((item) => item.id))
  }

  const collapseToTopLevelFolders = () => {
    setExpandedFolderIds(buildInitialExpandedFolderIds(documents, selectedDocumentId))
  }

  const handleTreeItemKeyDown = (itemId, event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      if (batchMode) {
        toggleDocumentSelection(itemId)
        return
      }

      setSelectedDocumentId(itemId)
    }
  }

  const navigateToEditor = (documentId = selectedDocument?.id) => {
    if (!documentId) {
      return
    }

    navigate(`/docs/${documentId}/edit`)
  }

  const handleOpenEditorPage = (targetDocument = selectedDocument) => {
    if (!targetDocument || targetDocument.docType !== 'DOC' || !canWriteKnowledgeBase) {
      return
    }
    navigateToEditor(targetDocument.id)
  }

  const handleToggleFocusMode = () => {
    if (!selectedDocument || selectedDocument.docType !== 'DOC') {
      return
    }

    setFocusMode((current) => !current)
  }

  const openCreateDocumentModal = (docType, targetDocument = selectedDocument) => {
    if (!canWriteKnowledgeBase) {
      return
    }

    setDocumentModalMode('create')
    setDocumentModalType(docType)
    setDocumentModalError('')
    setDocumentModalInitialValues({
      title: '',
      summary: '',
      parentId: resolveDefaultParentId(targetDocument),
      format: DOCUMENT_FORMATS.RICH_TEXT,
    })
    setDocumentModalOpen(true)
  }

  const openEditDocumentModal = (targetDocument = selectedDocument) => {
    if (!targetDocument || !canWriteKnowledgeBase) {
      return
    }

    setSelectedDocumentId(targetDocument.id)
    setDocumentModalMode('edit')
    setDocumentModalType(targetDocument.docType)
    setDocumentModalError('')
    setDocumentModalInitialValues({
      title: targetDocument.title,
      summary: targetDocument.summary || '',
      parentId: targetDocument.parentId ?? 0,
      format: targetDocument.format || DOCUMENT_FORMATS.RICH_TEXT,
    })
    setDocumentModalOpen(true)
  }

  const handleSubmitDocument = async (formData) => {
    if (!canWriteKnowledgeBase) {
      return
    }

    try {
      setDocumentSubmitting(true)
      setDocumentModalError('')

      if (documentModalMode === 'create') {
        const documentFormat = formData.format || DOCUMENT_FORMATS.RICH_TEXT
        const response = await documentApi.createDocument({
          knowledgeBaseId: Number(id),
          parentId: formData.parentId,
          title: formData.title,
          docType: documentModalType,
          format: documentModalType === 'DOC' ? documentFormat : undefined,
          summary: documentModalType === 'DOC' ? formData.summary : undefined,
          content: documentModalType === 'DOC' ? buildInitialDocumentContent(formData.title, documentFormat) : undefined,
        })
        setDocumentModalOpen(false)
        await loadData()
        const createdDocumentId = response?.data?.id || selectedDocumentId
        setSelectedDocumentId(createdDocumentId)
        if (documentModalType === 'DOC') {
          navigateToEditor(createdDocumentId)
        }
        toast.success(`${TYPE_LABELS[documentModalType] || documentModalType}”${formData.title}”已创建`)
        return
      }

      if (!selectedDocument) {
        return
      }

      await documentApi.updateDocument(selectedDocument.id, {
        title: formData.title,
        parentId: formData.parentId,
        summary: selectedDocument.docType === 'DOC' ? formData.summary : undefined,
      })
      setDocumentModalOpen(false)
      await loadData()
      setSelectedDocumentId(selectedDocument.id)
      toast.success(`${TYPE_LABELS[selectedDocument.docType] || selectedDocument.docType}”${formData.title}”已更新`)
    } catch (error) {
      console.error('保存文档节点失败', error)
      setDocumentModalError(error?.message || '保存文档节点失败，请稍后重试')
    } finally {
      setDocumentSubmitting(false)
    }
  }

  const handleDeleteDocument = async (targetDocument = selectedDocument) => {
    if (!targetDocument || !canWriteKnowledgeBase) {
      return
    }

    const confirmed = await confirm({
      title: '确认删除',
      description: `确认删除${TYPE_LABELS[targetDocument.docType] || '节点'}”${targetDocument.title}”吗？删除后可在文档回收站恢复。`,
      confirmLabel: '确认删除',
      danger: true,
    })
    if (!confirmed) return

    try {
      await documentApi.deleteDocument(targetDocument.id)
      setSelectedDocumentId(null)
      await loadData()
      toast.success(`${TYPE_LABELS[targetDocument.docType] || targetDocument.docType}”${targetDocument.title}”已删除，可在文档回收站恢复`)
    } catch (error) {
      console.error('删除文档节点失败', error)
      toast.error(error?.message || '删除文档节点失败，请稍后重试')
    }
  }

  const handleReorderDocument = async (direction) => {
    if (!selectedDocument || !canWriteKnowledgeBase) {
      return
    }

    const targetIndex = selectedSiblingIndex + direction
    if (targetIndex < 0 || targetIndex >= siblingDocuments.length) {
      return
    }

    const reordered = [...siblingDocuments]
    const [movedItem] = reordered.splice(selectedSiblingIndex, 1)
    reordered.splice(targetIndex, 0, movedItem)

    try {
      await documentApi.updateSortOrder(
        reordered.map((item, indexValue) => ({
          id: item.id,
          sortOrder: indexValue,
        }))
      )
      await loadData()
      setSelectedDocumentId(selectedDocument.id)
      toast.success(`已调整${TYPE_LABELS[selectedDocument.docType] || selectedDocument.docType}”${selectedDocument.title}”的顺序`)
    } catch (error) {
      console.error('调整文档顺序失败', error)
      toast.error(error?.message || '调整文档顺序失败，请稍后重试')
    }
  }

  const handleBatchMove = async ({ parentId }) => {
    if (topLevelSelectedDocuments.length === 0 || !canWriteKnowledgeBase) {
      return
    }

    const alreadyInTargetParent = topLevelSelectedDocuments.every((item) => (item.parentId ?? 0) === parentId)
    if (alreadyInTargetParent) {
      setBatchMoveError('所选节点已经位于目标目录')
      return
    }

    try {
      setBatchMoveSubmitting(true)
      setBatchMoveError('')
      await documentApi.batchMoveDocuments(
        topLevelSelectedDocuments.map((item) => item.id),
        parentId
      )
      setBatchMoveOpen(false)
      await loadData()
      setSelectedDocumentId(topLevelSelectedDocuments[0]?.id || null)
      clearBatchSelection()
      toast.success(`已批量移动 ${topLevelSelectedDocuments.length} 个节点`)
    } catch (error) {
      console.error('批量移动文档节点失败', error)
      setBatchMoveError(error?.message || '批量移动失败，请稍后重试')
    } finally {
      setBatchMoveSubmitting(false)
    }
  }

  const handleDragStart = (event, item) => {
    if (!dragSortEnabled) {
      return
    }

    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(item.id))
    setDraggingDocumentId(item.id)
    setDragOverDocumentId(null)
    setDragOverPosition('before')
  }

  const handleDragOver = (event, item) => {
    if (!dragSortEnabled || !draggingDocumentId || draggingDocumentId === item.id) {
      return
    }

    const draggingDocument = documents.find((candidate) => candidate.id === draggingDocumentId)
    if (!draggingDocument) {
      return
    }

    const isDraggingFolderIntoSelf = draggingDocument.docType === 'FOLDER'
      && ((item.path || '') === (draggingDocument.path || '') || (item.path || '').startsWith(`${draggingDocument.path || ''}/`))
    if (isDraggingFolderIntoSelf) {
      return
    }

    event.preventDefault()
    const bounds = event.currentTarget.getBoundingClientRect()
    const offsetY = event.clientY - bounds.top
    const sameParent = (draggingDocument.parentId ?? 0) === (item.parentId ?? 0)
    let position = offsetY > bounds.height / 2 ? 'after' : 'before'

    if (item.docType === 'FOLDER' && offsetY > bounds.height * 0.25 && offsetY < bounds.height * 0.75) {
      position = 'inside'
    } else if (!sameParent && item.docType !== 'FOLDER') {
      return
    }

    event.dataTransfer.dropEffect = 'move'
    setDragOverDocumentId(item.id)
    setDragOverPosition(position)
  }

  const handleDrop = async (event, item) => {
    if (!dragSortEnabled || !draggingDocumentId || draggingDocumentId === item.id) {
      clearDragState()
      return
    }

    event.preventDefault()
    const draggingDocument = documents.find((candidate) => candidate.id === draggingDocumentId)
    if (!draggingDocument) {
      clearDragState()
      return
    }

    try {
      setDragSorting(true)
      if (dragOverPosition === 'inside') {
        if (item.docType !== 'FOLDER') {
          clearDragState()
          return
        }
        if ((draggingDocument.parentId ?? 0) === item.id) {
          clearDragState()
          return
        }

        await documentApi.batchMoveDocuments([draggingDocument.id], item.id)
        setExpandedFolderIds((current) => (current.includes(item.id) ? current : [...current, item.id]))
        await loadData()
        setSelectedDocumentId(draggingDocument.id)
        toast.success(`已将${TYPE_LABELS[draggingDocument.docType] || draggingDocument.docType}”${draggingDocument.title}”移入目录“${item.title}”`)
        return
      }

      if ((draggingDocument.parentId ?? 0) !== (item.parentId ?? 0)) {
        clearDragState()
        return
      }

      const reorderedDocuments = reorderSiblingDocuments(
        getSiblingDocuments(documents, draggingDocument),
        draggingDocument.id,
        item.id,
        dragOverPosition
      )

      if (!reorderedDocuments) {
        clearDragState()
        return
      }

      await documentApi.updateSortOrder(
        reorderedDocuments.map((documentItem, indexValue) => ({
          id: documentItem.id,
          sortOrder: indexValue,
        }))
      )
      await loadData()
      setSelectedDocumentId(draggingDocument.id)
      toast.success(`已通过拖拽调整${TYPE_LABELS[draggingDocument.docType] || draggingDocument.docType}”${draggingDocument.title}”的顺序`)
    } catch (error) {
      console.error('拖拽调整文档顺序失败', error)
      toast.error(error?.message || '拖拽调整文档顺序失败，请稍后重试')
    } finally {
      setDragSorting(false)
      clearDragState()
    }
  }

  const handleBatchDelete = async () => {
    if (selectedDocumentIds.length === 0 || !canWriteKnowledgeBase) {
      return
    }

    const validationMessage = validateBatchDeleteSelection(documents, selectedDocumentIds)
    if (validationMessage) {
      toast.error(validationMessage)
      return
    }

    const confirmed = await confirm({
      title: '确认批量删除',
      description: `确认批量删除已选择的 ${selectedDocumentIds.length} 个节点吗？删除后可在文档回收站恢复。`,
      confirmLabel: '确认删除',
      danger: true,
    })
    if (!confirmed) return

    try {
      setBatchDeleting(true)
      await documentApi.batchDeleteDocuments(selectedDocumentIds)
      setSelectedDocumentId(null)
      await loadData()
      clearBatchSelection()
      toast.success(`已批量删除 ${selectedDocumentIds.length} 个节点，可在文档回收站恢复`)
    } catch (error) {
      console.error('批量删除文档节点失败', error)
      toast.error(error?.message || '批量删除失败，请稍后重试')
    } finally {
      setBatchDeleting(false)
    }
  }

  const handleDeleteKnowledgeBase = async () => {
    if (!canManageKnowledgeBase || !knowledgeBase) {
      return
    }

    const confirmed = await confirm({
      title: '确认删除知识库',
      description: `确认删除知识库”${knowledgeBase.name}”吗？删除后可在知识库回收站恢复。`,
      confirmLabel: '确认删除',
      danger: true,
    })
    if (!confirmed) return

    try {
      await knowledgeBaseApi.deleteKnowledgeBase(id)
      emitKnowledgeBasesChanged()
      navigate('/workspace/manage/trash', {
        state: {
          feedback: {
            type: 'success',
            message: `知识库”${knowledgeBase.name}”已删除，可在知识库回收站恢复`,
          },
        },
      })
    } catch (error) {
      console.error('删除知识库失败', error)
      toast.error(error?.message || '删除知识库失败，请稍后重试')
    }
  }

  return {
    pageStatus,
    pageErrorMessage,
    knowledgeBase,
    documents,
    selectedDocument,
    search,
    setSearch,
    editing,
    setEditing,
    submitting,
    modalError,
    setModalError,
    documentModalOpen,
    setDocumentModalOpen,
    documentModalMode,
    documentModalType,
    documentModalInitialValues,
    documentModalError,
    setDocumentModalError,
    documentSubmitting,
    batchMode,
    selectedDocumentIds,
    batchMoveOpen,
    setBatchMoveOpen,
    batchMoveSubmitting,
    batchMoveError,
    setBatchMoveError,
    batchDeleting,
    draggingDocumentId,
    dragOverDocumentId,
    dragOverPosition,
    dragSorting,
    expandedFolderIdSet,
    treePanelCollapsed,
    setTreePanelCollapsed,
    focusMode,
    knowledgeBaseInfoVisible,
    setKnowledgeBaseInfoVisible,
    siteSubmitting,
    siteError,
    setSiteError,
    documentPublishingSubmitting,
    documentPublishingError,
    setDocumentPublishingError,
    readLinkOpen,
    setReadLinkOpen,
    permissionModalOpen,
    setPermissionModalOpen,
    permissionMembers,
    permissionAssignments,
    permissionLoading,
    permissionSubmitting,
    permissionError,
    setPermissionError,
    documentTrashOpen,
    setDocumentTrashOpen,
    deletedDocuments,
    documentTrashLoading,
    documentTrashError,
    setDocumentTrashError,
    restoringDocumentId,
    visibleDocuments,
    safeSelectedDocumentContent,
    canMoveUp,
    canMoveDown,
    documentModalFolderOptions,
    selectedDocumentIdSet,
    topLevelSelectedDocuments,
    batchFolderOptions,
    batchMoveInitialParentId,
    canWriteKnowledgeBase,
    canManageKnowledgeBase,
    dragSortEnabled,
    shouldRenderRichPreview,
    compactKnowledgeBaseDescription,
    hasActiveSearch,
    treePanelStatusText,
    treeHintText,
    hasFolderNodes,
    selectedFolderDocumentCount,
    selectedFolderDirectoryCount,
    loadData,
    handleSaveKnowledgeBase,
    handleSaveSiteSettings,
    handlePublishDocument,
    handleUnpublishDocument,
    openPermissionModal,
    handleSubmitPermissions,
    openDocumentTrash,
    handleRestoreDocument,
    clearBatchSelection,
    handleToggleBatchMode,
    clearDragState,
    toggleFolderExpanded,
    expandAllFolders,
    collapseToTopLevelFolders,
    handleTreeItemKeyDown,
    handleOpenEditorPage,
    handleToggleFocusMode,
    openCreateDocumentModal,
    openEditDocumentModal,
    handleSubmitDocument,
    handleDeleteDocument,
    handleReorderDocument,
    handleBatchMove,
    handleDragStart,
    handleDragOver,
    handleDrop,
    handleBatchDelete,
    handleDeleteKnowledgeBase,
    toggleDocumentSelection,
    setSelectedDocumentId,
  }
}

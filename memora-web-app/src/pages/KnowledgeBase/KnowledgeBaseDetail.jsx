import { useCallback, useDeferredValue, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import dayjs from 'dayjs'
import { useAuth } from '../../contexts/AuthContext'
import DocumentActionModal from '../../components/Document/DocumentActionModal'
import DocumentBatchMoveModal from '../../components/Document/DocumentBatchMoveModal'
import DocumentShareDrawer from '../../components/Document/DocumentShareDrawer'
import KnowledgeBaseFormModal from '../../components/KnowledgeBase/KnowledgeBaseFormModal'
import KnowledgeBasePermissionModal from '../../components/KnowledgeBase/KnowledgeBasePermissionModal'
import { documentApi } from '../../services/api/documentApi'
import { knowledgeBaseApi } from '../../services/api/knowledgeBaseApi'
import { workspaceApi } from '../../services/api/workspaceApi'
import { emitKnowledgeBasesChanged } from '../../utils/knowledgeBaseEvents'
import { rememberKnowledgeBase } from '../../utils/knowledgeBaseRoute'
import styles from './KnowledgeBaseDetail.module.css'

const STATUS_LABELS = {
  SYNCED: '已同步',
  PENDING: '待同步',
  DISABLED: '未启用',
  MANUAL: '手工维护',
  IDLE: '空闲',
}

const TYPE_LABELS = {
  FOLDER: '目录',
  DOC: '文档',
}

const ROLE_LABELS = {
  OWNER: '所有者',
  ADMIN: '管理员',
  EDITOR: '编辑者',
  REVIEWER: '审核者',
  VIEWER: '只读',
}

const PAGE_STATUS = {
  LOADING: 'loading',
  READY: 'ready',
  FORBIDDEN: 'forbidden',
  NOT_FOUND: 'not_found',
  ERROR: 'error',
}

const compareByTreeOrder = (left, right) => {
  const sortDiff = (left.sortOrder ?? 0) - (right.sortOrder ?? 0)
  if (sortDiff !== 0) {
    return sortDiff
  }
  return (left.path || '').localeCompare(right.path || '')
}

const getSiblingDocuments = (documents, targetDocument) => {
  if (!targetDocument) {
    return []
  }
  return documents
    .filter((item) => (item.parentId ?? 0) === (targetDocument.parentId ?? 0))
    .sort(compareByTreeOrder)
}

const reorderSiblingDocuments = (documents, draggedDocumentId, targetDocumentId, position) => {
  const orderedDocuments = [...documents].sort(compareByTreeOrder)
  const draggedIndex = orderedDocuments.findIndex((item) => item.id === draggedDocumentId)
  const targetIndex = orderedDocuments.findIndex((item) => item.id === targetDocumentId)

  if (draggedIndex < 0 || targetIndex < 0 || draggedDocumentId === targetDocumentId) {
    return null
  }

  const [draggedDocument] = orderedDocuments.splice(draggedIndex, 1)
  const nextTargetIndex = orderedDocuments.findIndex((item) => item.id === targetDocumentId)

  if (nextTargetIndex < 0) {
    return null
  }

  const insertIndex = position === 'after' ? nextTargetIndex + 1 : nextTargetIndex
  orderedDocuments.splice(insertIndex, 0, draggedDocument)
  return orderedDocuments
}

const resolveDefaultParentId = (selectedDocument) => {
  if (!selectedDocument) {
    return 0
  }
  if (selectedDocument.docType === 'FOLDER') {
    return selectedDocument.id
  }
  return selectedDocument.parentId ?? 0
}

const buildFolderOptions = (documents, currentDocument = null) => {
  const options = [{ id: 0, label: '根目录' }]
  const currentPath = currentDocument?.path || ''

  documents
    .filter((item) => item.docType === 'FOLDER')
    .filter((item) => {
      if (!currentDocument) {
        return true
      }
      if (item.id === currentDocument.id) {
        return false
      }
      if (currentDocument.docType === 'FOLDER' && currentPath && (item.path || '').startsWith(`${currentPath}/`)) {
        return false
      }
      return true
    })
    .forEach((item) => {
      options.push({
        id: item.id,
        label: `${item.title} (${item.path})`,
      })
    })

  return options
}

const getDocumentByIdMap = (documents) => {
  return new Map(documents.map((item) => [item.id, item]))
}

const getTopLevelSelectedDocuments = (documents, selectedIds) => {
  const selectedIdSet = new Set(selectedIds)
  const documentMap = getDocumentByIdMap(documents)

  return documents.filter((item) => {
    if (!selectedIdSet.has(item.id)) {
      return false
    }

    let parentId = item.parentId ?? 0
    while (parentId) {
      if (selectedIdSet.has(parentId)) {
        return false
      }
      const parent = documentMap.get(parentId)
      parentId = parent?.parentId ?? 0
    }
    return true
  })
}

const buildBatchFolderOptions = (documents, selectedDocuments) => {
  const forbiddenFolderIds = new Set()

  selectedDocuments.forEach((item) => {
    if (item.docType === 'FOLDER') {
      forbiddenFolderIds.add(item.id)
      documents.forEach((candidate) => {
        if ((candidate.path || '').startsWith(`${item.path}/`)) {
          forbiddenFolderIds.add(candidate.id)
        }
      })
    }
  })

  return [{ id: 0, label: '根目录' }].concat(
    documents
      .filter((item) => item.docType === 'FOLDER' && !forbiddenFolderIds.has(item.id))
      .map((item) => ({
        id: item.id,
        label: `${item.title} (${item.path})`,
      }))
  )
}

const resolveBatchMoveInitialParentId = (selectedDocuments) => {
  if (selectedDocuments.length === 0) {
    return 0
  }

  const parentId = selectedDocuments[0].parentId ?? 0
  return selectedDocuments.every((item) => (item.parentId ?? 0) === parentId) ? parentId : 0
}

const validateBatchDeleteSelection = (documents, selectedIds) => {
  const selectedIdSet = new Set(selectedIds)

  for (const item of documents) {
    if (item.docType !== 'FOLDER' || !selectedIdSet.has(item.id)) {
      continue
    }

    const hasUnselectedDescendant = documents.some((candidate) => {
      if (candidate.id === item.id) {
        return false
      }
      if (!(candidate.path || '').startsWith(`${item.path}/`)) {
        return false
      }
      return !selectedIdSet.has(candidate.id)
    })

    if (hasUnselectedDescendant) {
      return `目录“${item.title}”仍有未选中的子节点，不能批量删除`
    }
  }

  return ''
}

const buildInitialExpandedFolderIds = (documents, targetDocumentId = null) => {
  const expandedIds = new Set(
    documents
      .filter((item) => item.docType === 'FOLDER' && (item.parentId ?? 0) === 0)
      .map((item) => item.id)
  )

  if (!targetDocumentId) {
    return Array.from(expandedIds)
  }

  const documentMap = getDocumentByIdMap(documents)
  let currentDocument = documentMap.get(targetDocumentId) || null

  if (currentDocument?.docType === 'FOLDER') {
    expandedIds.add(currentDocument.id)
  }

  while (currentDocument?.parentId) {
    const parentDocument = documentMap.get(currentDocument.parentId) || null
    if (!parentDocument) {
      break
    }
    if (parentDocument.docType === 'FOLDER') {
      expandedIds.add(parentDocument.id)
    }
    currentDocument = parentDocument
  }

  return Array.from(expandedIds)
}

const isTreeItemVisible = (item, documentMap, expandedFolderIdSet) => {
  let parentId = item.parentId ?? 0

  while (parentId) {
    if (!expandedFolderIdSet.has(parentId)) {
      return false
    }
    const parentDocument = documentMap.get(parentId)
    parentId = parentDocument?.parentId ?? 0
  }

  return true
}

const KnowledgeBaseDetail = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { currentUser } = useAuth()
  const [pageStatus, setPageStatus] = useState(PAGE_STATUS.LOADING)
  const [pageErrorMessage, setPageErrorMessage] = useState('')
  const [knowledgeBase, setKnowledgeBase] = useState(null)
  const [documents, setDocuments] = useState([])
  const [selectedDocumentId, setSelectedDocumentId] = useState(null)
  const [search, setSearch] = useState('')
  const [syncing, setSyncing] = useState(false)
  const [editing, setEditing] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [modalError, setModalError] = useState('')
  const [feedback, setFeedback] = useState(null)
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
  const [scrolled, setScrolled] = useState(false)
  const [knowledgeBaseInfoVisible, setKnowledgeBaseInfoVisible] = useState(false)
  const [shareOpen, setShareOpen] = useState(false)
  const [permissionModalOpen, setPermissionModalOpen] = useState(false)
  const [permissionMembers, setPermissionMembers] = useState([])
  const [permissionAssignments, setPermissionAssignments] = useState([])
  const [permissionLoading, setPermissionLoading] = useState(false)
  const [permissionSubmitting, setPermissionSubmitting] = useState(false)
  const [permissionError, setPermissionError] = useState('')
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

      const [documentTreeResult] = await Promise.allSettled([documentApi.getDocumentTreeByKnowledgeBaseId(id)])

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
    const handleScroll = () => {
      setScrolled(window.scrollY > 24)
    }

    handleScroll()
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  useEffect(() => {
    if (documents.length === 0) {
      setSelectedDocumentId(null)
      return
    }

    const preferredDocumentId = Number(location.state?.selectedDocumentId || 0)
    if (preferredDocumentId && documents.some((item) => item.id === preferredDocumentId)) {
      setSelectedDocumentId(preferredDocumentId)
      return
    }

    if (!selectedDocumentId || !documents.some((item) => item.id === selectedDocumentId)) {
      const firstDoc = documents.find((item) => item.docType === 'DOC') || documents[0]
      setSelectedDocumentId(firstDoc.id)
    }
  }, [documents, location.state, selectedDocumentId])

  useEffect(() => {
    const folderIds = new Set(documents.filter((item) => item.docType === 'FOLDER').map((item) => item.id))
    const initialExpandedIds = buildInitialExpandedFolderIds(documents, selectedDocumentId)

    setExpandedFolderIds((current) => {
      const nextIds = new Set(initialExpandedIds)
      current.forEach((id) => {
        if (folderIds.has(id)) {
          nextIds.add(id)
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
  const shouldRenderRichPreview = selectedDocument?.docType === 'DOC' && !!selectedDocument?.content
  const compactKnowledgeBaseDescription = knowledgeBase?.description?.trim()
  const hasActiveSearch = !!deferredSearch.trim()
  const treePanelStatusText = hasActiveSearch
    ? `找到 ${visibleDocuments.length} 项`
    : batchMode
      ? `已选 ${selectedDocumentIds.length} 项`
      : `${documents.length} 个节点`
  const hasFolderNodes = documents.some((item) => item.docType === 'FOLDER')
  const selectedFolderChildren = selectedDocument
    ? documents.filter((item) => (item.parentId ?? 0) === selectedDocument.id)
    : []
  const selectedFolderDocumentCount = selectedFolderChildren.filter((item) => item.docType === 'DOC').length
  const selectedFolderDirectoryCount = selectedFolderChildren.filter((item) => item.docType === 'FOLDER').length

  useEffect(() => {
    if (selectedDocument?.docType !== 'DOC') {
      setFocusMode(false)
      setShareOpen(false)
    }
  }, [selectedDocument?.docType])

  useEffect(() => {
    if (selectedDocumentIds.length === 0) {
      return
    }

    const documentIds = new Set(documents.map((item) => item.id))
    setSelectedDocumentIds((current) => current.filter((itemId) => documentIds.has(itemId)))
  }, [documents, selectedDocumentIds.length])

  const handleTriggerSync = async () => {
    if (!canWriteKnowledgeBase) {
      return
    }

    try {
      setSyncing(true)
      setFeedback(null)
      const response = await knowledgeBaseApi.triggerSync(id)
      if (response.code === 200) {
        await loadData()
        setFeedback({ type: 'success', message: '同步任务已触发，正在刷新最新状态' })
      }
    } catch (error) {
      console.error('触发同步失败', error)
      setFeedback({ type: 'error', message: error?.message || '触发同步失败，请稍后重试' })
    } finally {
      setSyncing(false)
    }
  }

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
      setFeedback({ type: 'success', message: `知识库“${formData.name}”已更新` })
    } catch (error) {
      console.error('更新知识库失败', error)
      setModalError(error?.message || '更新知识库失败，请稍后重试')
    } finally {
      setSubmitting(false)
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

      if (dashboardResponse.code === 200) {
        setPermissionMembers(dashboardResponse.data?.members || [])
      } else {
        setPermissionMembers([])
      }

      if (memberResponse.code === 200) {
        setPermissionAssignments(memberResponse.data || [])
      } else {
        setPermissionAssignments([])
      }
    } catch (error) {
      console.error('加载知识库权限配置失败', error)
      setPermissionError(error?.message || '加载知识库权限配置失败，请稍后重试')
    } finally {
      setPermissionLoading(false)
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
      setFeedback({
        type: 'success',
        message: members.length > 0 ? '知识库独立权限已更新' : '知识库已恢复继承租户权限',
      })
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

  const handleOpenEditorPage = () => {
    if (!selectedDocument || selectedDocument.docType !== 'DOC' || !canWriteKnowledgeBase) {
      return
    }
    navigateToEditor(selectedDocument.id)
  }

  const handleToggleFocusMode = () => {
    if (!selectedDocument || selectedDocument.docType !== 'DOC') {
      return
    }

    setFocusMode((current) => !current)
  }

  const openCreateDocumentModal = (docType) => {
    if (!canWriteKnowledgeBase) {
      return
    }

    setDocumentModalMode('create')
    setDocumentModalType(docType)
    setDocumentModalError('')
    setDocumentModalInitialValues({
      title: '',
      summary: '',
      parentId: resolveDefaultParentId(selectedDocument),
    })
    setDocumentModalOpen(true)
  }

  const openEditDocumentModal = () => {
    if (!selectedDocument || !canWriteKnowledgeBase) {
      return
    }

    setDocumentModalMode('edit')
    setDocumentModalType(selectedDocument.docType)
    setDocumentModalError('')
    setDocumentModalInitialValues({
      title: selectedDocument.title,
      summary: selectedDocument.summary || '',
      parentId: selectedDocument.parentId ?? 0,
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
        const response = await documentApi.createDocument({
          knowledgeBaseId: Number(id),
          parentId: formData.parentId,
          title: formData.title,
          docType: documentModalType,
          format: 'MARKDOWN',
          summary: documentModalType === 'DOC' ? formData.summary : undefined,
          content: documentModalType === 'DOC' ? `# ${formData.title}` : '',
          contentText: documentModalType === 'DOC' ? (formData.summary || formData.title) : formData.title,
        })
        setDocumentModalOpen(false)
        await loadData()
        const createdDocumentId = response?.data?.id || selectedDocumentId
        setSelectedDocumentId(createdDocumentId)
        if (documentModalType === 'DOC') {
          navigateToEditor(createdDocumentId)
        }
        setFeedback({
          type: 'success',
          message: `${TYPE_LABELS[documentModalType] || documentModalType}“${formData.title}”已创建`,
        })
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
      setFeedback({
        type: 'success',
        message: `${TYPE_LABELS[selectedDocument.docType] || selectedDocument.docType}“${formData.title}”已更新`,
      })
    } catch (error) {
      console.error('保存文档节点失败', error)
      setDocumentModalError(error?.message || '保存文档节点失败，请稍后重试')
    } finally {
      setDocumentSubmitting(false)
    }
  }

  const handleDeleteDocument = async () => {
    if (!selectedDocument || !canWriteKnowledgeBase) {
      return
    }

    if (!window.confirm(`确认删除${TYPE_LABELS[selectedDocument.docType] || '节点'}“${selectedDocument.title}”吗？`)) {
      return
    }

    try {
      await documentApi.deleteDocument(selectedDocument.id)
      setSelectedDocumentId(null)
      await loadData()
      setFeedback({
        type: 'success',
        message: `${TYPE_LABELS[selectedDocument.docType] || selectedDocument.docType}“${selectedDocument.title}”已删除`,
      })
    } catch (error) {
      console.error('删除文档节点失败', error)
      setFeedback({ type: 'error', message: error?.message || '删除文档节点失败，请稍后重试' })
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
      setFeedback({
        type: 'success',
        message: `已调整${TYPE_LABELS[selectedDocument.docType] || selectedDocument.docType}“${selectedDocument.title}”的顺序`,
      })
    } catch (error) {
      console.error('调整文档顺序失败', error)
      setFeedback({ type: 'error', message: error?.message || '调整文档顺序失败，请稍后重试' })
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
      setFeedback({
        type: 'success',
        message: `已批量移动 ${topLevelSelectedDocuments.length} 个节点`,
      })
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
    if (!draggingDocument || (draggingDocument.parentId ?? 0) !== (item.parentId ?? 0)) {
      return
    }

    event.preventDefault()
    const bounds = event.currentTarget.getBoundingClientRect()
    const position = event.clientY - bounds.top > bounds.height / 2 ? 'after' : 'before'
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
    if (!draggingDocument || (draggingDocument.parentId ?? 0) !== (item.parentId ?? 0)) {
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

    try {
      setDragSorting(true)
      await documentApi.updateSortOrder(
        reorderedDocuments.map((documentItem, indexValue) => ({
          id: documentItem.id,
          sortOrder: indexValue,
        }))
      )
      await loadData()
      setSelectedDocumentId(draggingDocument.id)
      setFeedback({
        type: 'success',
        message: `已通过拖拽调整${TYPE_LABELS[draggingDocument.docType] || draggingDocument.docType}“${draggingDocument.title}”的顺序`,
      })
    } catch (error) {
      console.error('拖拽调整文档顺序失败', error)
      setFeedback({ type: 'error', message: error?.message || '拖拽调整文档顺序失败，请稍后重试' })
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
      setFeedback({ type: 'error', message: validationMessage })
      return
    }

    if (!window.confirm(`确认批量删除已选择的 ${selectedDocumentIds.length} 个节点吗？`)) {
      return
    }

    try {
      setBatchDeleting(true)
      await documentApi.batchDeleteDocuments(selectedDocumentIds)
      setSelectedDocumentId(null)
      await loadData()
      clearBatchSelection()
      setFeedback({
        type: 'success',
        message: `已批量删除 ${selectedDocumentIds.length} 个节点`,
      })
    } catch (error) {
      console.error('批量删除文档节点失败', error)
      setFeedback({ type: 'error', message: error?.message || '批量删除失败，请稍后重试' })
    } finally {
      setBatchDeleting(false)
    }
  }

  const handleDeleteKnowledgeBase = async () => {
    if (!canManageKnowledgeBase) {
      return
    }

    if (!window.confirm(`确认删除知识库“${knowledgeBase.name}”吗？`)) {
      return
    }

    try {
      await knowledgeBaseApi.deleteKnowledgeBase(id)
      emitKnowledgeBasesChanged()
      navigate('/')
    } catch (error) {
      console.error('删除知识库失败', error)
      setFeedback({ type: 'error', message: error?.message || '删除知识库失败，请稍后重试' })
    }
  }

  if (pageStatus === PAGE_STATUS.LOADING) {
    return <div className={styles.state}>正在加载知识库...</div>
  }

  if (pageStatus !== PAGE_STATUS.READY || !knowledgeBase) {
    const stateTitleMap = {
      [PAGE_STATUS.FORBIDDEN]: '当前会话无权访问该知识库',
      [PAGE_STATUS.NOT_FOUND]: '当前知识库不存在',
      [PAGE_STATUS.ERROR]: '知识库详情暂时不可用',
    }

    return (
      <div className={styles.stateCard}>
        <h1>{stateTitleMap[pageStatus] || '知识库详情暂时不可用'}</h1>
        <p>{pageErrorMessage || '请返回工作台查看当前可访问的知识库。'}</p>
        <div className={styles.stateActions}>
          <button type="button" className={styles.primaryButton} onClick={() => navigate('/')}>
            返回工作台
          </button>
          <button type="button" className={styles.secondaryButton} onClick={() => loadData({ resetState: true })}>
            重新加载
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className={`${styles.page} ${scrolled ? styles.pageScrolled : ''}`}>
      <header className={`${styles.hero} ${scrolled ? styles.heroScrolled : ''}`}>
        <div className={styles.heroMain}>
          <div className={styles.breadcrumb}>
            <Link to="/">工作台</Link>
            <span>/</span>
            <span>{knowledgeBase.name}</span>
          </div>
          <div className={styles.heroTitleRow}>
            <h1 className={styles.title}>{knowledgeBase.name}</h1>
            <div className={styles.syncBadge}>{STATUS_LABELS[knowledgeBase.syncStatus] || knowledgeBase.syncStatus}</div>
          </div>
          <div className={styles.heroMeta}>
            <span className={styles.metaPill}>文档工作区</span>
            <span className={styles.metaPill}>{knowledgeBase.documentCount} 个节点</span>
            <span className={styles.metaPill}>{ROLE_LABELS[knowledgeBase.currentRole] || knowledgeBase.currentRole || '未知角色'}</span>
            <span className={styles.metaPill}>{knowledgeBase.syncEnabled ? '已启用同步' : '未启用同步'}</span>
            {knowledgeBase.permissionRestricted && <span className={styles.metaPill}>独立权限</span>}
          </div>
          {knowledgeBaseInfoVisible && (
            <p className={styles.heroDescription}>
              {compactKnowledgeBaseDescription || '当前知识库用于承载文档协作和目录整理，主流程仍以继续写作和阅读为主。'}
            </p>
          )}
        </div>
        <div className={styles.heroActions}>
          <div className={styles.heroActionGrid}>
            <button
              type="button"
              className={styles.primaryButton}
              disabled={!canWriteKnowledgeBase}
              onClick={() => openCreateDocumentModal('DOC')}
            >
              新建文档
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              disabled={!canWriteKnowledgeBase}
              onClick={() => openCreateDocumentModal('FOLDER')}
            >
              新建目录
            </button>
            <details className={styles.moreActions}>
              <summary className={styles.secondaryButton}>更多</summary>
              <div className={styles.moreActionsMenu}>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  onClick={() => setKnowledgeBaseInfoVisible((current) => !current)}
                >
                  {knowledgeBaseInfoVisible ? '收起说明' : '知识库说明'}
                </button>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  disabled={!knowledgeBase.syncEnabled || syncing || !canWriteKnowledgeBase}
                  onClick={handleTriggerSync}
                >
                  {syncing ? '同步中...' : '同步一次'}
                </button>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  disabled={!canManageKnowledgeBase}
                  onClick={() => {
                    setModalError('')
                    setEditing(true)
                  }}
                >
                  知识库设置
                </button>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  disabled={!canManageKnowledgeBase}
                  onClick={openPermissionModal}
                >
                  访问权限
                </button>
                <button
                  type="button"
                  className={styles.dangerButton}
                  disabled={!canManageKnowledgeBase}
                  onClick={handleDeleteKnowledgeBase}
                >
                  删除知识库
                </button>
              </div>
            </details>
          </div>
        </div>
      </header>

      <section
        className={[
          styles.contentGrid,
          focusMode ? styles.contentGridFocus : '',
          !focusMode && treePanelCollapsed ? styles.contentGridDocumentOnly : styles.contentGridWide,
        ]
          .filter(Boolean)
          .join(' ')}
      >
        <aside
          className={[
            styles.treePanel,
            focusMode ? styles.focusHidden : '',
            !focusMode && treePanelCollapsed ? styles.treePanelCollapsed : '',
          ]
            .filter(Boolean)
            .join(' ')}
        >
          {feedback && (
            <div className={`${styles.feedback} ${feedback.type === 'error' ? styles.feedbackError : styles.feedbackSuccess}`}>
              {feedback.message}
            </div>
          )}
          <div className={styles.panelHeader}>
            <div>
              <h2>目录</h2>
              <span className={styles.panelHint}>{treePanelStatusText}</span>
            </div>
            <div className={styles.panelActions}>
              <button
                type="button"
                className={styles.smallButton}
                onClick={() => setTreePanelCollapsed(true)}
              >
                收起
              </button>
              <button
                type="button"
                className={styles.primaryCompactButton}
                disabled={!canWriteKnowledgeBase}
                onClick={() => openCreateDocumentModal('DOC')}
              >
                新建文档
              </button>
              <details className={styles.inlineMoreActions}>
                <summary className={styles.smallButton}>更多</summary>
                <div className={styles.inlineMoreActionsMenu}>
                  <button
                    type="button"
                    className={styles.toolButton}
                    disabled={!canWriteKnowledgeBase}
                    onClick={() => openCreateDocumentModal('FOLDER')}
                  >
                    新建目录
                  </button>
                  <button
                    type="button"
                    className={styles.toolButton}
                    onClick={() => setKnowledgeBaseInfoVisible((current) => !current)}
                  >
                    {knowledgeBaseInfoVisible ? '收起说明' : '知识库说明'}
                  </button>
                </div>
              </details>
            </div>
          </div>
          <div className={styles.treeToolbar}>
            <input
              className={styles.search}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="搜索文档标题"
            />
          </div>
          <div className={styles.treeModeBar}>
            <div className={styles.treeModeMeta}>
              <span className={`${styles.modeBadge} ${batchMode ? styles.modeBadgeActive : ''}`}>
                {batchMode ? '批量整理中' : hasActiveSearch ? '搜索结果' : '全部内容'}
              </span>
            </div>
            <div className={styles.treeModeActions}>
              {hasFolderNodes && (
                <>
                <button type="button" className={styles.textButton} onClick={expandAllFolders}>
                  展开
                </button>
                <button type="button" className={styles.textButton} onClick={collapseToTopLevelFolders}>
                  收起
                </button>
                </>
              )}
              <button
                type="button"
                className={`${styles.modeButton} ${batchMode ? styles.modeButtonActive : ''}`}
                disabled={!canWriteKnowledgeBase}
                onClick={handleToggleBatchMode}
              >
                {batchMode ? '退出批量' : '批量整理'}
              </button>
            </div>
          </div>
          <div className={styles.treeHint}>
            {batchMode
              ? '选择文档或目录后，再移动或删除。'
              : dragSortEnabled
                ? '可直接拖拽排序。'
                : hasActiveSearch
                  ? '按标题过滤，层级关系保持不变。'
                  : '继续像文档目录一样浏览即可。'}
          </div>
          {batchMode && (
            <div className={styles.batchToolbar}>
              <span>已选 {selectedDocumentIds.length} 项</span>
              <div className={styles.batchActions}>
                <button
                  type="button"
                  className={styles.toolButton}
                  disabled={selectedDocumentIds.length === 0 || !canWriteKnowledgeBase}
                  onClick={() => {
                    setBatchMoveError('')
                    setBatchMoveOpen(true)
                  }}
                >
                  移动
                </button>
                <button
                  type="button"
                  className={styles.toolButtonDanger}
                  disabled={selectedDocumentIds.length === 0 || batchDeleting || !canWriteKnowledgeBase}
                  onClick={handleBatchDelete}
                >
                  {batchDeleting ? '删除中...' : '删除'}
                </button>
                <button type="button" className={styles.toolButton} onClick={clearBatchSelection}>
                  取消
                </button>
              </div>
            </div>
          )}
          <div className={styles.treeViewport}>
            <div className={styles.treeList}>
              {documents.length === 0 ? (
                <div className={styles.emptyTreeCta}>
                  <strong>这个知识库还是空的</strong>
                  <p>先写第一篇文档，目录后面再补。</p>
                  <div className={styles.emptyTreeActions}>
                    <button
                      type="button"
                      className={styles.smallButton}
                      disabled={!canWriteKnowledgeBase}
                      onClick={() => openCreateDocumentModal('DOC')}
                    >
                      新建第一篇文档
                    </button>
                    <button
                      type="button"
                      className={styles.toolButton}
                      disabled={!canWriteKnowledgeBase}
                      onClick={() => openCreateDocumentModal('FOLDER')}
                    >
                      新建目录
                    </button>
                  </div>
                </div>
              ) : visibleDocuments.length > 0 ? (
                visibleDocuments.map((item) => (
                  <div
                    key={item.id}
                    role="button"
                    tabIndex={0}
                    className={[
                      styles.treeItem,
                      !batchMode && selectedDocument?.id === item.id ? styles.active : '',
                      batchMode && selectedDocumentIdSet.has(item.id) ? styles.batchSelected : '',
                      draggingDocumentId === item.id ? styles.dragging : '',
                      dragOverDocumentId === item.id
                        ? dragOverPosition === 'after'
                          ? styles.dragOverAfter
                          : styles.dragOverBefore
                        : '',
                    ]
                      .filter(Boolean)
                      .join(' ')}
                    draggable={dragSortEnabled}
                    onClick={() => {
                      if (batchMode) {
                        toggleDocumentSelection(item.id)
                        return
                      }

                      setSelectedDocumentId(item.id)
                    }}
                    onKeyDown={(event) => handleTreeItemKeyDown(item.id, event)}
                    onDragStart={(event) => handleDragStart(event, item)}
                    onDragOver={(event) => handleDragOver(event, item)}
                    onDrop={(event) => handleDrop(event, item)}
                    onDragEnd={clearDragState}
                  >
                    <div className={styles.treeMain}>
                      <span className={styles.treeIndent} style={{ width: `${item.depth * 10}px` }} aria-hidden="true" />
                      {item.docType === 'FOLDER' ? (
                        <button
                          type="button"
                          className={styles.folderToggle}
                          onClick={(event) => toggleFolderExpanded(item.id, event)}
                        >
                          {expandedFolderIdSet.has(item.id) ? '▾' : '▸'}
                        </button>
                      ) : (
                        <span className={styles.folderTogglePlaceholder} />
                      )}
                      {batchMode && (
                        <input
                          type="checkbox"
                          className={styles.treeCheckbox}
                          checked={selectedDocumentIdSet.has(item.id)}
                          onChange={(event) => {
                            event.stopPropagation()
                            toggleDocumentSelection(item.id)
                          }}
                          onClick={(event) => event.stopPropagation()}
                        />
                      )}
                      <span
                        className={`${styles.treeNodeMark} ${item.docType === 'FOLDER' ? styles.treeNodeFolder : styles.treeNodeDoc}`}
                      />
                      <span className={styles.treeTitle}>{item.title}</span>
                    </div>
                    <span className={styles.treeItemTail} aria-hidden="true">
                      {!batchMode ? '›' : ''}
                    </span>
                  </div>
                ))
              ) : (
                <div className={styles.emptyTree}>当前筛选条件下没有匹配节点。</div>
              )}
            </div>
          </div>
        </aside>

        <div className={`${styles.documentColumn} ${focusMode ? styles.documentColumnFocus : ''}`}>
          {!focusMode && treePanelCollapsed && (
            <div className={styles.dockBar}>
              <button
                type="button"
                className={styles.smallButton}
                onClick={() => setTreePanelCollapsed(false)}
              >
                展开文档树
              </button>
            </div>
          )}
          {documents.length === 0 ? (
            <div className={`${styles.emptyPanel} ${styles.emptyDocumentPanel}`}>
              <h2>从第一篇文档开始</h2>
              <p>输入标题后会直接进入编辑。</p>
              <div className={styles.emptyDocumentActions}>
                <button
                  type="button"
                  className={styles.primaryButton}
                  disabled={!canWriteKnowledgeBase}
                  onClick={() => openCreateDocumentModal('DOC')}
                >
                  新建第一篇文档
                </button>
                <button
                  type="button"
                  className={styles.secondaryButton}
                  disabled={!canWriteKnowledgeBase}
                  onClick={() => openCreateDocumentModal('FOLDER')}
                >
                  新建目录
                </button>
              </div>
            </div>
          ) : selectedDocument ? (
            <section className={`${styles.documentCard} ${focusMode ? styles.documentCardFocus : ''}`}>
              <div
                className={[
                  styles.documentHeader,
                  selectedDocument.docType === 'DOC' ? styles.documentHeaderDoc : styles.documentHeaderFolder,
                ]
                  .filter(Boolean)
                  .join(' ')}
              >
                <div
                  className={[
                    styles.documentHeading,
                    selectedDocument.docType === 'DOC' ? styles.documentHeadingDoc : '',
                  ]
                    .filter(Boolean)
                    .join(' ')}
                >
                  {selectedDocument.docType === 'DOC' ? (
                    <>
                      <span className={styles.documentEyebrow}>当前文档</span>
                      <div className={styles.documentSubline}>
                        <span>v{selectedDocument.versionNo}</span>
                        {selectedDocument.updatedAt && <span>{dayjs(selectedDocument.updatedAt).format('MM-DD HH:mm')}</span>}
                        {selectedDocument.syncStatus && selectedDocument.syncStatus !== 'SYNCED' && (
                          <span>{STATUS_LABELS[selectedDocument.syncStatus] || selectedDocument.syncStatus}</span>
                        )}
                      </div>
                    </>
                  ) : (
                    <>
                      <div className={styles.documentEyebrow}>目录</div>
                      <div className={styles.documentSubline}>
                        <span>继续在这里整理内容</span>
                        {selectedDocument.updatedAt && <span>{dayjs(selectedDocument.updatedAt).format('MM-DD HH:mm')}</span>}
                      </div>
                    </>
                  )}
                </div>
                <div className={styles.documentToolbar}>
                  {selectedDocument.docType === 'DOC' ? (
                    <>
                      <button
                        type="button"
                        className={styles.primaryButton}
                        disabled={!canWriteKnowledgeBase}
                        onClick={handleOpenEditorPage}
                      >
                        继续编辑
                      </button>
                      <button
                        type="button"
                        className={styles.secondaryButton}
                        onClick={() => setShareOpen(true)}
                      >
                        分享文档
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        type="button"
                        className={styles.primaryButton}
                        disabled={!canWriteKnowledgeBase}
                        onClick={() => openCreateDocumentModal('DOC')}
                      >
                        新建文档
                      </button>
                      <button
                        type="button"
                        className={styles.secondaryButton}
                        disabled={!canWriteKnowledgeBase}
                        onClick={() => openCreateDocumentModal('FOLDER')}
                      >
                        新建目录
                      </button>
                    </>
                  )}
                  <details className={styles.inlineMoreActions}>
                    <summary className={styles.toolButton}>更多</summary>
                    <div className={styles.inlineMoreActionsMenu}>
                      <button
                        type="button"
                        className={styles.toolButton}
                        disabled={!canWriteKnowledgeBase}
                        onClick={openEditDocumentModal}
                      >
                        整理节点
                      </button>
                      {selectedDocument.docType === 'DOC' && (
                        <button
                          type="button"
                          className={styles.toolButton}
                          onClick={() => navigate(`/docs/${selectedDocument.id}`)}
                        >
                          阅读文档
                        </button>
                      )}
                      {selectedDocument.docType === 'DOC' && (
                        <button type="button" className={styles.toolButton} onClick={handleToggleFocusMode}>
                          {focusMode ? '退出专注' : '专注模式'}
                        </button>
                      )}
                      <button
                        type="button"
                        className={styles.toolButton}
                        disabled={!canMoveUp || dragSorting || !canWriteKnowledgeBase}
                        onClick={() => handleReorderDocument(-1)}
                      >
                        上移
                      </button>
                      <button
                        type="button"
                        className={styles.toolButton}
                        disabled={!canMoveDown || dragSorting || !canWriteKnowledgeBase}
                        onClick={() => handleReorderDocument(1)}
                      >
                        下移
                      </button>
                      <button
                        type="button"
                        className={styles.toolButtonDanger}
                        disabled={!canWriteKnowledgeBase}
                        onClick={handleDeleteDocument}
                      >
                        删除节点
                      </button>
                    </div>
                  </details>
                </div>
              </div>
              {focusMode && (
                <div className={styles.focusBanner}>
                  已进入专注模式，只保留当前文档正文。
                </div>
              )}
              <div className={styles.documentStage}>
                {selectedDocument.docType === 'FOLDER' ? (
                  <section className={styles.folderStage}>
                    <div className={styles.folderStageLabel}>当前目录</div>
                    <h3 className={styles.folderStageTitle}>{selectedDocument.title}</h3>
                    <div className={styles.folderStageMeta}>
                      <span className={styles.folderStageStat}>{selectedFolderDocumentCount} 篇文档</span>
                      <span className={styles.folderStageStat}>{selectedFolderDirectoryCount} 个目录</span>
                    </div>
                    <p className={styles.folderStageText}>目录本身不写正文，继续新建文档或目录就可以。</p>
                    <div className={styles.folderStageActions}>
                      <button
                        type="button"
                        className={styles.primaryButton}
                        disabled={!canWriteKnowledgeBase}
                        onClick={() => openCreateDocumentModal('DOC')}
                      >
                        新建文档
                      </button>
                      <button
                        type="button"
                        className={styles.secondaryButton}
                        disabled={!canWriteKnowledgeBase}
                        onClick={() => openCreateDocumentModal('FOLDER')}
                      >
                        新建目录
                      </button>
                    </div>
                  </section>
                ) : shouldRenderRichPreview ? (
                  <article className={`${styles.richPreview} ${focusMode ? styles.stageFocus : ''}`}>
                    <div className={`${styles.richPreviewBody} ${focusMode ? styles.richPreviewBodyFocus : ''}`}>
                      <div className={styles.documentPaperHeader}>
                        <h2 className={styles.documentTitle}>{selectedDocument.title}</h2>
                        <div className={styles.documentSubline}>
                          <span>v{selectedDocument.versionNo}</span>
                          {selectedDocument.updatedAt && <span>{dayjs(selectedDocument.updatedAt).format('MM-DD HH:mm')}</span>}
                          {selectedDocument.syncStatus && selectedDocument.syncStatus !== 'SYNCED' && (
                            <span>{STATUS_LABELS[selectedDocument.syncStatus] || selectedDocument.syncStatus}</span>
                          )}
                        </div>
                        {selectedDocument.summary ? (
                          <p className={styles.documentSummary}>{selectedDocument.summary}</p>
                        ) : null}
                      </div>
                      <div
                        className={styles.richPreviewContent}
                        dangerouslySetInnerHTML={{ __html: selectedDocument.content }}
                      />
                    </div>
                  </article>
                ) : (
                  <div className={`${styles.preview} ${focusMode ? styles.stageFocus : ''}`}>
                    <div className={styles.documentPaperHeader}>
                      <h2 className={styles.documentTitle}>{selectedDocument.title}</h2>
                      <div className={styles.documentSubline}>
                        <span>v{selectedDocument.versionNo}</span>
                        {selectedDocument.updatedAt && <span>{dayjs(selectedDocument.updatedAt).format('MM-DD HH:mm')}</span>}
                        {selectedDocument.syncStatus && selectedDocument.syncStatus !== 'SYNCED' && (
                          <span>{STATUS_LABELS[selectedDocument.syncStatus] || selectedDocument.syncStatus}</span>
                        )}
                      </div>
                      {selectedDocument.summary ? (
                        <p className={styles.documentSummary}>{selectedDocument.summary}</p>
                      ) : null}
                    </div>
                    <div className={styles.previewContent}>
                      {selectedDocument.contentText || selectedDocument.content || '当前节点是目录或尚未录入正文。'}
                    </div>
                  </div>
                )}
              </div>
            </section>
          ) : (
            <div className={styles.emptyPanel}>当前没有可展示的文档。</div>
          )}
        </div>
      </section>

      <KnowledgeBaseFormModal
        mode="edit"
        open={editing}
        initialValues={knowledgeBase}
        submitting={submitting}
        errorMessage={modalError}
        onClose={() => {
          setModalError('')
          setEditing(false)
        }}
        onSubmit={handleSaveKnowledgeBase}
      />
      <DocumentActionModal
        open={documentModalOpen}
        mode={documentModalMode}
        docType={documentModalType}
        initialValues={documentModalInitialValues}
        folderOptions={documentModalFolderOptions}
        submitting={documentSubmitting}
        errorMessage={documentModalError}
        onClose={() => {
          setDocumentModalError('')
          setDocumentModalOpen(false)
        }}
        onSubmit={handleSubmitDocument}
      />
      <DocumentBatchMoveModal
        open={batchMoveOpen}
        selectedCount={topLevelSelectedDocuments.length}
        folderOptions={batchFolderOptions}
        submitting={batchMoveSubmitting}
        errorMessage={batchMoveError}
        initialParentId={batchMoveInitialParentId}
        onClose={() => {
          setBatchMoveError('')
          setBatchMoveOpen(false)
        }}
        onSubmit={handleBatchMove}
      />
      <DocumentShareDrawer
        open={shareOpen}
        documentId={selectedDocument?.docType === 'DOC' ? selectedDocument.id : null}
        title={selectedDocument?.title || ''}
        onClose={() => setShareOpen(false)}
      />
      <KnowledgeBasePermissionModal
        open={permissionModalOpen}
        loading={permissionLoading}
        members={permissionMembers}
        currentMembers={permissionAssignments}
        currentUserId={currentUser.id}
        submitting={permissionSubmitting}
        errorMessage={permissionError}
        onClose={() => {
          setPermissionError('')
          setPermissionModalOpen(false)
        }}
        onSubmit={handleSubmitPermissions}
      />
    </div>
  )
}

export default KnowledgeBaseDetail

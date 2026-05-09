import { useCallback, useEffect, useState } from 'react'
import dayjs from 'dayjs'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import DocumentActionModal from '../../components/Document/DocumentActionModal'
import DocumentBatchMoveModal from '../../components/Document/DocumentBatchMoveModal'
import DocumentReadLinkDrawer from '../../components/Document/DocumentReadLinkDrawer'
import DocumentShareDrawer from '../../components/Document/DocumentShareDrawer'
import PageState from '../../components/Feedback/PageState'
import KnowledgeBaseContextPanel from '../../components/KnowledgeBase/KnowledgeBaseContextPanel'
import KnowledgeBaseFormModal from '../../components/KnowledgeBase/KnowledgeBaseFormModal'
import KnowledgeBaseDocumentPanel from '../../components/KnowledgeBase/KnowledgeBaseDocumentPanel'
import KnowledgeBasePermissionModal from '../../components/KnowledgeBase/KnowledgeBasePermissionModal'
import KnowledgeBaseTreePanel from '../../components/KnowledgeBase/KnowledgeBaseTreePanel'
import TrashListModal from '../../components/KnowledgeBase/TrashListModal'
import { PAGE_STATUS, useKnowledgeBaseDetailController } from '../../hooks/useKnowledgeBaseDetailController'
import { auditApi } from '../../services/api/auditApi'
import styles from './KnowledgeBaseDetail.module.css'

const ROLE_LABELS = {
  OWNER: '所有者',
  ADMIN: '管理员',
  EDITOR: '编辑者',
  REVIEWER: '审核者',
  VIEWER: '只读',
}

const KnowledgeBaseDetail = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { currentUser } = useAuth()
  const [knowledgeBaseAuditEvents, setKnowledgeBaseAuditEvents] = useState([])
  const [knowledgeBaseAuditLoading, setKnowledgeBaseAuditLoading] = useState(false)
  const [knowledgeBaseAuditError, setKnowledgeBaseAuditError] = useState('')
  const [documentAuditEvents, setDocumentAuditEvents] = useState([])
  const [documentAuditLoading, setDocumentAuditLoading] = useState(false)
  const [documentAuditError, setDocumentAuditError] = useState('')
  const [shareDrawerOpen, setShareDrawerOpen] = useState(false)
  const controller = useKnowledgeBaseDetailController({
    id,
    currentUser,
    navigate,
    locationState: location.state,
  })

  const {
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
    feedback,
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
    scrolled,
    knowledgeBaseInfoVisible,
    setKnowledgeBaseInfoVisible,
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
  } = controller

  const loadKnowledgeBaseAuditEvents = useCallback(async () => {
    if (!knowledgeBase?.id || !canManageKnowledgeBase) {
      setKnowledgeBaseAuditEvents([])
      setKnowledgeBaseAuditError('')
      return
    }

    try {
      setKnowledgeBaseAuditLoading(true)
      setKnowledgeBaseAuditError('')
      const response = await auditApi.listAuditLogs({
        knowledgeBaseId: knowledgeBase.id,
        size: 6,
      })
      setKnowledgeBaseAuditEvents(response?.data?.records || [])
    } catch (error) {
      console.error('加载知识库审计记录失败', error)
      setKnowledgeBaseAuditEvents([])
      setKnowledgeBaseAuditError(error?.message || '加载知识库审计记录失败，请稍后重试')
    } finally {
      setKnowledgeBaseAuditLoading(false)
    }
  }, [canManageKnowledgeBase, knowledgeBase?.id])

  const loadDocumentAuditEvents = useCallback(async () => {
    if (!selectedDocument?.id || !canManageKnowledgeBase) {
      setDocumentAuditEvents([])
      setDocumentAuditError('')
      return
    }

    try {
      setDocumentAuditLoading(true)
      setDocumentAuditError('')
      const response = await auditApi.listAuditLogs({
        objectType: 'DOCUMENT',
        objectId: selectedDocument.id,
        size: 6,
      })
      setDocumentAuditEvents(response?.data?.records || [])
    } catch (error) {
      console.error('加载节点审计记录失败', error)
      setDocumentAuditEvents([])
      setDocumentAuditError(error?.message || '加载节点审计记录失败，请稍后重试')
    } finally {
      setDocumentAuditLoading(false)
    }
  }, [canManageKnowledgeBase, selectedDocument?.id])

  useEffect(() => {
    loadKnowledgeBaseAuditEvents()
  }, [documents, feedback?.message, knowledgeBase?.updatedAt, loadKnowledgeBaseAuditEvents])

  useEffect(() => {
    loadDocumentAuditEvents()
  }, [feedback?.message, loadDocumentAuditEvents, selectedDocument?.id, selectedDocument?.updatedAt])

  if (pageStatus === PAGE_STATUS.LOADING) {
    return <div className={styles.state}>正在加载知识库...</div>
  }

  if (pageStatus !== PAGE_STATUS.READY || !knowledgeBase) {
    return (
      <PageState
        eyebrow={pageStatus === PAGE_STATUS.FORBIDDEN ? '无权访问' : pageStatus === PAGE_STATUS.NOT_FOUND ? '内容不存在' : '知识库不可用'}
        title={pageStatus === PAGE_STATUS.FORBIDDEN ? '当前会话无权访问该知识库' : pageStatus === PAGE_STATUS.NOT_FOUND ? '当前知识库不存在' : '知识库详情暂时不可用'}
        description={pageErrorMessage || '请返回工作台查看当前可访问的知识库。'}
        primaryAction={{ label: '返回工作台', onClick: () => navigate('/') }}
        secondaryAction={{ label: '重新加载', onClick: () => loadData({ resetState: true }) }}
      />
    )
  }

  return (
    <div className={`${styles.page} ${scrolled ? styles.pageScrolled : ''}`}>
      <KnowledgeBaseTreePanel
        styles={styles}
        feedback={feedback}
        knowledgeBase={knowledgeBase}
        roleLabels={ROLE_LABELS}
        compactKnowledgeBaseDescription={compactKnowledgeBaseDescription}
        knowledgeBaseInfoVisible={knowledgeBaseInfoVisible}
        setKnowledgeBaseInfoVisible={setKnowledgeBaseInfoVisible}
        canWriteKnowledgeBase={canWriteKnowledgeBase}
        canManageKnowledgeBase={canManageKnowledgeBase}
        setModalError={setModalError}
        setEditing={setEditing}
        openPermissionModal={openPermissionModal}
        openDocumentTrash={openDocumentTrash}
        handleDeleteKnowledgeBase={handleDeleteKnowledgeBase}
        focusMode={focusMode}
        treePanelCollapsed={treePanelCollapsed}
        scrolled={scrolled}
        treePanelStatusText={treePanelStatusText}
        search={search}
        setSearch={setSearch}
        hasFolderNodes={hasFolderNodes}
        batchMode={batchMode}
        hasActiveSearch={hasActiveSearch}
        treeHintText={treeHintText}
        selectedDocumentIds={selectedDocumentIds}
        batchDeleting={batchDeleting}
        handleBatchDelete={handleBatchDelete}
        clearBatchSelection={clearBatchSelection}
        setBatchMoveError={setBatchMoveError}
        setBatchMoveOpen={setBatchMoveOpen}
        documents={documents}
        visibleDocuments={visibleDocuments}
        selectedDocument={selectedDocument}
        selectedDocumentIdSet={selectedDocumentIdSet}
        draggingDocumentId={draggingDocumentId}
        dragOverDocumentId={dragOverDocumentId}
        dragOverPosition={dragOverPosition}
        dragSortEnabled={dragSortEnabled}
        handleToggleBatchMode={handleToggleBatchMode}
        setTreePanelCollapsed={setTreePanelCollapsed}
        openCreateDocumentModal={openCreateDocumentModal}
        expandAllFolders={expandAllFolders}
        collapseToTopLevelFolders={collapseToTopLevelFolders}
        handleTreeItemKeyDown={handleTreeItemKeyDown}
        handleDragStart={handleDragStart}
        handleDragOver={handleDragOver}
        handleDrop={handleDrop}
        clearDragState={clearDragState}
        toggleDocumentSelection={toggleDocumentSelection}
        setSelectedDocumentId={setSelectedDocumentId}
        toggleFolderExpanded={toggleFolderExpanded}
        expandedFolderIdSet={expandedFolderIdSet}
      />

      <section
        className={[
          styles.contentGrid,
          focusMode ? styles.contentGridFocus : '',
          !focusMode && treePanelCollapsed ? styles.contentGridNoLeft : '',
        ]
          .filter(Boolean)
          .join(' ')}
      >
        <KnowledgeBaseDocumentPanel
          styles={styles}
          focusMode={focusMode}
          treePanelCollapsed={treePanelCollapsed}
          setTreePanelCollapsed={setTreePanelCollapsed}
          documents={documents}
          selectedDocument={selectedDocument}
          canWriteKnowledgeBase={canWriteKnowledgeBase}
          canManageKnowledgeBase={canManageKnowledgeBase}
          handleOpenEditorPage={handleOpenEditorPage}
          setReadLinkOpen={setReadLinkOpen}
          setShareDrawerOpen={setShareDrawerOpen}
          openCreateDocumentModal={openCreateDocumentModal}
          openEditDocumentModal={openEditDocumentModal}
          navigate={navigate}
          handleToggleFocusMode={handleToggleFocusMode}
          canMoveUp={canMoveUp}
          dragSorting={dragSorting}
          handleReorderDocument={handleReorderDocument}
          canMoveDown={canMoveDown}
          handleDeleteDocument={handleDeleteDocument}
          selectedFolderDocumentCount={selectedFolderDocumentCount}
          selectedFolderDirectoryCount={selectedFolderDirectoryCount}
          shouldRenderRichPreview={shouldRenderRichPreview}
          safeSelectedDocumentContent={safeSelectedDocumentContent}
        />
        {!focusMode && (
          <KnowledgeBaseContextPanel
            styles={styles}
            knowledgeBase={knowledgeBase}
            documents={documents}
            selectedDocument={selectedDocument}
            canWriteKnowledgeBase={canWriteKnowledgeBase}
            canManageKnowledgeBase={canManageKnowledgeBase}
            roleLabels={ROLE_LABELS}
            knowledgeBaseAuditEvents={knowledgeBaseAuditEvents}
            knowledgeBaseAuditLoading={knowledgeBaseAuditLoading}
            knowledgeBaseAuditError={knowledgeBaseAuditError}
            documentAuditEvents={documentAuditEvents}
            documentAuditLoading={documentAuditLoading}
            documentAuditError={documentAuditError}
          />
        )}
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
      <DocumentReadLinkDrawer
        open={readLinkOpen}
        documentId={selectedDocument?.docType === 'DOC' ? selectedDocument.id : null}
        title={selectedDocument?.title || ''}
        onClose={() => setReadLinkOpen(false)}
      />
      <DocumentShareDrawer
        open={shareDrawerOpen && selectedDocument?.docType === 'DOC'}
        documentId={selectedDocument?.docType === 'DOC' ? selectedDocument.id : null}
        title={selectedDocument?.title || ''}
        onClose={() => setShareDrawerOpen(false)}
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

      <TrashListModal
        open={documentTrashOpen}
        eyebrow="文档回收站"
        title={knowledgeBase.name}
        description="这里保留当前知识库已删除的文档和目录。恢复时仍会校验父级目录和知识库状态。"
        items={deletedDocuments}
        loading={documentTrashLoading}
        errorMessage={documentTrashError}
        restoringItemId={restoringDocumentId}
        emptyTitle="当前知识库回收站为空"
        emptyDescription="删除后的文档或目录会暂存到这里，便于继续恢复主链路。"
        onClose={() => {
          setDocumentTrashError('')
          setDocumentTrashOpen(false)
        }}
        onRestore={(item) => handleRestoreDocument(item.id)}
        getItemTitle={(item) => item.title}
        getItemDescription={(item) => `${item.docType === 'FOLDER' ? '目录' : '文档'} · ${item.path || '未记录路径'}`}
        getItemMeta={(item) => [
          item.deletedAt ? `删除于 ${dayjs(item.deletedAt).format('MM-DD HH:mm')}` : '',
          item.deletedBy ? `删除人 #${item.deletedBy}` : '',
          item.versionNo ? `当前版本 ${item.versionNo}` : '',
        ]}
      />
    </div>
  )
}

export default KnowledgeBaseDetail

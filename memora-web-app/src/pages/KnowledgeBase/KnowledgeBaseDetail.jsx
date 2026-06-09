import { useCallback, useEffect, useState } from 'react'
import dayjs from 'dayjs'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import { prefetchEditor, prefetchReader } from '../../router/prefetch'
import DocumentActionModal from '../../components/Document/DocumentActionModal'
import DocumentBatchMoveModal from '../../components/Document/DocumentBatchMoveModal'
import DocumentReadLinkDrawer from '../../components/Document/DocumentReadLinkDrawer'
import DocumentShareDrawer from '../../components/Document/DocumentShareDrawer'
import PageState from '../../components/Feedback/PageState'
import { KnowledgeBaseSkeleton } from '../../components/Feedback/Skeleton'
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
  } = controller

  useDocumentTitle(knowledgeBase?.name ? `${knowledgeBase.name}` : '知识库')
  const currentRoleLabel = ROLE_LABELS[knowledgeBase?.currentRole] || knowledgeBase?.currentRole || '未知角色'
  const folderCount = documents.filter((item) => item.docType === 'FOLDER').length
  const documentCount = documents.filter((item) => item.docType === 'DOC').length
  const knowledgeBaseSiteUrl = knowledgeBase?.siteUrl
    || (knowledgeBase?.siteEnabled && knowledgeBase?.siteSlug ? `/site/${knowledgeBase.siteSlug}` : '')
  const selectedContextLabel = selectedDocument
    ? selectedDocument.docType === 'DOC' ? '当前文档' : '当前目录'
    : '知识库视图'
  const selectedContextHint = !selectedDocument
    ? '从左侧文档树继续选择一个目录或文档。'
    : selectedDocument.docType === 'DOC'
      ? `${selectedDocument.title}${selectedDocument.publishStatus === 'PUBLISHED' ? '，已正式发布。' : '，尚未正式发布。'}`
      : `${selectedDocument.title}，包含 ${selectedFolderDocumentCount} 篇文档和 ${selectedFolderDirectoryCount} 个目录。`

  useEffect(() => {
    if (pageStatus === 'ready') {
      prefetchEditor()
      prefetchReader()
    }
  }, [pageStatus])

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
  }, [documents, knowledgeBase?.updatedAt, loadKnowledgeBaseAuditEvents])

  useEffect(() => {
    loadDocumentAuditEvents()
  }, [loadDocumentAuditEvents, selectedDocument?.id, selectedDocument?.updatedAt])

  if (pageStatus === PAGE_STATUS.LOADING) {
    return <KnowledgeBaseSkeleton />
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
    <div className={styles.page}>
      <section
        className={[
          styles.workspaceLayout,
          focusMode ? styles.workspaceLayoutSingle : '',
          !focusMode && treePanelCollapsed ? styles.workspaceLayoutSingle : '',
        ]
          .filter(Boolean)
          .join(' ')}
      >
        <KnowledgeBaseTreePanel
          styles={styles}
          canWriteKnowledgeBase={canWriteKnowledgeBase}
          focusMode={focusMode}
          treePanelCollapsed={treePanelCollapsed}
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
          openEditDocumentModal={openEditDocumentModal}
          handleDeleteDocument={handleDeleteDocument}
          handleOpenEditorPage={handleOpenEditorPage}
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

        <div className={styles.workspaceMain}>
          <header className={styles.hero}>
            <div className={styles.heroMain}>
              <div className={styles.breadcrumb}>
                <Link to="/">工作台</Link>
                <span>/</span>
                <span>知识库</span>
              </div>
              <div className={styles.heroTitleRow}>
                <h1 className={styles.title}>{knowledgeBase.name}</h1>
                <span className={focusMode ? styles.modeBadgeActive : styles.modeBadge}>
                  {focusMode ? '专注模式' : selectedContextLabel}
                </span>
              </div>
              {knowledgeBaseInfoVisible && compactKnowledgeBaseDescription ? (
                <p className={styles.heroDescription}>{compactKnowledgeBaseDescription}</p>
              ) : null}
              <p className={styles.heroHint}>{selectedContextHint}</p>
              <div className={styles.heroMeta}>
                <span className={styles.metaPill}>{documents.length} 个节点</span>
                <span className={styles.metaPill}>{documentCount} 篇文档</span>
                <span className={styles.metaPill}>{folderCount} 个目录</span>
                <span className={styles.metaPill}>{currentRoleLabel}</span>
                {knowledgeBase.permissionRestricted ? <span className={styles.metaPill}>独立权限</span> : null}
                {knowledgeBase.siteEnabled ? <span className={styles.metaPill}>已开放站点</span> : null}
              </div>
            </div>
            <div className={styles.heroActions}>
              <div className={styles.heroActionGrid}>
                {knowledgeBaseSiteUrl ? (
                  <a
                    className={styles.secondaryButton}
                    href={knowledgeBaseSiteUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    打开站点
                  </a>
                ) : null}
                {canWriteKnowledgeBase ? (
                  <button
                    type="button"
                    className={styles.primaryButton}
                    onClick={() => openCreateDocumentModal('DOC')}
                  >
                    新建文档
                  </button>
                ) : null}
                <details className={styles.moreActions}>
                  <summary className={styles.secondaryButton}>更多操作</summary>
                  <div className={styles.moreActionsMenu}>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      disabled={!canWriteKnowledgeBase}
                      onClick={() => openCreateDocumentModal('FOLDER')}
                    >
                      新建目录
                    </button>
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      onClick={() => setKnowledgeBaseInfoVisible((current) => !current)}
                    >
                      {knowledgeBaseInfoVisible ? '收起说明' : '显示说明'}
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
                      className={styles.secondaryButton}
                      disabled={!canWriteKnowledgeBase}
                      onClick={openDocumentTrash}
                    >
                      文档回收站
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
              knowledgeBase={knowledgeBase}
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
                siteSubmitting={siteSubmitting}
                siteError={siteError}
                setSiteError={setSiteError}
                onSaveSiteSettings={handleSaveSiteSettings}
                documentPublishingSubmitting={documentPublishingSubmitting}
                documentPublishingError={documentPublishingError}
                setDocumentPublishingError={setDocumentPublishingError}
                onPublishDocument={handlePublishDocument}
                onUnpublishDocument={handleUnpublishDocument}
              />
            )}
          </section>
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
        description="已删除的文档和目录"
        items={deletedDocuments}
        loading={documentTrashLoading}
        errorMessage={documentTrashError}
        restoringItemId={restoringDocumentId}
        emptyTitle="当前知识库回收站为空"
        emptyDescription="暂无已删除内容"
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

import { Link } from 'react-router-dom'

const KnowledgeBaseTreePanel = ({
  styles,
  feedback,
  knowledgeBase,
  roleLabels,
  compactKnowledgeBaseDescription,
  knowledgeBaseInfoVisible,
  setKnowledgeBaseInfoVisible,
  canWriteKnowledgeBase,
  canManageKnowledgeBase,
  setModalError,
  setEditing,
  openPermissionModal,
  openDocumentTrash,
  handleDeleteKnowledgeBase,
  focusMode,
  treePanelCollapsed,
  scrolled,
  treePanelStatusText,
  search,
  setSearch,
  hasFolderNodes,
  batchMode,
  hasActiveSearch,
  treeHintText,
  selectedDocumentIds,
  batchDeleting,
  handleBatchDelete,
  clearBatchSelection,
  setBatchMoveError,
  setBatchMoveOpen,
  documents,
  visibleDocuments,
  selectedDocument,
  selectedDocumentIdSet,
  draggingDocumentId,
  dragOverDocumentId,
  dragOverPosition,
  dragSortEnabled,
  handleToggleBatchMode,
  setTreePanelCollapsed,
  openCreateDocumentModal,
  expandAllFolders,
  collapseToTopLevelFolders,
  handleTreeItemKeyDown,
  handleDragStart,
  handleDragOver,
  handleDrop,
  clearDragState,
  toggleDocumentSelection,
  setSelectedDocumentId,
  toggleFolderExpanded,
  expandedFolderIdSet,
}) => {
  const currentRoleLabel = roleLabels[knowledgeBase.currentRole] || knowledgeBase.currentRole || '未知角色'
  const roleBoundaryMessage = !canWriteKnowledgeBase
    ? `当前角色为${currentRoleLabel}，只能阅读和搜索当前知识库，不能新建、移动或删除节点。`
    : canManageKnowledgeBase
      ? `当前角色为${currentRoleLabel}，可编辑文档，并可继续管理知识库设置、成员权限和回收站。`
      : `当前角色为${currentRoleLabel}，可编辑文档和整理目录，但不能修改知识库设置或成员权限。`

  return (
    <>
      <header className={`${styles.hero} ${scrolled ? styles.heroScrolled : ''}`}>
        <div className={styles.heroMain}>
          <div className={styles.breadcrumb}>
            <Link to="/">工作台</Link>
            <span>/</span>
            <span>{knowledgeBase.name}</span>
          </div>
          <div className={styles.heroTitleRow}>
            <h1 className={styles.title}>{knowledgeBase.name}</h1>
          </div>
          <div className={styles.heroMeta}>
            <span className={styles.metaPill}>文档工作区</span>
            <span className={styles.metaPill}>{knowledgeBase.documentCount} 个节点</span>
            <span className={styles.metaPill}>{currentRoleLabel}</span>
            {knowledgeBase.permissionRestricted && <span className={styles.metaPill}>独立权限</span>}
          </div>
          {knowledgeBaseInfoVisible && (
            <p className={styles.heroDescription}>
              {compactKnowledgeBaseDescription || '当前知识库用于承载文档协作和目录整理，主流程仍以继续写作和阅读为主。'}
            </p>
          )}
          <div className={`${styles.heroNotice} ${!canWriteKnowledgeBase ? styles.heroNoticeReadonly : ''}`}>
            {roleBoundaryMessage}
          </div>
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
                <button
                  type="button"
                  className={styles.toolButton}
                  disabled={!canWriteKnowledgeBase}
                  onClick={openDocumentTrash}
                >
                  文档回收站
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
            placeholder="按标题筛选当前知识库"
          />
        </div>
        <div className={styles.treeModeBar}>
          <div className={styles.treeModeMeta}>
            <span className={`${styles.modeBadge} ${batchMode ? styles.modeBadgeActive : ''}`}>
              {batchMode ? '批量整理中' : hasActiveSearch ? '标题筛选结果' : '全部内容'}
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
        <div className={styles.treeHint}>{treeHintText}</div>
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
    </>
  )
}

export default KnowledgeBaseTreePanel

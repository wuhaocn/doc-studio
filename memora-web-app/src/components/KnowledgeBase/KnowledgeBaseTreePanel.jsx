import { useEffect, useMemo, useState } from 'react'

const KnowledgeBaseTreePanel = ({
  styles,
  canWriteKnowledgeBase,
  focusMode,
  treePanelCollapsed,
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
  openEditDocumentModal,
  handleDeleteDocument,
  handleOpenEditorPage,
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
  const [contextMenu, setContextMenu] = useState(null)
  const contextMenuItem = useMemo(
    () => documents.find((item) => item.id === contextMenu?.itemId) || null,
    [contextMenu?.itemId, documents]
  )

  useEffect(() => {
    if (!contextMenu) {
      return undefined
    }

    const closeContextMenu = () => setContextMenu(null)
    window.addEventListener('click', closeContextMenu)
    window.addEventListener('blur', closeContextMenu)
    window.addEventListener('scroll', closeContextMenu, true)

    return () => {
      window.removeEventListener('click', closeContextMenu)
      window.removeEventListener('blur', closeContextMenu)
      window.removeEventListener('scroll', closeContextMenu, true)
    }
  }, [contextMenu])

  useEffect(() => {
    if (!canWriteKnowledgeBase || batchMode || focusMode) {
      setContextMenu(null)
    }
  }, [batchMode, canWriteKnowledgeBase, focusMode])

  const openContextMenu = (event, item) => {
    if (!canWriteKnowledgeBase || batchMode) {
      return
    }

    event.preventDefault()
    setSelectedDocumentId(item.id)
    setContextMenu({
      itemId: item.id,
      x: Math.min(event.clientX, window.innerWidth - 220),
      y: Math.min(event.clientY, window.innerHeight - 220),
    })
  }

  const runContextAction = (action) => {
    if (!contextMenuItem) {
      return
    }
    action(contextMenuItem)
    setContextMenu(null)
  }

  return (
    <aside
      className={[
        styles.treePanel,
        focusMode ? styles.focusHidden : '',
        !focusMode && treePanelCollapsed ? styles.treePanelCollapsed : '',
      ]
        .filter(Boolean)
        .join(' ')}
    >
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
                        : dragOverPosition === 'inside'
                          ? styles.dragOverInside
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
                  onContextMenu={(event) => openContextMenu(event, item)}
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
        {contextMenu && contextMenuItem ? (
          <div
            className={styles.contextMenu}
            style={{
              left: `${contextMenu.x}px`,
              top: `${contextMenu.y}px`,
            }}
            onClick={(event) => event.stopPropagation()}
          >
            {contextMenuItem.docType === 'FOLDER' ? (
              <>
                <button
                  type="button"
                  className={styles.contextMenuItem}
                  onClick={() => runContextAction((item) => openCreateDocumentModal('DOC', item))}
                >
                  新建文档
                </button>
                <button
                  type="button"
                  className={styles.contextMenuItem}
                  onClick={() => runContextAction((item) => openCreateDocumentModal('FOLDER', item))}
                >
                  新建目录
                </button>
              </>
            ) : (
              <button
                type="button"
                className={styles.contextMenuItem}
                onClick={() => runContextAction((item) => handleOpenEditorPage(item))}
              >
                继续编辑
              </button>
            )}
            <button
              type="button"
              className={styles.contextMenuItem}
              onClick={() => runContextAction((item) => openEditDocumentModal(item))}
            >
              重命名与移动
            </button>
            <button
              type="button"
              className={`${styles.contextMenuItem} ${styles.contextMenuItemDanger}`}
              onClick={() => runContextAction((item) => handleDeleteDocument(item))}
            >
              删除
            </button>
          </div>
        ) : null}
      </aside>
  )
}

export default KnowledgeBaseTreePanel

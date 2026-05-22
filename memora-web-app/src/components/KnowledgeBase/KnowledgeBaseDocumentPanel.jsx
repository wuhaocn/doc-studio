import dayjs from 'dayjs'
import DocumentEntryCard from '../Document/DocumentEntryCard'
import DocumentRenderedContent from '../Document/DocumentRenderedContent'

const KnowledgeBaseDocumentPanel = ({
  styles,
  focusMode,
  treePanelCollapsed,
  setTreePanelCollapsed,
  documents,
  selectedDocument,
  canWriteKnowledgeBase,
  canManageKnowledgeBase,
  handleOpenEditorPage,
  setReadLinkOpen,
  setShareDrawerOpen,
  openCreateDocumentModal,
  openEditDocumentModal,
  navigate,
  handleToggleFocusMode,
  canMoveUp,
  dragSorting,
  handleReorderDocument,
  canMoveDown,
  handleDeleteDocument,
  selectedFolderDocumentCount,
  selectedFolderDirectoryCount,
  shouldRenderRichPreview,
  safeSelectedDocumentContent,
  knowledgeBase,
}) => {
  const selectedDocumentPublicUrl = selectedDocument?.publicUrl
    || (knowledgeBase?.siteUrl && selectedDocument?.publicSlug ? `${knowledgeBase.siteUrl}/${selectedDocument.publicSlug}` : '')

  return (
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
                    {selectedDocument.publishStatus === 'PUBLISHED' ? <span>已发布</span> : null}
                  </div>
                </>
              ) : (
                <>
                  <div className={styles.documentEyebrow}>目录</div>
                  <div className={styles.documentSubline}>
                    <span>目录内容</span>
                    {selectedDocument.updatedAt && <span>{dayjs(selectedDocument.updatedAt).format('MM-DD HH:mm')}</span>}
                  </div>
                </>
              )}
            </div>
            <div className={styles.documentToolbar}>
              {selectedDocument.docType === 'DOC' ? (
                <button
                  type="button"
                  className={styles.primaryButton}
                  disabled={!canWriteKnowledgeBase}
                  onClick={handleOpenEditorPage}
                >
                  继续编辑
                </button>
              ) : (
                <button
                  type="button"
                  className={styles.primaryButton}
                  disabled={!canWriteKnowledgeBase}
                  onClick={() => openCreateDocumentModal('DOC')}
                >
                  新建文档
                </button>
              )}
              <details className={styles.inlineMoreActions}>
                <summary className={styles.toolButton}>更多操作</summary>
                <div className={styles.inlineMoreActionsMenu}>
                  {selectedDocument.docType === 'DOC' ? (
                    <>
                      <button
                        type="button"
                        className={styles.toolButton}
                        onClick={() => navigate(`/docs/${selectedDocument.id}`)}
                      >
                        阅读文档
                      </button>
                      {selectedDocumentPublicUrl ? (
                        <button
                          type="button"
                          className={styles.toolButton}
                          onClick={() => window.open(selectedDocumentPublicUrl, '_blank', 'noopener,noreferrer')}
                        >
                          打开公开页
                        </button>
                      ) : null}
                      <button
                        type="button"
                        className={styles.toolButton}
                        onClick={() => setReadLinkOpen(true)}
                      >
                        复制阅读链接
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      className={styles.toolButton}
                      disabled={!canWriteKnowledgeBase}
                      onClick={() => openCreateDocumentModal('FOLDER')}
                    >
                      新建目录
                    </button>
                  )}
                  <button
                    type="button"
                    className={styles.toolButton}
                    disabled={!canWriteKnowledgeBase}
                    onClick={openEditDocumentModal}
                  >
                    整理节点
                  </button>
                  {selectedDocument.docType === 'DOC' && canManageKnowledgeBase ? (
                    <button
                      type="button"
                      className={styles.toolButton}
                      onClick={() => setShareDrawerOpen(true)}
                    >
                      受控分享
                    </button>
                  ) : null}
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
                  <DocumentEntryCard
                    className={styles.folderActionCard}
                    eyebrow="正文入口"
                    title="新建文档"
                    summary="在当前目录下开始写一篇正文，继续沉淀内容。"
                    details={<div className={styles.folderActionMeta}>保存后会直接进入独立编辑页。</div>}
                    actions={(
                      <button
                        type="button"
                        className={styles.primaryButton}
                        disabled={!canWriteKnowledgeBase}
                        onClick={() => openCreateDocumentModal('DOC')}
                      >
                        新建文档
                      </button>
                    )}
                  />
                  <DocumentEntryCard
                    className={styles.folderActionCard}
                    eyebrow="结构入口"
                    title="新建目录"
                    summary="继续整理结构，把后续文档收进更清晰的层级。"
                    details={<div className={styles.folderActionMeta}>适合先搭目录，再逐步补正文。</div>}
                    actions={(
                      <button
                        type="button"
                        className={styles.secondaryButton}
                        disabled={!canWriteKnowledgeBase}
                        onClick={() => openCreateDocumentModal('FOLDER')}
                      >
                        新建目录
                      </button>
                    )}
                  />
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
                    </div>
                    {selectedDocument.summary ? (
                      <p className={styles.documentSummary}>{selectedDocument.summary}</p>
                    ) : null}
                  </div>
                  <DocumentRenderedContent
                    className={styles.richPreviewContent}
                    html={safeSelectedDocumentContent}
                    variant="article"
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
  )
}

export default KnowledgeBaseDocumentPanel

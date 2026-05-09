import AuditEventList from '../Audit/AuditEventList'

const KnowledgeBaseContextPanel = ({
  styles,
  knowledgeBase,
  documents,
  selectedDocument,
  canWriteKnowledgeBase,
  canManageKnowledgeBase,
  roleLabels,
  knowledgeBaseAuditEvents,
  knowledgeBaseAuditLoading,
  knowledgeBaseAuditError,
  documentAuditEvents,
  documentAuditLoading,
  documentAuditError,
}) => {
  const folderCount = documents.filter((item) => item.docType === 'FOLDER').length
  const documentCount = documents.filter((item) => item.docType === 'DOC').length
  const capabilitySummary = [
    canWriteKnowledgeBase ? '可编辑' : '只读访问',
    canManageKnowledgeBase ? '可管理知识库' : '无管理权限',
  ].join(' · ')
  const currentRoleLabel = roleLabels?.[knowledgeBase.currentRole] || knowledgeBase.currentRole || '未知角色'
  const selectedDocumentTypeLabel = selectedDocument
    ? selectedDocument.docType === 'FOLDER'
      ? '目录'
      : '文档'
    : '未选择'
  const permissionBoundaryTitle = canManageKnowledgeBase
    ? '当前角色可继续管理这个知识库'
    : canWriteKnowledgeBase
      ? '当前角色可以继续编辑，但不能管理权限'
      : '当前角色处于只读模式'
  const permissionBoundaryMessage = canManageKnowledgeBase
    ? '你可以创建、移动、删除文档，也可以修改知识库设置、成员权限并查看回收站。'
    : canWriteKnowledgeBase
      ? '你可以创建、编辑、移动和删除文档，但不能修改知识库设置，也不能调整成员权限。'
      : '你只能阅读和搜索当前知识库内容。新建、移动、删除、回收站和权限配置入口都会保持不可用。'

  return (
    <aside className={styles.contextPanel}>
      <details className={styles.contextDisclosure} open>
        <summary className={styles.contextDisclosureSummary}>
          <div className={styles.contextSummaryMain}>
            <span className={styles.contextSummaryEyebrow}>知识库上下文</span>
            <strong className={styles.contextSummaryTitle}>当前工作区以阅读、编辑和目录整理为主</strong>
            <span className={styles.contextSummaryMeta}>{capabilitySummary}</span>
            <span className={styles.contextSummaryHint}>
              {knowledgeBase.description?.trim() || '当前知识库还没有补充说明，可在知识库设置中完善描述。'}
            </span>
          </div>
          <span className={styles.contextBadge}>{currentRoleLabel}</span>
        </summary>

        <div className={styles.contextCard}>
          <div className={styles.panelHeader}>
            <div>
              <h2>当前概览</h2>
              <span className={styles.panelHint}>这里展示知识库边界、内容规模和当前节点信息。</span>
            </div>
          </div>

          <div className={styles.contextList}>
            <article className={styles.contextItem}>
              <div className={styles.contextItemTop}>
                <span>知识库信息</span>
                <span className={styles.contextBadge}>{knowledgeBase.permissionRestricted ? '独立权限' : '继承租户权限'}</span>
              </div>
              <div className={styles.contextMessage}>{knowledgeBase.name}</div>
              <div className={styles.contextMeta}>
                <span className={styles.metaPill}>{documentCount} 篇文档</span>
                <span className={styles.metaPill}>{folderCount} 个目录</span>
              </div>
            </article>
            <article className={styles.contextItem}>
              <div className={styles.contextItemTop}>
                <span>当前节点</span>
                <span className={styles.contextBadge}>{selectedDocumentTypeLabel}</span>
              </div>
              <div className={styles.contextMessage}>
                {selectedDocument?.title || '当前还没有选中文档节点。'}
              </div>
              <div className={styles.contextMeta}>
                <span className={styles.metaPill}>{canWriteKnowledgeBase ? '允许编辑' : '仅允许阅读'}</span>
                <span className={styles.metaPill}>{canManageKnowledgeBase ? '允许管理' : '不可管理'}</span>
              </div>
            </article>
            <article className={styles.contextItem}>
              <div className={styles.contextItemTop}>
                <span>权限边界</span>
                <span className={styles.contextBadge}>{canWriteKnowledgeBase ? '可写' : '只读'}</span>
              </div>
              <div className={styles.contextMessage}>{permissionBoundaryTitle}</div>
              <div className={styles.contextMessage}>{permissionBoundaryMessage}</div>
            </article>
            {canManageKnowledgeBase ? (
              <article className={styles.contextItem}>
                <div className={styles.contextItemTop}>
                  <span>知识库最近变更</span>
                  <span className={styles.contextBadge}>审计</span>
                </div>
                <AuditEventList
                  events={knowledgeBaseAuditEvents}
                  loading={knowledgeBaseAuditLoading}
                  errorMessage={knowledgeBaseAuditError}
                  emptyTitle="当前知识库还没有治理记录"
                  emptyDescription="权限变更、删除恢复、节点写操作会显示在这里。"
                  compact
                />
              </article>
            ) : null}
            {canManageKnowledgeBase && selectedDocument ? (
              <article className={styles.contextItem}>
                <div className={styles.contextItemTop}>
                  <span>当前节点最近变更</span>
                  <span className={styles.contextBadge}>{selectedDocumentTypeLabel}</span>
                </div>
                <AuditEventList
                  events={documentAuditEvents}
                  loading={documentAuditLoading}
                  errorMessage={documentAuditError}
                  emptyTitle="当前节点还没有关键操作记录"
                  emptyDescription="删除、恢复、移动、回滚和正文更新会显示在这里。"
                  compact
                />
              </article>
            ) : null}
          </div>
        </div>
      </details>
    </aside>
  )
}

export default KnowledgeBaseContextPanel

import { useEffect, useMemo, useState } from 'react'
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
  siteSubmitting,
  siteError,
  setSiteError,
  onSaveSiteSettings,
  documentPublishingSubmitting,
  documentPublishingError,
  setDocumentPublishingError,
  onPublishDocument,
  onUnpublishDocument,
}) => {
  const [siteForm, setSiteForm] = useState({
    siteEnabled: false,
    siteSlug: '',
    siteTitle: '',
    siteDescription: '',
  })
  const [publicSlug, setPublicSlug] = useState('')
  const folderCount = documents.filter((item) => item.docType === 'FOLDER').length
  const documentCount = documents.filter((item) => item.docType === 'DOC').length
  const capabilitySummary = `${documentCount} 篇文档 · ${folderCount} 个目录`
  const currentRoleLabel = roleLabels?.[knowledgeBase.currentRole] || knowledgeBase.currentRole || '未知角色'
  const knowledgeBaseDescription = knowledgeBase.description?.trim()
  const selectedDocumentTypeLabel = selectedDocument
    ? selectedDocument.docType === 'FOLDER'
      ? '目录'
      : '文档'
    : '未选择'
  const permissionBoundaryTitle = canManageKnowledgeBase
    ? '可管理知识库'
    : canWriteKnowledgeBase
      ? '可编辑文档'
      : '只读访问'
  const permissionBoundaryMessage = canManageKnowledgeBase
    ? '文档、权限、回收站和公开配置可用。'
    : canWriteKnowledgeBase
      ? '可创建、编辑、移动和删除文档。'
      : '仅可阅读和搜索。'
  const publicSiteUrl = useMemo(() => {
    if (knowledgeBase.siteUrl) {
      return knowledgeBase.siteUrl
    }
    return knowledgeBase.siteEnabled && knowledgeBase.siteSlug ? `/site/${knowledgeBase.siteSlug}` : ''
  }, [knowledgeBase.siteEnabled, knowledgeBase.siteSlug, knowledgeBase.siteUrl])
  const selectedDocumentPublicUrl = useMemo(() => {
    if (selectedDocument?.publicUrl) {
      return selectedDocument.publicUrl
    }
    if (!publicSiteUrl || !selectedDocument?.publicSlug) {
      return ''
    }
    return `${publicSiteUrl}/${selectedDocument.publicSlug}`
  }, [publicSiteUrl, selectedDocument?.publicSlug, selectedDocument?.publicUrl])
  const documentPublished = selectedDocument?.publishStatus === 'PUBLISHED'

  useEffect(() => {
    setSiteForm({
      siteEnabled: Boolean(knowledgeBase?.siteEnabled),
      siteSlug: knowledgeBase?.siteSlug || '',
      siteTitle: knowledgeBase?.siteTitle || knowledgeBase?.name || '',
      siteDescription: knowledgeBase?.siteDescription || knowledgeBase?.description || '',
    })
  }, [
    knowledgeBase?.description,
    knowledgeBase?.name,
    knowledgeBase?.siteDescription,
    knowledgeBase?.siteEnabled,
    knowledgeBase?.siteSlug,
    knowledgeBase?.siteTitle,
  ])

  useEffect(() => {
    setPublicSlug(selectedDocument?.publicSlug || '')
  }, [selectedDocument?.id, selectedDocument?.publicSlug])

  const handleSiteChange = (key, value) => {
    if (siteError) {
      setSiteError('')
    }
    setSiteForm((current) => ({
      ...current,
      [key]: value,
    }))
  }

  const handleSubmitSite = async (event) => {
    event.preventDefault()
    await onSaveSiteSettings({
      siteEnabled: siteForm.siteEnabled,
      siteSlug: siteForm.siteSlug.trim() || undefined,
      siteTitle: siteForm.siteTitle.trim() || undefined,
      siteDescription: siteForm.siteDescription.trim(),
    })
  }

  const handleSubmitPublish = async (event) => {
    event.preventDefault()
    await onPublishDocument({
      publicSlug: publicSlug.trim() || undefined,
    })
  }

  return (
    <aside className={styles.contextPanel}>
      <details className={styles.contextDisclosure} open>
        <summary className={styles.contextDisclosureSummary}>
          <div className={styles.contextSummaryMain}>
            <span className={styles.contextSummaryEyebrow}>知识库</span>
            <strong className={styles.contextSummaryTitle}>{knowledgeBase.name}</strong>
            <span className={styles.contextSummaryMeta}>{capabilitySummary}</span>
            {knowledgeBaseDescription ? <span className={styles.contextSummaryHint}>{knowledgeBaseDescription}</span> : null}
          </div>
          <span className={styles.contextBadge}>{currentRoleLabel}</span>
        </summary>

        <div className={styles.contextCard}>
          <div className={styles.panelHeader}>
            <div>
              <h2>概览</h2>
            </div>
          </div>

          <div className={styles.contextList}>
            <article className={styles.contextItem}>
              <div className={styles.contextItemTop}>
                <span>知识库</span>
                <span className={styles.contextBadge}>{knowledgeBase.permissionRestricted ? '独立权限' : '继承权限'}</span>
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
              <div className={styles.contextMessage}>{selectedDocument?.title || '未选择节点'}</div>
              <div className={styles.contextMeta}>
                <span className={styles.metaPill}>{canWriteKnowledgeBase ? '可编辑' : '只读'}</span>
                <span className={styles.metaPill}>{canManageKnowledgeBase ? '可管理' : '不可管理'}</span>
                {selectedDocument?.docType === 'DOC' ? (
                  <span className={styles.metaPill}>{documentPublished ? '已正式发布' : '未发布'}</span>
                ) : null}
              </div>
            </article>
            <article className={styles.contextItem}>
              <div className={styles.contextItemTop}>
                <span>权限</span>
                <span className={styles.contextBadge}>{canManageKnowledgeBase ? '管理' : canWriteKnowledgeBase ? '编辑' : '只读'}</span>
              </div>
              <div className={styles.contextMessage}>{permissionBoundaryTitle}</div>
              <div className={styles.contextMessage}>{permissionBoundaryMessage}</div>
            </article>
          </div>
        </div>
      </details>

      <details className={styles.contextDisclosure} open={Boolean(knowledgeBase.siteEnabled)}>
        <summary className={styles.contextDisclosureSummary}>
          <div className={styles.contextSummaryMain}>
            <span className={styles.contextSummaryEyebrow}>公开站点</span>
            <strong className={styles.contextSummaryTitle}>{knowledgeBase.siteTitle || knowledgeBase.name}</strong>
            <span className={styles.contextSummaryMeta}>
              {knowledgeBase.siteEnabled ? '已开放稳定入口' : '尚未对外开放'}
            </span>
          </div>
          <span className={styles.contextBadge}>{knowledgeBase.siteEnabled ? '已启用' : '未启用'}</span>
        </summary>
        <div className={styles.contextCard}>
          {canManageKnowledgeBase ? (
            <form className={styles.contextForm} onSubmit={handleSubmitSite}>
              <label className={styles.formField}>
                <span className={styles.formLabel}>站点状态</span>
                <span className={styles.contextSwitch}>
                  <input
                    type="checkbox"
                    checked={siteForm.siteEnabled}
                    onChange={(event) => handleSiteChange('siteEnabled', event.target.checked)}
                  />
                  <span className={styles.contextSwitchLabel}>
                    {siteForm.siteEnabled ? '启用知识库公开页' : '暂不开放'}
                  </span>
                </span>
              </label>

              <label className={styles.formField}>
                <span className={styles.formLabel}>站点标题</span>
                <input
                  className={styles.formInput}
                  value={siteForm.siteTitle}
                  onChange={(event) => handleSiteChange('siteTitle', event.target.value)}
                  placeholder="默认使用知识库名称"
                />
              </label>

              <label className={styles.formField}>
                <span className={styles.formLabel}>站点标识</span>
                <input
                  className={styles.formInput}
                  value={siteForm.siteSlug}
                  onChange={(event) => handleSiteChange('siteSlug', event.target.value)}
                  placeholder="例如：memora-public-docs"
                />
              </label>

              <label className={styles.formField}>
                <span className={styles.formLabel}>站点描述</span>
                <textarea
                  className={styles.formTextarea}
                  rows={3}
                  value={siteForm.siteDescription}
                  onChange={(event) => handleSiteChange('siteDescription', event.target.value)}
                  placeholder="首页会展示这段说明，帮助外部访客理解站点内容。"
                />
              </label>

              <div className={styles.formHelp}>
                启用后，知识库下已正式发布的文档会通过稳定 slug 对外提供只读访问。
              </div>

              {siteError ? <div className={styles.errorBox}>{siteError}</div> : null}

              <div className={styles.contextActions}>
                {publicSiteUrl ? (
                  <a className={styles.contextLink} href={publicSiteUrl} target="_blank" rel="noopener noreferrer">
                    打开站点首页
                  </a>
                ) : <span className={styles.formHelp}>保存后会生成公开首页地址。</span>}
                <button type="submit" className={styles.primaryButton} disabled={siteSubmitting}>
                  {siteSubmitting ? '保存中...' : '保存站点设置'}
                </button>
              </div>
            </form>
          ) : (
            <div className={styles.contextList}>
              <article className={styles.contextItem}>
                <div className={styles.contextItemTop}>
                  <span>站点入口</span>
                  <span className={styles.contextBadge}>{knowledgeBase.siteEnabled ? '可访问' : '未开放'}</span>
                </div>
                <div className={styles.contextMessage}>
                  {knowledgeBase.siteEnabled && publicSiteUrl ? (
                    <a className={styles.contextLink} href={publicSiteUrl} target="_blank" rel="noopener noreferrer">
                      {publicSiteUrl}
                    </a>
                  ) : '当前知识库还没有开放公开站点。'}
                </div>
              </article>
            </div>
          )}
        </div>
      </details>

      {selectedDocument?.docType === 'DOC' ? (
        <details className={styles.contextDisclosure} open={documentPublished}>
          <summary className={styles.contextDisclosureSummary}>
            <div className={styles.contextSummaryMain}>
              <span className={styles.contextSummaryEyebrow}>正式发布</span>
              <strong className={styles.contextSummaryTitle}>{selectedDocument.title}</strong>
              <span className={styles.contextSummaryMeta}>
                {documentPublished ? '公开页会跟随当前保存内容更新' : '仅工作区内可读，外部无法通过稳定 URL 消费'}
              </span>
            </div>
            <span className={styles.contextBadge}>{documentPublished ? '已发布' : '草稿'}</span>
          </summary>
          <div className={styles.contextCard}>
            {canManageKnowledgeBase ? (
              <form className={styles.contextForm} onSubmit={handleSubmitPublish}>
                <label className={styles.formField}>
                  <span className={styles.formLabel}>公开文档标识</span>
                  <input
                    className={styles.formInput}
                    value={publicSlug}
                    onChange={(event) => {
                      if (documentPublishingError) {
                        setDocumentPublishingError('')
                      }
                      setPublicSlug(event.target.value)
                    }}
                    placeholder="默认按标题生成 slug"
                  />
                </label>

                <div className={styles.formHelp}>
                  这是正式发布 URL 的最后一段，分享 token 和公开站点 slug 会彻底分离。
                </div>

                {selectedDocumentPublicUrl ? (
                  <a className={styles.contextLink} href={selectedDocumentPublicUrl} target="_blank" rel="noopener noreferrer">
                    {selectedDocumentPublicUrl}
                  </a>
                ) : (
                  <div className={styles.formHelp}>需要先启用公开站点，再通过正式发布生成稳定 URL。</div>
                )}

                {documentPublishingError ? <div className={styles.errorBox}>{documentPublishingError}</div> : null}

                <div className={styles.contextActions}>
                  <button type="submit" className={styles.primaryButton} disabled={documentPublishingSubmitting}>
                    {documentPublishingSubmitting ? '处理中...' : documentPublished ? '更新发布' : '正式发布'}
                  </button>
                  {documentPublished ? (
                    <button
                      type="button"
                      className={styles.secondaryButton}
                      disabled={documentPublishingSubmitting}
                      onClick={onUnpublishDocument}
                    >
                      取消发布
                    </button>
                  ) : null}
                </div>
              </form>
            ) : (
              <div className={styles.contextList}>
                <article className={styles.contextItem}>
                  <div className={styles.contextItemTop}>
                    <span>发布状态</span>
                    <span className={styles.contextBadge}>{documentPublished ? '已发布' : '草稿'}</span>
                  </div>
                  <div className={styles.contextMessage}>
                    {selectedDocumentPublicUrl ? (
                      <a className={styles.contextLink} href={selectedDocumentPublicUrl} target="_blank" rel="noopener noreferrer">
                        {selectedDocumentPublicUrl}
                      </a>
                    ) : '当前文档还没有正式发布。'}
                  </div>
                </article>
              </div>
            )}
          </div>
        </details>
      ) : null}

      {canManageKnowledgeBase ? (
        <details className={styles.contextDisclosure}>
          <summary className={styles.contextDisclosureSummary}>
            <div className={styles.contextSummaryMain}>
              <span className={styles.contextSummaryEyebrow}>审计</span>
              <strong className={styles.contextSummaryTitle}>知识库记录</strong>
              <span className={styles.contextSummaryMeta}>{knowledgeBaseAuditLoading ? '加载中' : `${knowledgeBaseAuditEvents.length} 条记录`}</span>
            </div>
            <span className={styles.contextBadge}>审计</span>
          </summary>
          <div className={styles.contextCard}>
            <AuditEventList
              events={knowledgeBaseAuditEvents}
              loading={knowledgeBaseAuditLoading}
              errorMessage={knowledgeBaseAuditError}
              emptyTitle="暂无记录"
              compact
            />
          </div>
        </details>
      ) : null}

      {canManageKnowledgeBase && selectedDocument ? (
        <details className={styles.contextDisclosure}>
          <summary className={styles.contextDisclosureSummary}>
            <div className={styles.contextSummaryMain}>
              <span className={styles.contextSummaryEyebrow}>审计</span>
              <strong className={styles.contextSummaryTitle}>节点记录</strong>
              <span className={styles.contextSummaryMeta}>{documentAuditLoading ? '加载中' : `${documentAuditEvents.length} 条记录`}</span>
            </div>
            <span className={styles.contextBadge}>{selectedDocumentTypeLabel}</span>
          </summary>
          <div className={styles.contextCard}>
            <AuditEventList
              events={documentAuditEvents}
              loading={documentAuditLoading}
              errorMessage={documentAuditError}
              emptyTitle="暂无记录"
              compact
            />
          </div>
        </details>
      ) : null}
    </aside>
  )
}

export default KnowledgeBaseContextPanel

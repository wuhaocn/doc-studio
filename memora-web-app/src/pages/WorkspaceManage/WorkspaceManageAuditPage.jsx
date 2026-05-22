import { useEffect } from 'react'
import dayjs from 'dayjs'
import { useOutletContext } from 'react-router-dom'
import AuditEventList from '../../components/Audit/AuditEventList'
import PageState from '../../components/Feedback/PageState'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import styles from './WorkspaceManage.module.css'

const WorkspaceManageAuditPage = () => {
  const {
    canViewWorkspaceAudit,
    recentAuditEvents,
    auditLoading,
    auditError,
    auditLoaded,
    auditSummary,
    auditSummaryLoading,
    auditSummaryLoaded,
    auditExportingScope,
    auditRetentionRunning,
    loadRecentAuditEvents,
    loadAuditSummary,
    handleRefreshAudit,
    handleRunAuditRetention,
    handleExportAuditLogs,
  } = useOutletContext()
  useDocumentTitle('工作区审计')

  useEffect(() => {
    if (canViewWorkspaceAudit && !auditLoaded) {
      void loadRecentAuditEvents()
    }
  }, [auditLoaded, canViewWorkspaceAudit, loadRecentAuditEvents])

  useEffect(() => {
    if (canViewWorkspaceAudit && !auditSummaryLoaded) {
      void loadAuditSummary()
    }
  }, [auditSummaryLoaded, canViewWorkspaceAudit, loadAuditSummary])

  if (!canViewWorkspaceAudit) {
    return (
      <PageState
        title="当前角色不能查看工作区审计"
        description="仅 OWNER / ADMIN 可查看。"
      />
    )
  }

  const lastArchivedAtText = auditSummary?.lastArchivedAt
    ? dayjs(auditSummary.lastArchivedAt).format('YYYY-MM-DD HH:mm')
    : '未执行'

  return (
    <div className={styles.stack}>
      <section className={styles.surfaceCard}>
        <div className={styles.sectionHeader}>
          <div>
            <h2 className={styles.sectionTitle}>审计与留存</h2>
            <span className={styles.sectionMeta}>查看关键操作、导出记录并执行归档。</span>
          </div>
          <div className={styles.sectionActions}>
            <button
              type="button"
              className={styles.secondaryButton}
              disabled={Boolean(auditExportingScope) || auditRetentionRunning}
              onClick={handleRefreshAudit}
            >
              刷新
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              disabled={Boolean(auditExportingScope) || auditRetentionRunning}
              onClick={handleRunAuditRetention}
            >
              {auditRetentionRunning ? '归档中...' : '执行归档'}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              disabled={Boolean(auditExportingScope) || auditRetentionRunning}
              onClick={() => handleExportAuditLogs('ACTIVE')}
            >
              {auditExportingScope === 'ACTIVE' ? '导出中...' : '导出活跃 CSV'}
            </button>
            <button
              type="button"
              className={styles.secondaryButton}
              disabled={Boolean(auditExportingScope) || auditRetentionRunning}
              onClick={() => handleExportAuditLogs('ARCHIVED')}
            >
              {auditExportingScope === 'ARCHIVED' ? '导出中...' : '导出归档 CSV'}
            </button>
          </div>
        </div>

        <div className={styles.auditSummaryGrid}>
          <article className={styles.auditSummaryCard}>
            <span>总记录</span>
            <strong>{auditSummaryLoading ? '...' : auditSummary?.totalCount ?? '-'}</strong>
          </article>
          <article className={styles.auditSummaryCard}>
            <span>活跃记录</span>
            <strong>{auditSummaryLoading ? '...' : auditSummary?.activeCount ?? '-'}</strong>
          </article>
          <article className={styles.auditSummaryCard}>
            <span>归档记录</span>
            <strong>{auditSummaryLoading ? '...' : auditSummary?.archivedCount ?? '-'}</strong>
          </article>
          <article className={styles.auditSummaryCard}>
            <span>失败记录</span>
            <strong>{auditSummaryLoading ? '...' : auditSummary?.failureCount ?? '-'}</strong>
          </article>
          <article className={styles.auditSummaryCard}>
            <span>待归档</span>
            <strong>{auditSummaryLoading ? '...' : auditSummary?.pendingArchiveCount ?? '-'}</strong>
          </article>
          <article className={styles.auditSummaryCard}>
            <span>归档阈值</span>
            <strong>{auditSummaryLoading ? '...' : `${auditSummary?.configuredRetentionDays ?? '-'} 天`}</strong>
          </article>
        </div>

        <div className={styles.auditGovernanceMeta}>
          <span>
            自动归档：
            {auditSummary?.automationEnabled
              ? `已开启（${auditSummary?.automationCron || '-'} / ${auditSummary?.automationZone || '-'})`
              : '未开启'}
          </span>
          <span>
            最近归档：
            {lastArchivedAtText}
          </span>
          <span>单次批量：{auditSummary?.archiveBatchSize ?? '-'}</span>
          <span>导出上限：{auditSummary?.exportMaxSize ?? '-'}</span>
        </div>

        <div className={styles.contentBlock}>
          <AuditEventList
            events={recentAuditEvents}
            loading={auditLoading}
            errorMessage={auditError}
            emptyTitle="暂无审计记录"
            emptyDescription="关键操作会记录在这里"
            showKnowledgeBaseName
          />
        </div>
      </section>
    </div>
  )
}

export default WorkspaceManageAuditPage

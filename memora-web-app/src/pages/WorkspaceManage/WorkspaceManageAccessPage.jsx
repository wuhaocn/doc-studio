import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import PageState from '../../components/Feedback/PageState'
import OpenApiWorkbench from '../../components/Workspace/OpenApiWorkbench'
import ServiceAccessPanel from '../../components/Workspace/ServiceAccessPanel'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import styles from './WorkspaceManage.module.css'

const WorkspaceManageAccessPage = () => {
  const {
    currentUser,
    knowledgeBases,
    canManageServiceAccounts,
    handleWorkspaceManageChanged,
  } = useOutletContext()
  const [workbenchExpanded, setWorkbenchExpanded] = useState(false)
  useDocumentTitle('开放接入')

  if (!canManageServiceAccounts) {
    return (
      <PageState
        title="当前角色不能管理开放接入"
        description="仅工作区所有者或管理员可管理。"
      />
    )
  }

  return (
    <div className={styles.stack}>
      <section className={styles.surfaceCard}>
        <div className={styles.sectionHeader}>
          <div>
            <h2 className={styles.sectionTitle}>开放接入</h2>
            <span className={styles.sectionMeta}>先完成机器主体和访问密钥配置，再按需验证保存、回读与浏览器入口。</span>
          </div>
        </div>
        <div className={styles.metaRow}>
          <span className={styles.metaPill}>{knowledgeBases.length} 个知识库</span>
          <span className={styles.metaPill}>机器接入凭证</span>
          <span className={styles.metaPill}>操作可追溯</span>
        </div>
      </section>

      <ServiceAccessPanel
        tenantId={currentUser?.tenantId}
        knowledgeBases={knowledgeBases}
        onChanged={handleWorkspaceManageChanged}
      />

      <section className={styles.surfaceCard}>
        <div className={styles.sectionHeader}>
          <div>
            <h2 className={styles.sectionTitle}>导入验证工具</h2>
            <span className={styles.sectionMeta}>用于验证保存、按来源回读，以及浏览器捕获入口。</span>
          </div>
          <div className={styles.sectionActions}>
            <button
              type="button"
              className={styles.secondaryButton}
              onClick={() => setWorkbenchExpanded((current) => !current)}
            >
              {workbenchExpanded ? '收起工具' : '展开工具'}
            </button>
          </div>
        </div>

        {workbenchExpanded ? (
          <div className={styles.content}>
            <OpenApiWorkbench knowledgeBases={knowledgeBases} />
          </div>
        ) : (
          <div className={styles.subtleText}>验证工具默认收起，避免干扰日常凭证管理。</div>
        )}
      </section>
    </div>
  )
}

export default WorkspaceManageAccessPage

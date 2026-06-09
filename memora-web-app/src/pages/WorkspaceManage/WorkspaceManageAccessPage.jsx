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
      <ServiceAccessPanel
        tenantId={currentUser?.tenantId}
        knowledgeBases={knowledgeBases}
        onChanged={handleWorkspaceManageChanged}
      />

      <section className={styles.surfaceCard}>
        <div className={styles.sectionHeader}>
          <div>
            <h2 className={styles.sectionTitle}>接入调试</h2>
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
        ) : null}
      </section>
    </div>
  )
}

export default WorkspaceManageAccessPage

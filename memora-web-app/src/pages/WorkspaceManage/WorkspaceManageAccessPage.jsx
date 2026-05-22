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
            <span className={styles.sectionMeta}>先创建主体和密钥，再验证保存链路是否跑通。</span>
          </div>
        </div>
        <div className={styles.metaRow}>
          <span className={styles.metaPill}>{knowledgeBases.length} 个知识库</span>
          <span className={styles.metaPill}>可签发密钥</span>
          <span className={styles.metaPill}>写入可追溯</span>
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
            <h2 className={styles.sectionTitle}>接入调试</h2>
            <span className={styles.sectionMeta}>先用一篇文档把保存、回读和打开阅读页跑通。</span>
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
          <div className={styles.subtleText}>日常先管理主体和密钥，需要联调保存链路时再展开。</div>
        )}
      </section>
    </div>
  )
}

export default WorkspaceManageAccessPage

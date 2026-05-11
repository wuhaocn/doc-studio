import dayjs from 'dayjs'
import styles from './AuditEventList.module.css'

const ROLE_LABELS = {
  OWNER: '所有者',
  ADMIN: '管理员',
  EDITOR: '编辑者',
  REVIEWER: '审核者',
  VIEWER: '只读',
}

const ACTION_LABELS = {
  REGISTER_OWNER: '创建工作区',
  LOGIN: '登录工作区',
  LOGOUT: '退出工作区',
  SWITCH_WORKSPACE: '切换工作区',
  CREATE_INVITE: '创建成员邀请',
  ACCEPT_INVITE: '接受成员邀请',
  REVOKE_INVITE: '撤销成员邀请',
  CREATE_KNOWLEDGE_BASE: '创建知识库',
  UPDATE_KNOWLEDGE_BASE: '更新知识库',
  DELETE_KNOWLEDGE_BASE: '删除知识库',
  RESTORE_KNOWLEDGE_BASE: '恢复知识库',
  UPDATE_KNOWLEDGE_BASE_MEMBERS: '更新知识库权限',
  CREATE_DOCUMENT: '创建节点',
  UPDATE_DOCUMENT: '更新节点',
  MOVE_DOCUMENT: '移动节点',
  REORDER_DOCUMENT: '调整排序',
  DELETE_DOCUMENT: '删除节点',
  RESTORE_DOCUMENT: '恢复节点',
  ROLLBACK_DOCUMENT: '回滚文档版本',
  EXPORT_AUDIT_LOG: '导出审计记录',
  APPLY_AUDIT_RETENTION: '执行审计归档',
  CREATE_DOCUMENT_SHARE: '创建受控分享',
  REVOKE_DOCUMENT_SHARE: '撤销受控分享',
  ACCESS_DOCUMENT_SHARE: '访问受控分享',
  CREATE_SERVICE_ACCOUNT: '创建机器主体',
  CREATE_API_KEY: '签发 API key',
  DISABLE_API_KEY: '禁用 API key',
  REVOKE_API_KEY: '吊销 API key',
  ROTATE_API_KEY: '轮换 API key',
  OPEN_API_LIST_KNOWLEDGE_BASES: '列出开放知识库',
  OPEN_API_LIST_DOCUMENTS: '列出开放文档',
  OPEN_API_GET_DOCUMENT: '读取开放文档',
  OPEN_API_GET_DOCUMENT_VERSIONS: '读取开放版本',
  OPEN_API_CREATE_DOCUMENT: '开放接口创建文档',
  OPEN_API_UPDATE_DOCUMENT: '开放接口更新文档',
}

const SOURCE_LABELS = {
  MEMORA_WEB_APP: 'Web 控制台',
  DIRECT_API: '接口调用',
  PUBLIC_SHARE: '公开分享',
  OPEN_API: '开放接口',
}

const RESULT_LABELS = {
  SUCCESS: '成功',
  FAILURE: '失败',
}

const resolveActionLabel = (event) => {
  return ACTION_LABELS[event?.actionType] || event?.actionType || '关键操作'
}

const resolveRoleLabel = (role) => {
  return ROLE_LABELS[role] || role || ''
}

const resolveSourceLabel = (sourceType) => {
  return SOURCE_LABELS[sourceType] || sourceType || '未知来源'
}

const buildFallbackDetail = (event) => {
  if (event?.objectTitle) {
    return `对象：${event.objectTitle}`
  }
  return '关键操作已写入审计记录。'
}

const AuditEventList = ({
  events = [],
  loading = false,
  errorMessage = '',
  emptyTitle = '还没有审计记录',
  emptyDescription = '关键操作发生后会显示在这里。',
  showKnowledgeBaseName = false,
  compact = false,
}) => {
  if (loading) {
    return <div className={styles.state}>正在加载审计记录...</div>
  }

  if (errorMessage) {
    return <div className={`${styles.state} ${styles.stateError}`}>{errorMessage}</div>
  }

  if (events.length === 0) {
    return (
      <div className={styles.emptyState}>
        <strong>{emptyTitle}</strong>
        <p>{emptyDescription}</p>
      </div>
    )
  }

  return (
    <div className={`${styles.list} ${compact ? styles.compactList : ''}`}>
      {events.map((event) => (
        <article key={event.id} className={styles.item}>
          <div className={styles.topline}>
            <div className={styles.toplineMain}>
              <strong>{resolveActionLabel(event)}</strong>
              {event?.resultType ? (
                <span className={`${styles.resultBadge} ${event.resultType === 'FAILURE' ? styles.resultBadgeFailure : ''}`}>
                  {RESULT_LABELS[event.resultType] || event.resultType}
                </span>
              ) : null}
            </div>
            <span>{event.createdAt ? dayjs(event.createdAt).format('MM-DD HH:mm') : '刚刚'}</span>
          </div>
          <div className={styles.detail}>{event.detail || buildFallbackDetail(event)}</div>
          <div className={styles.meta}>
            {event.actorDisplayName ? <span>{event.actorDisplayName}</span> : null}
            {resolveRoleLabel(event.actorRole) ? <span>{resolveRoleLabel(event.actorRole)}</span> : null}
            {showKnowledgeBaseName && event.knowledgeBaseName ? <span>{event.knowledgeBaseName}</span> : null}
            {event.objectTitle ? <span>{event.objectTitle}</span> : null}
            <span>{resolveSourceLabel(event.sourceType)}</span>
          </div>
        </article>
      ))}
    </div>
  )
}

export default AuditEventList

import { useMemo } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router-dom'
import OpenApiWorkbench from '../../components/Workspace/OpenApiWorkbench'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import {
  buildCaptureInitialValues,
  parseCaptureDraftHash,
} from '../../utils/openApiWorkbench'
import styles from './CaptureSavePage.module.css'

const CaptureSavePage = () => {
  const location = useLocation()
  const [searchParams] = useSearchParams()
  useDocumentTitle('保存到 Memora')

  const dialogDraft = useMemo(
    () => parseCaptureDraftHash(location.hash),
    [location.hash],
  )

  const queryDraft = useMemo(() => ({
    knowledgeBaseId: Number(searchParams.get('knowledgeBaseId') || 0),
    parentId: Number(searchParams.get('parentId') || 0),
    title: searchParams.get('title') || '网页剪藏',
    sourceUrl: searchParams.get('url') || '',
    selection: searchParams.get('selection') || '',
  }), [searchParams])

  const initialValues = useMemo(
    () => buildCaptureInitialValues(dialogDraft || queryDraft),
    [dialogDraft, queryDraft],
  )

  return (
    <div className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.hero}>
          <p className={styles.eyebrow}>网页保存</p>
          <h1 className={styles.title}>保存当前页面到 Memora</h1>
          <p className={styles.description}>
            这是浏览器书签脚本、对话框插件和轻量外部工具使用的保存入口。外部工具既可以只带标题与选中文本进来，也可以直接携带预生成的 Markdown / HTML 草稿，再通过一把具备写入权限的访问密钥保存到指定知识库。
          </p>
          <div className={styles.metaRow}>
            <span className={styles.metaPill}>知识库 #{initialValues.knowledgeBaseId || '-'}</span>
            <span className={styles.metaPill}>{initialValues.parentId > 0 ? `父目录 #${initialValues.parentId}` : '根目录'}</span>
            <span className={styles.metaPill}>{dialogDraft ? '已携带草稿' : '浏览器捕获模式'}</span>
            <span className={styles.metaPill}>{initialValues.format}</span>
          </div>
        </header>

        <OpenApiWorkbench
          mode="capture"
          initialValues={initialValues}
          resetKey={`${searchParams.toString()}|${location.hash}`}
        />

        <div className={styles.footerNote}>
          如果还没有机器接入凭证，先到 <Link to="/workspace/manage/access">工作区管理 / 开放接入</Link> 创建机器主体和访问密钥。
        </div>
      </div>
    </div>
  )
}

export default CaptureSavePage

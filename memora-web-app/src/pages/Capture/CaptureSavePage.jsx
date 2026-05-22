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
          <h1 className={styles.title}>把当前内容保存到 Memora</h1>
          <p className={styles.description}>
            这个入口可以接住网页剪藏和外部草稿。把当前页面、选中文本或预生成的 Markdown / HTML 带进来后，直接保存到指定知识库即可。
          </p>
          <div className={styles.metaRow}>
            <span className={styles.metaPill}>知识库 #{initialValues.knowledgeBaseId || '-'}</span>
            <span className={styles.metaPill}>{initialValues.parentId > 0 ? `目录 #${initialValues.parentId}` : '根目录'}</span>
            <span className={styles.metaPill}>{dialogDraft ? '草稿已带入' : '网页捕获'}</span>
            <span className={styles.metaPill}>{initialValues.format === 'HTML' ? 'HTML 正文' : 'Markdown 正文'}</span>
          </div>
        </header>

        <OpenApiWorkbench
          mode="capture"
          initialValues={initialValues}
          resetKey={`${searchParams.toString()}|${location.hash}`}
        />

        <div className={styles.footerNote}>
          还没有可写密钥时，先到 <Link to="/workspace/manage/access">工作区管理 / 开放接入</Link> 创建主体和密钥。
        </div>
      </div>
    </div>
  )
}

export default CaptureSavePage

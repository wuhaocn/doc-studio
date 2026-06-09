import { useMemo, useState } from 'react'
import { extractMiniAppSpecFromHtml } from '../../utils/documentExamples'
import styles from './DocumentRenderedContent.module.css'

const VARIANT_CLASS_MAP = {
  article: styles.article,
  compact: styles.compact,
}

const MiniAppExamplePreview = ({ spec }) => {
  const [inspector, setInspector] = useState('')
  const [status, setStatus] = useState(spec?.pages?.[0]?.components?.find((item) => item.name === 'status')?.options?.[0] || '正常')
  const [remark, setRemark] = useState('')
  const [submitCount, setSubmitCount] = useState(0)
  const [lastSubmission, setLastSubmission] = useState(null)
  const statusOptions = spec?.pages?.[0]?.components?.find((item) => item.name === 'status')?.options || ['正常', '待复核', '需整改']

  const handleSubmit = () => {
    const nextSubmission = {
      inspector: inspector.trim() || '未填写',
      status,
      remark: remark.trim() || '无备注',
    }
    setSubmitCount((current) => current + 1)
    setLastSubmission(nextSubmission)
  }

  return (
    <section className={styles.miniAppPreview} aria-label="H5 小程序点击演示">
      <div className={styles.miniAppHeader}>
        <div>
          <span>H5 小程序演示</span>
          <strong>{spec?.title || '巡检登记小程序'}</strong>
        </div>
        <small>点击事件：{spec?.events?.submitInspection?.label || '提交登记'}</small>
      </div>

      <div className={styles.miniAppForm}>
        <label>
          <span>巡检人</span>
          <input value={inspector} onChange={(event) => setInspector(event.target.value)} placeholder="例如：张三" />
        </label>
        <label>
          <span>状态</span>
          <select value={status} onChange={(event) => setStatus(event.target.value)}>
            {statusOptions.map((option) => (
              <option key={option} value={option}>{option}</option>
            ))}
          </select>
        </label>
        <label>
          <span>现场备注</span>
          <textarea value={remark} onChange={(event) => setRemark(event.target.value)} placeholder="输入本次巡检说明" />
        </label>
      </div>

      <div className={styles.miniAppActions}>
        <button type="button" onClick={handleSubmit}>
          提交登记
        </button>
        <span>已点击 {submitCount} 次</span>
      </div>

      {lastSubmission ? (
        <div className={styles.miniAppResult}>
          <strong>最近一次提交</strong>
          <span>巡检人：{lastSubmission.inspector}</span>
          <span>状态：{lastSubmission.status}</span>
          <span>备注：{lastSubmission.remark}</span>
        </div>
      ) : null}
    </section>
  )
}

const DocumentRenderedContent = ({
  html,
  variant = 'article',
  className = '',
}) => {
  const classes = [styles.root, VARIANT_CLASS_MAP[variant] || styles.article, className]
    .filter(Boolean)
    .join(' ')
  const miniAppSpec = useMemo(() => extractMiniAppSpecFromHtml(html), [html])

  return (
    <div className={classes}>
      {miniAppSpec ? <MiniAppExamplePreview spec={miniAppSpec} /> : null}
      <div dangerouslySetInnerHTML={{ __html: html }} />
    </div>
  )
}

export default DocumentRenderedContent

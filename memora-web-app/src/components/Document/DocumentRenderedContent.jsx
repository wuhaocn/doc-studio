import styles from './DocumentRenderedContent.module.css'

const VARIANT_CLASS_MAP = {
  article: styles.article,
  compact: styles.compact,
}

const DocumentRenderedContent = ({
  html,
  variant = 'article',
  className = '',
}) => {
  const classes = [styles.root, VARIANT_CLASS_MAP[variant] || styles.article, className]
    .filter(Boolean)
    .join(' ')

  return <div className={classes} dangerouslySetInnerHTML={{ __html: html }} />
}

export default DocumentRenderedContent

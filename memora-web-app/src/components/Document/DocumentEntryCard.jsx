import { Link } from 'react-router-dom'
import styles from './DocumentEntryCard.module.css'

const DocumentEntryCard = ({
  to = '',
  title,
  summary = '',
  eyebrow = '',
  badges = [],
  details = null,
  actions = null,
  className = '',
}) => {
  const classes = [
    styles.card,
    to ? styles.cardInteractive : '',
    actions ? styles.cardWithActions : '',
    className,
  ]
    .filter(Boolean)
    .join(' ')

  const content = (
    <>
      {(eyebrow || badges.length > 0) ? (
        <div className={styles.topline}>
          {eyebrow ? <span className={styles.eyebrow}>{eyebrow}</span> : null}
          {badges.length > 0 ? (
            <div className={styles.badges}>
              {badges.map((badge) => (
                <span key={badge.key || badge.label} className={`${styles.badge} ${badge.tone ? styles[`badge${badge.tone}`] : ''}`}>
                  {badge.label}
                </span>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}

      <div className={styles.body}>
        <h3 className={styles.title}>{title}</h3>
        {summary ? <div className={styles.summary}>{summary}</div> : null}
        {details ? <div className={styles.details}>{details}</div> : null}
      </div>

      {actions ? <div className={styles.actions}>{actions}</div> : null}
    </>
  )

  if (to) {
    return (
      <Link to={to} className={classes}>
        {content}
      </Link>
    )
  }

  return <article className={classes}>{content}</article>
}

export default DocumentEntryCard

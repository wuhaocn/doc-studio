import styles from './PageState.module.css'

const PageState = ({
  eyebrow = '',
  title,
  description,
  primaryAction = null,
  secondaryAction = null,
}) => {
  return (
    <div className={styles.page}>
      <section className={styles.card}>
        {eyebrow ? <p className={styles.eyebrow}>{eyebrow}</p> : null}
        <h1 className={styles.title}>{title}</h1>
        <p className={styles.description}>{description}</p>
        {(primaryAction || secondaryAction) ? (
          <div className={styles.actions}>
            {primaryAction ? (
              <button type="button" className={styles.primaryButton} onClick={primaryAction.onClick}>
                {primaryAction.label}
              </button>
            ) : null}
            {secondaryAction ? (
              <button type="button" className={styles.secondaryButton} onClick={secondaryAction.onClick}>
                {secondaryAction.label}
              </button>
            ) : null}
          </div>
        ) : null}
      </section>
    </div>
  )
}

export default PageState

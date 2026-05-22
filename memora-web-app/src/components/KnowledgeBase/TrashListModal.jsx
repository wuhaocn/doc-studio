import { useEscapeKey } from '../../hooks/useEscapeKey'
import styles from './TrashListModal.module.css'

const TrashListModal = ({
  open,
  eyebrow = '回收站',
  title,
  description,
  items = [],
  loading = false,
  errorMessage = '',
  restoringItemId = null,
  emptyTitle = '当前回收站为空',
  emptyDescription = '暂无内容。',
  restoreLabel = '恢复',
  onClose,
  onRestore,
  getItemTitle,
  getItemDescription,
  getItemMeta,
}) => {
  useEscapeKey(open, onClose)

  if (!open) {
    return null
  }

  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.modal}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>{eyebrow}</p>
            <h2 className={styles.title}>{title}</h2>
            <p className={styles.description}>{description}</p>
          </div>
          <button type="button" className={styles.closeButton} onClick={onClose}>
            关闭
          </button>
        </div>

        <div className={styles.body}>
          {errorMessage ? <div className={styles.errorMessage}>{errorMessage}</div> : null}

          {loading ? (
            <div className={styles.state}>正在加载回收站内容...</div>
          ) : items.length > 0 ? (
            <div className={styles.list}>
              {items.map((item) => {
                const metaItems = getItemMeta ? (getItemMeta(item) || []).filter(Boolean) : []
                const itemDescription = getItemDescription ? getItemDescription(item) : ''

                return (
                  <article key={item.id} className={styles.itemCard}>
                    <div className={styles.itemMain}>
                      <div className={styles.itemTitle}>{getItemTitle(item)}</div>
                      {itemDescription ? <p className={styles.itemDescription}>{itemDescription}</p> : null}
                      {metaItems.length > 0 ? (
                        <div className={styles.itemMeta}>
                          {metaItems.map((metaItem) => (
                            <span key={metaItem}>{metaItem}</span>
                          ))}
                        </div>
                      ) : null}
                    </div>
                    <button
                      type="button"
                      className={styles.primaryButton}
                      disabled={restoringItemId === item.id}
                      onClick={() => onRestore(item)}
                    >
                      {restoringItemId === item.id ? '恢复中...' : restoreLabel}
                    </button>
                  </article>
                )
              })}
            </div>
          ) : (
            <div className={styles.emptyState}>
              <strong>{emptyTitle}</strong>
              <p>{emptyDescription}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default TrashListModal

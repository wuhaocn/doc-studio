import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import styles from './ConfirmDialog.module.css'

const ConfirmContext = createContext(null)

export const ConfirmProvider = ({ children }) => {
  const [state, setState] = useState(null)
  const resolveRef = useRef(null)

  const confirm = useCallback(({ title, description, confirmLabel = '确认', cancelLabel = '取消', danger = false }) => {
    return new Promise((resolve) => {
      resolveRef.current = resolve
      setState({ title, description, confirmLabel, cancelLabel, danger })
    })
  }, [])

  const handleConfirm = () => {
    resolveRef.current?.(true)
    setState(null)
  }

  const handleCancel = () => {
    resolveRef.current?.(false)
    setState(null)
  }

  useEffect(() => {
    if (!state) return
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') handleCancel()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  })

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      {state && (
        <div className={styles.overlay} onClick={handleCancel} role="dialog" aria-modal="true">
          <div className={styles.dialog} onClick={(e) => e.stopPropagation()}>
            <h3 className={styles.title}>{state.title}</h3>
            {state.description && <p className={styles.description}>{state.description}</p>}
            <div className={styles.actions}>
              <button type="button" className={styles.cancelButton} onClick={handleCancel}>
                {state.cancelLabel}
              </button>
              <button
                type="button"
                className={`${styles.confirmButton} ${state.danger ? styles.confirmButtonDanger : ''}`}
                onClick={handleConfirm}
                autoFocus
              >
                {state.confirmLabel}
              </button>
            </div>
          </div>
        </div>
      )}
    </ConfirmContext.Provider>
  )
}

export const useConfirm = () => {
  const ctx = useContext(ConfirmContext)
  if (!ctx) throw new Error('useConfirm must be used within ConfirmProvider')
  return ctx
}

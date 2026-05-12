import { createContext, useCallback, useContext, useState } from 'react'
import styles from './Toast.module.css'

const ToastContext = createContext(null)

let toastId = 0

const DURATION = { success: 3000, error: 5000, info: 3000 }

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([])

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const addToast = useCallback((message, type = 'info') => {
    const id = ++toastId
    setToasts((prev) => [...prev, { id, message, type }])
    setTimeout(() => removeToast(id), DURATION[type] || 3000)
    return id
  }, [removeToast])

  const toast = useCallback((message, type) => addToast(message, type), [addToast])
  toast.success = (message) => addToast(message, 'success')
  toast.error = (message) => addToast(message, 'error')
  toast.info = (message) => addToast(message, 'info')

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className={styles.container} aria-live="polite" aria-atomic="false">
        {toasts.map((t) => (
          <div key={t.id} className={`${styles.toast} ${styles[t.type]}`}>
            <span className={styles.message}>{t.message}</span>
            <button
              type="button"
              className={styles.close}
              onClick={() => removeToast(t.id)}
              aria-label="关闭"
            >
              &times;
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}

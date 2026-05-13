import { Component } from 'react'
import styles from './ErrorBoundary.module.css'

class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className={styles.page}>
          <section className={styles.card}>
            <p className={styles.eyebrow}>应用异常</p>
            <h1 className={styles.title}>页面遇到了问题</h1>
            <p className={styles.description}>
              当前页面发生了未预期的错误，你可以尝试重试或刷新页面。如果问题持续出现，请联系管理员。
            </p>
            <div className={styles.actions}>
              <button type="button" className={styles.primaryButton} onClick={this.handleReset}>
                重试
              </button>
              <button type="button" className={styles.secondaryButton} onClick={this.handleReload}>
                刷新页面
              </button>
            </div>
          </section>
        </div>
      )
    }

    return this.props.children
  }
}

export default ErrorBoundary

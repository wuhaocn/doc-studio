import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDocumentTitle } from '../../hooks/useDocumentTitle'
import styles from './NotFound.module.css'

const NotFound = () => {
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')
  useDocumentTitle('页面不存在')

  const handleSearch = (e) => {
    e.preventDefault()
    const trimmed = keyword.trim()
    if (trimmed) {
      navigate(`/search?keyword=${encodeURIComponent(trimmed)}`)
    } else {
      navigate('/search')
    }
  }

  return (
    <div className={styles.page}>
      <section className={styles.card}>
        <p className={styles.eyebrow}>页面不存在</p>
        <h1 className={styles.title}>404</h1>
        <p className={styles.description}>当前地址没有对应内容。可以搜索你要找的文档，或者回到首页继续浏览。</p>
        <form className={styles.searchForm} onSubmit={handleSearch}>
          <input
            className={styles.searchInput}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="搜索文档..."
          />
          <button type="submit" className={styles.searchButton}>搜索</button>
        </form>
        <div className={styles.actions}>
          <Link to="/" className={styles.primaryButton}>
            返回首页
          </Link>
          <Link to="/search" className={styles.secondaryButton}>
            进入搜索
          </Link>
        </div>
      </section>
    </div>
  )
}

export default NotFound

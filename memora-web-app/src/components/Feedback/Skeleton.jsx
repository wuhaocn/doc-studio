import styles from './Skeleton.module.css'

const Bar = ({ size = 'full', tall = false }) => (
  <div className={`${styles.bar} ${styles[`bar${size.charAt(0).toUpperCase() + size.slice(1)}`]} ${tall ? styles.barTall : ''}`} />
)

export const EditorSkeleton = () => (
  <div className={styles.skeleton}>
    <Bar size="short" tall />
    <Bar size="medium" />
    <div className={styles.group}>
      <Bar size="full" />
      <Bar size="long" />
      <Bar size="full" />
      <Bar size="medium" />
    </div>
    <div className={styles.group}>
      <Bar size="full" />
      <Bar size="long" />
      <Bar size="short" />
    </div>
  </div>
)

export const DashboardSkeleton = () => (
  <div className={styles.skeleton}>
    <Bar size="short" tall />
    <div className={styles.row}>
      <div className={styles.circle} />
      <Bar size="medium" />
    </div>
    <div className={styles.group}>
      <Bar size="full" />
      <Bar size="long" />
      <Bar size="medium" />
    </div>
    <div className={styles.group}>
      <Bar size="full" />
      <Bar size="short" />
    </div>
  </div>
)

export const KnowledgeBaseSkeleton = () => (
  <div className={styles.skeleton}>
    <Bar size="medium" tall />
    <div className={styles.group}>
      <div className={styles.row}>
        <div className={styles.circle} />
        <Bar size="long" />
      </div>
      <div className={styles.row}>
        <div className={styles.circle} />
        <Bar size="medium" />
      </div>
      <div className={styles.row}>
        <div className={styles.circle} />
        <Bar size="long" />
      </div>
    </div>
  </div>
)

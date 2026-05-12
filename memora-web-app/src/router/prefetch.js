// Prefetch route chunks for better perceived performance
export const prefetchHome = () => import('../pages/Home/Home')
export const prefetchEditor = () => import('../pages/Document/DocumentEditorPage')
export const prefetchReader = () => import('../pages/Document/DocumentReaderPage')
export const prefetchKnowledgeBase = () => import('../pages/KnowledgeBase/KnowledgeBaseDetail')

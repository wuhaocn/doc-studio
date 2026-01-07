import { Routes, Route } from 'react-router-dom'
import Home from '../pages/Home/Home'
import KnowledgeBaseDetail from '../pages/KnowledgeBase/KnowledgeBaseDetail'
import DocumentEdit from '../pages/Document/DocumentEdit'
import NotFound from '../pages/NotFound/NotFound'

const Router = () => {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/kb/:id" element={<KnowledgeBaseDetail />} />
      <Route path="/kb/:kbId/doc/:id" element={<DocumentEdit />} />
      <Route path="/kb/:kbId/doc/new" element={<DocumentEdit />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}

export default Router


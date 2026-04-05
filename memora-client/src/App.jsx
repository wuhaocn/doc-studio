import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import Layout from './components/Layout/Layout'
import Home from './pages/Home/Home'
import DocumentEdit from './pages/Document/DocumentEdit'
import KnowledgeBaseDetail from './pages/KnowledgeBase/KnowledgeBaseDetail'
import FileManager from './pages/FileManager/FileManager'
import Settings from './pages/Settings/Settings'
import Sync from './pages/Sync/Sync'
import NotFound from './pages/NotFound/NotFound'

function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/kb/:id" element={<KnowledgeBaseDetail />} />
          <Route path="/kb/:kbId/doc/:id" element={<DocumentEdit />} />
          <Route path="/kb/:kbId/doc/new" element={<DocumentEdit />} />
          <Route path="/file-manager" element={<FileManager />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/sync" element={<Sync />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Layout>
    </Router>
  )
}

export default App
import React from 'react'
import { createBrowserRouter } from 'react-router-dom'
import Layout from '../components/Layout/Layout'
import Home from '../pages/Home/Home'
import DocumentEdit from '../pages/Document/DocumentEdit'
import KnowledgeBaseDetail from '../pages/KnowledgeBase/KnowledgeBaseDetail'
import MemoraAIHome from '../pages/MemoraAI/MemoraAIHome'
import AiQnAPage from '../pages/MemoraAI/AiQnAPage'
import AiQuestionPage from '../pages/MemoraAI/AiQuestionPage'
import AiVisualizationPage from '../pages/MemoraAI/AiVisualizationPage'
import NotFound from '../pages/NotFound/NotFound'

// 资源库页面
import ResourceHome from '../pages/Resource/ResourceHome'
import ResourceEdit from '../pages/Resource/ResourceEdit'
import ResourceDetailPage from '../pages/Resource/ResourceDetailPage'

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        index: true,
        element: <Home />,
      },
      {
        path: 'document/edit/:id',
        element: <DocumentEdit />,
      },
      {
        path: 'kb/:id',
        element: <KnowledgeBaseDetail />,
      },
      {
        path: 'knowledge-base/:id',
        element: <KnowledgeBaseDetail />,
      },
      {
        path: 'memora-ai',
        element: <MemoraAIHome />,
      },
      {
        path: 'memora-ai/qna',
        element: <AiQnAPage />,
      },
      {
        path: 'memora-ai/question',
        element: <AiQuestionPage />,
      },
      {
        path: 'memora-ai/visualization',
        element: <AiVisualizationPage />,
      },
      // 资源库路由
      {
        path: 'resource',
        element: <ResourceHome />,
      },
      {
        path: 'resource/new',
        element: <ResourceEdit />,
      },
      {
        path: 'resource/edit/:id',
        element: <ResourceEdit />,
      },
      {
        path: 'resource/:id',
        element: <ResourceDetailPage />,
      },
      {
        path: '*',
        element: <NotFound />,
      },
    ],
  },
], {
  future: {
    v7_startTransition: true,
    v7_relativeSplatPath: true,
  },
})

export default router

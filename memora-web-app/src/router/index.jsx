import { Suspense, lazy } from 'react'
import { createBrowserRouter } from 'react-router-dom'

const RequireAuth = lazy(() => import('../components/Auth/RequireAuth'))
const Layout = lazy(() => import('../components/Layout/Layout'))
const LoginPage = lazy(() => import('../pages/Auth/LoginPage'))
const RegisterOwnerPage = lazy(() => import('../pages/Auth/RegisterOwnerPage'))
const AcceptInvitePage = lazy(() => import('../pages/Auth/AcceptInvitePage'))
const PublicSharePage = lazy(() => import('../pages/Share/PublicSharePage'))
const Home = lazy(() => import('../pages/Home/Home'))
const SearchPage = lazy(() => import('../pages/Search/SearchPage'))
const DocumentEditorPage = lazy(() => import('../pages/Document/DocumentEditorPage'))
const DocumentReaderPage = lazy(() => import('../pages/Document/DocumentReaderPage'))
const KnowledgeBaseDetail = lazy(() => import('../pages/KnowledgeBase/KnowledgeBaseDetail'))
const NotFound = lazy(() => import('../pages/NotFound/NotFound'))

const renderLazyPage = (Component) => (
  <Suspense fallback={<div>页面加载中...</div>}>
    <Component />
  </Suspense>
)

const router = createBrowserRouter([
  {
    path: '/login',
    element: renderLazyPage(LoginPage),
  },
  {
    path: '/register',
    element: renderLazyPage(RegisterOwnerPage),
  },
  {
    path: '/accept-invite',
    element: renderLazyPage(AcceptInvitePage),
  },
  {
    path: '/share/:token',
    element: renderLazyPage(PublicSharePage),
  },
  {
    element: renderLazyPage(RequireAuth),
    children: [
      {
        path: '/docs/:documentId',
        element: renderLazyPage(DocumentReaderPage),
      },
      {
        path: '/docs/:documentId/edit',
        element: renderLazyPage(DocumentEditorPage),
      },
      {
        path: '/',
        element: renderLazyPage(Layout),
        children: [
          {
            index: true,
            element: renderLazyPage(Home),
          },
          {
            path: 'search',
            element: renderLazyPage(SearchPage),
          },
          {
            path: 'kb/:id',
            element: renderLazyPage(KnowledgeBaseDetail),
          },
          {
            path: '*',
            element: renderLazyPage(NotFound),
          },
        ],
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

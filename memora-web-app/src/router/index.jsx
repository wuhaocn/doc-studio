import { Suspense, lazy } from 'react'
import { Navigate, createBrowserRouter } from 'react-router-dom'
import { DashboardSkeleton } from '../components/Feedback/Skeleton'

const RequireAuth = lazy(() => import('../components/Auth/RequireAuth'))
const Layout = lazy(() => import('../components/Layout/Layout'))
const LoginPage = lazy(() => import('../pages/Auth/LoginPage'))
const RegisterOwnerPage = lazy(() => import('../pages/Auth/RegisterOwnerPage'))
const AcceptInvitePage = lazy(() => import('../pages/Auth/AcceptInvitePage'))
const CaptureSavePage = lazy(() => import('../pages/Capture/CaptureSavePage'))
const PublicSharePage = lazy(() => import('../pages/Share/PublicSharePage'))
const PublicSitePage = lazy(() => import('../pages/PublicSite/PublicSitePage'))
const Home = lazy(() => import('../pages/Home/Home'))
const SearchPage = lazy(() => import('../pages/Search/SearchPage'))
const DocumentEditorPage = lazy(() => import('../pages/Document/DocumentEditorPage'))
const DocumentReaderPage = lazy(() => import('../pages/Document/DocumentReaderPage'))
const KnowledgeBaseDetail = lazy(() => import('../pages/KnowledgeBase/KnowledgeBaseDetail'))
const NotFound = lazy(() => import('../pages/NotFound/NotFound'))
const WorkspaceManageLayout = lazy(() => import('../pages/WorkspaceManage/WorkspaceManageLayout'))
const WorkspaceManageIndexPage = lazy(() => import('../pages/WorkspaceManage/WorkspaceManageIndexPage'))
const WorkspaceManageMembersPage = lazy(() => import('../pages/WorkspaceManage/WorkspaceManageMembersPage'))
const WorkspaceManageTrashPage = lazy(() => import('../pages/WorkspaceManage/WorkspaceManageTrashPage'))
const WorkspaceManageAccessPage = lazy(() => import('../pages/WorkspaceManage/WorkspaceManageAccessPage'))
const WorkspaceManageSecurityPage = lazy(() => import('../pages/WorkspaceManage/WorkspaceManageSecurityPage'))
const WorkspaceManageAuditPage = lazy(() => import('../pages/WorkspaceManage/WorkspaceManageAuditPage'))

const renderLazyPage = (Component) => (
  <Suspense fallback={<DashboardSkeleton />}>
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
    path: '/capture/save',
    element: renderLazyPage(CaptureSavePage),
  },
  {
    path: '/share/:token',
    element: renderLazyPage(PublicSharePage),
  },
  {
    path: '/site/:siteSlug',
    element: renderLazyPage(PublicSitePage),
  },
  {
    path: '/site/:siteSlug/:publicSlug',
    element: renderLazyPage(PublicSitePage),
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
            path: 'access',
            element: <Navigate to="/workspace/manage/access" replace />,
          },
          {
            path: 'workspace/manage',
            element: renderLazyPage(WorkspaceManageLayout),
            children: [
              {
                index: true,
                element: renderLazyPage(WorkspaceManageIndexPage),
              },
              {
                path: 'members',
                element: renderLazyPage(WorkspaceManageMembersPage),
              },
              {
                path: 'trash',
                element: renderLazyPage(WorkspaceManageTrashPage),
              },
              {
                path: 'access',
                element: renderLazyPage(WorkspaceManageAccessPage),
              },
              {
                path: 'security',
                element: renderLazyPage(WorkspaceManageSecurityPage),
              },
              {
                path: 'audit',
                element: renderLazyPage(WorkspaceManageAuditPage),
              },
            ],
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

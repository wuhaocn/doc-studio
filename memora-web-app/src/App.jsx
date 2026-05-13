import { RouterProvider } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { ConfirmProvider } from './components/Feedback/ConfirmDialog'
import ErrorBoundary from './components/Feedback/ErrorBoundary'
import { ToastProvider } from './components/Feedback/Toast'
import router from './router'

function App() {
  return (
    <ErrorBoundary>
      <ToastProvider>
        <ConfirmProvider>
          <AuthProvider>
            <RouterProvider router={router} />
          </AuthProvider>
        </ConfirmProvider>
      </ToastProvider>
    </ErrorBoundary>
  )
}

export default App

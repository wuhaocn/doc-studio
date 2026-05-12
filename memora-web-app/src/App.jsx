import { RouterProvider } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { ConfirmProvider } from './components/Feedback/ConfirmDialog'
import { ToastProvider } from './components/Feedback/Toast'
import router from './router'

function App() {
  return (
    <ToastProvider>
      <ConfirmProvider>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </ConfirmProvider>
    </ToastProvider>
  )
}

export default App

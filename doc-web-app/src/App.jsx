import { BrowserRouter } from 'react-router-dom'
import Router from './router'
import Layout from './components/Layout/Layout'

function App() {
  return (
    <BrowserRouter
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true,
      }}
    >
      <Layout>
        <Router />
      </Layout>
    </BrowserRouter>
  )
}

export default App


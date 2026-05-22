import React from 'react'
import ReactDOM from 'react-dom/client'
import '@arco-design/web-react/dist/css/arco.css'
import './styles/global.css'
import { bootstrapRuntimeConfig } from './services/runtime/runtimeConfig'

const renderApp = async () => {
  await bootstrapRuntimeConfig()
  const { default: App } = await import('./App')

  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  )
}

renderApp()

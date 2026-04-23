import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'
import { fileURLToPath } from 'url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(currentDir, './src'),
    },
  },
  define: {
    'process.env': {},
  },
  server: {
    port: 8800,
    strictPort: true,
    open: true,
  },
})

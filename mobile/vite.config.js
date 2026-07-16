import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

const apiProxyTarget = process.env.MOBILE_API_PROXY_TARGET || 'http://localhost:8080'

export default defineConfig({
  plugins: [uni()],
  server: {
    proxy: {
      '/api': {
        target: apiProxyTarget,
        changeOrigin: true
      },
      '/herb': {
        target: `${apiProxyTarget}/api`,
        changeOrigin: true
      }
    }
  }
})

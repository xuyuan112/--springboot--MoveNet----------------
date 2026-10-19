import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // Element Plus API 按需自动引入（ElMessage / ElMessageBox 等）
    AutoImport({
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts'
    }),
    // Element Plus 组件按需自动注册（模板中的 el-* 组件）
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts'
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 后端 context-path 即 /api/v1，这里不做 rewrite
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // MinIO 预签名直传/下载同源代理：浏览器只访问 /minio/...，由开发服务器转发，
      // 规避直连 9000 端口的 CORS 限制；生产环境需在 Nginx 配置同名反代
      '/minio': {
        target: 'http://192.168.10.102:9000',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/minio/, '')
      }
    }
  },
  css: {
    preprocessorOptions: {
      // 使用 Sass 现代编译 API，消除 legacy JS API 弃用告警（需 sass >= 1.70）
      scss: { api: 'modern-compiler' }
    }
  }
})

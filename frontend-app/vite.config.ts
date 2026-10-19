import { createReadStream, existsSync, statSync } from 'node:fs'
import { extname, join, normalize } from 'node:path'
import { fileURLToPath, URL } from 'node:url'

import type { Connect, PluginOption } from 'vite'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 用户端（患者/治疗师）前端工程；管理端在 ../frontend
// 本工程面向移动端浏览器，组件库使用 Vant 4（main.ts 全量引入）

/**
 * onnxruntime-web 运行时静态服务（仅 dev）。
 *
 * ort 经动态 import() 加载 /ort/ort-wasm-*.jsep.mjs；依赖预打包/导入分析链路会给该 URL
 * 追加 ?import，而 Vite 对 public 目录文件的模块导入一律 500（“should not be imported
 * from source code”）。生产由 Nginx 直接静态分发 /ort（见 docs/部署约定.md），此处仅为
 * dev 服务器在 Vite 转译中间件之前短路，按原始文件内容返回（不做任何编译处理）。
 */
const ortRuntimePlugin = (): PluginOption => ({
  name: 'serve-ort-runtime-raw',
  configureServer(server) {
    const ortRoot = fileURLToPath(new URL('./public/ort', import.meta.url))
    const mime: Record<string, string> = {
      '.mjs': 'text/javascript; charset=utf-8',
      '.js': 'text/javascript; charset=utf-8',
      '.wasm': 'application/wasm',
      '.map': 'application/json; charset=utf-8'
    }
    const middleware: Connect.NextHandleFunction = (req, res, next) => {
      if (!req.url) return next()
      // 去掉 ?import / 时间戳等查询串，只服务 /ort 前缀，防路径穿越
      const pathname = decodeURIComponent(req.url.split('?')[0])
      if (!pathname.startsWith('/ort/')) return next()
      const rel = normalize(pathname.slice('/ort/'.length))
      if (rel.startsWith('..') || extname(rel) === '') return next()
      const file = join(ortRoot, rel)
      if (!file.startsWith(ortRoot) || !existsSync(file) || !statSync(file).isFile()) return next()
      res.setHeader('Content-Type', mime[extname(file)] ?? 'application/octet-stream')
      res.setHeader('Cache-Control', 'no-cache')
      createReadStream(file).pipe(res)
    }
    // configureServer 内直接 use：pre 钩子，先于 Vite 内置的 public/transform 中间件执行
    server.middlewares.use(middleware)
  }
})

export default defineConfig({
  plugins: [vue(), ortRuntimePlugin()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // 与管理端 5173 区分，便于双端同时联调
    port: 5174,
    // COOP/COEP：启用跨域隔离以支持 onnxruntime-web 的 SharedArrayBuffer 多线程推理；
    // 生产环境 Nginx 必须为用户端站点配置相同响应头（见 docs/部署约定.md）。
    // 所有业务资源均走同源（/api、/minio 均经反代），require-corp 不影响接口调用。
    headers: {
      'Cross-Origin-Opener-Policy': 'same-origin',
      'Cross-Origin-Embedder-Policy': 'require-corp'
    },
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

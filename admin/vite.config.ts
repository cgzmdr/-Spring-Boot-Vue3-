import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      dts: 'src/auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/components.d.ts',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;`
      }
    }
  },
  build: {
    // 不使用手动 manualChunks，避免 element-plus 与业务模块循环依赖。
    chunkSizeWarningLimit: 1000,
  },
  server: {
    port: 5273,
    proxy: {
      // 后端接口代理：/admin、/auth 等均以 / 开头无前缀，直接代理根路径
      // 同源代理可自动携带 Cookie，保证 sa-token 登录态在 /auth/me 等接口正常
      '/auth': { target: 'http://localhost:20256', changeOrigin: true },
      '/admin': { target: 'http://localhost:20256', changeOrigin: true },
      '/ethnic-groups': { target: 'http://localhost:20256', changeOrigin: true },
      '/festivals': { target: 'http://localhost:20256', changeOrigin: true },
      '/arts': { target: 'http://localhost:20256', changeOrigin: true },
      '/topics': { target: 'http://localhost:20256', changeOrigin: true },
      '/search': { target: 'http://localhost:20256', changeOrigin: true },
      '/me': { target: 'http://localhost:20256', changeOrigin: true },
      '/share': { target: 'http://localhost:20256', changeOrigin: true },
      '/contents': { target: 'http://localhost:20256', changeOrigin: true },
      '/sitemap.xml': { target: 'http://localhost:20256', changeOrigin: true },
    }
  }
})

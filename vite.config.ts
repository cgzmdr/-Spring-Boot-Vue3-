import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";
import { compression } from "vite-plugin-compression2";

// https://vite.dev/config/
export default defineConfig(({ mode, isSsrBuild }) => {
	const isProd = mode === "production";
	/**
	 * 是否客户端构建。
	 *
	 * SSR 构建（预渲染用）要区别对待：
	 * · **不能**丢弃 console —— 预渲染脚本靠日志报告进度与排查问题；
	 * · **不能**做 manualChunks —— 服务端没有「首屏分包」概念，
	 *   拆出来的 chunk 反而增加 Node 的模块解析开销。
	 */
	const isClient = !isSsrBuild;

	return {
		plugins: [
			vue(),
			AutoImport({
				resolvers: [ElementPlusResolver()],
				dts: "src/auto-imports.d.ts",
			}),
			Components({
				resolvers: [ElementPlusResolver()],
				dts: "src/components.d.ts",
			}),
			// 预压缩：构建期产出 .br / .gz，由 nginx 的 brotli_static / gzip_static 直接命中，
			// 既省掉逐请求压缩的 CPU，也让现代浏览器拿到比 gzip 更小的体积。
			// 线上目前只有 gzip，且未预压缩，详见 docs/performance-3g.md 的 nginx 配置。
			compression({
				algorithms: ["brotliCompress", "gzip"],
				threshold: 1024,
				include: /\.(js|mjs|css|html|svg|json|txt|xml)$/i,
				exclude: /\.(png|jpe?g|webp|avif|gif|ico|woff2?|ttf|eot)$/i,
			}),
		],
		resolve: {
			alias: {
				"@": fileURLToPath(new URL("./src", import.meta.url)),
			},
		},
		css: {
			preprocessorOptions: {
				scss: {
					additionalData: '@use "@/styles/variables.scss" as *;',
				},
			},
		},
		build: {
			chunkSizeWarningLimit: 800,
			// 生产构建丢弃 console 调用。
			// 走 Rolldown 的 output.minify（Vite 8 的 oxc 压缩链路），
			// 而不是 Vite 顶层 oxc 选项——后者只管语法转换，压缩选项会被忽略。
			// 日志文本几乎无法被压缩算法有效压缩，去掉后主 chunk 明显变小。
			// 仅客户端构建生效：SSR 构建要保留 console，供预渲染脚本报告进度与排查。
			rollupOptions: {
				output: {
					minify:
						isProd && isClient
							? {
									compress: { dropConsole: true, dropDebugger: true },
									mangle: true,
									codegen: true,
								}
							: false,
					/**
					 * 拆包策略（仅客户端）。
					 *
					 * 注意：**不要**把 Element Plus 单独拆成一个入口可达的 chunk。
					 * 实测这样做会让入口产生静态依赖，浏览器必须在首屏前
					 * 多下载 434 KB / gzip 138 KB —— 比不拆还慢。
					 * 这里只把真正「每个页面都要用」的 Vue 运行时拆出来，
					 * 其余交给按需加载的路由分包即可。
					 */
					manualChunks(id) {
						if (!isClient) return;
						if (!id.includes("node_modules")) return;
						if (
							/[\\/]node_modules[\\/](vue|@vue|vue-router|pinia)[\\/]/.test(id)
						)
							return "vendor-vue";
						return;
					},
				},
			},
		},
		/**
		 * SSR / 预渲染构建专用配置。
		 *
		 * `noExternal: true` 是关键：默认情况下 Vite 的 SSR 构建会把
		 * node_modules 里的依赖保留为外部引用（external），运行时才由 Node 去 import。
		 * 但本项目依赖了 Element Plus 的 CSS（theme-chalk 基础样式
		 * 以及按需引入的组件 `style/css`），Node 无法直接 import `.css`，
		 * 会抛 ERR_UNKNOWN_FILE_EXTENSION。
		 * 打开 noExternal 让这些依赖一并被打进 SSR 产物，CSS 在构建期就被处理掉。
		 */
		ssr: {
			noExternal: true,
		},
		server: {
			port: 5173,
			proxy: {
				"/backend-api": {
					target: "http://localhost:20256",
					changeOrigin: true,
					rewrite: (path) => path.replace(/^\/backend-api/, ""),
				},
			},
		},
	};
});

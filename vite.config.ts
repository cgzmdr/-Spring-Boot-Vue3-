import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";

// https://vite.dev/config/
export default defineConfig({
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
	],
	resolve: {
		alias: {
			"@": fileURLToPath(new URL("./src", import.meta.url)),
		},
	},
	css: {
		preprocessorOptions: {
			scss: {
				additionalData: `@use "@/styles/variables.scss" as *;`,
			},
		},
	},
	build: {
		chunkSizeWarningLimit: 800,
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
});

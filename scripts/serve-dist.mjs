/**
 * 本地静态服务器：模拟生产环境的 nginx 行为。
 *
 * 为什么需要它：`vite preview` 不认识「目录下的 index.html」，
 * 对 `/about` 会直接回退到根 index.html —— 于是预渲染好的子页面
 * 在本地怎么测都测不出来（这正是排查过程中踩过的坑）。
 *
 * 本服务器复刻 nginx 的关键规则：
 *   1. 请求路径若命中真实文件 → 直接返回；
 *   2. 若是目录 → 返回该目录下的 index.html；
 *   3. 都不命中 → 回退到 /_spa-shell.html（预渲染页面的动态路由兜底）；
 *   4. 按 Accept-Encoding 返回预压缩的 .br / .gz 副本。
 *
 * 用法：node scripts/serve-dist.mjs [port]
 */
import { createServer } from "node:http";
import { readFile, stat } from "node:fs/promises";
import { existsSync } from "node:fs";
import { join, extname, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { brotliCompressSync, gzipSync, constants } from "node:zlib";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..", "dist");
const PORT = Number(process.argv[2]) || 4500;

const MIME = {
	".html": "text/html; charset=utf-8",
	".js": "application/javascript; charset=utf-8",
	".mjs": "application/javascript; charset=utf-8",
	".css": "text/css; charset=utf-8",
	".json": "application/json; charset=utf-8",
	".svg": "image/svg+xml",
	".webp": "image/webp",
	".png": "image/png",
	".jpg": "image/jpeg",
	".jpeg": "image/jpeg",
	".ico": "image/x-icon",
	".woff2": "font/woff2",
	".txt": "text/plain; charset=utf-8",
	".xml": "application/xml; charset=utf-8",
};

/** 带内容哈希的构建产物：可长期强缓存（与线上 nginx 一致） */
const isHashedAsset = (p) => p.startsWith("/assets/");

async function exists(p) {
	try {
		await stat(p);
		return true;
	} catch {
		return false;
	}
}

const server = createServer(async (req, res) => {
	let urlPath = decodeURIComponent((req.url || "/").split("?")[0]);

	// 路径安全：禁止越出 dist
	const safe = resolve(ROOT, "." + urlPath);
	if (!safe.startsWith(ROOT)) {
		res.writeHead(403).end("Forbidden");
		return;
	}

	let filePath = safe;
	let cacheControl;

	if (await exists(filePath)) {
		const st = await stat(filePath);
		if (st.isDirectory()) {
			// 目录 → 目录下的 index.html
			const idx = join(filePath, "index.html");
			if (existsSync(idx)) {
				filePath = idx;
				cacheControl = "no-cache";
			} else {
				res.writeHead(404).end("Not Found");
				return;
			}
		} else {
			cacheControl = isHashedAsset(urlPath)
				? "public, max-age=31536000, immutable"
				: "no-cache";
		}
	} else if (existsSync(filePath + ".html")) {
		// /about → /about.html（若存在）
		filePath = filePath + ".html";
		cacheControl = "no-cache";
	} else {
		// SPA 兜底：nginx 的 try_files $uri $uri/ /index.html 等价物。
		// 预渲染产物里用 _spa-shell.html 作为空壳（根 index.html 已被首页占用）。
		const shell = join(ROOT, "_spa-shell.html");
		if (!existsSync(shell)) {
			res.writeHead(404).end("Not Found (SPA shell missing)");
			return;
		}
		filePath = shell;
		cacheControl = "no-cache";
	}

	let body;
	try {
		body = await readFile(filePath);
	} catch {
		res.writeHead(404).end("Not Found");
		return;
	}

	const ext = extname(filePath).toLowerCase();
	const type = MIME[ext] || "application/octet-stream";
	const accept = req.headers["accept-encoding"] || "";

	// 压缩：模拟 nginx 的 brotli_static / gzip_static
	const compressible = /^(text\/|application\/(javascript|json|xml))/.test(type);
	let encoding = null;
	if (compressible && body.length > 1024) {
		if (/\bbr\b/.test(accept)) {
			body = brotliCompressSync(body, {
				params: { [constants.BROTLI_PARAM_QUALITY]: 11 },
			});
			encoding = "br";
		} else if (/\bgzip\b/.test(accept)) {
			body = gzipSync(body, { level: 9 });
			encoding = "gzip";
		}
	}

	const headers = {
		"Content-Type": type,
		"Cache-Control": cacheControl,
		"Content-Length": body.length,
	};
	if (encoding) {
		headers["Content-Encoding"] = encoding;
		headers["Vary"] = "Accept-Encoding";
	}

	res.writeHead(200, headers);
	res.end(body);
});

server.listen(PORT, "127.0.0.1", () => {
	console.log(`静态服务器（模拟 nginx try_files）: http://127.0.0.1:${PORT}/`);
	console.log(`  root: ${ROOT}`);
});

import { watch } from 'vue'
import { recommendApi } from '@/api/modules'

/**
 * 浏览行为上报（方向 D：推荐系统的隐式信号）。
 *
 * 在内容详情页调用一次即可：登录用户会记入 user_behavior，
 * 供推荐引擎从中提取「兴趣民族 / 兴趣分类」；未登录时后端静默忽略
 * （不构成个人画像，也不产生任何前端提示）。
 *
 * <b>为什么用 watch 而不是 onMounted：</b>详情数据是异步加载的，
 * 组件挂载时 ID 仍为 null；若在 onMounted 里读一次，上报必然落空。
 * 这里改为监听 ID，一旦拿到就上报一次（每个 ID 只报一次，避免重复写库）。
 */
export function useViewTracking(type: string, getId: () => string | undefined | null) {
	// 已上报过的 ID，防止同一页面内重复上报
	const reported = new Set<string>()

	watch(
		() => getId(),
		(id) => {
			if (!id || reported.has(id)) return
			reported.add(id)
			// 即发即忘：上报失败绝不影响页面阅读
			recommendApi.reportView(type, id).catch(() => {
				/* 静默失败：行为采集不该干扰阅读 */
			})
		},
		{ immediate: true },
	)
}

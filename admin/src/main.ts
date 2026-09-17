import { createApp } from "vue";
import { createPinia } from "pinia";
import {
	ArrowDown,
	Calendar,
	ChatDotRound,
	CircleCheck,
	Collection,
	DocumentChecked,
	Download,
	EditPen,
	Expand,
	Flag,
	Fold,
	Headset,
	List,
	Lock,
	MagicStick,
	Odometer,
	Plus,
	Promotion,
	Share,
	SwitchButton,
	Upload,
	User,
	View,
} from "@element-plus/icons-vue";

import App from "./App.vue";
import router from "./router";
import { setupRouterGuards } from "./router/guards";
import "./styles/index.scss";
// Element Plus 函数式组件（ElMessage / ElMessageBox）样式按需引入
import "element-plus/es/components/message/style/css";
import "element-plus/es/components/message-box/style/css";

const app = createApp(App);

/**
 * 注册全局图标。
 *
 * 约定：凡是在**模板里以字符串**引用（<component :is="'Xxx'" /> 或菜单配置里的 icon 字符串）
 * 的图标都必须注册在这里，否则渲染时会取到 null 组件并抛
 * 「Cannot read properties of null (reading 'ce')」。
 * 审核工作流新增了 List / View / Upload / Download / Promotion / CircleCheck 等环节图标。
 */
const globalIcons = {
	ArrowDown,
	Calendar,
	ChatDotRound,
	CircleCheck,
	Collection,
	DocumentChecked,
	Download,
	EditPen,
	Expand,
	Flag,
	Fold,
	Headset,
	List,
	Lock,
	MagicStick,
	Odometer,
	Plus,
	Promotion,
	Share,
	SwitchButton,
	Upload,
	User,
	View,
};
for (const [key, component] of Object.entries(globalIcons)) {
	app.component(key, component);
}

app.use(createPinia());
// 守卫必须在 app.use(router) **之前**注册：
// vue-router 在 use() 时会立即发起首次导航；若守卫注册在其后，首个导航不经过守卫，
// 未登录用户会直接落到受保护路由，造成状态错乱。
// 同时它又必须在 createPinia() 之后 —— 守卫里的 useUserStore() 需要 Pinia 上下文。
// 因此正确顺序是 use(pinia) → setupRouterGuards() → use(router)。
setupRouterGuards();
app.use(router);

app.mount("#app");

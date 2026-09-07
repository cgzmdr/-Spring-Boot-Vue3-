import { createApp } from "vue";
import { createPinia } from "pinia";
import {
	ArrowDown,
	Calendar,
	Collection,
	DocumentChecked,
	Expand,
	Flag,
	Fold,
	Headset,
	Lock,
	Odometer,
	Plus,
	SwitchButton,
	User,
	EditPen,
} from "@element-plus/icons-vue";

import App from "./App.vue";
import router from "./router";
import "./router/guards";
import "./styles/index.scss";
// Element Plus 函数式组件（ElMessage / ElMessageBox）样式按需引入
import "element-plus/es/components/message/style/css";
import "element-plus/es/components/message-box/style/css";

const app = createApp(App);

// 注册全局图标（侧边栏菜单 / 仪表盘 / 布局内以字符串或模板引用的图标）
const globalIcons = {
	ArrowDown,
	Calendar,
	Collection,
	DocumentChecked,
	Expand,
	Flag,
	Fold,
	Headset,
	Lock,
	Odometer,
	Plus,
	SwitchButton,
	User,
	EditPen,
};
for (const [key, component] of Object.entries(globalIcons)) {
	app.component(key, component);
}

app.use(createPinia());
app.use(router);

app.mount("#app");

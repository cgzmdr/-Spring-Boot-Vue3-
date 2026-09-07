import { createApp } from "vue";
import { createPinia } from "pinia";

import App from "./App.vue";
import router from "./router";

import "./styles/common.css";
// Element Plus 函数式组件（ElMessage）样式按需引入
import "element-plus/es/components/message/style/css";
import "./styles/index.scss";
const app = createApp(App);

app.use(createPinia());
app.use(router);

// 应用持久化的页面字号设置（在挂载前生效，避免闪烁）
import { useSettingsStore } from "./stores/settings";
useSettingsStore().apply();

app.mount("#app");

// 静默恢复登录态（失败不影响浏览）
import { useAuthStore } from "./stores/auth";
useAuthStore().restore();

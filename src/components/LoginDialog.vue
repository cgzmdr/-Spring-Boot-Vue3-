<script setup lang="ts">
import { ref, onMounted, reactive, onBeforeUnmount, nextTick } from "vue";
import { ElMessage } from "element-plus";
import { useAuthStore } from "@/stores/auth";
import type { FormInstance, FormRules } from "element-plus";
const auth = useAuthStore();

const ruleFormRef = ref<FormInstance>();
const visible = ref(false);
const mode = ref<"login" | "register">("login");

const form = ref({ account: "", nickname: "", password: "" });
const loading = ref(false);

function open() {
	visible.value = true;
	mode.value = "login";
	form.value = { account: "", nickname: "", password: "" };
}

function onAuthRequired() {
	open();
}

function switchMode(m: "login" | "register") {
	form.value = {
		nickname: "",
		account: "",
		password: "",
	};
	mode.value = m;
}

async function submit() {
	if (mode.value === "login" && !form.value.account.trim()) {
		ElMessage.warning("请输入账号（用户名 / 邮箱 / 手机号）");
		return;
	}
	if (mode.value === "register" && !form.value.nickname.trim()) {
		ElMessage.warning("请输入昵称");
		return;
	}
	if (!form.value.password) {
		ElMessage.warning("请输入密码");
		return;
	}
	loading.value = true;
	try {
		if (mode.value === "login") {
			await auth.login(form.value.account.trim(), form.value.password);
		} else {
			await auth.register(form.value.nickname.trim(), form.value.password);
		}
		ElMessage.success(mode.value === "login" ? "登录成功" : "注册成功");
		visible.value = false;
	} catch {
		/* 错误提示已由拦截器处理 */
	} finally {
		loading.value = false;
	}
}

onMounted(() => window.addEventListener("auth:required", onAuthRequired));
onBeforeUnmount(() =>
	window.removeEventListener("auth:required", onAuthRequired),
);

defineExpose({ open });
void nextTick;

const validatePass = (_rule: any, value: any, callback: any) => {
	const strongPasswordRegex =
		/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/;
	if (!strongPasswordRegex.test(value)) {
		callback(new Error("至少8位，包含大写字母、小写字母、数字、特殊字符"));
	} else {
		if (form.value.password !== "") {
			if (!ruleFormRef.value) return;
			ruleFormRef.value.validateField("password");
		}
		callback();
	}
};
const validatePass2 = (_rule: any, value: any, callback: any) => {
	const nicknameRegex = /^[\w\u4e00-\u9fa5]{4,16}$/;
	if (!nicknameRegex.test(value)) {
		callback(new Error("长度为4-16位，仅支持中文、英文字母、数字和下划线"));
	} else {
		callback();
	}
};
const validatePass3 = (_rule: any, value: any, callback: any) => {
	if (!value || !value.trim()) {
		return callback(new Error("请输入账号（用户名 / 邮箱 / 手机号）"));
	}
	const account = value.trim();
	const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
	if (emailRegex.test(account)) {
		return callback();
	}
	const phoneRegex = /^1[3-9]\d{9}$/;
	if (phoneRegex.test(account)) {
		return callback();
	}
	const usernameRegex = /^[\w\u4e00-\u9fa5]{4,16}$/;
	if (usernameRegex.test(account)) {
		return callback();
	}
	callback(new Error("账号格式不正确，请输入有效的用户名、邮箱或手机号"));
};
const rules = reactive<FormRules<typeof form>>({
	password: [{ validator: validatePass, trigger: "blur" }],
	nickname: [{ validator: validatePass2, trigger: "blur" }],
	account: [{ validator: validatePass3, trigger: "blur" }],
});
</script>

<template>
	<el-dialog
		v-model="visible"
		:title="mode === 'login' ? '登录' : '注册'"
		width="400px"
		align-center
		destroy-on-close
	>
		<div class="login-switch">
			<button
				:class="{ active: mode === 'login' }"
				@click="switchMode('login')"
			>
				登录
			</button>
			<button
				:class="{ active: mode === 'register' }"
				@click="switchMode('register')"
			>
				注册
			</button>
		</div>

		<el-form
			ref="ruleFormRef"
			label-position="top"
			:model="form"
			@submit.prevent="submit"
			:rules="rules"
		>
			<el-form-item
				v-if="mode === 'register'"
				label="昵称"
				prop="nickname"
			>
				<el-input
					v-model="form.nickname"
					placeholder="设置昵称（可用于登录）"
					maxlength="20"
				/>
			</el-form-item>
			<el-form-item
				v-if="mode === 'login'"
				label="账号"
				prop="account"
			>
				<el-input
					v-model="form.account"
					placeholder="用户名 / 邮箱 / 手机号"
					@keyup.enter="submit"
				/>
			</el-form-item>
			<el-form-item
				label="密码"
				prop="password"
			>
				<el-input
					v-model="form.password"
					type="password"
					show-password
					placeholder="密码"
					@keyup.enter="submit"
				/>
			</el-form-item>
			<el-button
				type="primary"
				class="login-btn"
				:loading="loading"
				@click="submit"
			>
				{{ mode === "login" ? "登 录" : "注 册" }}
			</el-button>
		</el-form>
	</el-dialog>
</template>

<style scoped>
.login-switch {
	display: flex;
	border-bottom: 2px solid var(--ink);
	margin-bottom: 18px;
}
.login-switch button {
	flex: 1;
	padding: 10px 0;
	border: none;
	background: none;
	font-size: 14px;
	letter-spacing: 2px;
	color: var(--muted);
	cursor: pointer;
	border-bottom: 2px solid transparent;
	margin-bottom: -2px;
	font-family: var(--serif);
}
.login-switch button.active {
	color: var(--accent);
	font-weight: 700;
	border-color: var(--accent);
}
.login-btn {
	width: 100%;
	margin-top: 6px;
	border: none;
	background: var(--accent);
}
</style>

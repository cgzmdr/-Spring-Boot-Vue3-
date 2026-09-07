<template>
	<div class="login-page">
		<div class="login-page-2">
			<div class="login-card">
				<div class="brand">
					<img src="/logo.svg" alt="logo" class="brand-icon" />
					<h1>走进多彩 56 个民族世界</h1>
					<p>OA 管理系统</p>
				</div>
				<el-form
					ref="formRef"
					:model="form"
					:rules="rules"
					size="large"
					@keyup.enter="handleLogin"
				>
					<el-form-item prop="account">
						<el-input
							v-model="form.account"
							placeholder="账号（邮箱 / 手机号）"
							:prefix-icon="User"
							clearable
						/>
					</el-form-item>
					<el-form-item prop="password">
						<el-input
							v-model="form.password"
							type="password"
							placeholder="密码"
							:prefix-icon="Lock"
							show-password
						/>
					</el-form-item>
					<el-button
						type="primary"
						class="login-btn"
						:loading="loading"
						@click="handleLogin"
					>
						登 录
					</el-button>
				</el-form>
			</div>
		</div>
	</div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { User, Lock } from "@element-plus/icons-vue";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({ account: "", password: "" });

const rules: FormRules = {
	account: [{ required: true, message: "请输入账号", trigger: "blur" }],
	password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

async function handleLogin() {
	const valid = await formRef.value?.validate().catch(() => false);
	if (!valid) return;
	loading.value = true;
	try {
		await userStore.login(form.account, form.password);
		ElMessage.success("登录成功");
		const redirect = (route.query.redirect as string) || "/dashboard";
		router.replace(redirect);
	} catch {
		// 错误已由拦截器提示
	} finally {
		loading.value = false;
	}
}
</script>

<style scoped lang="scss">
.login-page {
	height: 100vh;
	display: flex;
	align-items: center;
	justify-content: center;
	background: url("../../../assets/background.png") center center no-repeat;
	background-size: 100%;
	.login-page-2 {
		width: 100vw;
		height: 100vh;
		display: flex;
		align-items: center;
		justify-content: center;
		backdrop-filter: blur(5px);
	}
	.login-card {
		width: 400px;
		background: #fff;
		border-radius: 12px;
		padding: 40px 36px;
		box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);

		.brand {
			text-align: center;
			margin-bottom: 28px;

			.brand-icon {
				width: 56px;
				height: 56px;
				border-radius: 12px;
				display: block;
				margin: 0 auto 14px;
			}

			h1 {
				font-size: 18px;
				margin: 0 0 4px;
				color: #1f2937;
			}

			p {
				margin: 0;
				font-size: 13px;
				color: #9ca3af;
				letter-spacing: 4px;
			}
		}

		.login-btn {
			width: 100%;
		}
	}
}
</style>

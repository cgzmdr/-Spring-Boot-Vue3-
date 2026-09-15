<script setup lang="ts">
import { ref, computed, reactive, onMounted, onBeforeUnmount } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElUpload } from "element-plus";
import type { UploadFile } from "element-plus";
import { useAuthStore } from "@/stores/auth";
import { useLangStore } from "@/stores/lang";
import { useSettingsStore, FONT_SIZE_OPTIONS } from "@/stores/settings";
import type { FontSize } from "@/stores/settings";
import { authApi, interactionApi, meApi } from "@/api/modules";
import type { FavoriteItem, PasswordChangePayload } from "@/api/types";
import {
	favoriteRoutePath,
	favoriteTypeLabel,
	formatDate,
	formatDateTime,
	resolveStaticUrl,
} from "@/utils/format";
import PageHead from "@/components/PageHead.vue";
import AppPagination from "@/components/AppPagination.vue";

const router = useRouter();
const auth = useAuthStore();
const lang = useLangStore();
const settings = useSettingsStore();

/* ============================== 通用 ============================== */

const STRONG_PWD =
	/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/;
const EMAIL_RE = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
const MOBILE_RE = /^1[3-9]\d{9}$/;

const roleLabel = computed(() => {
	const roles = auth.user?.roles || [];
	if (roles.includes("super_admin")) return "超级管理员";
	if (roles.includes("content_admin")) return "内容管理员";
	return "普通用户";
});

const userInitial = computed(() =>
	auth.user?.nickname
		? auth.user.nickname.trim().slice(0, 1).toUpperCase()
		: "民",
);

const avatarSrc = computed(() => resolveStaticUrl(auth.user?.avatar));

let cdTimer: ReturnType<typeof setInterval> | null = null;
const countdown = ref(0);
function startCountdown() {
	countdown.value = 60;
	cdTimer = setInterval(() => {
		countdown.value -= 1;
		if (countdown.value <= 0 && cdTimer) {
			clearInterval(cdTimer);
			cdTimer = null;
		}
	}, 1000);
}
onBeforeUnmount(() => {
	if (cdTimer) clearInterval(cdTimer);
});

/** 刷新用户信息（昵称/头像/绑定/密保） */
async function refreshMe() {
	try {
		await auth.fetchMe();
	} catch {
		/* 登录态失效时拦截器已处理 */
	}
}

/* ========================== 一、基本信息 ========================== */

/** 头像上传（Data URL，暂存本地，点击保存后提交） */
const avatarDraft = ref("");
const avatarSaving = ref(false);
function onAvatarChange(file: UploadFile) {
	const raw = file.raw;
	if (!raw) return;
	if (!raw.type.startsWith("image/")) {
		ElMessage.warning("请选择图片文件");
		return;
	}
	if (raw.size > 2 * 1024 * 1024) {
		ElMessage.warning("图片不能超过 2MB");
		return;
	}
	const reader = new FileReader();
	reader.onload = () => (avatarDraft.value = reader.result as string);
	reader.readAsDataURL(raw);
}
async function saveAvatar() {
	if (!auth.user?.id) return;
	avatarSaving.value = true;
	try {
		await authApi.update(auth.user.id, { avatar: avatarDraft.value });
		avatarDraft.value = "";
		await refreshMe();
		ElMessage.success("头像已更新");
	} catch {
		/* 拦截器已提示 */
	} finally {
		avatarSaving.value = false;
	}
}
async function removeAvatar() {
	if (!auth.user?.id) return;
	avatarSaving.value = true;
	try {
		await authApi.update(auth.user.id, { avatar: "" });
		await refreshMe();
		ElMessage.success("已移除头像");
	} catch {
		/* 拦截器已提示 */
	} finally {
		avatarSaving.value = false;
	}
}

/** 昵称 */
const nicknameForm = reactive({ nickname: "" });
const nicknameSaving = ref(false);
async function saveNickname() {
	const v = nicknameForm.nickname.trim();
	if (!/^[\w\u4e00-\u9fa5]{4,16}$/.test(v)) {
		ElMessage.warning("昵称长度为 4-16 位，仅支持中文、英文、数字和下划线");
		return;
	}
	if (!auth.user?.id) return;
	nicknameSaving.value = true;
	try {
		await authApi.update(auth.user.id, { nickname: v });
		await refreshMe();
		ElMessage.success("昵称已更新");
	} catch {
		/* 拦截器已提示 */
	} finally {
		nicknameSaving.value = false;
	}
}

/** 邮箱绑定 */
const emailForm = reactive({ email: "", code: "" });
const emailBinding = ref(false);
const emailSending = ref(false);
const emailValid = computed(() => EMAIL_RE.test(emailForm.email.trim()));
async function sendEmailCode() {
	if (!emailValid.value) {
		ElMessage.warning("请输入正确的邮箱地址");
		return;
	}
	emailSending.value = true;
	try {
		await authApi.sendCode(emailForm.email.trim());
		ElMessage.success("验证码已发送至邮箱，10 分钟内有效");
		startCountdown();
	} catch {
		/* 拦截器已提示 */
	} finally {
		emailSending.value = false;
	}
}
async function bindEmail() {
	if (!emailValid.value) {
		ElMessage.warning("请输入正确的邮箱地址");
		return;
	}
	if (!emailForm.code.trim()) {
		ElMessage.warning("请输入验证码");
		return;
	}
	if (!auth.user?.id) return;
	emailBinding.value = true;
	try {
		await authApi.checkCode(emailForm.email.trim(), emailForm.code.trim());
		await authApi.update(auth.user.id, { email: emailForm.email.trim() });
		emailForm.code = "";
		await refreshMe();
		ElMessage.success("邮箱绑定成功");
	} catch {
		/* 拦截器已提示 */
	} finally {
		emailBinding.value = false;
	}
}

/** 手机号绑定（短信通道暂未开放） */
const mobileForm = reactive({ mobile: "", code: "" });

/* ========================== 二、账号密码 ========================== */

type PwdMethod = "old" | "code" | "security";
const pwdForm = reactive({
	method: "old" as PwdMethod,
	oldPassword: "",
	account: "",
	code: "",
	securityAnswer: "",
	newPassword: "",
	confirm: "",
});
const pwdSubmitting = ref(false);
const pwdSending = ref(false);
const pwdCodeTarget = computed(() => {
	if (pwdForm.account.trim()) return pwdForm.account.trim();
	return auth.user?.email || auth.user?.mobile || "";
});
const pwdCodeIsMobile = computed(() => MOBILE_RE.test(pwdCodeTarget.value));
const securitySet = computed(() => !!auth.user?.securityQuestion);

async function sendPwdCode() {
	const target = pwdCodeTarget.value;
	if (!target) {
		ElMessage.warning("请先绑定邮箱或手机号，或手动输入账号");
		return;
	}
	if (!EMAIL_RE.test(target) && !MOBILE_RE.test(target)) {
		ElMessage.warning("账号格式不正确，请输入有效的邮箱或手机号");
		return;
	}
	if (MOBILE_RE.test(target)) {
		ElMessage.warning("手机短信通道暂未开放，请使用邮箱验证码");
		return;
	}
	pwdSending.value = true;
	try {
		await authApi.sendCode(target);
		ElMessage.success("验证码已发送至邮箱，10 分钟内有效");
		startCountdown();
	} catch {
		/* 拦截器已提示 */
	} finally {
		pwdSending.value = false;
	}
}

async function submitPassword() {
	const np = pwdForm.newPassword;
	if (!np) {
		ElMessage.warning("请输入新密码");
		return;
	}
	if (!STRONG_PWD.test(np)) {
		ElMessage.warning("新密码至少 8 位，需包含大小写字母、数字和特殊字符");
		return;
	}
	if (np !== pwdForm.confirm) {
		ElMessage.warning("两次输入的密码不一致");
		return;
	}
	const payload: PasswordChangePayload = { newPassword: np };
	if (pwdForm.method === "old") {
		if (!pwdForm.oldPassword) {
			ElMessage.warning("请输入旧密码");
			return;
		}
		payload.oldPassword = pwdForm.oldPassword;
	} else if (pwdForm.method === "code") {
		const target = pwdCodeTarget.value;
		if (!target) {
			ElMessage.warning("请输入接收验证码的账号");
			return;
		}
		if (!pwdForm.code) {
			ElMessage.warning("请输入验证码");
			return;
		}
		payload.account = target;
		payload.code = pwdForm.code;
	} else {
		if (!securitySet.value) {
			ElMessage.warning("尚未设置密保问题，请先在下方向设置密保");
			return;
		}
		if (!pwdForm.securityAnswer.trim()) {
			ElMessage.warning("请输入密保答案");
			return;
		}
		payload.securityAnswer = pwdForm.securityAnswer.trim();
	}

	pwdSubmitting.value = true;
	try {
		await authApi.changePassword(payload);
		ElMessage.success("密码修改成功，请牢记新密码");
		pwdForm.oldPassword = "";
		pwdForm.code = "";
		pwdForm.securityAnswer = "";
		pwdForm.newPassword = "";
		pwdForm.confirm = "";
	} catch {
		/* 拦截器已提示 */
	} finally {
		pwdSubmitting.value = false;
	}
}

/** 密保问题 */
const SECURITY_QUESTIONS = [
	"您母亲的姓名是？",
	"您父亲的姓名是？",
	"您配偶的姓名是？",
	"您出生的城市是？",
	"您小学的名称是？",
	"您小学班主任的姓氏是？",
	"您宠物的名字是？",
	"您最喜欢的一本书是？",
	"您最喜欢的一道菜是？",
];
const secForm = reactive({ question: "", answer: "", confirm: "" });
const secSaving = ref(false);
const secEditing = ref(false);
async function saveSecurity() {
	if (!secForm.question) {
		ElMessage.warning("请选择密保问题");
		return;
	}
	if (!secForm.answer.trim()) {
		ElMessage.warning("请输入密保答案");
		return;
	}
	if (secForm.answer.trim() !== secForm.confirm.trim()) {
		ElMessage.warning("两次输入的密保答案不一致");
		return;
	}
	if (!auth.user?.id) return;
	secSaving.value = true;
	try {
		await authApi.update(auth.user.id, {
			securityQuestion: secForm.question,
			securityAnswer: secForm.answer.trim(),
		});
		await refreshMe();
		secEditing.value = false;
		ElMessage.success("密保问题设置成功");
	} catch {
		/* 拦截器已提示 */
	} finally {
		secSaving.value = false;
	}
}

/* ========================== 三、基础设置 ========================== */

const fontOptions = FONT_SIZE_OPTIONS;
const langOptions = [
	{ value: "zh", label: "中文" },
	{ value: "en", label: "English" },
];
/** el-radio-group 的值类型较宽，这里收窄为 FontSize 后交给 store */
function onFontSizeChange(v: string | number | boolean | undefined) {
	if (typeof v === "string") settings.setFontSize(v as FontSize);
}
async function logout() {
	await auth.logout();
	ElMessage.success("已退出登录");
	router.replace("/");
}

/* ========================== 四、我的收藏 ========================== */

const favType = ref("");
const favorites = ref<FavoriteItem[]>([]);
const favTotal = ref(0);
const favLoading = ref(false);
const favPage = ref(0);
const favSize = 8;

const favFilters = computed(() => [
	{ value: "", label: lang.pick("全部", "All") },
	{ value: "ethnic", label: "民族" },
	{ value: "festival", label: "节日" },
	{ value: "art", label: "艺术" },
	{ value: "topic", label: "专题" },
	{ value: "food", label: "美食" },
]);

async function loadFavorites(page = favPage.value) {
	if (!auth.isLoggedIn) return;
	favLoading.value = true;
	try {
		const res = await meApi.favorites(
			favType.value || undefined,
			page,
			favSize,
		);
		favorites.value = res.data || [];
		favTotal.value = res.total || 0;
		favPage.value = page;
	} catch {
		/* 拦截器已提示 */
	} finally {
		favLoading.value = false;
	}
}

function switchFavType(t: string) {
	favType.value = t;
	loadFavorites(0);
}

function onFavPage(p: number) {
	loadFavorites(p);
}

function openFavorite(item: FavoriteItem) {
	const base = favoriteRoutePath[item.entryType];
	if (base) router.push(`${base}/${item.entryId}`);
}

async function removeFavorite(item: FavoriteItem) {
	try {
		await interactionApi.unfavorite(item.entryType, item.entryId);
		ElMessage.success("已取消收藏");
		// 当前页删除后若为空且非首页，回退一页
		if (favorites.value.length === 1 && favPage.value > 0) {
			loadFavorites(favPage.value - 1);
		} else {
			loadFavorites();
		}
	} catch {
		/* 拦截器已提示 */
	}
}

/* ============================== 挂载 ============================== */

onMounted(async () => {
	if (!auth.isLoggedIn) {
		window.dispatchEvent(new Event("auth:required"));
		router.replace("/");
		return;
	}
	await refreshMe();
	nicknameForm.nickname = auth.user?.nickname || "";
	emailForm.email = auth.user?.email || "";
	mobileForm.mobile = auth.user?.mobile || "";
	pwdForm.account = auth.user?.email || auth.user?.mobile || "";
	loadFavorites();
});
</script>

<template>
	<div class="page">
		<PageHead
			:kicker="'MY PROFILE'"
			:title="lang.pick('个人中心', 'My Profile')"
			:dek="
				lang.pick(
					'我的资料 · 账号安全 · 偏好设置 · 收藏',
					'Profile · Account · Preferences · Favorites',
				)
			"
		/>

		<div class="profile-wrap">
			<el-tabs
				tab-position="left"
				class="profile-tabs"
				:before-leave="() => auth.isLoggedIn"
			>
				<!-- 一、基本信息 -->
				<el-tab-pane
					:label="lang.pick('基本信息', 'Basic')"
					lazy
				>
					<div class="pane">
						<section class="card profile-card">
							<div class="avatar-box">
								<div class="avatar">
									<img
										v-if="avatarDraft || avatarSrc"
										:src="avatarDraft || avatarSrc"
										alt="头像"
									/>
									<span v-else>{{ userInitial }}</span>
								</div>
								<div class="avatar-actions">
									<el-upload
										action="#"
										:auto-upload="false"
										:show-file-list="false"
										accept="image/*"
										:on-change="onAvatarChange"
									>
										<el-button size="small">修改头像</el-button>
									</el-upload>
									<el-button
										v-if="avatarDraft"
										size="small"
										type="primary"
										:loading="avatarSaving"
										@click="saveAvatar"
									>
										保存头像
									</el-button>
									<el-button
										v-if="!avatarDraft && auth.user?.avatar"
										size="small"
										:disabled="avatarSaving"
										@click="removeAvatar"
									>
										移除头像
									</el-button>
								</div>
								<p class="avatar-tip">支持 JPG / PNG，不超过 2MB</p>
							</div>
							<div class="info">
								<h3>{{ auth.user?.nickname || "—" }}</h3>
								<p class="meta">
									{{ roleLabel }} · ID: {{ auth.user?.id?.slice(0, 8) }}
								</p>
								<p class="meta">
									{{ lang.pick("注册于", "Member since") }}
									{{ formatDate(auth.user?.createdAt) }}
								</p>
							</div>
						</section>

						<section class="card">
							<h4 class="sec-title">
								{{ lang.pick("账号绑定", "Account Binding") }}
							</h4>

							<div class="bind-row">
								<span class="bind-label">邮箱</span>
								<el-input
									v-model="emailForm.email"
									class="bind-input"
									placeholder="请输入邮箱"
									maxlength="128"
								/>
								<el-button
									class="bind-btn"
									:disabled="!emailValid || countdown > 0"
									:loading="emailSending"
									@click="sendEmailCode"
								>
									{{ countdown > 0 ? `${countdown}s` : "发送验证码" }}
								</el-button>
								<el-input
									v-model="emailForm.code"
									class="bind-code"
									placeholder="验证码"
									maxlength="6"
								/>
								<el-button
									type="primary"
									:disabled="!emailValid"
									:loading="emailBinding"
									@click="bindEmail"
								>
									{{ auth.user?.email ? "更换绑定" : "绑定邮箱" }}
								</el-button>
							</div>
							<p class="bind-tip">
								当前绑定：
								<strong>{{ auth.user?.email || "未绑定" }}</strong>
								—— 验证码将发送至该邮箱，10 分钟内有效。
							</p>

							<div class="bind-row">
								<span class="bind-label">手机号</span>
								<el-input
									v-model="mobileForm.mobile"
									class="bind-input"
									placeholder="请输入手机号"
									maxlength="11"
									disabled
								/>
								<el-button
									class="bind-btn"
									disabled
									>发送验证码</el-button
								>
								<el-input
									v-model="mobileForm.code"
									class="bind-code"
									placeholder="验证码"
									maxlength="6"
									disabled
								/>
								<el-button
									type="primary"
									disabled
									>绑定手机号</el-button
								>
							</div>
							<p class="bind-tip">
								当前绑定：
								<strong>{{ auth.user?.mobile || "未绑定" }}</strong>
								—— 手机短信通道暂未开放（
								<em>手机号暂时不支持</em>
								），上线后可通过短信验证码绑定。
							</p>
						</section>

						<section class="card">
							<h4 class="sec-title">{{ lang.pick("昵称", "Nickname") }}</h4>
							<div class="bind-row">
								<el-input
									v-model="nicknameForm.nickname"
									class="bind-input"
									placeholder="4-16 位，支持中文、英文、数字和下划线"
									maxlength="16"
								/>
								<el-button
									type="primary"
									class="bind-btn"
									:loading="nicknameSaving"
									@click="saveNickname"
								>
									保存昵称
								</el-button>
							</div>
						</section>
					</div>
				</el-tab-pane>

				<!-- 二、账号密码 -->
				<el-tab-pane
					:label="lang.pick('账号密码', 'Account')"
					lazy
				>
					<div class="pane">
						<section class="card">
							<h4 class="sec-title">
								{{ lang.pick("修改密码", "Change Password") }}
							</h4>
							<p class="sec-desc">
								修改密码需先通过一种方式验证身份：旧密码、邮箱验证码或密保问题。
							</p>

							<div class="pwd-methods">
								<el-radio-group v-model="pwdForm.method">
									<el-radio value="old">旧密码</el-radio>
									<el-radio value="code">短信 / 邮箱验证码</el-radio>
									<el-radio value="security">密保问题</el-radio>
								</el-radio-group>
							</div>

							<el-form
								label-width="110px"
								class="pwd-form"
								@submit.prevent
							>
								<el-form-item
									v-if="pwdForm.method === 'old'"
									label="旧密码"
								>
									<el-input
										v-model="pwdForm.oldPassword"
										type="password"
										show-password
										placeholder="请输入当前密码"
										maxlength="32"
									/>
								</el-form-item>

								<template v-if="pwdForm.method === 'code'">
									<el-form-item label="验证账号">
										<div class="code-row">
											<el-input
												v-model="pwdForm.account"
												placeholder="已绑定邮箱 / 手机号"
												maxlength="128"
											/>
											<el-button
												:disabled="countdown > 0 || pwdCodeIsMobile"
												:loading="pwdSending"
												@click="sendPwdCode"
											>
												{{ countdown > 0 ? `${countdown}s` : "发送验证码" }}
											</el-button>
										</div>
									</el-form-item>
									<p
										v-if="pwdCodeIsMobile"
										class="field-tip"
									>
										手机短信通道暂未开放，请使用邮箱接收验证码。
									</p>
									<el-form-item label="验证码">
										<el-input
											v-model="pwdForm.code"
											placeholder="6 位验证码"
											maxlength="6"
										/>
									</el-form-item>
								</template>

								<template v-if="pwdForm.method === 'security'">
									<el-form-item label="密保问题">
										<el-input
											:model-value="
												auth.user?.securityQuestion || '尚未设置密保问题'
											"
											disabled
										/>
									</el-form-item>
									<p
										v-if="!securitySet"
										class="field-tip"
									>
										尚未设置密保问题，请先在下方向「设置密保」后使用该方式。
									</p>
									<el-form-item label="密保答案">
										<el-input
											v-model="pwdForm.securityAnswer"
											placeholder="请输入密保答案"
											maxlength="64"
										/>
									</el-form-item>
								</template>

								<el-form-item label="新密码">
									<el-input
										v-model="pwdForm.newPassword"
										type="password"
										show-password
										placeholder="8 位以上，含大小写字母、数字、特殊字符"
										maxlength="32"
									/>
								</el-form-item>
								<el-form-item label="确认新密码">
									<el-input
										v-model="pwdForm.confirm"
										type="password"
										show-password
										placeholder="再次输入新密码"
										maxlength="32"
									/>
								</el-form-item>
								<el-form-item>
									<el-button
										type="primary"
										:loading="pwdSubmitting"
										@click="submitPassword"
									>
										确认修改
									</el-button>
								</el-form-item>
							</el-form>
						</section>

						<section class="card">
							<h4 class="sec-title">
								{{ lang.pick("密保问题", "Security Question") }}
							</h4>
							<p class="sec-desc">
								设置密保问题后，忘记密码时可凭「密保答案」验证身份并修改密码。
							</p>

							<div
								v-if="securitySet && !secEditing"
								class="sec-current"
							>
								<p class="bind-tip">
									已设置：
									<strong>{{ auth.user?.securityQuestion }}</strong>
								</p>
								<el-button
									size="small"
									@click="
										() => {
											secEditing = true;
											secForm.question = auth.user?.securityQuestion || '';
											secForm.answer = '';
											secForm.confirm = '';
										}
									"
								>
									重新设置
								</el-button>
							</div>

							<el-form
								v-else
								label-width="110px"
								class="pwd-form"
								@submit.prevent
							>
								<el-form-item label="密保问题">
									<el-select
										v-model="secForm.question"
										placeholder="请选择密保问题"
										style="width: 100%"
									>
										<el-option
											v-for="q in SECURITY_QUESTIONS"
											:key="q"
											:label="q"
											:value="q"
										/>
									</el-select>
								</el-form-item>
								<el-form-item label="密保答案">
									<el-input
										v-model="secForm.answer"
										placeholder="请输入答案"
										maxlength="64"
									/>
								</el-form-item>
								<el-form-item label="确认答案">
									<el-input
										v-model="secForm.confirm"
										placeholder="再次输入答案"
										maxlength="64"
									/>
								</el-form-item>
								<el-form-item>
									<el-button
										type="primary"
										:loading="secSaving"
										@click="saveSecurity"
									>
										{{ securitySet ? "更新密保" : "设置密保" }}
									</el-button>
									<el-button
										v-if="securitySet"
										@click="secEditing = false"
										>取消</el-button
									>
								</el-form-item>
							</el-form>
						</section>
					</div>
				</el-tab-pane>

				<!-- 三、基础设置 -->
				<el-tab-pane
					:label="lang.pick('基础设置', 'Settings')"
					lazy
				>
					<div class="pane">
						<section class="card">
							<h4 class="sec-title">
								{{ lang.pick("页面字号", "Font Size") }}
							</h4>
							<p class="sec-desc">调整页面的显示字号（整体缩放）。</p>
							<el-radio-group
								:model-value="settings.fontSize"
								@update:model-value="onFontSizeChange"
							>
								<el-radio
									v-for="o in fontOptions"
									:key="o.value"
									:value="o.value"
								>
									{{ o.label }}
								</el-radio>
							</el-radio-group>
						</section>

						<section class="card">
							<h4 class="sec-title">{{ lang.pick("界面语言", "Language") }}</h4>
							<p class="sec-desc">语言偏好会随账号保存，下次登录自动恢复。</p>
							<el-radio-group
								:model-value="lang.lang"
								@update:model-value="(v: any) => lang.setLang(v)"
							>
								<el-radio
									v-for="o in langOptions"
									:key="o.value"
									:value="o.value"
								>
									{{ o.label }}
								</el-radio>
							</el-radio-group>
						</section>

						<section class="card">
							<h4 class="sec-title">{{ lang.pick("账号", "Account") }}</h4>
							<p class="sec-desc">退出当前登录账号。</p>
							<el-button
								type="danger"
								plain
								@click="logout"
							>
								{{ lang.pick("退出登录", "Sign out") }}
							</el-button>
						</section>
					</div>
				</el-tab-pane>

				<!-- 四、我的收藏 -->
				<el-tab-pane
					:label="`${lang.pick('我的收藏', 'Favorites')} (${favTotal})`"
					lazy
				>
					<div class="pane">
						<div class="fav-filter">
							<button
								v-for="f in favFilters"
								:key="f.value"
								class="chip"
								:class="{ active: favType === f.value }"
								@click="switchFavType(f.value)"
							>
								{{ f.label }}
							</button>
						</div>

						<div v-loading="favLoading">
							<el-empty
								v-if="!favLoading && favorites.length === 0"
								:description="
									lang.pick(
										'暂无收藏，去发现感兴趣的内容吧',
										'No favorites yet',
									)
								"
							>
								<el-button
									type="primary"
									@click="router.push('/')"
								>
									{{ lang.pick("去逛逛", "Explore") }}
								</el-button>
							</el-empty>

							<ul
								v-else
								class="fav-list"
							>
								<li
									v-for="item in favorites"
									:key="item.id"
									class="fav-item"
								>
									<div
										class="fav-thumb"
										@click="openFavorite(item)"
									>
										<img
											v-if="item.coverImage"
											:src="resolveStaticUrl(item.coverImage)"
											:alt="item.entryName"
										/>
										<span v-else>{{ item.entryName.slice(0, 1) }}</span>
									</div>
									<div
										class="fav-main"
										@click="openFavorite(item)"
									>
										<h5>{{ item.entryName }}</h5>
										<p class="fav-meta">
											<span class="fav-type">{{
												favoriteTypeLabel[item.entryType] || item.entryType
											}}</span>
											· {{ formatDateTime(item.createdAt) }}
										</p>
									</div>
									<div class="fav-actions">
										<el-button
											size="small"
											@click="openFavorite(item)"
										>
											{{ lang.pick("查看", "View") }}
										</el-button>
										<el-button
											size="small"
											type="danger"
											plain
											@click="removeFavorite(item)"
										>
											{{ lang.pick("取消收藏", "Unfavorite") }}
										</el-button>
									</div>
								</li>
							</ul>

							<AppPagination
								v-if="favTotal > favSize"
								:current="favPage"
								:total="favTotal"
								:size="favSize"
								@change="onFavPage"
							/>
						</div>
					</div>
				</el-tab-pane>
			</el-tabs>
		</div>
	</div>
</template>

<style scoped>
.page {
	width: var(--container);
	margin: 0 auto;
	padding-bottom: 48px;
}
.profile-wrap {
	margin-top: 16px;
}
.profile-tabs {
	min-height: 640px;
}
.profile-tabs :deep(.el-tabs__header) {
	background: var(--paper);
	border: 1px solid var(--line);
	border-right: 3px solid var(--ink);
	margin-right: 0;
}
.profile-tabs :deep(.el-tabs__nav-wrap) {
	padding: 10px 0;
}
.profile-tabs :deep(.el-tabs__item) {
	font-size: 14px;
	letter-spacing: 2px;
	color: var(--muted);
	height: 46px;
	line-height: 46px;
	text-align: left;
	padding: 0 22px;
}
.profile-tabs :deep(.el-tabs__item.is-active) {
	color: var(--accent);
	font-weight: 700;
}
.profile-tabs :deep(.el-tabs__active-bar) {
	background: var(--accent);
}
.pane {
	padding: 0 0 8px 24px;
}
.card {
	background: var(--paper);
	border: 1px solid var(--line);
	padding: 24px 26px;
	margin-bottom: 18px;
}
.sec-title {
	font-family: var(--serif);
	font-size: 20px;
	letter-spacing: 2px;
	padding-bottom: 12px;
	border-bottom: 1px solid var(--line);
	margin-bottom: 16px;
}
.sec-desc {
	font-size: 13px;
	color: var(--muted);
	margin: -6px 0 16px;
}

/* ---- 资料卡 ---- */
.profile-card {
	display: flex;
	gap: 26px;
	align-items: center;
}
.avatar-box {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 10px;
}
.avatar {
	width: 96px;
	height: 96px;
	border-radius: 50%;
	background: var(--ink);
	color: #fff;
	display: flex;
	align-items: center;
	justify-content: center;
	font-family: var(--serif);
	font-size: 40px;
	overflow: hidden;
	flex-shrink: 0;
}
.avatar img {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
.avatar-actions {
	display: flex;
	gap: 8px;
	flex-wrap: wrap;
	justify-content: center;
}
.avatar-tip {
	font-size: 11px;
	color: var(--muted);
}
.info h3 {
	font-family: var(--serif);
	font-size: 26px;
	letter-spacing: 2px;
}
.info .meta {
	font-size: 13px;
	color: var(--muted);
	margin-top: 6px;
}

/* ---- 绑定行 ---- */
.bind-row {
	display: flex;
	align-items: center;
	gap: 10px;
	margin-bottom: 12px;
	flex-wrap: wrap;
}
.bind-label {
	font-size: 13px;
	color: var(--muted);
	width: 56px;
	flex-shrink: 0;
	letter-spacing: 1px;
}
.bind-input {
	width: 300px;
}
.bind-code {
	width: 130px;
}
.bind-tip {
	font-size: 12px;
	color: var(--muted);
	margin: 4px 0 16px 66px;
}
.bind-tip strong {
	color: var(--ink);
	font-weight: 600;
}
.bind-tip em {
	color: var(--accent);
	font-style: normal;
}

/* ---- 修改密码 ---- */
.pwd-methods {
	margin-bottom: 18px;
}
.pwd-form {
	max-width: 560px;
}
.code-row {
	display: flex;
	gap: 10px;
	width: 100%;
}
.code-row .el-input {
	flex: 1;
}
.field-tip {
	font-size: 12px;
	color: var(--accent);
	margin: -8px 0 14px 110px;
}
.sec-current {
	display: flex;
	align-items: center;
	gap: 14px;
	flex-wrap: wrap;
}
.sec-current .bind-tip {
	margin: 0;
}

/* ---- 收藏 ---- */
.fav-filter {
	display: flex;
	gap: 10px;
	flex-wrap: wrap;
	margin-bottom: 18px;
}
.chip {
	display: inline-block;
	font-size: 13px;
	padding: 6px 16px;
	border: 1px solid var(--line);
	cursor: pointer;
	background: transparent;
	color: var(--ink);
	transition: all 0.2s ease;
	font-family: var(--sans);
}
.chip.active {
	background: var(--ink);
	color: #fff;
	border-color: var(--ink);
}
.chip:hover {
	border-color: var(--ink);
}
.fav-list {
	list-style: none;
}
.fav-item {
	display: flex;
	align-items: center;
	gap: 16px;
	padding: 14px 6px;
	border-bottom: 1px solid var(--line);
}
.fav-item:last-child {
	border-bottom: none;
}
.fav-thumb {
	width: 72px;
	height: 72px;
	flex-shrink: 0;
	background: var(--paper-2);
	border: 1px solid var(--line);
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
	cursor: pointer;
	font-family: var(--serif);
	font-size: 26px;
	color: var(--accent);
}
.fav-thumb img {
	width: 100%;
	height: 100%;
	object-fit: cover;
}
.fav-main {
	flex: 1;
	min-width: 0;
	cursor: pointer;
}
.fav-main h5 {
	font-family: var(--serif);
	font-size: 18px;
	letter-spacing: 1px;
	transition: color 0.2s;
}
.fav-main:hover h5 {
	color: var(--accent);
}
.fav-meta {
	font-size: 12px;
	color: var(--muted);
	margin-top: 6px;
}
.fav-type {
	display: inline-block;
	font-size: 11px;
	letter-spacing: 1px;
	padding: 1px 8px;
	border: 1px solid var(--accent);
	color: var(--accent);
}
.fav-actions {
	flex-shrink: 0;
	display: flex;
	gap: 8px;
}

@media (max-width: 900px) {
	.pane {
		padding: 0 0 8px 0;
	}
	.profile-card {
		flex-direction: column;
		text-align: center;
	}
	.bind-input {
		width: 100%;
	}
	.fav-item {
		flex-wrap: wrap;
	}
	.fav-actions {
		width: 100%;
		justify-content: flex-end;
	}
}
</style>

/** C 端全站类型定义 —— 与后端 openapi.json 契约对齐 */

/** 统一响应包装 */
export interface ApiResponse<T = unknown> {
	code: number;
	message: string;
	data: T;
	timestamp: number;
}

/** 分页结果（后端 EasyPageResult） */
export interface PageResult<T> {
	total: number;
	data: T[];
}

/** 民族列表项 */
export interface EthnicListItem {
	id: string;
	name: string;
	pinyin: string;
	population: number;
	region: string[];
	languageFamily: string;
	summary: string;
	coverImage: string | null;
	themeColor: string;
}

/** 民族全家福简略项 */
export interface EthnicBrief {
	id: string;
	name: string;
	themeColor: string;
	coverImage: string | null;
}

/** 民族习俗 */
export interface EthnicCustom {
	id: string;
	ethnicGroupId: string;
	category: string;
	title: string;
	content: string;
	image?: string | null;
	orderNum?: number;
}

/** 民族习俗详情（C 端独立页面） */
export interface EthnicCustomDetail {
	id: string;
	ethnicGroupId: string;
	ethnicGroupName: string;
	category: string;
	title: string;
	content: string;
	image?: string | null;
	orderNum?: number;
}

/** 聚居地 */
export interface EthnicLocation {
	id: string;
	ethnicGroupId: string;
	province: string;
	city: string;
	longitude: number;
	latitude: number;
	description: string;
}

/** 美食 */
export interface Food {
	id: string;
	ethnicGroupId: string;
	name: string;
	nameEn: string;
	description: string;
	/** 发展沿革 */
	origin?: string | null;
	image?: string | null;
}

/** 节日（嵌套于民族详情） */
export interface FestivalBrief {
	id: string;
	slug?: string;
	ethnicGroupId: string;
	name: string;
	nameEn?: string;
	type: string;
	solarDate?: string | null;
	lunarDate?: string | null;
	origin?: string;
	description?: string;
	/** 可能是 JSON 字符串（详情嵌套时） */
	customs?: string[] | string;
	/** 可能是 JSON 字符串 */
	images?: string[] | string;
	coverImage?: string | null;
}

/** 艺术（嵌套于民族详情） */
export interface ArtBrief {
	id: string;
	slug?: string;
	ethnicGroupId: string;
	name: string;
	nameEn?: string;
	category: string;
	intangibleHeritage?: string;
	description?: string;
	/** 发展沿革 */
	origin?: string | null;
	/** 可能是 JSON 字符串（详情嵌套时） */
	inheritors?: string[] | string;
	coverImage?: string | null;
}

/** 民族详情 */
export interface EthnicDetail {
	id: string;
	slug: string;
	name: string;
	nameEn: string;
	selfName: string;
	pinyin: string;
	population: number;
	languageFamily: string;
	region: string[];
	languages: string[];
	scripts: string[];
	religion: string[];
	summary: string;
	summaryEn: string | null;
	description: string;
	descriptionEn: string | null;
	coverImage: string | null;
	themeColor: string;
	tags: string[];
	status: string;
	orderNum: number;
	createdAt?: string;
	updatedAt?: string;
	customs: EthnicCustom[];
	locations: EthnicLocation[];
	foods: Food[];
	festivals: FestivalBrief[];
	arts: ArtBrief[];
}

/** 节日列表项 */
export interface FestivalListItem {
	id: string;
	name: string;
	nameEn: string;
	type: string;
	ethnicGroupName: string;
	solarDate: string | null;
	lunarDate: string | null;
	origin: string;
	description?: string;
	customs: string[];
	images: string[];
	coverImage: string | null;
}

/** 节日详情 */
export interface FestivalDetail {
	id: string;
	slug?: string;
	name: string;
	nameEn: string;
	type: string;
	ethnicGroupName: string;
	solarDate: string | null;
	lunarDate: string | null;
	origin: string;
	description?: string;
	customs: string[];
	images: string[];
	coverImage: string | null;
}

/** 艺术列表项 */
export interface ArtListItem {
	id: string;
	name: string;
	nameEn: string;
	category: string;
	ethnicGroupName: string;
	description: string;
	intangibleHeritage: string;
	inheritors: string[];
	coverImage: string | null;
}

/** 艺术详情 */
export interface ArtDetail {
	id: string;
	slug?: string;
	name: string;
	nameEn: string;
	category: string;
	ethnicGroupName: string;
	description: string;
	/** 发展沿革 */
	origin?: string | null;
	intangibleHeritage: string;
	inheritors: string[];
	coverImage: string | null;
}

/** 专题列表项 */
export interface TopicListItem {
	id: string;
	slug?: string;
	title: string;
	subtitle?: string;
	description?: string;
	coverImage?: string | null;
	orderNum?: number;
}

/** 专题详情 */
export interface TopicDetail {
	id: string;
	slug: string;
	title: string;
	subtitle: string;
	description: string;
	coverImage: string | null;
	orderNum: number;
	entries: TopicEntry[];
}

/** 搜索分组 */
export interface SearchGroup<T> {
	total: number;
	list: T[];
}

/** 搜索结果 */
export interface SearchResult {
	ethnic: SearchGroup<EthnicListItem>;
	festival: SearchGroup<FestivalListItem>;
	art: SearchGroup<ArtListItem>;
}

/** 互动统计 */
export interface ContentStats {
	likeCount: number;
	favoriteCount: number;
	/** 浏览量 */
	viewCount: number;
}

/** 点赞结果 */
export interface LikeResource {
	liked: boolean;
	likeCount: number;
}

/** 分享结果 */
export interface ShareResource {
	shareUrl: string;
	posterUrl: string;
}

/** 我的收藏项（/me/favorites） */
export interface FavoriteItem {
	id: string;
	entryType: "ethnic" | "festival" | "art" | "topic";
	entryId: string;
	entryName: string;
	coverImage: string | null;
	createdAt: string;
}

/** 当前用户 */
export interface UserInfo {
	id: string;
	nickname: string;
	avatar: string | null;
	/** 界面语言偏好（zh / en），登录后随用户持久化 */
	lang?: string;
	roles: string[];
	/** 手机号（空串表示未绑定） */
	mobile?: string;
	/** 邮箱（空串表示未绑定） */
	email?: string;
	/** 注册日期 */
	createdAt?: string;
	/** 密保问题（未设置时为 null） */
	securityQuestion?: string | null;
}

/** 修改密码请求（旧密码 / 验证码 / 密保答案 三选一验证） */
export interface PasswordChangePayload {
	/** 验证方式一：旧密码 */
	oldPassword?: string;
	/** 验证方式二：接收验证码的账号（邮箱 / 手机号） */
	account?: string;
	/** 验证方式二：验证码 */
	code?: string;
	/** 验证方式三：密保答案 */
	securityAnswer?: string;
	/** 新密码（强密码：8 位以上且含大小写字母、数字、特殊字符） */
	newPassword: string;
}

/** 专题条目（topic_entry） */
export interface TopicEntry {
	id: string;
	entryType: "ethnic" | "festival" | "art" | "topic";
	entryId: string;
	sortOrder?: number;
}

/** 动态表单配置（后端 form_config） */
export interface FormConfig {
	id: string;
	code: string;
	name: string;
	description?: string | null;
	/** 表单 schema（JSON 字符串：{ fields: [...] }） */
	schema: string;
	status: "active" | "disabled";
	createdAt?: string;
	updatedAt?: string;
}

/** 用户查询（列表）项 */
export interface UserQueryInfo {
	id: string;
	account: string;
	nickname: string;
	avatar: string | null;
	mobile: string;
	email: string;
	status: string;
	createdAt: string;
	updatedAt: string;
	roles: unknown[];
}

export interface FeedBackFormType {
	name: string;
	contact: string;
	topic: "correction" | "suggestion" | "bug";
	rating: "good" | "ok" | "bad";
	content: string;
	visitDate: string;
}

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

/** 民族分布地图点位（一个聚居地 = 一个点） */
export interface EthnicMapPoint {
	id: string;
	ethnicGroupId: string;
	ethnicGroupName: string;
	ethnicGroupSlug: string;
	themeColor: string;
	province: string;
	city: string;
	longitude: number;
	latitude: number;
	description: string;
}

/** 人口统计中的一项聚合 */
export interface PopulationStatItem {
	name: string;
	groupCount: number;
	population: number;
}

/** 人口规模分档 */
export interface PopulationBucket {
	label: string;
	groupCount: number;
	population: number;
}

/** 民族人口统计（七普口径） */
export interface EthnicPopulationStats {
	censusYear: string;
	totalGroups: number;
	totalPopulation: number;
	largestGroupName: string | null;
	largestGroupPopulation: number;
	buckets: PopulationBucket[];
	topGroups: PopulationStatItem[];
	languageFamilies: PopulationStatItem[];
	regions: PopulationStatItem[];
}

/** 日历中的一个节日 */
export interface CalendarFestival {
	id: string;
	name: string;
	nameEn: string | null;
	ethnicGroupName: string | null;
	type: string;
	/** 公历日期 yyyy-MM-dd（农历已换算） */
	date: string;
	day: number;
	lunarDate: string | null;
	/** 日期来源：solar 原始公历 / lunar 农历换算 / approx 估算（仅精确到月） */
	dateSource: "solar" | "lunar" | "approx";
	daysFromToday: number;
}

/** 日历中的一个公历月 */
export interface CalendarMonth {
	month: number;
	count: number;
	festivals: CalendarFestival[];
}

/** 节日日历 */
export interface FestivalCalendar {
	year: number;
	months: CalendarMonth[];
	today: CalendarFestival | null;
	upcoming: CalendarFestival[];
}

/* ---- B-4 非遗名录 ---- */

/** 非遗名录中的一个分组统计项 */
export interface HeritageGroup {
	code: string;
	label: string;
	count: number;
}

/** 非遗名录·传承人聚焦项 */
export interface HeritageSpotlight {
	id: string;
	name: string;
	intangibleHeritage: string;
	ethnicGroupName: string | null;
	inheritors: string[];
}

/** 非遗名录统计概览 */
export interface HeritageStats {
	total: number;
	withInheritor: number;
	ethnicCount: number;
	levels: HeritageGroup[];
	categories: HeritageGroup[];
	spotlight: HeritageSpotlight[];
}

/* ---- B-3 民族语文专栏 ---- */

/** 语文专栏·民族引用 */
export interface LanguageGroupRef {
	id: string;
	name: string;
	pinyin: string;
	themeColor: string;
	population: number;
	languages: string[];
	scripts: string[];
}

/** 语文专栏·语系 */
export interface LanguageFamily {
	/** 原始值，如「汉藏语系·藏缅语族」 */
	name: string;
	/** 大语系 */
	family: string;
	/** 语族（可能为空） */
	branch: string | null;
	groups: LanguageGroupRef[];
	population: number;
}

/** 语文专栏·文字 */
export interface LanguageScript {
	name: string;
	groups: LanguageGroupRef[];
	/** 是否为该民族的传统文字（false 表示借用文字，如汉字、阿拉伯文） */
	nativeScript: boolean;
}

/** 民族语文专栏数据 */
export interface LanguageAtlas {
	summary: {
		groupCount: number;
		languageCount: number;
		scriptCount: number;
		familyCount: number;
	};
	families: LanguageFamily[];
	scripts: LanguageScript[];
	groupsWithOwnScript: number;
	groupsUsingChinese: number;
}

/* ---- B-1 民族服饰 / B-2 民居建筑（文化专题） ---- */

/** 专题下的单条内容 */
export interface CultureTopicItem {
	/** custom=风俗习惯 / art=非遗项目 */
	source: "custom" | "art";
	id: string | null;
	title: string;
	category: string | null;
	content: string | null;
	intangibleHeritage: string | null;
	image: string | null;
	detailPath: string;
}

/** 专题·按民族聚合的条目 */
export interface CultureTopicEntry {
	ethnicGroupId: string;
	ethnicGroupName: string;
	themeColor: string;
	region: string | null;
	coverImage: string | null;
	items: CultureTopicItem[];
}

/** 专题分类维度 */
export interface CultureTopicCategory {
	code: string;
	label: string;
	count: number;
}

/** 文化专题（服饰 / 民居建筑） */
export interface CultureTopic {
	topic: string;
	title: string;
	titleEn: string;
	intro: string;
	summary: {
		groupCount: number;
		entryCount: number;
		heritageCount: number;
		withImage: number;
	};
	categories: CultureTopicCategory[];
	entries: CultureTopicEntry[];
}

/* ---- B-7 人物 / 传承人专栏 ---- */

/** 人物关联的非遗项目 */
export interface PersonProjectRef {
	id: string;
	name: string;
	intangibleHeritage: string;
	ethnicGroupName: string | null;
	detailPath: string;
}

/** 人物列表项 */
export interface PersonItem {
	name: string;
	ethnicGroupName: string | null;
	/** inheritor 代表性传承人 / master 历史文化名家 */
	roleType: "inheritor" | "master";
	roleLabel: string;
	domain: string | null;
	lifespan: string | null;
	bio: string | null;
	projects: PersonProjectRef[];
	topLevel: string | null;
}

/** 人物筛选选项 */
export interface PersonOption {
	value: string;
	label: string;
	count: number;
}

/** 人物专栏名录 */
export interface PersonDirectory {
	summary: {
		personCount: number;
		inheritorCount: number;
		masterCount: number;
		ethnicCount: number;
		projectCount: number;
	};
	filters: {
		domains: PersonOption[];
		ethnics: PersonOption[];
		roles: PersonOption[];
	};
	persons: PersonItem[];
	total: number;
}

/* ---- B-6 民族自治地方 ---- */

/** 一个自治地方 */
export interface AutonomousArea {
	name: string;
	level: string;
	levelLabel: string;
	ethnicGroups: string[];
	province: string | null;
	establishedYear: number | null;
	seat: string | null;
}

/** 按级别分组 */
export interface AreaLevelGroup {
	level: string;
	label: string;
	count: number;
	areas: AutonomousArea[];
}

/** 按自治民族聚合 */
export interface AreaEthnicGrouping {
	ethnic: string;
	count: number;
	/** 内容库中若存在该民族则有值，用于跳转民族详情 */
	matchedName: string | null;
	matchedSlug: string | null;
	themeColor: string | null;
	areas: AutonomousArea[];
}

/** 按省级行政区聚合 */
export interface AreaProvinceGrouping {
	province: string;
	count: number;
	areas: AutonomousArea[];
}

/** 民族自治地方名录 */
export interface AutonomousAreaDirectory {
	summary: {
		total: number;
		regionCount: number;
		prefectureCount: number;
		countyCount: number;
		ethnicCount: number;
		provinceCount: number;
	};
	levels: AreaLevelGroup[];
	ethnics: AreaEthnicGrouping[];
	provinces: AreaProvinceGrouping[];
}

/* ---- B-5 传统体育 ---- */

/** 传统体育项目匹配到的民族 */
export interface SportEthnicRef {
	id: string;
	name: string;
	slug: string;
	themeColor: string;
}

/** 一个传统体育项目 */
export interface TraditionalSport {
	name: string;
	category: string;
	categoryLabel: string;
	ethnicOrigins: string[];
	description: string | null;
	equipment: string | null;
	venue: string | null;
	teamSize: string | null;
	firstEventYear: number | null;
	subEvents: string[];
	heritageLink: string | null;
	matchedEthnics: SportEthnicRef[];
}

/** 传统体育名录 */
export interface TraditionalSportDirectory {
	summary: {
		total: number;
		categoryCount: number;
		ethnicCount: number;
		withSubEvents: number;
		withHeritage: number;
	};
	sports: TraditionalSport[];
}

/* ---- 方向 C-1 内容来源（可溯源） ---- */

/** 内容来源 / 参考资料 */
export interface ContentSource {
	id: string;
	name: string;
	publisher: string | null;
	publisherShort: string | null;
	documentTitle: string | null;
	url: string | null;
	/** official / academic / open / other */
	sourceType: string;
	sourceTypeLabel: string;
	/** scrape / ocr / manual / api */
	collectMethod: string;
	collectMethodLabel: string;
	remark: string | null;
	/** 该来源在本条内容上的具体说明（仅按内容查询时返回） */
	note: string | null;
}

/* ---- 方向 C-4 图片版权署名 ---- */

/** 图片署名 */
export interface ImageCredit {
	imagePath: string;
	caption: string | null;
	/** verified 已核实 / unverified 来源待核 / original 原创无需署名 */
	creditStatus: "verified" | "unverified" | "original";
	creditStatusLabel: string;
	author: string | null;
	license: string | null;
	licenseUrl: string | null;
	sourceUrl: string | null;
	sourceSite: string | null;
	attributionRequired: boolean | null;
	remark: string | null;
	/** 可直接展示的署名文本；未核实时为 null */
	creditLine: string | null;
}

/** 署名核实进度 */
export interface ImageCreditStats {
	total: number;
	verified: number;
	unverified: number;
	original: number;
	/** 需要署名但尚未核实的数量（合规缺口） */
	pendingAttribution: number;
	verifiedRate: number;
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

/** 美食详情（C 端独立页面 /foods/{id}） */
export interface FoodDetail {
	id: string;
	ethnicGroupId: string | null;
	ethnicGroupName: string | null;
	name: string;
	nameEn: string | null;
	description: string;
	/** 英文正文（方向 C-3）；为空时页面回退显示 description */
	descriptionEn?: string | null;
	/** 英文正文来源：machine/reviewed/manual */
	descriptionEnSource?: string | null;
	/** 发展沿革 */
	origin: string | null;
	image: string | null;
	orderNum?: number | null;
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
	/** 历史沿革结构化（方向 C-2）：时间轴 / 时代分期 / 段落索引 */
	history?: EthnicHistory;
}

/* ---------------------------------------------------------------------------
   历史沿革结构化（方向 C-2）
   后端只把原文明写出具体年份的段落放进 timeline，不做年份推断；
   其余专题叙述段落收在 paragraphs，供侧栏索引与折叠阅读。
   --------------------------------------------------------------------------- */

/** 时间轴条目 */
export interface EthnicHistoryTimelineItem {
	/** 原文年份（公元前为负数） */
	year: number;
	/** 年份原文写法，如「1206年」「公元前221年」 */
	yearText: string;
	/** 该段原文（未改写） */
	text: string;
	/** 对应 paragraphs 的下标 */
	index: number;
}

/** 时代分期 */
export interface EthnicHistoryEra {
	name: string;
	/** 命中该时代的段落下标 */
	paragraphIndexes: number[];
}

/** 段落索引项 */
export interface EthnicHistoryParagraph {
	index: number;
	text: string;
	eras: string[];
	/** 该段首个明确年份；无则为 null */
	year: number | null;
}

/** 历史沿革结构化结果 */
export interface EthnicHistory {
	timeline: EthnicHistoryTimelineItem[];
	eras: EthnicHistoryEra[];
	paragraphs: EthnicHistoryParagraph[];
	timelineCount: number;
	eraCount: number;
	paragraphCount: number;
	/** 有时间锚点的段落占比（0~1） */
	anchoredRatio: number;
}

/* ---------------------------------------------------------------------------
   全文检索与个性化推荐（方向 D）
   --------------------------------------------------------------------------- */

/** 统一全文检索的单条命中 */
export interface SearchHit {
	docType: string;
	docId: string;
	url: string;
	title: string;
	/** 标题（关键词已高亮，含 <em class="hl">） */
	titleHtml: string;
	summary: string | null;
	summaryHtml: string;
	ethnicName: string | null;
	category: string | null;
	region: string | null;
	coverImage: string | null;
	themeColor: string | null;
	score: number;
	/** 命中方式：title / titleEn / pinyin / abbr / body */
	matchBy: string;
}

/** 统一全文检索结果 */
export interface FullTextSearchResult {
	keyword: string;
	total: number;
	tookMs: number;
	type: string;
	list: SearchHit[];
	/** 分面计数：内容类型 → 命中数 */
	facets: Record<string, number>;
	/** 命中来源分布 */
	highlights: Record<string, number>;
}

/** 兴趣标签 */
export interface InterestTag {
	id: string;
	/** ethnic 民族 / region 地域 / type 内容类型 / topic 主题 */
	dimension: string;
	name: string;
	nameEn: string | null;
	description: string | null;
	color: string | null;
	enabled: boolean;
	orderNum: number;
}

/** 推荐条目 */
export interface RecoItem {
	docType: string;
	docId: string;
	url: string;
	title: string;
	summary: string | null;
	ethnicName: string | null;
	category: string | null;
	coverImage: string | null;
	themeColor: string | null;
	score: number;
	/** 推荐理由（如「你关注了藏族」） */
	reason: string | null;
}

/** 个性化推荐结果 */
export interface Recommendation {
	list: RecoItem[];
	/** personalized 个性化 / interest 兴趣标签 / popularity 热度兜底 */
	basis: string;
	basisLabel: string;
	/** 如实说明当前可用样本量 */
	dataNote: string;
	confidence: number;
	behaviorCount: number;
	interestCount: number;
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
	/** 英文正文（方向 C-3） */
	descriptionEn?: string | null;
	descriptionEnSource?: string | null;
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
	/** 英文正文（方向 C-3）；为空时页面回退显示 description */
	descriptionEn?: string | null;
	/** 英文正文来源：machine/reviewed/manual */
	descriptionEnSource?: string | null;
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
	/** 英文正文（方向 C-3） */
	descriptionEn?: string | null;
	descriptionEnSource?: string | null;
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
	/** 英文正文（方向 C-3）；为空时页面回退显示 description */
	descriptionEn?: string | null;
	descriptionEnSource?: string | null;
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

/* ==========================================================================
   讨论区 / 社区
   ========================================================================== */

/** 讨论区板块 */
export interface DiscussionBoard {
	id: string;
	slug: string;
	name: string;
	nameEn: string | null;
	description: string | null;
	icon: string | null;
	themeColor: string | null;
	topicCount: number;
}

/** 讨论区作者 */
export interface DiscussionAuthor {
	id: string | null;
	nickname: string;
	avatar: string | null;
	trustLevel: number;
	owner: boolean;
}

export type DiscussionTopicStatus = "published" | "pending" | "hidden" | "rejected" | "deleted";

/** 帖子列表项 */
export interface DiscussionTopicBrief {
	id: string;
	boardId: string;
	boardName: string | null;
	title: string;
	excerpt: string;
	images: string[];
	author: DiscussionAuthor;
	lang: string;
	status: DiscussionTopicStatus;
	pinned: boolean;
	featured: boolean;
	locked: boolean;
	replyCount: number;
	likeCount: number;
	viewCount: number;
	lastReplyAt: string | null;
	createdAt: string;
	mine: boolean;
}

/** 帖子详情 */
export interface DiscussionTopicDetail {
	id: string;
	boardId: string;
	boardName: string | null;
	title: string;
	content: string;
	images: string[];
	author: DiscussionAuthor;
	lang: string;
	status: DiscussionTopicStatus;
	pinned: boolean;
	featured: boolean;
	locked: boolean;
	linkedType: string | null;
	linkedId: string | null;
	replyCount: number;
	likeCount: number;
	favoriteCount: number;
	viewCount: number;
	createdAt: string;
	editedAt: string | null;
	mine: boolean;
	/** 本人内容待审 / 驳回原因提示 */
	reviewNote: string | null;
}

/** 楼层 */
export interface DiscussionPost {
	id: string;
	topicId: string;
	floorNo: number;
	content: string;
	images: string[];
	author: DiscussionAuthor;
	lang: string;
	status: string;
	likeCount: number;
	quotePostId: string | null;
	quoteExcerpt: string | null;
	quoteAuthor: string | null;
	createdAt: string;
	mine: boolean;
}

/** 站内通知 */
export interface NotificationItem {
	id: string;
	type: string;
	actorName: string | null;
	actorAvatar: string | null;
	title: string | null;
	content: string | null;
	targetType: string | null;
	targetId: string | null;
	/** 站内公告的自定义跳转链接 */
	link: string | null;
	read: boolean;
	createdAt: string;
}

/** 我的举报记录 */
export interface DiscussionReportItem {
	id: string;
	targetType: string;
	targetId: string;
	targetExcerpt: string | null;
	targetAuthor: string | null;
	targetPath: string | null;
	reporter: string | null;
	reason: string;
	detail: string | null;
	status: "pending" | "accepted" | "rejected";
	resultNote: string | null;
	createdAt: string;
	handledAt: string | null;
}

/** 发帖请求 */
export interface DiscussionTopicPayload {
	boardId: string;
	title: string;
	content: string;
	images?: string[];
	lang?: string;
	linkedType?: string | null;
	linkedId?: string | null;
}

/** 回复请求 */
export interface DiscussionPostPayload {
	content: string;
	images?: string[];
	lang?: string;
	quotePostId?: string | null;
}

/* ==========================================================================
   社区社交：用户主页 / 关注 / 私信
   ========================================================================== */

/** @提及联想用户（回复框输入「@」时的候选） */
export interface MentionUser {
	id: string;
	/** 昵称：插入到内容中的 `@昵称` 必须与它完全一致，后端才能解析并通知 */
	nickname: string;
	avatar: string | null;
	bio: string | null;
}

/** 社区用户主页 */
export interface CommunityUser {
	id: string;
	nickname: string;
	avatar: string | null;
	bio: string | null;
	locale: string | null;
	trustLevel: number;
	joinedAt: string | null;
	followingCount: number;
	followerCount: number;
	topicCount: number;
	followed: boolean;
	/** 互相关注（互关后才可私信） */
	mutual: boolean;
	self: boolean;
}

/** 私信会话 */
export interface Conversation {
	id: string;
	peerId: string;
	peerName: string;
	peerAvatar: string | null;
	lastPreview: string | null;
	lastMessageAt: string | null;
	lastMine: boolean;
	unread: number;
	/** 我是否已拉黑对方 */
	blocked: boolean;
}

/** 私信消息 */
export interface PrivateMessage {
	id: string;
	conversationId: string;
	senderId: string;
	mine: boolean;
	content: string;
	images: string[];
	lang: string;
	recalled: boolean;
	/** 我是否可以撤回（自己发送且 2 分钟内） */
	recallable: boolean;
	read: boolean;
	createdAt: string;
}

/** 我的订阅项 */
export interface SubscriptionItem {
	id: string;
	targetType: "topic" | "board" | "user";
	targetId: string;
	targetName: string;
	cover: string | null;
	path: string;
	/** all 全部通知 / mention 仅被 @ 时通知 / off 免打扰 */
	level: "all" | "mention" | "off";
	createdAt: string;
}

/* ==========================================================================
   机器翻译（自建服务：libretranslate / ollama / 本地词表兜底）
   ========================================================================== */

/** 翻译能力状态（决定前端是否显示「译」按钮） */
export interface TranslateStatus {
	enabled: boolean;
	provider: "none" | "libretranslate" | "ollama" | "glossary";
	label: string;
	cacheEnabled: boolean;
}

/** 翻译结果；translated=false 表示已降级为「仅原文」 */
export interface TranslationResult {
	targetType: string;
	targetId: string;
	targetLocale: string;
	sourceLocale: string | null;
	content: string;
	provider: string;
	translated: boolean;
	cached: boolean;
	stale: boolean;
	message: string | null;
}

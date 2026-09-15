import { get, post, put, del } from './request'
import type {
  ArtDetail,
  ArtListItem,
  AutonomousAreaDirectory,
  CommunityUser,
  ContentStats,
  Conversation,
  ContentSource,
  CultureTopic,
  DiscussionBoard,
  DiscussionPost,
  DiscussionPostPayload,
  DiscussionReportItem,
  DiscussionTopicBrief,
  DiscussionTopicDetail,
  DiscussionTopicPayload,
  EthnicBrief,
  EthnicCustomDetail,
  EthnicDetail,
  EthnicListItem,
  EthnicMapPoint,
  EthnicPopulationStats,
  FavoriteItem,
  FeedBackFormType,
  FestivalCalendar,
  FestivalDetail,
  FestivalListItem,
  FoodDetail,
  FormConfig,
  FullTextSearchResult,
  HeritageStats,
  ImageCredit,
  ImageCreditStats,
  InterestTag,
  LanguageAtlas,
  LikeResource,
  MentionUser,
  NotificationItem,
  PageResult,
  PasswordChangePayload,
  PersonDirectory,
  Recommendation,
  PrivateMessage,
  SearchResult,
  ShareResource,
  SubscriptionItem,
  TopicDetail,
  TopicListItem,
  TraditionalSportDirectory,
  TranslateStatus,
  TranslationResult,
  UserInfo,
} from './types'

/** 分页参数：后端 Spring Pageable 使用 page（0 基）与 size */
export interface PageParams {
  page?: number // 0 基
  size?: number
}

export interface EthnicListParams extends PageParams {
  region?: string
  languageFamily?: string
  populationMin?: number
  populationMax?: number
  keyword?: string
  sort?: string // 如 "population,desc" / "pinyin,asc" / "orderNum,asc"
}

/** 民族：列表 / 全家福 / 详情 / 分布地图 / 人口统计 / 语文专栏 */
export const ethnicApi = {
  list: (params: EthnicListParams) => get<PageResult<EthnicListItem>>('/ethnic-groups', params as Record<string, unknown>),
  all: () => get<EthnicBrief[]>('/ethnic-groups/all'),
  detail: (id: string) => get<EthnicDetail>(`/ethnic-groups/${id}`),
  /** 民族分布地图点位（聚居地经纬度）；传 ethnicGroupId 只看某个民族 */
  map: (ethnicGroupId?: string) =>
    get<EthnicMapPoint[]>('/ethnic-groups/map', ethnicGroupId ? { ethnicGroupId } : undefined),
  /** 民族人口统计（七普口径）：Top N / 语系 / 地域 / 规模分档 */
  populationStats: (topN = 10) => get<EthnicPopulationStats>('/ethnic-groups/population-stats', { topN }),
  /** 民族语文专栏：语系分布 / 文字一览 / 语言统计 */
  languageAtlas: () => get<LanguageAtlas>('/ethnic-groups/language-atlas'),
}

/** 民族风俗习惯：详情 */
export const customApi = {
  detail: (id: string) => get<EthnicCustomDetail>(`/ethnic-customs/${id}`),
}

/** 民族美食：详情 */
export const foodApi = {
  detail: (id: string) => get<FoodDetail>(`/foods/${id}`),
}

export interface FestivalListParams extends PageParams {
  type?: string
  month?: number
  keyword?: string
  sort?: string
}

/** 节日：列表 / 详情 / 日历 */
export const festivalApi = {
  list: (params: FestivalListParams) => get<PageResult<FestivalListItem>>('/festivals', params as Record<string, unknown>),
  detail: (id: string) => get<FestivalDetail>(`/festivals/${id}`),
  /** 节日日历：农历自动换算为公历，按月份聚合 */
  calendar: (year?: number) => get<FestivalCalendar>('/festivals/calendar', year ? { year } : undefined),
}

export interface ArtListParams extends PageParams {
  category?: string
  /** 所属民族 ID */
  ethnicGroupId?: string
  intangibleHeritage?: string
  keyword?: string
  sort?: string
}

/** 艺术：列表 / 详情 / 非遗名录统计 */
export const artApi = {
  list: (params: ArtListParams) => get<PageResult<ArtListItem>>('/arts', params as Record<string, unknown>),
  detail: (id: string) => get<ArtDetail>(`/arts/${id}`),
  /** 非遗名录统计：级别 / 类别分布 + 传承人覆盖与聚焦榜 */
  heritageStats: () => get<HeritageStats>('/arts/heritage-stats'),
}

/** 专题：列表 / 详情 */
export const topicApi = {
  list: (params?: PageParams & { keyword?: string }) =>
    get<PageResult<TopicListItem>>('/topics', (params || {}) as Record<string, unknown>),
  detail: (id: string) => get<TopicDetail>(`/topics/${id}`),
}

/** 文化专题（民族服饰 / 民居建筑）：按民族合流风俗习惯与非遗项目 */
export const cultureTopicApi = {
  /** topic: costume（民族服饰）/ dwelling（民居建筑） */
  get: (topic: 'costume' | 'dwelling') => get<CultureTopic>(`/culture-topics/${topic}`),
}

/** 人物专栏（B-7）：非遗代表性传承人 + 历史文化名家 */
export const personApi = {
  list: (params?: { keyword?: string; domain?: string; ethnic?: string; roleType?: string }) =>
    get<PersonDirectory>('/persons', (params || {}) as Record<string, unknown>),
}

/** 民族自治地方（B-6）：自治区 / 自治州 / 自治县·旗 */
export const autonomousAreaApi = {
  list: (params?: { level?: string; keyword?: string; ethnic?: string }) =>
    get<AutonomousAreaDirectory>('/autonomous-areas', (params || {}) as Record<string, unknown>),
}

/** 传统体育（B-5）：全国少数民族传统体育运动会竞赛项目 */
export const traditionalSportApi = {
  list: (params?: { category?: string; ethnic?: string; keyword?: string }) =>
    get<TraditionalSportDirectory>('/traditional-sports', (params || {}) as Record<string, unknown>),
}

/** 内容来源（方向 C-1：可溯源） */
export const sourceApi = {
  /** 全部来源（数据来源汇总） */
  all: () => get<ContentSource[]>('/sources'),
  /** 某条内容的数据出处（详情页「参考资料」） */
  byContent: (targetType: 'ethnic' | 'festival' | 'art' | 'food' | 'topic' | 'person' | 'area' | 'sport', targetId: string) =>
    get<ContentSource[]>(`/sources/${targetType}/${targetId}`, undefined, { silent: true }),
}

/** 图片版权署名（方向 C-4） */
export const imageCreditApi = {
  /** 署名核实进度（多少张已核实 / 待核） */
  stats: () => get<ImageCreditStats>('/image-credits/stats', undefined, { silent: true }),
  /** 按图片路径批量查询（图集页用，逗号分隔） */
  byPaths: (paths: string[]) =>
    get<ImageCredit[]>(
      '/image-credits/by-paths',
      { paths: paths.join(',') },
      { silent: true },
    ),
  /** 按内容查询（详情页「图片来源」汇总区） */
  byContent: (targetType: 'ethnic' | 'festival' | 'art' | 'food' | 'topic', targetId: string) =>
    get<ImageCredit[]>(`/image-credits/${targetType}/${targetId}`, undefined, { silent: true }),
}

/** 搜索 */
export const searchApi = {
  search: (q: string, type: 'ethnic' | 'festival' | 'art' | 'all' = 'all') =>
    get<SearchResult>('/search', { q, type }),
  hot: () => get<string[]>('/search/hot'),
  /** 统一全文检索（方向 D）：8 类内容混排，支持中文子串/拼音/首字母/英文，含分面与高亮 */
  full: (params: { q?: string; type?: string; ethnic?: string; page?: number; size?: number }) =>
    get<FullTextSearchResult>('/search/full', params as Record<string, unknown>),
}

/** 兴趣标签与个性化推荐（方向 D） */
export const recommendApi = {
  /** 兴趣标签字典（按维度分组） */
  tags: () => get<Record<string, InterestTag[]>>('/interests/tags'),
  /** 我选中的兴趣标签 */
  mine: () => get<InterestTag[]>('/interests/mine'),
  /** 覆盖式保存兴趣标签 */
  saveMine: (tagIds: string[]) => post<number>('/interests/mine', { tagIds }),
  /** 个性化推荐（游客可用，会退化为热度推荐并如实说明） */
  recommend: (params?: { size?: number; excludeType?: string }) =>
    get<Recommendation>('/recommend', (params || {}) as Record<string, unknown>),
  /** 上报浏览行为（隐式信号；未登录时后端静默忽略） */
  reportView: (targetType: string, targetId: string) =>
    post('/behaviors/view', { targetType, targetId }),
}

/** 认证 */
export const authApi = {
  register: (nickname: string, password: string) => post('/auth/users', { nickname, password }),
  login: (account: string, password: string) => post<string>('/auth/login', { account, password }),
  me: () => get<UserInfo>('/auth/me'),
  logout: () => post('/auth/logout'),
  /** 更新当前用户信息（昵称/头像/邮箱/手机/密保/语言偏好等，仅提交非空字段） */
  update: (
    id: string,
    data: Partial<
      Pick<UserInfo, 'nickname' | 'avatar' | 'lang' | 'mobile' | 'email'> & {
        securityQuestion?: string
        securityAnswer?: string
      }
    >,
  ) => put(`/auth/${id}`, data),
  /** 发送验证码（邮箱可发送；手机短信通道暂未开放） */
  sendCode: (account: string) => get('/auth/code', { account }),
  /** 校验验证码 */
  checkCode: (account: string, code: string) => get('/auth/check', { account, code }),
  /** 修改密码（旧密码 / 验证码 / 密保答案 三选一验证） */
  changePassword: (data: PasswordChangePayload) => post('/auth/password', data),
}

/** 动态表单 */
export const formApi = {
  /** 按 ID 获取启用中的表单配置（含 schema） */
  get: (id: string) => get<FormConfig>(`/forms/${id}`),
  /** 按编码获取启用中的表单配置 */
  getByCode: (code: string) => get<FormConfig>(`/forms/code/${code}`),
}

/** 用户反馈（首页浮动按钮提交） */
export const feedbackApi = {
  submit: (data: FeedBackFormType) => post<string>('/feedback', data),
}

/** 互动（点赞/收藏/浏览/统计） */
export const interactionApi = {
  like: (type: string, id: string) => post<LikeResource>(`/contents/${type}/${id}/like`),
  unlike: (type: string, id: string) => del<LikeResource>(`/contents/${type}/${id}/like`),
  favorite: (type: string, id: string) => post(`/contents/${type}/${id}/favorite`),
  unfavorite: (type: string, id: string) => del(`/contents/${type}/${id}/favorite`),
  /** 记录一次浏览量，返回最新浏览量 */
  view: (type: string, id: string) => post<number>(`/contents/${type}/${id}/view`),
  stats: (type: string, id: string) => get<ContentStats>(`/contents/${type}/${id}/stats`),
}

/** 分享 */
export const shareApi = {
  create: (type: string, id: string) => post<ShareResource>('/share', { type, id }),
}

/** 我的收藏 */
export const meApi = {
  favorites: (type?: string, page = 0, size = 20) =>
    get<PageResult<FavoriteItem>>('/me/favorites', { type, page, size }),
}

/* ==========================================================================
   讨论区 / 社区
   ========================================================================== */

export interface DiscussionTopicQuery extends PageParams {
  boardId?: string
  keyword?: string
  sort?: 'latest' | 'hot' | 'featured'
  linkedType?: string
  linkedId?: string
  authorId?: string
}

/** 讨论区：板块 / 帖子 / 楼层 / 举报 */
export const discussionApi = {
  boards: () => get<DiscussionBoard[]>('/discussion/boards'),
  topics: (params: DiscussionTopicQuery) =>
    get<PageResult<DiscussionTopicBrief>>('/discussion/topics', params as Record<string, unknown>),
  linkedTopics: (linkedType: string, linkedId: string, size = 5) =>
    get<DiscussionTopicBrief[]>('/discussion/topics/linked', { linkedType, linkedId, size }),
  topic: (id: string) => get<DiscussionTopicDetail>(`/discussion/topics/${id}`),
  createTopic: (data: DiscussionTopicPayload) => post<string>('/discussion/topics', data),
  updateTopic: (id: string, data: Partial<DiscussionTopicPayload>) => put(`/discussion/topics/${id}`, data),
  deleteTopic: (id: string) => del(`/discussion/topics/${id}`),
  posts: (id: string, params?: PageParams) =>
    get<PageResult<DiscussionPost>>(`/discussion/topics/${id}/posts`, (params || {}) as Record<string, unknown>),
  createPost: (id: string, data: DiscussionPostPayload) => post<DiscussionPost>(`/discussion/topics/${id}/posts`, data),
  deletePost: (id: string) => del(`/discussion/posts/${id}`),
  report: (data: { targetType: string; targetId: string; reason: string; detail?: string }) =>
    post('/discussion/reports', data),
  /** 上传帖子配图（前端已压缩，服务端限制 5MB 与格式白名单） */
  uploadImage: (file: File | Blob, filename = 'image.png') => {
    const form = new FormData()
    form.append('file', file, filename)
    return post<string>('/uploads/image', form)
  },
}

/** 我的社区数据 */
export const myDiscussionApi = {
  topics: (params?: PageParams) =>
    get<PageResult<DiscussionTopicBrief>>('/me/discussion-topics', (params || {}) as Record<string, unknown>),
  posts: (params?: PageParams) =>
    get<PageResult<DiscussionPost>>('/me/discussion-posts', (params || {}) as Record<string, unknown>),
  reports: (params?: PageParams) =>
    get<PageResult<DiscussionReportItem>>('/me/discussion-reports', (params || {}) as Record<string, unknown>),
}

/** 站内通知 */
export const notificationApi = {
  list: (params?: PageParams & { unreadOnly?: boolean }) =>
    get<PageResult<NotificationItem>>('/me/notifications', (params || {}) as Record<string, unknown>),
  unreadCount: () => get<number>('/me/notifications/unread-count'),
  markRead: (ids: string[] = []) => post<number>('/me/notifications/read', ids),
}

/* ==========================================================================
   订阅（帖子 / 板块 / 用户 的三级通知强度）
   ========================================================================== */

export type SubscriptionLevel = 'all' | 'mention' | 'off'

export const subscriptionApi = {
  set: (targetType: 'topic' | 'board' | 'user', targetId: string, level: SubscriptionLevel) =>
    post<SubscriptionLevel>(
      `/discussion/subscriptions?targetType=${targetType}&targetId=${targetId}&level=${level}`,
    ),
  remove: (targetType: 'topic' | 'board' | 'user', targetId: string) =>
    del(`/discussion/subscriptions?targetType=${targetType}&targetId=${targetId}`),
  level: (targetType: 'topic' | 'board' | 'user', targetId: string) =>
    get<SubscriptionLevel | null>('/discussion/subscriptions/level', { targetType, targetId }),
  mine: () => get<SubscriptionItem[]>('/me/subscriptions'),
}

/* ==========================================================================
   社区社交：用户主页 / 关注 / 私信
   ========================================================================== */

/** 社区用户主页与关注关系 */
export const socialApi = {
  profile: (userId: string) => get<CommunityUser>(`/discussion/users/${userId}`),
  follow: (userId: string) => post<boolean>(`/discussion/users/${userId}/follow`),
  unfollow: (userId: string) => del<boolean>(`/discussion/users/${userId}/follow`),
  follows: (userId: string, type: 'following' | 'followers' = 'following', params?: PageParams) =>
    get<PageResult<CommunityUser>>(`/discussion/users/${userId}/follows`, {
      type,
      ...(params || {}),
    } as Record<string, unknown>),
  followingFeed: (params?: PageParams) =>
    get<PageResult<DiscussionTopicBrief>>('/discussion/topics/following', (params || {}) as Record<string, unknown>),
  mutual: () => get<string[]>('/discussion/users/mutual'),
  /** @提及联想：回复框 / 发帖页输入「@」时提示候选用户（默认 10 条，最多 20） */
  suggestUsers: (keyword = '', limit = 10) =>
    get<MentionUser[]>('/discussion/users/suggest', { keyword, limit }, { silent: true }),
}

/** 私信（仅互相关注的好友可会话） */
export const messageApi = {
  openWith: (userId: string) => post<Conversation>(`/me/conversations/with/${userId}`),
  conversations: (params?: PageParams) =>
    get<PageResult<Conversation>>('/me/conversations', (params || {}) as Record<string, unknown>),
  conversation: (id: string) => get<Conversation>(`/me/conversations/${id}`),
  messages: (id: string, params?: PageParams) =>
    get<PageResult<PrivateMessage>>(`/me/conversations/${id}/messages`, (params || {}) as Record<string, unknown>),
  send: (id: string, content: string, lang?: string, images: string[] = []) =>
    post<PrivateMessage>(`/me/conversations/${id}/messages`, { content, lang, images }),
  recall: (messageId: string) => post<PrivateMessage>(`/me/messages/${messageId}/recall`),
  block: (userId: string, blocked = true) => post<boolean>(`/me/blocks/${userId}?block=${blocked}`),
  read: (id: string) => post<number>(`/me/conversations/${id}/read`),
  unreadCount: () => get<number>('/me/conversations/unread-count'),
}

/* ==========================================================================
   机器翻译（自建 provider：libretranslate / ollama / 本地词表兜底）
   ========================================================================== */

export const translateApi = {
  /** 能力状态：enabled=false 时前端隐藏「译」按钮 */
  status: () => get<TranslateStatus>('/api/v1/translate/status'),
  /** 按需翻译；scope=title 供列表页只翻标题（成本可控） */
  translate: (
    targetType: 'topic' | 'post' | 'message' | 'board',
    targetId: string,
    targetLocale: string,
    scope: 'title' | 'body' | 'all' = 'all',
  ) =>
    post<TranslationResult>('/api/v1/translate', {
      targetType,
      targetId,
      targetLocale,
      scope,
    }),
}

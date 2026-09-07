import { get, post, put, del } from './request'
import type {
  ArtDetail,
  ArtListItem,
  ContentStats,
  EthnicBrief,
  EthnicCustomDetail,
  EthnicDetail,
  EthnicListItem,
  FavoriteItem,
  FeedBackFormType,
  FestivalDetail,
  FestivalListItem,
  FormConfig,
  LikeResource,
  PageResult,
  PasswordChangePayload,
  SearchResult,
  ShareResource,
  TopicDetail,
  TopicListItem,
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

/** 民族：列表 / 全家福 / 详情 */
export const ethnicApi = {
  list: (params: EthnicListParams) => get<PageResult<EthnicListItem>>('/ethnic-groups', params as Record<string, unknown>),
  all: () => get<EthnicBrief[]>('/ethnic-groups/all'),
  detail: (id: string) => get<EthnicDetail>(`/ethnic-groups/${id}`),
}

/** 民族风俗习惯：详情 */
export const customApi = {
  detail: (id: string) => get<EthnicCustomDetail>(`/ethnic-customs/${id}`),
}

export interface FestivalListParams extends PageParams {
  type?: string
  month?: number
  keyword?: string
  sort?: string
}

/** 节日：列表 / 详情 */
export const festivalApi = {
  list: (params: FestivalListParams) => get<PageResult<FestivalListItem>>('/festivals', params as Record<string, unknown>),
  detail: (id: string) => get<FestivalDetail>(`/festivals/${id}`),
}

export interface ArtListParams extends PageParams {
  category?: string
  intangibleHeritage?: string
  keyword?: string
  sort?: string
}

/** 艺术：列表 / 详情 */
export const artApi = {
  list: (params: ArtListParams) => get<PageResult<ArtListItem>>('/arts', params as Record<string, unknown>),
  detail: (id: string) => get<ArtDetail>(`/arts/${id}`),
}

/** 专题：列表 / 详情 */
export const topicApi = {
  list: (params?: PageParams & { keyword?: string }) =>
    get<PageResult<TopicListItem>>('/topics', (params || {}) as Record<string, unknown>),
  detail: (id: string) => get<TopicDetail>(`/topics/${id}`),
}

/** 搜索 */
export const searchApi = {
  search: (q: string, type: 'ethnic' | 'festival' | 'art' | 'all' = 'all') =>
    get<SearchResult>('/search', { q, type }),
  hot: () => get<string[]>('/search/hot'),
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

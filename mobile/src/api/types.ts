/** 后端统一响应包装：code === 0 表示成功 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: string
}

/** Spring Pageable 分页结果（EasyPageResult） */
export interface PageResult<T> {
  total: number
  data: T[]
}

/** 民族列表项 */
export interface EthnicListItem {
  id: string
  name: string
  pinyin?: string
  population?: number
  languageFamily?: string
  region?: string[]
  coverImage?: string | null
  themeColor?: string
  summary?: string
}

/** 民族全家福（/all） */
export interface EthnicBrief {
  id: string
  name: string
  themeColor?: string
  coverImage?: string | null
}

/** 民族详情子对象 */
export interface EthnicCustom {
  title?: string
  description?: string
  image?: string | null
}

export interface EthnicLocation {
  province?: string
  cities?: string[]
  description?: string
}

export interface Food {
  name?: string
  description?: string
  image?: string | null
}

export interface EthnicFestivalRef {
  id?: string
  name?: string
  month?: string
  description?: string
}

export interface EthnicArtRef {
  id?: string
  name?: string
  category?: string
  description?: string
}

/** 民族详情 */
export interface EthnicDetail {
  id: string
  slug?: string
  name: string
  nameEn?: string
  selfName?: string
  pinyin?: string
  population?: number
  languageFamily?: string
  region?: string[]
  languages?: string[]
  scripts?: string[]
  religion?: string[]
  summary?: string
  summaryEn?: string
  description?: string
  descriptionEn?: string
  coverImage?: string | null
  themeColor?: string
  tags?: string[]
  status?: string
  orderNum?: number
  createdAt?: string
  updatedAt?: string
  customs?: EthnicCustom[]
  locations?: EthnicLocation[]
  foods?: Food[]
  festivals?: EthnicFestivalRef[]
  arts?: EthnicArtRef[]
}

/** 节日列表项 */
export interface FestivalListItem {
  id: string
  name: string
  nameEn?: string
  type?: string
  month?: string
  lunarDate?: string
  solarDate?: string
  description?: string
  coverImage?: string | null
  themeColor?: string
  ethnicGroupId?: string
  ethnicName?: string
}

/** 节日详情 */
export interface FestivalDetail extends FestivalListItem {
  origin?: string
  customs?: string[]
  images?: string[]
  orderNum?: number
  status?: string
  createdAt?: string
  updatedAt?: string
}

/** 艺术列表项 */
export interface ArtListItem {
  id: string
  name: string
  nameEn?: string
  category?: string
  intangible?: boolean
  description?: string
  coverImage?: string | null
  themeColor?: string
  ethnicGroupId?: string
  ethnicName?: string
}

/** 艺术详情 */
export interface ArtDetail extends ArtListItem {
  intro?: string
  history?: string
  features?: string[]
  heritage?: string
  crafts?: string[]
  images?: string[]
  orderNum?: number
  status?: string
  createdAt?: string
  updatedAt?: string
}

/** 专题列表项 */
export interface TopicListItem {
  id: string
  title: string
  subtitle?: string
  description?: string
  coverImage?: string | null
  themeColor?: string
  status?: string
  orderNum?: number
  createdAt?: string
}

/** 专题详情（含关联条目） */
export interface TopicDetail extends TopicListItem {
  entries?: TopicEntry[]
}

export interface TopicEntry {
  id?: string
  type?: string
  refId?: string
  title?: string
  summary?: string
  image?: string | null
  orderNum?: number
}

/** 搜索结果 */
export interface SearchGroup {
  ethnic?: SearchResultItem[]
  festival?: SearchResultItem[]
  art?: SearchResultItem[]
  topic?: SearchResultItem[]
}

export interface SearchResultItem {
  id: string
  title: string
  type: 'ethnic' | 'festival' | 'art' | 'topic'
  description?: string
  coverImage?: string | null
}

/** 互动统计 */
export interface ContentStats {
  likeCount: number
  favoriteCount: number
  shareCount: number
  liked?: boolean
  favorited?: boolean
}

/** 分享资源 */
export interface ShareResource {
  url: string
  title?: string
  description?: string
}

/** 当前用户信息 */
export interface UserInfo {
  id: string
  nickname: string
  avatar?: string | null
  email?: string
  mobile?: string
  createdAt?: string
}

export type ContentType = 'ethnic' | 'festival' | 'art' | 'topic'

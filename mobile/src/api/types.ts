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
  /** 非遗级别编码，如 world / national */
  intangibleHeritage?: string
  inheritors?: string[]
  description?: string
  coverImage?: string | null
  themeColor?: string
  ethnicGroupId?: string
  ethnicName?: string
  ethnicGroupName?: string
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

/* ---- 节日日历 ---- */

export interface CalendarFestival {
  id: string
  name: string
  nameEn: string | null
  ethnicGroupName: string | null
  type: string
  /** 公历日期 yyyy-MM-dd */
  date: string
  day: number
  lunarDate: string | null
  dateSource: 'solar' | 'lunar' | 'approx'
  daysFromToday: number
}

export interface CalendarMonth {
  month: number
  count: number
  festivals: CalendarFestival[]
}

export interface FestivalCalendar {
  year: number
  months: CalendarMonth[]
  today: CalendarFestival | null
  upcoming: CalendarFestival[]
}

/* ---- 非遗名录 ---- */

export interface HeritageGroup {
  code: string
  label: string
  count: number
}

export interface HeritageSpotlight {
  id: string
  name: string
  intangibleHeritage: string
  ethnicGroupName: string | null
  inheritors: string[]
}

export interface HeritageStats {
  total: number
  withInheritor: number
  ethnicCount: number
  levels: HeritageGroup[]
  categories: HeritageGroup[]
  spotlight: HeritageSpotlight[]
}

/* ---- 民族语文 ---- */

export interface LanguageGroupRef {
  id: string
  name: string
  pinyin: string
  themeColor: string
  population: number
  languages: string[]
  scripts: string[]
}

export interface LanguageFamily {
  name: string
  family: string
  branch: string | null
  groups: LanguageGroupRef[]
  population: number
}

export interface LanguageScript {
  name: string
  groups: LanguageGroupRef[]
  nativeScript: boolean
}

export interface LanguageAtlas {
  summary: {
    groupCount: number
    languageCount: number
    scriptCount: number
    familyCount: number
  }
  families: LanguageFamily[]
  scripts: LanguageScript[]
  groupsWithOwnScript: number
  groupsUsingChinese: number
}

/* ---- 文化专题（服饰 / 民居） ---- */

export interface CultureTopicItem {
  source: 'custom' | 'art'
  id: string | null
  title: string
  category: string | null
  content: string | null
  intangibleHeritage: string | null
  image: string | null
  detailPath: string
}

export interface CultureTopicEntry {
  ethnicGroupId: string
  ethnicGroupName: string
  themeColor: string
  region: string | null
  coverImage: string | null
  items: CultureTopicItem[]
}

export interface CultureTopicCategory {
  code: string
  label: string
  count: number
}

export interface CultureTopic {
  topic: string
  title: string
  titleEn: string
  intro: string
  summary: {
    groupCount: number
    entryCount: number
    heritageCount: number
    withImage: number
  }
  categories: CultureTopicCategory[]
  entries: CultureTopicEntry[]
}

/* ---- 人物专栏 ---- */

export interface PersonProjectRef {
  id: string
  name: string
  intangibleHeritage: string
  ethnicGroupName: string | null
  detailPath: string
}

export interface PersonItem {
  name: string
  ethnicGroupName: string | null
  roleType: 'inheritor' | 'master'
  roleLabel: string
  domain: string | null
  lifespan: string | null
  bio: string | null
  projects: PersonProjectRef[]
  topLevel: string | null
}

export interface PersonOption {
  value: string
  label: string
  count: number
}

export interface PersonDirectory {
  summary: {
    personCount: number
    inheritorCount: number
    masterCount: number
    ethnicCount: number
    projectCount: number
  }
  filters: {
    domains: PersonOption[]
    ethnics: PersonOption[]
    roles: PersonOption[]
  }
  persons: PersonItem[]
  total: number
}

/* ---- 民族自治地方 ---- */

export interface AutonomousArea {
  name: string
  level: string
  levelLabel: string
  ethnicGroups: string[]
  province: string | null
  establishedYear: number | null
  seat: string | null
}

export interface AreaLevelGroup {
  level: string
  label: string
  count: number
  areas: AutonomousArea[]
}

export interface AreaEthnicGrouping {
  ethnic: string
  count: number
  matchedName: string | null
  matchedSlug: string | null
  themeColor: string | null
  areas: AutonomousArea[]
}

export interface AreaProvinceGrouping {
  province: string
  count: number
  areas: AutonomousArea[]
}

export interface AutonomousAreaDirectory {
  summary: {
    total: number
    regionCount: number
    prefectureCount: number
    countyCount: number
    ethnicCount: number
    provinceCount: number
  }
  levels: AreaLevelGroup[]
  ethnics: AreaEthnicGrouping[]
  provinces: AreaProvinceGrouping[]
}

/* ---- 传统体育 ---- */

export interface SportEthnicRef {
  id: string
  name: string
  slug: string
  themeColor: string
}

export interface TraditionalSport {
  name: string
  category: string
  categoryLabel: string
  ethnicOrigins: string[]
  description: string | null
  equipment: string | null
  venue: string | null
  teamSize: string | null
  firstEventYear: number | null
  subEvents: string[]
  heritageLink: string | null
  matchedEthnics: SportEthnicRef[]
}

export interface TraditionalSportDirectory {
  summary: {
    total: number
    categoryCount: number
    ethnicCount: number
    withSubEvents: number
    withHeritage: number
  }
  sports: TraditionalSport[]
}

/* ---- 全文检索 / 兴趣推荐 ---- */

export interface SearchHit {
  docType: string
  docId: string
  url: string
  title: string
  titleHtml: string
  summary: string | null
  summaryHtml: string
  ethnicName: string | null
  category: string | null
  region: string | null
  coverImage: string | null
  themeColor: string | null
  score: number
  matchBy: string
}

export interface FullTextSearchResult {
  keyword: string
  total: number
  tookMs: number
  type: string
  list: SearchHit[]
  facets: Record<string, number>
  highlights: Record<string, number>
}

export interface InterestTag {
  id: string
  dimension: string
  name: string
  nameEn: string | null
  description: string | null
  color: string | null
  enabled: boolean
  orderNum: number
}

export interface RecoItem {
  docType: string
  docId: string
  url: string
  title: string
  summary: string | null
  ethnicName: string | null
  category: string | null
  coverImage: string | null
  themeColor: string | null
  score: number
  reason: string | null
}

export interface Recommendation {
  list: RecoItem[]
  basis: string
  basisLabel: string
  dataNote: string
  confidence: number
  behaviorCount: number
  interestCount: number
}

import { http } from './client'
import type {
  ArtDetail,
  ArtListItem,
  AutonomousAreaDirectory,
  ContentStats,
  ContentType,
  CultureTopic,
  EthnicBrief,
  EthnicDetail,
  EthnicListItem,
  FestivalCalendar,
  FestivalDetail,
  FestivalListItem,
  FullTextSearchResult,
  HeritageStats,
  InterestTag,
  LanguageAtlas,
  PageResult,
  PersonDirectory,
  Recommendation,
  SearchGroup,
  ShareResource,
  TopicDetail,
  TopicListItem,
  TraditionalSportDirectory,
} from './types'

/** 民族 */
export const ethnicApi = {
  list: (params: { page: number; size: number; region?: string; languageFamily?: string; keyword?: string; sort?: string }) =>
    http<PageResult<EthnicListItem>>({ url: '/ethnic-groups', method: 'GET', params }),
  all: () => http<EthnicBrief[]>({ url: '/ethnic-groups/all', method: 'GET' }),
  detail: (id: string) => http<EthnicDetail>({ url: `/ethnic-groups/${id}`, method: 'GET' }),
  languageAtlas: () => http<LanguageAtlas>({ url: '/ethnic-groups/language-atlas', method: 'GET' }),
}

/** 节日 */
export const festivalApi = {
  list: (params: { page: number; size: number; type?: string; keyword?: string }) =>
    http<PageResult<FestivalListItem>>({ url: '/festivals', method: 'GET', params }),
  detail: (id: string) => http<FestivalDetail>({ url: `/festivals/${id}`, method: 'GET' }),
  calendar: (year?: number) =>
    http<FestivalCalendar>({ url: '/festivals/calendar', method: 'GET', params: year ? { year } : undefined }),
}

/** 艺术 */
export const artApi = {
  list: (params: {
    page: number
    size: number
    category?: string
    ethnicGroupId?: string
    intangibleHeritage?: string
    keyword?: string
  }) => http<PageResult<ArtListItem>>({ url: '/arts', method: 'GET', params }),
  detail: (id: string) => http<ArtDetail>({ url: `/arts/${id}`, method: 'GET' }),
  heritageStats: () => http<HeritageStats>({ url: '/arts/heritage-stats', method: 'GET' }),
}

/** 专题 */
export const topicApi = {
  list: (params: { page: number; size: number }) =>
    http<PageResult<TopicListItem>>({ url: '/topics', method: 'GET', params }),
  detail: (id: string) => http<TopicDetail>({ url: `/topics/${id}`, method: 'GET' }),
}

/** 文化专题（服饰 / 民居） */
export const cultureTopicApi = {
  get: (topic: 'costume' | 'dwelling') =>
    http<CultureTopic>({ url: `/culture-topics/${topic}`, method: 'GET' }),
}

/** 人物专栏 */
export const personApi = {
  list: (params?: { keyword?: string; domain?: string; ethnic?: string; roleType?: string }) =>
    http<PersonDirectory>({ url: '/persons', method: 'GET', params }),
}

/** 民族自治地方 */
export const autonomousAreaApi = {
  list: (params?: { level?: string; keyword?: string; ethnic?: string }) =>
    http<AutonomousAreaDirectory>({ url: '/autonomous-areas', method: 'GET', params }),
}

/** 传统体育 */
export const traditionalSportApi = {
  list: (params?: { category?: string; ethnic?: string; keyword?: string }) =>
    http<TraditionalSportDirectory>({ url: '/traditional-sports', method: 'GET', params }),
}

/** 搜索 */
export const searchApi = {
  search: (keyword: string) =>
    http<SearchGroup>({ url: '/search', method: 'GET', params: { q: keyword } }),
  hot: () => http<string[]>({ url: '/search/hot', method: 'GET' }),
  full: (params: { q?: string; type?: string; ethnic?: string; page?: number; size?: number }) =>
    http<FullTextSearchResult>({ url: '/search/full', method: 'GET', params }),
}

/** 兴趣标签与个性化推荐 */
export const recommendApi = {
  tags: () => http<Record<string, InterestTag[]>>({ url: '/interests/tags', method: 'GET' }),
  mine: () => http<InterestTag[]>({ url: '/interests/mine', method: 'GET' }),
  saveMine: (tagIds: string[]) =>
    http<number>({ url: '/interests/mine', method: 'POST', data: { tagIds } }),
  recommend: (params?: { size?: number; excludeType?: string }) =>
    http<Recommendation>({ url: '/recommend', method: 'GET', params }),
  reportView: (targetType: string, targetId: string) =>
    http<unknown>({ url: '/behaviors/view', method: 'POST', data: { targetType, targetId } }),
}

/** 互动（点赞 / 收藏） */
export const interactionApi = {
  like: (type: ContentType, id: string) =>
    http<unknown>({ url: `/contents/${type}/${id}/like`, method: 'POST' }),
  unlike: (type: ContentType, id: string) =>
    http<unknown>({ url: `/contents/${type}/${id}/like`, method: 'DELETE' }),
  favorite: (type: ContentType, id: string) =>
    http<unknown>({ url: `/contents/${type}/${id}/favorite`, method: 'POST' }),
  unfavorite: (type: ContentType, id: string) =>
    http<unknown>({ url: `/contents/${type}/${id}/favorite`, method: 'DELETE' }),
  stats: (type: ContentType, id: string) =>
    http<ContentStats>({ url: `/contents/${type}/${id}/stats`, method: 'GET' }),
}

/** 我的 */
export const meApi = {
  favorites: (params: { page: number; size: number }) =>
    http<PageResult<unknown>>({ url: '/me/favorites', method: 'GET', params }),
}

/** 分享 */
export const shareApi = {
  create: (data: { type: ContentType; refId: string }) =>
    http<ShareResource>({ url: '/share', method: 'POST', data }),
}

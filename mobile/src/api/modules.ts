import { http } from './client'
import type {
  ArtDetail,
  ArtListItem,
  ContentStats,
  ContentType,
  EthnicBrief,
  EthnicDetail,
  EthnicListItem,
  FestivalDetail,
  FestivalListItem,
  PageResult,
  SearchGroup,
  ShareResource,
  TopicDetail,
  TopicListItem,
} from './types'

/** 民族 */
export const ethnicApi = {
  list: (params: { page: number; size: number; region?: string; languageFamily?: string; keyword?: string; sort?: string }) =>
    http<PageResult<EthnicListItem>>({ url: '/ethnic-groups', method: 'GET', params }),
  all: () => http<EthnicBrief[]>({ url: '/ethnic-groups/all', method: 'GET' }),
  detail: (id: string) => http<EthnicDetail>({ url: `/ethnic-groups/${id}`, method: 'GET' }),
}

/** 节日 */
export const festivalApi = {
  list: (params: { page: number; size: number; type?: string; keyword?: string }) =>
    http<PageResult<FestivalListItem>>({ url: '/festivals', method: 'GET', params }),
  detail: (id: string) => http<FestivalDetail>({ url: `/festivals/${id}`, method: 'GET' }),
}

/** 艺术 */
export const artApi = {
  list: (params: { page: number; size: number; category?: string; intangibleHeritage?: string; keyword?: string }) =>
    http<PageResult<ArtListItem>>({ url: '/arts', method: 'GET', params }),
  detail: (id: string) => http<ArtDetail>({ url: `/arts/${id}`, method: 'GET' }),
}

/** 专题 */
export const topicApi = {
  list: (params: { page: number; size: number }) =>
    http<PageResult<TopicListItem>>({ url: '/topics', method: 'GET', params }),
  detail: (id: string) => http<TopicDetail>({ url: `/topics/${id}`, method: 'GET' }),
}

/** 搜索 */
export const searchApi = {
  search: (keyword: string) =>
    http<SearchGroup>({ url: '/search', method: 'GET', params: { keyword } }),
  hot: () => http<string[]>({ url: '/search/hot', method: 'GET' }),
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

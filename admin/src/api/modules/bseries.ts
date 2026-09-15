import { get, post, put, del } from '../request'
import type { PageData } from '../types'

/* ==========================================================================
   B 系列内容：人物档案 / 民族自治地方 / 传统体育
   对应后端 /admin/persons、/admin/areas、/admin/sports
   ========================================================================== */

/** 人物档案（person_profile） */
export interface PersonProfile {
  id?: string
  /** 姓名（与 art.inheritors 中的写法一致） */
  personName: string
  /** 所属民族（同名不同族靠此区分，必填以免误合并） */
  ethnicGroupName: string | null
  /** inheritor 代表性传承人 / master 历史文化名家 */
  roleType: string
  /** 领域：音乐 / 舞蹈 / 戏剧 / 服饰 / 技艺 / 建筑 */
  domain: string | null
  bio: string | null
  /** 生卒年，如「1894—1961」 */
  lifespan: string | null
  createdAt?: string
  updatedAt?: string
}

/** 民族自治地方（autonomous_area） */
export interface AutonomousArea {
  id?: string
  name: string
  /** autonomous_region / autonomous_prefecture / autonomous_county */
  level: string
  /** 自治民族名称数组（后端为 jsonb，前端以数组编辑） */
  ethnicGroups: string[] | string
  province: string | null
  establishedYear: number | null
  seat: string | null
  createdAt?: string
  updatedAt?: string
}

/** 传统体育项目（traditional_sport） */
export interface TraditionalSport {
  id?: string
  name: string
  /** ball / water / strength / accuracy / speed / martial / equestrian / gymnastics / swing */
  category: string
  ethnicOrigins: string[] | string
  description: string | null
  equipment: string | null
  venue: string | null
  teamSize: string | null
  firstEventYear: number | null
  subEvents: string[] | string
  heritageLink: string | null
  createdAt?: string
  updatedAt?: string
}

export interface BSeriesQuery {
  page?: number // 0 基
  size?: number
  keyword?: string
  [k: string]: unknown
}

/* ---- 人物档案 ---- */
export function listPersons(params: BSeriesQuery): Promise<PageData<PersonProfile>> {
  return get<PageData<PersonProfile>>('/admin/persons', params as Record<string, unknown>)
}
export function getPerson(id: string): Promise<PersonProfile> {
  return get<PersonProfile>(`/admin/persons/${id}`)
}
export function createPerson(data: PersonProfile): Promise<string> {
  return post<string>('/admin/persons', data)
}
export function updatePerson(id: string, data: PersonProfile): Promise<void> {
  return put<void>(`/admin/persons/${id}`, data)
}
export function deletePerson(id: string): Promise<void> {
  return del<void>(`/admin/persons/${id}`)
}

/* ---- 民族自治地方 ---- */
export function listAreas(params: BSeriesQuery): Promise<PageData<AutonomousArea>> {
  return get<PageData<AutonomousArea>>('/admin/areas', params as Record<string, unknown>)
}
export function getArea(id: string): Promise<AutonomousArea> {
  return get<AutonomousArea>(`/admin/areas/${id}`)
}
export function createArea(data: AutonomousArea): Promise<string> {
  return post<string>('/admin/areas', data)
}
export function updateArea(id: string, data: AutonomousArea): Promise<void> {
  return put<void>(`/admin/areas/${id}`, data)
}
export function deleteArea(id: string): Promise<void> {
  return del<void>(`/admin/areas/${id}`)
}

/* ---- 传统体育 ---- */
export function listSports(params: BSeriesQuery): Promise<PageData<TraditionalSport>> {
  return get<PageData<TraditionalSport>>('/admin/sports', params as Record<string, unknown>)
}
export function getSport(id: string): Promise<TraditionalSport> {
  return get<TraditionalSport>(`/admin/sports/${id}`)
}
export function createSport(data: TraditionalSport): Promise<string> {
  return post<string>('/admin/sports', data)
}
export function updateSport(id: string, data: TraditionalSport): Promise<void> {
  return put<void>(`/admin/sports/${id}`, data)
}
export function deleteSport(id: string): Promise<void> {
  return del<void>(`/admin/sports/${id}`)
}

/* ==========================================================================
   内容来源（方向 C-1 可溯源）—— 对应后端 /admin/sources
   ========================================================================== */

export interface ContentSource {
  id?: string
  name: string
  publisher: string | null
  publisherShort: string | null
  documentTitle: string | null
  url: string | null
  /** official / academic / open / other */
  sourceType: string
  /** scrape / ocr / manual / api */
  collectMethod: string | null
  remark: string | null
  orderNum: number | null
  createdAt?: string
  updatedAt?: string
}

export function listSources(params: BSeriesQuery): Promise<PageData<ContentSource>> {
  return get<PageData<ContentSource>>('/admin/sources', params as Record<string, unknown>)
}
export function getSource(id: string): Promise<ContentSource> {
  return get<ContentSource>(`/admin/sources/${id}`)
}
export function createSource(data: ContentSource): Promise<string> {
  return post<string>('/admin/sources', data)
}
export function updateSource(id: string, data: ContentSource): Promise<void> {
  return put<void>(`/admin/sources/${id}`, data)
}
export function deleteSource(id: string): Promise<void> {
  return del<void>(`/admin/sources/${id}`)
}
/** 该来源被多少条内容引用（删除前提示用） */
export function sourceUsage(id: string): Promise<number> {
  return get<number>(`/admin/sources/${id}/usage`)
}

export const SOURCE_TYPE_OPTIONS = [
  { value: 'official', label: '官方权威' },
  { value: 'academic', label: '学术资料' },
  { value: 'open', label: '开放图库' },
  { value: 'other', label: '其他' }
]

export const COLLECT_METHOD_OPTIONS = [
  { value: 'scrape', label: '程序抓取' },
  { value: 'ocr', label: 'OCR 识别' },
  { value: 'manual', label: '人工整理' },
  { value: 'api', label: '接口获取' }
]

/* ==========================================================================
   图片版权署名（方向 C-4）—— 对应后端 /admin/image-credits
   ========================================================================== */

export interface ImageCredit {
  id?: string
  imagePath: string
  targetType?: string | null
  targetId?: string | null
  caption: string | null
  /** verified 已核实 / unverified 来源待核 / original 原创无需署名 */
  creditStatus: string
  author: string | null
  license: string | null
  licenseUrl: string | null
  sourceUrl: string | null
  sourceSite: string | null
  attributionRequired: boolean | null
  remark: string | null
  updatedAt?: string
}

export function listImageCredits(params: BSeriesQuery): Promise<PageData<ImageCredit>> {
  return get<PageData<ImageCredit>>('/admin/image-credits', params as Record<string, unknown>)
}
export function getImageCredit(id: string): Promise<ImageCredit> {
  return get<ImageCredit>(`/admin/image-credits/${id}`)
}
export function updateImageCredit(id: string, data: ImageCredit): Promise<void> {
  return put<void>(`/admin/image-credits/${id}`, data)
}

export const CREDIT_STATUS_OPTIONS = [
  { value: 'unverified', label: '来源待核' },
  { value: 'verified', label: '已核实' },
  { value: 'original', label: '原创 / 无需署名' }
]

/* ---- 枚举（与后端常量保持一致） ---- */

export const ROLE_TYPE_OPTIONS = [
  { value: 'inheritor', label: '代表性传承人' },
  { value: 'master', label: '历史文化名家' }
]

export const DOMAIN_OPTIONS = ['音乐', '舞蹈', '戏剧', '服饰', '技艺', '建筑', '美术', '其他']

export const AREA_LEVEL_OPTIONS = [
  { value: 'autonomous_region', label: '自治区' },
  { value: 'autonomous_prefecture', label: '自治州' },
  { value: 'autonomous_county', label: '自治县 / 自治旗' }
]

export const SPORT_CATEGORY_OPTIONS = [
  { value: 'ball', label: '球类' },
  { value: 'water', label: '水上' },
  { value: 'strength', label: '力量对抗' },
  { value: 'accuracy', label: '射击与技巧' },
  { value: 'speed', label: '竞速' },
  { value: 'martial', label: '武术' },
  { value: 'equestrian', label: '马术' },
  { value: 'gymnastics', label: '健身操' },
  { value: 'swing', label: '秋千' }
]

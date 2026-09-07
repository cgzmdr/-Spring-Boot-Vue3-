import { create } from 'zustand'
import AsyncStorage from '@react-native-async-storage/async-storage'
import { zh, type ZhDict } from '../i18n/zh'
import { en } from '../i18n/en'

type Lang = 'zh' | 'en'

/** 递归路径类型：'home.sectionEthnic' 等 */
type Path<T> = {
  [K in keyof T]: T[K] extends object ? `${K & string}.${Path<T[K]>}` : K & string
}[keyof T]

const STORAGE_KEY = 'cend_lang'
const dicts: Record<Lang, ZhDict> = { zh, en }

interface LangState {
  lang: Lang
  isEn: boolean
  restore: () => Promise<void>
  setLang: (lang: Lang) => void
  toggle: () => void
  t: (key: Path<ZhDict>) => string
  /** 中英双语选择：undefined 时按当前语言取 */
  pick: (zhText: string, enText: string) => string
}

export const useLangStore = create<LangState>((set, get) => ({
  lang: 'zh',
  isEn: false,

  restore: async () => {
    try {
      const saved = await AsyncStorage.getItem(STORAGE_KEY)
      if (saved === 'zh' || saved === 'en') {
        set({ lang: saved, isEn: saved === 'en' })
      }
    } catch {
      // 静默失败，保持默认中文
    }
  },

  setLang: (lang) => {
    set({ lang, isEn: lang === 'en' })
    AsyncStorage.setItem(STORAGE_KEY, lang).catch(() => {})
  },

  toggle: () => {
    const next: Lang = get().isEn ? 'zh' : 'en'
    get().setLang(next)
  },

  t: (key) => {
    const dict = dicts[get().lang]
    const value = key.split('.').reduce<unknown>((acc, part) => {
      if (acc && typeof acc === 'object') return (acc as Record<string, unknown>)[part]
      return undefined
    }, dict)
    return typeof value === 'string' ? value : (key as string)
  },

  pick: (zhText, enText) => (get().isEn ? enText : zhText),
}))

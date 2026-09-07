import { Platform } from 'react-native'

/**
 * 设计令牌 —— 与 C 端 design/common.css「杂志编辑风」保持一致
 * 纸张色 / 墨色 / 朱红强调 / 衬线标题
 */
export const palette = {
  paper: '#FAF9F5', // 纸张底
  paperDeep: '#F1EEE5', // 深一档纸张（卡片/分隔）
  ink: '#141414', // 墨色正文
  accent: '#B6402E', // 朱红强调
  accentDark: '#96331F', // 朱红深（按压/实心按钮）
  accentSoft: '#F6E9E4', // 朱红浅底
  muted: '#6E6A63', // 次级文字
  faint: '#9A958C', // 弱文字
  border: '#E5E0D6', // 分割线
  white: '#FFFFFF',
  gold: '#C9A24B', // 点缀（家庭福墙等）
  coverFallback: '#8A6B57', // 封面无图兜底色
} as const

export const type = {
  /** 衬线标题（杂志编辑风） */
  serif: Platform.select({ ios: 'Georgia', android: 'serif', default: 'serif' })!,
  /** 无衬线正文 */
  sans: Platform.select({ ios: 'System', android: 'sans-serif', default: 'system-ui' })!,
} as const

export const spacing = {
  xs: 6,
  sm: 10,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
} as const

export const radius = {
  sm: 4,
  md: 8,
  lg: 14,
} as const

/** 通用排版/布局样式（StyleSheet.create 组合） */
export const layout = {
  screen: {
    flex: 1,
    backgroundColor: palette.paper,
  } as const,
  section: {
    paddingHorizontal: spacing.md,
  } as const,
}

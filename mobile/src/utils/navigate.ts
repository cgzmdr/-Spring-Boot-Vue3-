import type { RootStackParamList } from '../navigation/types'

/**
 * 兼容 NativeStackNavigationProp / TabNavigationProp 的最小导航接口
 */
type NavLike = {
  navigate: <Name extends keyof RootStackParamList>(
    ...args: Name extends unknown ? ([Name] | [Name, RootStackParamList[Name]]) : never
  ) => void
}

/**
 * 根据内容类型跳转到对应详情页（收藏 / 搜索结果 / 专题条目共用）。
 * 用 switch 保证类型安全，避免动态字符串路由。
 */
export function navigateByContentType(navigation: NavLike, type: string, id?: string) {
  if (!id) return
  switch (type) {
    case 'ethnic':
      navigation.navigate('EthnicDetail', { id })
      break
    case 'festival':
      navigation.navigate('FestivalDetail', { id })
      break
    case 'art':
      navigation.navigate('ArtDetail', { id })
      break
    case 'topic':
      navigation.navigate('TopicDetail', { id })
      break
    default:
      break
  }
}

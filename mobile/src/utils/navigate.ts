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
 */
export function navigateByContentType(navigation: NavLike, type: string, id?: string, url?: string) {
  if (!id && !url) return
  switch (type) {
    case 'ethnic':
      if (id) navigation.navigate('EthnicDetail', { id })
      break
    case 'festival':
      if (id) navigation.navigate('FestivalDetail', { id })
      break
    case 'art':
      if (id) navigation.navigate('ArtDetail', { id })
      break
    case 'topic':
      if (id) navigation.navigate('TopicDetail', { id })
      break
    case 'person':
      navigation.navigate('Persons')
      break
    case 'area':
      navigation.navigate('Autonomous')
      break
    case 'sport':
      navigation.navigate('Sports')
      break
    case 'food':
    case 'custom': {
      const m = url?.match(/\/ethnic\/([^/#?]+)/)
      if (m?.[1]) navigation.navigate('EthnicDetail', { id: m[1] })
      break
    }
    default:
      break
  }
}

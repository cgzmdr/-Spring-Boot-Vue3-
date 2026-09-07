/** 根导航栈参数 */
export type RootStackParamList = {
  Tabs: undefined
  EthnicDetail: { id: string }
  FestivalDetail: { id: string }
  ArtDetail: { id: string }
  TopicDetail: { id: string; title?: string }
  Search: undefined
  Login: { redirect?: string } | undefined
  Unity: undefined
  About: undefined
}

/** 底部 Tab 参数 */
export type TabParamList = {
  Home: undefined
  Ethnic: undefined
  Festival: undefined
  Art: undefined
  Profile: undefined
}

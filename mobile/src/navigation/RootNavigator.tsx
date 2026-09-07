import { useEffect } from 'react'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import { type NavigationContainerRef } from '@react-navigation/native'
import { onAuthExpired } from '../api/client'
import { palette, type } from '../theme'
import Tabs from './Tabs'
import EthnicDetailScreen from '../screens/ethnic/[id]'
import FestivalDetailScreen from '../screens/festival/[id]'
import ArtDetailScreen from '../screens/art/[id]'
import TopicDetailScreen from '../screens/topic/[id]'
import SearchScreen from '../screens/search'
import LoginScreen from '../screens/login'
import UnityScreen from '../screens/unity'
import AboutScreen from '../screens/about'
import type { RootStackParamList } from './types'

const Stack = createNativeStackNavigator<RootStackParamList>()

export default function RootNavigator({ navigationRef }: { navigationRef: React.RefObject<NavigationContainerRef<RootStackParamList> | null> }) {
  // 登录失效（1003/1005）时跳转登录页
  useEffect(() => {
    onAuthExpired(() => {
      const nav = navigationRef.current
      if (!nav) return
      const current = nav.getCurrentRoute()?.name
      if (current !== 'Login') {
        nav.navigate('Login' as never)
      }
    })
    return () => onAuthExpired(null)
  }, [navigationRef])

  return (
    <Stack.Navigator
      screenOptions={{
        headerStyle: { backgroundColor: palette.paper },
        headerTintColor: palette.ink,
        headerTitleStyle: { fontFamily: type.serif, fontSize: 17 },
        headerShadowVisible: false,
        contentStyle: { backgroundColor: palette.paper },
      }}
    >
      <Stack.Screen name="Tabs" component={Tabs} options={{ headerShown: false }} />
      <Stack.Screen name="EthnicDetail" component={EthnicDetailScreen} options={{ title: '民族' }} />
      <Stack.Screen name="FestivalDetail" component={FestivalDetailScreen} options={{ title: '节日' }} />
      <Stack.Screen name="ArtDetail" component={ArtDetailScreen} options={{ title: '艺术' }} />
      <Stack.Screen name="TopicDetail" component={TopicDetailScreen} options={{ title: '专题' }} />
      <Stack.Screen name="Search" component={SearchScreen} options={{ title: '搜索', headerShown: false }} />
      <Stack.Screen name="Login" component={LoginScreen} options={{ title: '登录', presentation: 'modal' }} />
      <Stack.Screen name="Unity" component={UnityScreen} options={{ title: '民族团结' }} />
      <Stack.Screen name="About" component={AboutScreen} options={{ title: '关于我们' }} />
    </Stack.Navigator>
  )
}

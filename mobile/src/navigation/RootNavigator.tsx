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
import FestivalCalendarScreen from '../screens/festival/calendar'
import HeritageScreen from '../screens/heritage'
import PersonsScreen from '../screens/persons'
import AutonomousScreen from '../screens/autonomous'
import SportsScreen from '../screens/sports'
import CultureTopicScreen from '../screens/culture'
import LanguagesScreen from '../screens/ethnic/languages'
import InterestsScreen from '../screens/interests'
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
      <Stack.Screen name="FestivalCalendar" component={FestivalCalendarScreen} options={{ title: '节日日历' }} />
      <Stack.Screen name="Heritage" component={HeritageScreen} options={{ title: '非遗名录' }} />
      <Stack.Screen name="Persons" component={PersonsScreen} options={{ title: '人物专栏' }} />
      <Stack.Screen name="Autonomous" component={AutonomousScreen} options={{ title: '自治地方' }} />
      <Stack.Screen name="Sports" component={SportsScreen} options={{ title: '传统体育' }} />
      <Stack.Screen name="CultureTopic" component={CultureTopicScreen} options={{ title: '文化专题' }} />
      <Stack.Screen name="Languages" component={LanguagesScreen} options={{ title: '民族语文' }} />
      <Stack.Screen name="Interests" component={InterestsScreen} options={{ title: '兴趣推荐' }} />
    </Stack.Navigator>
  )
}

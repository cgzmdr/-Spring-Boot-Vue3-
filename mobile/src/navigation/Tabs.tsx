import { Ionicons } from '@expo/vector-icons'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { palette, type } from '../theme'
import { useLangStore } from '../stores/lang'
import HomeScreen from '../screens/home'
import EthnicScreen from '../screens/ethnic'
import FestivalScreen from '../screens/festival'
import ArtScreen from '../screens/art'
import ProfileScreen from '../screens/profile'
import type { TabParamList } from './types'

const Tab = createBottomTabNavigator<TabParamList>()

const icons: Record<keyof TabParamList, [keyof typeof Ionicons.glyphMap, keyof typeof Ionicons.glyphMap]> = {
  Home: ['home-outline', 'home'],
  Ethnic: ['albums-outline', 'albums'],
  Festival: ['calendar-outline', 'calendar'],
  Art: ['color-palette-outline', 'color-palette'],
  Profile: ['person-circle-outline', 'person-circle'],
}

export default function Tabs() {
  const t = useLangStore((s) => s.t)

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: palette.accent,
        tabBarInactiveTintColor: palette.faint,
        tabBarStyle: {
          backgroundColor: palette.paper,
          borderTopColor: palette.border,
        },
        tabBarLabelStyle: {
          fontFamily: type.sans,
          fontSize: 11,
        },
        tabBarIcon: ({ focused, color, size }) => {
          const [outline, filled] = icons[route.name]
          return <Ionicons name={focused ? filled : outline} size={size - 2} color={color} />
        },
      })}
    >
      <Tab.Screen name="Home" component={HomeScreen} options={{ title: t('nav.home') }} />
      <Tab.Screen name="Ethnic" component={EthnicScreen} options={{ title: t('nav.ethnic') }} />
      <Tab.Screen name="Festival" component={FestivalScreen} options={{ title: t('nav.festival') }} />
      <Tab.Screen name="Art" component={ArtScreen} options={{ title: t('nav.art') }} />
      <Tab.Screen name="Profile" component={ProfileScreen} options={{ title: t('nav.mine') }} />
    </Tab.Navigator>
  )
}

import { useEffect, useRef } from 'react'
import { NavigationContainer, type NavigationContainerRef, DefaultTheme } from '@react-navigation/native'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import { StatusBar } from 'expo-status-bar'
import RootNavigator from './src/navigation/RootNavigator'
import type { RootStackParamList } from './src/navigation/types'
import { useAuthStore } from './src/stores/auth'
import { useLangStore } from './src/stores/lang'
import { palette } from './src/theme'

const navTheme = {
  ...DefaultTheme,
  colors: {
    ...DefaultTheme.colors,
    background: palette.paper,
    card: palette.paper,
    text: palette.ink,
    primary: palette.accent,
    border: palette.border,
  },
}

export default function App() {
  const navigationRef = useRef<NavigationContainerRef<RootStackParamList> | null>(null)
  const restoreAuth = useAuthStore((s) => s.restore)
  const restoreLang = useLangStore((s) => s.restore)

  // 启动时恢复语言与登录态（静默，失败不影响浏览）
  useEffect(() => {
    restoreLang()
    restoreAuth()
  }, [restoreLang, restoreAuth])

  return (
    <SafeAreaProvider>
      <StatusBar style="dark" />
      <NavigationContainer ref={navigationRef} theme={navTheme}>
        <RootNavigator navigationRef={navigationRef} />
      </NavigationContainer>
    </SafeAreaProvider>
  )
}

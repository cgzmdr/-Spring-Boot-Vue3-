import type { ReactNode } from 'react'
import { ScrollView, StyleSheet, type ScrollViewProps, type StyleProp, type ViewStyle } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { palette } from '../theme'

interface ScreenProps {
  children: ReactNode
  style?: StyleProp<ViewStyle>
}

/** 静态容器（FlexScreen 布局用，如列表） */
export function Screen({ children, style }: ScreenProps) {
  return <SafeAreaView style={[styles.screen, style]} edges={['top', 'left', 'right']}>{children}</SafeAreaView>
}

interface ScrollScreenProps extends ScrollViewProps {
  children: ReactNode
}

/** 可滚动容器（普通页面） */
export function ScrollScreen({ children, ...rest }: ScrollScreenProps) {
  return (
    <SafeAreaView style={styles.screen} edges={['top', 'left', 'right']}>
      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
        {...rest}
      >
        {children}
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: palette.paper,
  },
  content: {
    paddingBottom: 40,
  },
})

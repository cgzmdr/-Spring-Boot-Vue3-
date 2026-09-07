import { ActivityIndicator, StyleSheet, Text, View } from 'react-native'
import { palette, spacing, type } from '../theme'

interface EmptyViewProps {
  text?: string
  /** true 时显示加载骨架（首屏加载） */
  loading?: boolean
  loadingText?: string
}

/** 空态 / 加载态 */
export default function EmptyView({ text, loading = false, loadingText }: EmptyViewProps) {
  return (
    <View style={styles.wrap}>
      {loading ? (
        <>
          <ActivityIndicator color={palette.accent} />
          <Text style={styles.text}>{loadingText || '加载中…'}</Text>
        </>
      ) : (
        <>
          <View style={styles.mark}>
            <Text style={styles.markText}>·</Text>
          </View>
          <Text style={styles.text}>{text || '暂无内容'}</Text>
        </>
      )}
    </View>
  )
}

const styles = StyleSheet.create({
  wrap: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: spacing.xxl,
    gap: spacing.sm,
  },
  mark: {
    width: 44,
    height: 44,
    borderRadius: 22,
    borderWidth: 1,
    borderColor: palette.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  markText: {
    fontSize: 26,
    color: palette.faint,
    marginTop: -6,
  },
  text: {
    fontSize: 13,
    color: palette.faint,
  },
})

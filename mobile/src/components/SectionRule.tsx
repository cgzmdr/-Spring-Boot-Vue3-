import { StyleSheet, Text, View } from 'react-native'
import { palette, spacing, type } from '../theme'

interface SectionRuleProps {
  /** 章节编号（01/02/03…） */
  index: string
  title: string
}

/** 章节标题：朱红编号 + 标题 + 横线 */
export default function SectionRule({ index, title }: SectionRuleProps) {
  return (
    <View style={styles.wrap}>
      <Text style={styles.index}>{index}</Text>
      <Text style={styles.title}>{title}</Text>
      <View style={styles.line} />
    </View>
  )
}

const styles = StyleSheet.create({
  wrap: {
    flexDirection: 'row',
    alignItems: 'baseline',
    marginBottom: spacing.md,
    paddingHorizontal: spacing.md,
  },
  index: {
    fontFamily: type.serif,
    fontSize: 15,
    color: palette.accent,
    marginRight: spacing.sm,
  },
  title: {
    fontFamily: type.serif,
    fontSize: 22,
    color: palette.ink,
    marginRight: spacing.md,
  },
  line: {
    flex: 1,
    height: StyleSheet.hairlineWidth,
    backgroundColor: palette.border,
    alignSelf: 'center',
  },
})

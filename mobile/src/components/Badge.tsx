import { StyleSheet, Text, View } from 'react-native'
import { palette, spacing, type } from '../theme'

interface BadgeProps {
  text: string
  /** 实心墨色（默认）或描边 */
  variant?: 'solid' | 'outline'
}

export default function Badge({ text, variant = 'solid' }: BadgeProps) {
  return (
    <View style={[styles.base, variant === 'solid' ? styles.solid : styles.outline]}>
      <Text style={[styles.text, variant === 'solid' ? styles.textSolid : styles.textOutline]}>{text}</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  base: {
    paddingHorizontal: spacing.sm,
    paddingVertical: 2,
    borderRadius: 3,
    alignSelf: 'flex-start',
  },
  solid: {
    backgroundColor: palette.ink,
  },
  outline: {
    borderWidth: 1,
    borderColor: palette.border,
  },
  text: {
    fontSize: 11,
    fontFamily: type.sans,
  },
  textSolid: {
    color: palette.paper,
  },
  textOutline: {
    color: palette.muted,
  },
})

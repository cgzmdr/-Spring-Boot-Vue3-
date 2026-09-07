import { Pressable, StyleSheet, Text } from 'react-native'
import { palette, spacing, type } from '../theme'

interface ChipProps {
  label: string
  active: boolean
  onPress: () => void
}

/** 筛选胶囊 */
export default function Chip({ label, active, onPress }: ChipProps) {
  return (
    <Pressable
      style={[styles.chip, active && styles.chipActive]}
      onPress={onPress}
    >
      <Text style={[styles.label, active && styles.labelActive]}>{label}</Text>
    </Pressable>
  )
}

const styles = StyleSheet.create({
  chip: {
    paddingHorizontal: spacing.md,
    paddingVertical: 6,
    borderRadius: 99,
    borderWidth: 1,
    borderColor: palette.border,
    backgroundColor: palette.paper,
    marginRight: spacing.sm,
  },
  chipActive: {
    backgroundColor: palette.ink,
    borderColor: palette.ink,
  },
  label: {
    fontSize: 13,
    color: palette.muted,
  },
  labelActive: {
    color: palette.paper,
  },
})

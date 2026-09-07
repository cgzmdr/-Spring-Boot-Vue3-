import { Pressable, StyleSheet, Text, View } from 'react-native'
import { palette, radius, spacing, type } from '../theme'
import CoverImage from './CoverImage'
import Badge from './Badge'

interface ListItemProps {
  title: string
  subtitle?: string
  /** 右上角徽标 */
  tag?: string
  image?: string | null
  fallbackColor?: string
  fallbackLabel?: string
  onPress?: () => void
}

/** 横向列表条目：左图/色块 + 标题 + 副文 */
export default function ListItem({ title, subtitle, tag, image, fallbackColor, fallbackLabel, onPress }: ListItemProps) {
  return (
    <Pressable
      style={({ pressed }) => [styles.row, pressed && styles.pressed]}
      onPress={onPress}
    >
      <CoverImage
        uri={image}
        fallbackColor={fallbackColor}
        fallbackLabel={fallbackLabel || title}
        style={styles.thumb}
      />
      <View style={styles.body}>
        <View style={styles.titleRow}>
          <Text style={styles.title} numberOfLines={1}>{title}</Text>
          {tag ? <Badge text={tag} /> : null}
        </View>
        {subtitle ? <Text style={styles.subtitle} numberOfLines={2}>{subtitle}</Text> : null}
        <Text style={styles.arrow}>→</Text>
      </View>
    </Pressable>
  )
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    backgroundColor: palette.white,
    borderRadius: radius.md,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    padding: spacing.sm,
    marginBottom: spacing.sm,
  },
  pressed: {
    opacity: 0.85,
  },
  thumb: {
    width: 84,
    height: 84,
    borderRadius: radius.sm,
  },
  body: {
    flex: 1,
    marginLeft: spacing.sm,
    justifyContent: 'center',
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
  },
  title: {
    flexShrink: 1,
    fontFamily: type.serif,
    fontSize: 16,
    color: palette.ink,
  },
  subtitle: {
    marginTop: 4,
    fontSize: 12,
    lineHeight: 17,
    color: palette.muted,
  },
  arrow: {
    position: 'absolute',
    right: 2,
    bottom: 2,
    color: palette.faint,
    fontSize: 14,
  },
})

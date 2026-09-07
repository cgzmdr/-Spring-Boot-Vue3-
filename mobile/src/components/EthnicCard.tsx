import { Pressable, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import { palette, radius, spacing, type } from '../theme'
import CoverImage from './CoverImage'
import type { EthnicListItem } from '../api/types'

interface EthnicCardProps {
  item: EthnicListItem
  /** 强制两列布局（首页 56 民族墙） */
  twoCol?: boolean
}

/** 民族卡片：上盖图 + 名称 + 一句话简介 */
export default function EthnicCard({ item, twoCol = false }: EthnicCardProps) {
  const navigation = useNavigation()
  const go = () => {
    navigation.getParent()?.navigate('EthnicDetail', { id: item.id } as never)
  }

  return (
    <Pressable
      style={({ pressed }) => [
        styles.card,
        { width: twoCol ? '48.5%' : '31%' },
        pressed && styles.pressed,
      ]}
      onPress={go}
    >
      <CoverImage
        uri={item.coverImage}
        fallbackColor={item.themeColor || palette.coverFallback}
        fallbackLabel={item.name}
        style={twoCol ? styles.imageTwo : styles.image}
      />
      <View style={styles.body}>
        <Text style={styles.name} numberOfLines={1}>{item.name}</Text>
        {item.summary ? (
          <Text style={styles.summary} numberOfLines={twoCol ? 2 : 1}>{item.summary}</Text>
        ) : null}
      </View>
    </Pressable>
  )
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: palette.white,
    borderRadius: radius.md,
    overflow: 'hidden',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    marginBottom: spacing.sm,
  },
  pressed: {
    opacity: 0.85,
  },
  image: {
    height: 110,
    borderRadius: 0,
  },
  imageTwo: {
    height: 150,
    borderRadius: 0,
  },
  body: {
    padding: spacing.sm,
  },
  name: {
    fontFamily: type.serif,
    fontSize: 16,
    color: palette.ink,
  },
  summary: {
    marginTop: 2,
    fontSize: 12,
    lineHeight: 17,
    color: palette.muted,
  },
})

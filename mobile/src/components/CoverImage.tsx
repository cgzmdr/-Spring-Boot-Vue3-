import { useState } from 'react'
import { Image, StyleSheet, Text, View, type StyleProp, type ViewStyle, type ImageStyle } from 'react-native'
import { palette, radius, type } from '../theme'
import { resolveAssetUrl } from '../config'

interface CoverImageProps {
  uri?: string | null
  /** 兜底色（默认主题色或通用兜底色） */
  fallbackColor?: string
  fallbackLabel?: string
  style?: StyleProp<ImageStyle>
  wrapStyle?: StyleProp<ViewStyle>
}

/**
 * 封面图：图片加载失败 / 无图时回退为主题色块 + 中央大字，
 * 保证任何数据下都有可读的视觉呈现。
 */
export default function CoverImage({ uri, fallbackColor, fallbackLabel, style, wrapStyle }: CoverImageProps) {
  const [failed, setFailed] = useState(false)
  // 相对路径（后端业务图片）拼接后端地址；绝对 URL 原样使用
  const src = resolveAssetUrl(uri)
  const showImage = !!src && !failed

  if (!showImage) {
    return (
      <View
        style={[
          styles.fallback,
          { backgroundColor: fallbackColor || palette.coverFallback },
          wrapStyle,
          style as StyleProp<ViewStyle>,
        ]}
      >
        <Text style={styles.fallbackLabel}>{fallbackLabel?.[0] ?? '民'}</Text>
      </View>
    )
  }

  return (
    <Image
      source={{ uri: src }}
      style={[styles.image, style]}
      onError={() => setFailed(true)}
      resizeMode="cover"
    />
  )
}

const styles = StyleSheet.create({
  image: {
    width: '100%',
    height: 200,
    borderRadius: radius.md,
  },
  fallback: {
    width: '100%',
    height: 200,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  fallbackLabel: {
    fontFamily: type.serif,
    fontSize: 44,
    color: palette.white,
    opacity: 0.92,
  },
})

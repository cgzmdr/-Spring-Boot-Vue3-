import { useEffect, useState } from 'react'
import { Alert, Pressable, Share, StyleSheet, Text, View } from 'react-native'
import { interactionApi, shareApi } from '../api/modules'
import type { ContentStats, ContentType } from '../api/types'
import { useAuthStore } from '../stores/auth'
import { useLangStore } from '../stores/lang'
import { palette, spacing } from '../theme'

interface InteractionBarProps {
  type: ContentType
  id: string
  /** 登录失效/未登录时由导航层跳登录页 */
  onRequireLogin: () => void
}

/** 底部互动栏：点赞 / 收藏 / 分享 */
export default function InteractionBar({ type, id, onRequireLogin }: InteractionBarProps) {
  const token = useAuthStore((s) => s.token)
  const t = useLangStore((s) => s.t)
  const [stats, setStats] = useState<ContentStats | null>(null)
  const [busy, setBusy] = useState<'like' | 'favorite' | null>(null)

  const loadStats = () => {
    interactionApi.stats(type, id).then(setStats).catch(() => {})
  }

  useEffect(() => {
    loadStats()
  }, [type, id])

  /** 需要登录的互动：未登录先跳登录 */
  const guard = () => {
    if (!token) {
      onRequireLogin()
      return false
    }
    return true
  }

  const toggleLike = async () => {
    if (!guard() || !stats) return
    const next = !stats.liked
    setStats({ ...stats, liked: next, likeCount: stats.likeCount + (next ? 1 : -1) })
    setBusy('like')
    try {
      if (next) await interactionApi.like(type, id)
      else await interactionApi.unlike(type, id)
    } catch {
      setStats({ ...stats, liked: !next, likeCount: stats.likeCount })
    } finally {
      setBusy(null)
    }
  }

  const toggleFavorite = async () => {
    if (!guard() || !stats) return
    const next = !stats.favorited
    setStats({ ...stats, favorited: next, favoriteCount: stats.favoriteCount + (next ? 1 : -1) })
    setBusy('favorite')
    try {
      if (next) await interactionApi.favorite(type, id)
      else await interactionApi.unfavorite(type, id)
    } catch {
      setStats({ ...stats, favorited: !next, favoriteCount: stats.favoriteCount })
    } finally {
      setBusy(null)
    }
  }

  const doShare = async () => {
    try {
      const res = await shareApi.create({ type, refId: id })
      const url = res.url || `https://${type}/${id}`
      await Share.share({ message: url })
    } catch (e) {
      const msg = e instanceof Error ? e.message : ''
      Alert.alert(t('interact.shareFailed'), msg)
    }
  }

  return (
    <View style={styles.bar}>
      <Pressable style={[styles.btn, stats?.liked && styles.btnActive]} onPress={toggleLike}>
        <Text style={[styles.icon, stats?.liked && styles.iconActive]}>♡</Text>
        <Text style={[styles.label, stats?.liked && styles.labelActive]}>
          {t('interact.like')} {stats ? stats.likeCount : ''}
        </Text>
      </Pressable>
      <Pressable style={[styles.btn, stats?.favorited && styles.btnActive]} onPress={toggleFavorite}>
        <Text style={[styles.icon, stats?.favorited && styles.iconActive]}>☆</Text>
        <Text style={[styles.label, stats?.favorited && styles.labelActive]}>
          {t('interact.favorite')} {stats ? stats.favoriteCount : ''}
        </Text>
      </Pressable>
      <Pressable style={styles.btn} onPress={doShare}>
        <Text style={styles.icon}>↗</Text>
        <Text style={styles.label}>{t('interact.share')}</Text>
      </Pressable>
    </View>
  )
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: palette.border,
    paddingVertical: spacing.sm,
  },
  btn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: spacing.sm,
  },
  btnActive: {},
  icon: {
    fontSize: 20,
    color: palette.muted,
  },
  iconActive: {
    color: palette.accent,
  },
  label: {
    fontSize: 13,
    color: palette.muted,
  },
  labelActive: {
    color: palette.accent,
  },
})

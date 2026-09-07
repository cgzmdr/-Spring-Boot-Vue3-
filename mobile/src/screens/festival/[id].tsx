import { useEffect, useState } from 'react'
import { ScrollView, StyleSheet, Text, View } from 'react-native'
import { useRoute, useNavigation, type RouteProp } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import CoverImage from '../../components/CoverImage'
import SectionRule from '../../components/SectionRule'
import EmptyView from '../../components/EmptyView'
import InteractionBar from '../../components/InteractionBar'
import { festivalApi } from '../../api/modules'
import type { FestivalDetail } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, radius, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Route = RouteProp<RootStackParamList, 'FestivalDetail'>
type Nav = NativeStackNavigationProp<RootStackParamList>

export default function FestivalDetailScreen() {
  const route = useRoute<Route>()
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [detail, setDetail] = useState<FestivalDetail | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    festivalApi.detail(route.params.id).then(setDetail).catch(() => {}).finally(() => setLoading(false))
  }, [route.params.id])

  if (loading) {
    return (
      <Screen>
        <EmptyView loading />
      </Screen>
    )
  }
  if (!detail) {
    return (
      <Screen>
        <EmptyView text={t('common.empty')} />
      </Screen>
    )
  }

  const themeColor = detail.themeColor || palette.coverFallback
  const customs = parseList(detail.customs)

  return (
    <Screen style={styles.screen}>
      <ScrollView showsVerticalScrollIndicator={false}>
        <View style={[styles.hero, { backgroundColor: themeColor }]}>
          <Text style={styles.heroKicker}>{detail.ethnicName || t('festival.title')}</Text>
          <Text style={styles.heroName}>{detail.name}</Text>
          <Text style={styles.heroNameEn}>{detail.nameEn}</Text>
          {detail.lunarDate || detail.solarDate ? (
            <Text style={styles.heroDate}>{detail.lunarDate || detail.solarDate}</Text>
          ) : null}
        </View>

        {detail.coverImage ? <CoverImage uri={detail.coverImage} style={styles.cover} /> : null}

        <View style={styles.body}>
          <SectionRule index="01" title={t('festival.origin')} />
          <Text style={styles.para}>{detail.origin || detail.description || '·'}</Text>

          {customs.length > 0 ? (
            <>
              <SectionRule index="02" title={t('festival.customs')} />
              {customs.map((c, i) => (
                <View key={i} style={styles.customRow}>
                  <Text style={styles.dot}>·</Text>
                  <Text style={styles.para}>{c}</Text>
                </View>
              ))}
            </>
          ) : null}
        </View>
      </ScrollView>

      <InteractionBar type="festival" id={detail.id} onRequireLogin={() => navigation.navigate('Login')} />
    </Screen>
  )
}

/** customs 可能是 JSON 字符串（历史数据）或数组 */
function parseList(v?: string[] | string): string[] {
  if (!v) return []
  if (Array.isArray(v)) return v.filter(Boolean)
  try {
    const parsed = JSON.parse(v)
    return Array.isArray(parsed) ? parsed.filter(Boolean) : []
  } catch {
    return [v]
  }
}

const styles = StyleSheet.create({
  screen: {
    backgroundColor: palette.paper,
  },
  hero: {
    padding: spacing.md,
    paddingTop: spacing.lg,
  },
  heroKicker: {
    fontSize: 11,
    letterSpacing: 2,
    color: 'rgba(255,255,255,0.75)',
  },
  heroName: {
    fontFamily: type.serif,
    fontSize: 36,
    color: palette.white,
    marginTop: spacing.xs,
  },
  heroNameEn: {
    fontSize: 15,
    color: 'rgba(255,255,255,0.85)',
    marginTop: 2,
  },
  heroDate: {
    marginTop: spacing.sm,
    fontSize: 13,
    color: 'rgba(255,255,255,0.9)',
  },
  cover: {
    height: 180,
    borderRadius: 0,
  },
  body: {
    padding: spacing.md,
  },
  para: {
    flex: 1,
    fontSize: 15,
    lineHeight: 25,
    color: palette.ink,
    marginBottom: spacing.sm,
  },
  customRow: {
    flexDirection: 'row',
    marginBottom: spacing.xs,
  },
  dot: {
    fontSize: 16,
    color: palette.accent,
    marginRight: spacing.sm,
    marginTop: 1,
  },
})

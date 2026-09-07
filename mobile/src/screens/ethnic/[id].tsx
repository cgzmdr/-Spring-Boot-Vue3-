import { useEffect, useState } from 'react'
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native'
import { useRoute, useNavigation, type RouteProp } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import CoverImage from '../../components/CoverImage'
import Badge from '../../components/Badge'
import EmptyView from '../../components/EmptyView'
import InteractionBar from '../../components/InteractionBar'
import { ethnicApi } from '../../api/modules'
import type { EthnicDetail } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, radius, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Route = RouteProp<RootStackParamList, 'EthnicDetail'>
type Nav = NativeStackNavigationProp<RootStackParamList>

const TABS = ['概况', '风俗', '节日', '艺术', '美食', '聚居地'] as const

export default function EthnicDetailScreen() {
  const route = useRoute<Route>()
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [detail, setDetail] = useState<EthnicDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState(0)

  useEffect(() => {
    ethnicApi.detail(route.params.id).then(setDetail).catch(() => {}).finally(() => setLoading(false))
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

  return (
    <Screen style={styles.screen}>
      <ScrollView showsVerticalScrollIndicator={false}>
        {/* Hero */}
        <View style={[styles.hero, { backgroundColor: themeColor }]}>
          <Text style={styles.heroKicker}>{detail.pinyin?.toUpperCase() || t('ethnic.title')}</Text>
          <Text style={styles.heroName}>{detail.name}</Text>
          <Text style={styles.heroNameEn}>{detail.nameEn || detail.selfName || ''}</Text>
          <Text style={styles.heroSummary}>{detail.summary}</Text>
          <View style={styles.metrics}>
            <Metric value={formatNum(detail.population)} label={t('ethnic.population')} />
            <Metric value={String(detail.festivals?.length ?? 0)} label={t('ethnic.festival')} />
            <Metric value={String(detail.arts?.length ?? 0)} label={t('ethnic.art')} />
            <Metric value={String(detail.locations?.length ?? 0)} label={t('ethnic.home')} />
          </View>
        </View>

        {/* Tab 切换 */}
        <View style={styles.tabs}>
          {TABS.map((name, i) => (
            <Pressable key={name} style={[styles.tab, tab === i && styles.tabActive]} onPress={() => setTab(i)}>
              <Text style={[styles.tabText, tab === i && styles.tabTextActive]}>{name}</Text>
            </Pressable>
          ))}
        </View>

        <View style={styles.body}>
          {tab === 0 && <Overview detail={detail} />}
          {tab === 1 && <CustomList items={detail.customs} />}
          {tab === 2 && <RefList items={detail.festivals} onPress={(id) => id && navigation.navigate('FestivalDetail', { id })} emptyText={t('common.empty')} />}
          {tab === 3 && <RefList items={detail.arts} onPress={(id) => id && navigation.navigate('ArtDetail', { id })} emptyText={t('common.empty')} />}
          {tab === 4 && <FoodList items={detail.foods} />}
          {tab === 5 && <LocationList items={detail.locations} />}
        </View>
      </ScrollView>

      <InteractionBar type="ethnic" id={detail.id} onRequireLogin={() => navigation.navigate('Login')} />
    </Screen>
  )
}

/* ---------- 子区块 ---------- */

function Overview({ detail }: { detail: EthnicDetail }) {
  const pick = useLangStore((s) => s.pick)
  const t = useLangStore((s) => s.t)
  return (
    <View>
      <Text style={styles.para}>{detail.description}</Text>
      <View style={styles.infoCard}>
        <InfoRow label={t('ethnic.selfName')} value={detail.selfName} />
        <InfoRow label={t('ethnic.languages')} value={detail.languages?.join('、')} />
        <InfoRow label={t('ethnic.scripts')} value={detail.scripts?.join('、')} />
        <InfoRow label={t('ethnic.religion')} value={detail.religion?.join('、')} />
        <InfoRow label={t('ethnic.home')} value={detail.region?.join('、')} />
      </View>
    </View>
  )
}

function CustomList({ items }: { items?: EthnicDetail['customs'] }) {
  if (!items?.length) return <EmptyView />
  return (
    <View>
      {items.map((c, i) => (
        <View key={i} style={styles.customItem}>
          <Text style={styles.customTitle}>{c.title}</Text>
          <Text style={styles.para}>{c.description}</Text>
          {c.image ? <CoverImage uri={c.image} style={styles.inlineImage} /> : null}
        </View>
      ))}
    </View>
  )
}

function RefList({ items, onPress, emptyText }: { items?: { id?: string; name?: string; month?: string; description?: string }[]; onPress: (id?: string) => void; emptyText: string }) {
  if (!items?.length) return <EmptyView text={emptyText} />
  return (
    <View>
      {items.map((it, i) => (
        <Pressable key={i} style={styles.refItem} onPress={() => onPress(it.id)}>
          <View style={styles.refHead}>
            <Text style={styles.refName}>{it.name}</Text>
            {it.month ? <Badge text={it.month} /> : null}
          </View>
          {it.description ? <Text style={styles.para} numberOfLines={2}>{it.description}</Text> : null}
        </Pressable>
      ))}
    </View>
  )
}

function FoodList({ items }: { items?: EthnicDetail['foods'] }) {
  if (!items?.length) return <EmptyView />
  return (
    <View>
      {items.map((f, i) => (
        <View key={i} style={styles.refItem}>
          <Text style={styles.refName}>{f.name}</Text>
          <Text style={styles.para}>{f.description}</Text>
        </View>
      ))}
    </View>
  )
}

function LocationList({ items }: { items?: EthnicDetail['locations'] }) {
  if (!items?.length) return <EmptyView />
  return (
    <View>
      {items.map((l, i) => (
        <View key={i} style={styles.refItem}>
          <Text style={styles.refName}>{l.province}</Text>
          {l.cities?.length ? <Text style={styles.para}>{l.cities.join('、')}</Text> : null}
          {l.description ? <Text style={styles.para}>{l.description}</Text> : null}
        </View>
      ))}
    </View>
  )
}

function Metric({ value, label }: { value: string; label: string }) {
  return (
    <View style={styles.metric}>
      <Text style={styles.metricValue}>{value}</Text>
      <Text style={styles.metricLabel}>{label}</Text>
    </View>
  )
}

function InfoRow({ label, value }: { label: string; value?: string }) {
  if (!value) return null
  return (
    <View style={styles.infoRow}>
      <Text style={styles.infoLabel}>{label}</Text>
      <Text style={styles.infoValue}>{value}</Text>
    </View>
  )
}

function formatNum(n?: number): string {
  if (n == null) return '·'
  if (n >= 10000) return `${(n / 10000).toFixed(n % 10000 === 0 ? 0 : 1)}万`
  return String(n)
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
    fontSize: 40,
    color: palette.white,
    marginTop: spacing.xs,
  },
  heroNameEn: {
    fontSize: 15,
    color: 'rgba(255,255,255,0.85)',
    marginTop: 2,
  },
  heroSummary: {
    marginTop: spacing.md,
    fontSize: 14,
    lineHeight: 21,
    color: 'rgba(255,255,255,0.92)',
  },
  metrics: {
    flexDirection: 'row',
    marginTop: spacing.lg,
    gap: spacing.sm,
  },
  metric: {
    flex: 1,
    backgroundColor: 'rgba(255,255,255,0.16)',
    borderRadius: radius.sm,
    paddingVertical: spacing.sm,
    alignItems: 'center',
  },
  metricValue: {
    fontFamily: type.serif,
    fontSize: 18,
    color: palette.white,
  },
  metricLabel: {
    fontSize: 10,
    color: 'rgba(255,255,255,0.8)',
    marginTop: 2,
  },
  tabs: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: palette.border,
    backgroundColor: palette.paper,
    paddingHorizontal: spacing.xs,
  },
  tab: {
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  tabActive: {
    borderBottomWidth: 2,
    borderBottomColor: palette.accent,
  },
  tabText: {
    fontSize: 14,
    color: palette.muted,
  },
  tabTextActive: {
    color: palette.accent,
    fontWeight: '600',
  },
  body: {
    padding: spacing.md,
  },
  para: {
    fontSize: 15,
    lineHeight: 25,
    color: palette.ink,
    marginBottom: spacing.sm,
  },
  infoCard: {
    marginTop: spacing.sm,
    backgroundColor: palette.white,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    borderRadius: radius.md,
    padding: spacing.md,
  },
  infoRow: {
    flexDirection: 'row',
    paddingVertical: spacing.xs,
  },
  infoLabel: {
    width: 76,
    fontSize: 13,
    color: palette.faint,
  },
  infoValue: {
    flex: 1,
    fontSize: 13,
    color: palette.ink,
    lineHeight: 19,
  },
  customItem: {
    marginBottom: spacing.lg,
  },
  customTitle: {
    fontFamily: type.serif,
    fontSize: 19,
    color: palette.ink,
    marginBottom: spacing.xs,
  },
  inlineImage: {
    height: 160,
    marginTop: spacing.xs,
  },
  refItem: {
    backgroundColor: palette.white,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    borderRadius: radius.md,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  refHead: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing.xs,
  },
  refName: {
    fontFamily: type.serif,
    fontSize: 17,
    color: palette.ink,
  },
})

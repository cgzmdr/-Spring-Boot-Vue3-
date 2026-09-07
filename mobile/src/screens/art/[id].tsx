import { useEffect, useState } from 'react'
import { ScrollView, StyleSheet, Text, View } from 'react-native'
import { useRoute, useNavigation, type RouteProp } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import CoverImage from '../../components/CoverImage'
import SectionRule from '../../components/SectionRule'
import Badge from '../../components/Badge'
import EmptyView from '../../components/EmptyView'
import InteractionBar from '../../components/InteractionBar'
import { artApi } from '../../api/modules'
import type { ArtDetail } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, radius, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Route = RouteProp<RootStackParamList, 'ArtDetail'>
type Nav = NativeStackNavigationProp<RootStackParamList>

export default function ArtDetailScreen() {
  const route = useRoute<Route>()
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [detail, setDetail] = useState<ArtDetail | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    artApi.detail(route.params.id).then(setDetail).catch(() => {}).finally(() => setLoading(false))
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
        <View style={[styles.hero, { backgroundColor: themeColor }]}>
          <Text style={styles.heroKicker}>{detail.ethnicName || detail.category || t('art.title')}</Text>
          <Text style={styles.heroName}>{detail.name}</Text>
          <Text style={styles.heroNameEn}>{detail.nameEn}</Text>
          <View style={styles.heroTags}>
            {detail.intangible ? <Badge text="非遗" variant="outline" /> : null}
            {detail.category ? <Badge text={detail.category} variant="outline" /> : null}
          </View>
        </View>

        {detail.coverImage ? <CoverImage uri={detail.coverImage} style={styles.cover} /> : null}

        <View style={styles.body}>
          <SectionRule index="01" title={t('art.intro')} />
          <Text style={styles.para}>{detail.description || detail.intro || '·'}</Text>

          <SectionRule index="02" title={t('art.info')} />
          <View style={styles.infoCard}>
            <InfoRow label={t('art.category')} value={detail.category} />
            <InfoRow label={t('art.heritage')} value={detail.heritage || detail.intangible ? (detail.heritage || '是') : undefined} />
            <InfoRow label={t('ethnic.selfName')} value={detail.ethnicName} />
            {detail.crafts?.length ? <InfoRow label="技艺" value={detail.crafts.join('、')} /> : null}
          </View>
        </View>
      </ScrollView>

      <InteractionBar type="art" id={detail.id} onRequireLogin={() => navigation.navigate('Login')} />
    </Screen>
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
  heroTags: {
    flexDirection: 'row',
    gap: spacing.xs,
    marginTop: spacing.sm,
  },
  cover: {
    height: 180,
    borderRadius: 0,
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
})

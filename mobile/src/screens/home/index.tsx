import { useEffect, useState } from 'react'
import { Pressable, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { ScrollScreen } from '../../components/Screen'
import SectionRule from '../../components/SectionRule'
import EthnicCard from '../../components/EthnicCard'
import ListItem from '../../components/ListItem'
import EmptyView from '../../components/EmptyView'
import { ethnicApi, festivalApi, artApi, topicApi, recommendApi } from '../../api/modules'
import type { EthnicListItem, FestivalListItem, ArtListItem, TopicListItem, RecoItem } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { navigateByContentType } from '../../utils/navigate'
import { palette, radius, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

const DIRECTORY: {
  key: string
  labelKey:
    | 'dir.heritage'
    | 'dir.persons'
    | 'dir.autonomous'
    | 'dir.sports'
    | 'dir.costume'
    | 'dir.dwelling'
    | 'dir.languages'
    | 'dir.interests'
    | 'dir.calendar'
  route: keyof RootStackParamList
  topic?: 'costume' | 'dwelling'
}[] = [
  { key: 'heritage', labelKey: 'dir.heritage', route: 'Heritage' },
  { key: 'persons', labelKey: 'dir.persons', route: 'Persons' },
  { key: 'autonomous', labelKey: 'dir.autonomous', route: 'Autonomous' },
  { key: 'sports', labelKey: 'dir.sports', route: 'Sports' },
  { key: 'costume', labelKey: 'dir.costume', route: 'CultureTopic', topic: 'costume' },
  { key: 'dwelling', labelKey: 'dir.dwelling', route: 'CultureTopic', topic: 'dwelling' },
  { key: 'languages', labelKey: 'dir.languages', route: 'Languages' },
  { key: 'interests', labelKey: 'dir.interests', route: 'Interests' },
  { key: 'calendar', labelKey: 'dir.calendar', route: 'FestivalCalendar' },
]

export default function HomeScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [groups, setGroups] = useState<EthnicListItem[]>([])
  const [topics, setTopics] = useState<TopicListItem[]>([])
  const [festivals, setFestivals] = useState<FestivalListItem[]>([])
  const [arts, setArts] = useState<ArtListItem[]>([])
  const [reco, setReco] = useState<RecoItem[]>([])
  const [recoNote, setRecoNote] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    ;(async () => {
      try {
        const [g, t2, f, a] = await Promise.all([
          ethnicApi.list({ page: 0, size: 12 }),
          topicApi.list({ page: 0, size: 3 }),
          festivalApi.list({ page: 0, size: 3 }),
          artApi.list({ page: 0, size: 3 }),
        ])
        setGroups(g.data)
        setTopics(t2.data)
        setFestivals(f.data)
        setArts(a.data)
      } catch {
        // 骨架兜底空态
      } finally {
        setLoading(false)
      }
      try {
        const r = await recommendApi.recommend({ size: 6 })
        setReco(r.list)
        setRecoNote([r.basisLabel, r.dataNote].filter(Boolean).join(' · '))
      } catch {
        // 推荐失败不影响首页
      }
    })()
  }, [])

  const goFestival = (id: string) => navigation.navigate('FestivalDetail', { id })
  const goArt = (id: string) => navigation.navigate('ArtDetail', { id })
  const goTopic = (topic: TopicListItem) =>
    navigation.navigate('TopicDetail', { id: topic.id, title: topic.title })

  const openDir = (item: (typeof DIRECTORY)[number]) => {
    switch (item.route) {
      case 'CultureTopic':
        if (item.topic) navigation.navigate('CultureTopic', { topic: item.topic })
        break
      case 'Heritage':
        navigation.navigate('Heritage')
        break
      case 'Persons':
        navigation.navigate('Persons')
        break
      case 'Autonomous':
        navigation.navigate('Autonomous')
        break
      case 'Sports':
        navigation.navigate('Sports')
        break
      case 'Languages':
        navigation.navigate('Languages')
        break
      case 'Interests':
        navigation.navigate('Interests')
        break
      case 'FestivalCalendar':
        navigation.navigate('FestivalCalendar')
        break
      default:
        break
    }
  }

  return (
    <ScrollScreen>
      {/* Hero */}
      <View style={styles.hero}>
        <View style={styles.heroTop}>
          <Text style={styles.kicker}>THE 56 GROUPS · 中华民族</Text>
          <Pressable style={styles.searchBtn} onPress={() => navigation.navigate('Search')}>
            <Text style={styles.searchBtnText}>{t('common.search')}</Text>
          </Pressable>
        </View>
        <Text style={styles.heroTitle}>{t('home.heroTitle')}</Text>
        <Text style={styles.heroDek}>{t('home.heroDek')}</Text>
        <View style={styles.stats}>
          <StatBox value="56" label={t('home.statGroups')} />
          <StatBox value={String(festivals.length || '·')} label={t('home.statFestivals')} />
          <StatBox value={String(arts.length || '·')} label={t('home.statArts')} />
        </View>
      </View>

      {/* 文化目录 */}
      <View style={styles.section}>
        <SectionRule index="00" title={t('home.sectionDir')} />
        <View style={styles.dirGrid}>
          {DIRECTORY.map((item) => (
            <Pressable key={item.key} style={styles.dirItem} onPress={() => openDir(item)}>
              <Text style={styles.dirLabel}>{t(item.labelKey)}</Text>
            </Pressable>
          ))}
        </View>
      </View>

      {/* 为你推荐 */}
      {reco.length ? (
        <View style={styles.section}>
          <SectionRule index="0R" title={t('home.sectionReco')} />
          {recoNote ? <Text style={styles.recoNote}>{recoNote}</Text> : null}
          {reco.map((item) => (
            <ListItem
              key={`${item.docType}:${item.docId}`}
              title={item.title}
              subtitle={[item.ethnicName, item.reason].filter(Boolean).join(' · ')}
              image={item.coverImage}
              fallbackColor={item.themeColor || undefined}
              fallbackLabel={item.title}
              onPress={() => navigateByContentType(navigation, item.docType, item.docId, item.url)}
            />
          ))}
          <TextLink text={t('common.viewAll')} onPress={() => navigation.navigate('Interests')} />
        </View>
      ) : null}

      {/* 01 五十六个民族 */}
      <View style={styles.section}>
        <SectionRule index="01" title={t('home.sectionEthnic')} />
        {loading ? (
          <EmptyView loading />
        ) : (
          <View style={styles.grid}>
            {groups.slice(0, 6).map((g) => (
              <EthnicCard key={g.id} item={g} twoCol />
            ))}
          </View>
        )}
        <TextLink text={t('common.viewAll')} onPress={() => navigation.navigate('Tabs', { screen: 'Ethnic' } as never)} />
      </View>

      {/* 02 精选专题 */}
      <View style={styles.section}>
        <SectionRule index="02" title={t('home.sectionTopic')} />
        {loading ? (
          <EmptyView loading />
        ) : (
          topics.map((topic) => (
            <Pressable key={topic.id} style={styles.feature} onPress={() => goTopic(topic)}>
              <Text style={styles.featureTitle}>{topic.title}</Text>
              <Text style={styles.featureSub} numberOfLines={2}>{topic.subtitle || topic.description}</Text>
              <Text style={styles.featureMore}>{t('common.more')} →</Text>
            </Pressable>
          ))
        )}
      </View>

      {/* 03 文化之窗 */}
      <View style={styles.section}>
        <SectionRule index="03" title={t('home.sectionCulture')} />
        <Text style={styles.subHead}>{t('home.cultureFestival')}</Text>
        {loading ? (
          <EmptyView loading />
        ) : (
          festivals.map((f) => (
            <ListItem
              key={f.id}
              title={pick(f.name, f.nameEn || f.name)}
              subtitle={f.description}
              image={f.coverImage}
              fallbackLabel={f.name}
              onPress={() => goFestival(f.id)}
            />
          ))
        )}
        <Text style={[styles.subHead, styles.subHeadGap]}>{t('home.cultureArt')}</Text>
        {loading ? (
          <EmptyView loading />
        ) : (
          arts.map((a) => (
            <ListItem
              key={a.id}
              title={pick(a.name, a.nameEn || a.name)}
              subtitle={a.description}
              image={a.coverImage}
              fallbackLabel={a.name}
              onPress={() => goArt(a.id)}
            />
          ))
        )}
      </View>
    </ScrollScreen>
  )
}

function StatBox({ value, label }: { value: string; label: string }) {
  return (
    <View style={styles.statBox}>
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  )
}

function TextLink({ text, onPress }: { text: string; onPress: () => void }) {
  return (
    <Pressable style={styles.link} onPress={onPress}>
      <Text style={styles.linkText}>{text} →</Text>
    </Pressable>
  )
}

const styles = StyleSheet.create({
  hero: {
    backgroundColor: palette.paper,
    paddingHorizontal: spacing.md,
    paddingTop: spacing.xl,
    paddingBottom: spacing.lg,
    borderBottomWidth: 1,
    borderBottomColor: palette.border,
  },
  heroTop: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing.sm,
  },
  kicker: {
    fontSize: 11,
    letterSpacing: 2,
    color: palette.accent,
  },
  searchBtn: {
    borderWidth: 1,
    borderColor: palette.border,
    borderRadius: 99,
    paddingHorizontal: spacing.md,
    paddingVertical: 6,
    backgroundColor: palette.white,
  },
  searchBtnText: {
    fontSize: 12,
    color: palette.ink,
  },
  heroTitle: {
    fontFamily: type.serif,
    fontSize: 40,
    lineHeight: 50,
    color: palette.ink,
  },
  heroDek: {
    marginTop: spacing.md,
    fontSize: 14,
    lineHeight: 22,
    color: palette.muted,
  },
  stats: {
    flexDirection: 'row',
    marginTop: spacing.lg,
    gap: spacing.sm,
  },
  statBox: {
    flex: 1,
    backgroundColor: palette.white,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    borderRadius: radius.sm,
    paddingVertical: spacing.sm,
    alignItems: 'center',
  },
  statValue: {
    fontFamily: type.serif,
    fontSize: 22,
    color: palette.accent,
  },
  statLabel: {
    fontSize: 11,
    color: palette.faint,
    marginTop: 2,
  },
  section: {
    marginTop: spacing.lg,
    paddingBottom: spacing.sm,
  },
  dirGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: spacing.md,
    gap: spacing.sm,
  },
  dirItem: {
    width: '31%',
    flexGrow: 1,
    minWidth: '30%',
    backgroundColor: palette.white,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    borderRadius: radius.md,
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.sm,
    alignItems: 'center',
  },
  dirLabel: {
    fontSize: 13,
    color: palette.ink,
    textAlign: 'center',
  },
  recoNote: {
    paddingHorizontal: spacing.md,
    marginBottom: spacing.sm,
    fontSize: 12,
    color: palette.faint,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.md,
  },
  link: {
    marginTop: spacing.sm,
    paddingHorizontal: spacing.md,
  },
  linkText: {
    fontSize: 13,
    color: palette.accent,
  },
  feature: {
    marginHorizontal: spacing.md,
    marginBottom: spacing.sm,
    backgroundColor: palette.ink,
    borderRadius: radius.md,
    padding: spacing.lg,
  },
  featureTitle: {
    fontFamily: type.serif,
    fontSize: 20,
    color: palette.paper,
  },
  featureSub: {
    marginTop: spacing.sm,
    fontSize: 13,
    lineHeight: 19,
    color: '#C9C4BA',
  },
  featureMore: {
    marginTop: spacing.md,
    fontSize: 12,
    color: palette.gold,
  },
  subHead: {
    fontSize: 13,
    fontWeight: '600',
    color: palette.ink,
    paddingHorizontal: spacing.md,
    marginBottom: spacing.sm,
  },
  subHeadGap: {
    marginTop: spacing.sm,
  },
})

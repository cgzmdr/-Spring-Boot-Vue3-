import { useEffect, useState } from 'react'
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native'
import { useRoute, useNavigation, type RouteProp } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import CoverImage from '../../components/CoverImage'
import Badge from '../../components/Badge'
import EmptyView from '../../components/EmptyView'
import InteractionBar from '../../components/InteractionBar'
import { topicApi } from '../../api/modules'
import type { TopicDetail, TopicEntry } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { navigateByContentType } from '../../utils/navigate'
import { palette, radius, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Route = RouteProp<RootStackParamList, 'TopicDetail'>
type Nav = NativeStackNavigationProp<RootStackParamList>

export default function TopicDetailScreen() {
  const route = useRoute<Route>()
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)

  const [detail, setDetail] = useState<TopicDetail | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    topicApi.detail(route.params.id).then(setDetail).catch(() => {}).finally(() => setLoading(false))
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

  const themeColor = detail.themeColor || palette.ink

  return (
    <Screen style={styles.screen}>
      <ScrollView showsVerticalScrollIndicator={false}>
        <View style={[styles.hero, { backgroundColor: themeColor }]}>
          <Text style={styles.heroTitle}>{detail.title}</Text>
          {detail.subtitle ? <Text style={styles.heroSub}>{detail.subtitle}</Text> : null}
        </View>
        {detail.coverImage ? <CoverImage uri={detail.coverImage} style={styles.cover} /> : null}
        <View style={styles.body}>
          {detail.description ? <Text style={styles.para}>{detail.description}</Text> : null}
          {detail.entries?.length ? (
            <View style={styles.entries}>
              {detail.entries.map((entry, i) => (
                <EntryRow key={i} entry={entry} onPress={() => openEntry(entry, navigation)} />
              ))}
            </View>
          ) : null}
        </View>
      </ScrollView>
      <InteractionBar type="topic" id={detail.id} onRequireLogin={() => navigation.navigate('Login')} />
    </Screen>
  )
}

function EntryRow({ entry, onPress }: { entry: TopicEntry; onPress: () => void }) {
  return (
    <Pressable style={styles.entry} onPress={onPress}>
      <CoverImage uri={entry.image} fallbackLabel={entry.title || '·'} style={styles.entryImage} />
      <View style={styles.entryBody}>
        <View style={styles.entryHead}>
          <Text style={styles.entryTitle} numberOfLines={1}>{entry.title}</Text>
          {entry.type ? <Badge text={entry.type} variant="outline" /> : null}
        </View>
        {entry.summary ? <Text style={styles.entrySummary} numberOfLines={2}>{entry.summary}</Text> : null}
      </View>
    </Pressable>
  )
}

function openEntry(entry: TopicEntry, navigation: Nav) {
  navigateByContentType(navigation, entry.type ?? '', entry.refId)
}

const styles = StyleSheet.create({
  screen: {
    backgroundColor: palette.paper,
  },
  hero: {
    padding: spacing.md,
    paddingTop: spacing.lg,
  },
  heroTitle: {
    fontFamily: type.serif,
    fontSize: 30,
    color: palette.white,
  },
  heroSub: {
    marginTop: spacing.sm,
    fontSize: 14,
    lineHeight: 21,
    color: 'rgba(255,255,255,0.85)',
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
    marginBottom: spacing.md,
  },
  entries: {
    gap: spacing.sm,
  },
  entry: {
    flexDirection: 'row',
    backgroundColor: palette.white,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    borderRadius: radius.md,
    padding: spacing.sm,
  },
  entryImage: {
    width: 72,
    height: 72,
    borderRadius: radius.sm,
  },
  entryBody: {
    flex: 1,
    marginLeft: spacing.sm,
    justifyContent: 'center',
  },
  entryHead: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
  },
  entryTitle: {
    flexShrink: 1,
    fontFamily: type.serif,
    fontSize: 16,
    color: palette.ink,
  },
  entrySummary: {
    marginTop: 4,
    fontSize: 12,
    lineHeight: 17,
    color: palette.muted,
  },
})

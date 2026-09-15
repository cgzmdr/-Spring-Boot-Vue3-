import { useCallback, useEffect, useState } from 'react'
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import Chip from '../../components/Chip'
import EmptyView from '../../components/EmptyView'
import { traditionalSportApi } from '../../api/modules'
import type { TraditionalSport, TraditionalSportDirectory } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, radius, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

export default function SportsScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [data, setData] = useState<TraditionalSportDirectory | null>(null)
  const [allSports, setAllSports] = useState<TraditionalSport[]>([])
  const [category, setCategory] = useState('')
  const [expanded, setExpanded] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await traditionalSportApi.list({ category: category || undefined })
      setData(res)
      if (!category) setAllSports(res.sports)
    } catch {
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [category])

  useEffect(() => {
    load()
  }, [load])

  const sports = data?.sports ?? []
  const categories = Array.from(new Set(allSports.map((s) => s.category))).map((code) => {
    const sample = allSports.find((s) => s.category === code)
    return { code, label: sample?.categoryLabel || code }
  })

  const renderSport = ({ item }: { item: TraditionalSport }) => {
    const open = expanded === item.name
    return (
      <Pressable
        style={styles.card}
        onPress={() => setExpanded(open ? null : item.name)}
      >
        <View style={styles.cardHead}>
          <Text style={styles.cardTitle}>{item.name}</Text>
          <Text style={styles.cardTag}>{item.categoryLabel}</Text>
        </View>
        {item.description ? (
          <Text style={styles.cardDesc} numberOfLines={open ? undefined : 2}>
            {item.description}
          </Text>
        ) : null}
        {open ? (
          <View style={styles.detail}>
            {item.venue ? <Text style={styles.meta}>{t('sports.venue')}: {item.venue}</Text> : null}
            {item.equipment ? <Text style={styles.meta}>{t('sports.equipment')}: {item.equipment}</Text> : null}
            {item.subEvents?.length ? (
              <Text style={styles.meta}>{t('sports.subEvents')}: {item.subEvents.join('、')}</Text>
            ) : null}
            {item.matchedEthnics?.length ? (
              <View style={styles.links}>
                {item.matchedEthnics.map((e) => (
                  <Pressable key={e.id} onPress={() => navigation.navigate('EthnicDetail', { id: e.id })}>
                    <Text style={styles.link}>{e.name} →</Text>
                  </Pressable>
                ))}
              </View>
            ) : null}
          </View>
        ) : null}
      </Pressable>
    )
  }

  return (
    <Screen>
      <FlatList
        data={sports}
        keyExtractor={(item) => item.name}
        renderItem={renderSport}
        contentContainerStyle={styles.content}
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.title}>{t('sports.title')}</Text>
            {data ? (
              <Text style={styles.dek}>
                {pick(
                  `${data.summary.total} 项 · ${data.summary.ethnicCount} 个相关民族`,
                  `${data.summary.total} sports · ${data.summary.ethnicCount} related groups`
                )}
              </Text>
            ) : null}
            <View style={styles.chips}>
              <Chip label={t('common.all')} active={!category} onPress={() => setCategory('')} />
              {categories.map((c) => (
                <Chip
                  key={c.code}
                  label={c.label}
                  active={category === c.code}
                  onPress={() => setCategory(category === c.code ? '' : c.code)}
                />
              ))}
            </View>
          </View>
        }
        ListEmptyComponent={loading ? <EmptyView loading /> : <EmptyView />}
      />
    </Screen>
  )
}

const styles = StyleSheet.create({
  header: { paddingBottom: spacing.sm },
  title: { fontFamily: type.serif, fontSize: 28, color: palette.ink, marginBottom: spacing.xs },
  dek: { fontSize: 13, color: palette.muted, marginBottom: spacing.md },
  chips: { flexDirection: 'row', flexWrap: 'wrap' },
  content: { paddingHorizontal: spacing.md, paddingBottom: 40 },
  card: {
    backgroundColor: palette.white,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    borderRadius: radius.md,
    padding: spacing.md,
    marginBottom: spacing.sm,
  },
  cardHead: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  cardTitle: { flex: 1, fontFamily: type.serif, fontSize: 17, color: palette.ink },
  cardTag: { fontSize: 11, color: palette.accent },
  cardDesc: { marginTop: spacing.xs, fontSize: 13, lineHeight: 19, color: palette.muted },
  detail: { marginTop: spacing.sm, gap: 4 },
  meta: { fontSize: 12, color: palette.faint, lineHeight: 18 },
  links: { flexDirection: 'row', flexWrap: 'wrap', gap: spacing.sm, marginTop: spacing.xs },
  link: { fontSize: 13, color: palette.accent },
})

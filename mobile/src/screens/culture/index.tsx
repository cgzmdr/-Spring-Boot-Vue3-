import { useEffect, useMemo, useState } from 'react'
import { FlatList, StyleSheet, Text, View } from 'react-native'
import { useNavigation, useRoute, type RouteProp } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import ListItem from '../../components/ListItem'
import Chip from '../../components/Chip'
import EmptyView from '../../components/EmptyView'
import { cultureTopicApi } from '../../api/modules'
import type { CultureTopic, CultureTopicEntry } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>
type R = RouteProp<RootStackParamList, 'CultureTopic'>

export default function CultureTopicScreen() {
  const navigation = useNavigation<Nav>()
  const route = useRoute<R>()
  const topic = route.params.topic
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [data, setData] = useState<CultureTopic | null>(null)
  const [category, setCategory] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    navigation.setOptions({
      title: topic === 'costume' ? t('culture.costume') : t('culture.dwelling'),
    })
  }, [navigation, topic, t])

  useEffect(() => {
    ;(async () => {
      setLoading(true)
      setCategory('')
      try {
        setData(await cultureTopicApi.get(topic))
      } catch {
        setData(null)
      } finally {
        setLoading(false)
      }
    })()
  }, [topic])

  const entries: CultureTopicEntry[] = useMemo(() => {
    if (!data) return []
    if (!category) return data.entries
    return data.entries
      .map((e) => ({
        ...e,
        items: e.items.filter((it) => (it.category || '') === category),
      }))
      .filter((e) => e.items.length > 0)
  }, [data, category])

  return (
    <Screen>
      <FlatList
        data={entries}
        keyExtractor={(item) => item.ethnicGroupId}
        renderItem={({ item }) => (
          <ListItem
            title={item.ethnicGroupName}
            subtitle={item.items.map((it) => it.title).slice(0, 3).join(' · ')}
            tag={String(item.items.length)}
            image={item.coverImage}
            fallbackColor={item.themeColor}
            fallbackLabel={item.ethnicGroupName}
            onPress={() => navigation.navigate('EthnicDetail', { id: item.ethnicGroupId })}
          />
        )}
        contentContainerStyle={styles.content}
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.title}>
              {data ? pick(data.title, data.titleEn || data.title) : t('culture.title')}
            </Text>
            {data ? <Text style={styles.dek}>{data.intro}</Text> : null}
            {data ? (
              <Text style={styles.meta}>
                {pick(
                  `${data.summary.groupCount} 民族 · ${data.summary.entryCount} 条 · 非遗 ${data.summary.heritageCount}`,
                  `${data.summary.groupCount} groups · ${data.summary.entryCount} entries · ${data.summary.heritageCount} heritage`
                )}
              </Text>
            ) : null}
            <View style={styles.chips}>
              <Chip label={t('common.all')} active={!category} onPress={() => setCategory('')} />
              {(data?.categories ?? []).map((c) => (
                <Chip
                  key={c.code}
                  label={`${c.label} (${c.count})`}
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
  dek: { fontSize: 13, lineHeight: 20, color: palette.muted, marginBottom: spacing.sm },
  meta: { fontSize: 12, color: palette.faint, marginBottom: spacing.md },
  chips: { flexDirection: 'row', flexWrap: 'wrap' },
  content: { paddingHorizontal: spacing.md, paddingBottom: 40 },
})

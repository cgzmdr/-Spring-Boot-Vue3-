import { useCallback, useEffect, useState } from 'react'
import { FlatList, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import ListItem from '../../components/ListItem'
import Chip from '../../components/Chip'
import EmptyView from '../../components/EmptyView'
import { artApi } from '../../api/modules'
import type { ArtListItem, HeritageStats } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>
const PAGE_SIZE = 20

export default function HeritageScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [stats, setStats] = useState<HeritageStats | null>(null)
  const [level, setLevel] = useState('')
  const [category, setCategory] = useState('')
  const [list, setList] = useState<ArtListItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)

  const load = useCallback(
    async (pageNum: number, replace: boolean) => {
      if (loading) return
      setLoading(true)
      try {
        const res = await artApi.list({
          page: pageNum,
          size: PAGE_SIZE,
          intangibleHeritage: level || undefined,
          category: category || undefined,
        })
        setList((prev) => (replace ? res.data : [...prev, ...res.data]))
        setTotal(res.total)
        setPage(pageNum)
      } catch {
        // 静默
      } finally {
        setLoading(false)
      }
    },
    [loading, level, category]
  )

  useEffect(() => {
    artApi.heritageStats().then(setStats).catch(() => {})
  }, [])

  useEffect(() => {
    setList([])
    setPage(0)
    load(0, true)
  }, [level, category])

  return (
    <Screen>
      <FlatList
        data={list}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <ListItem
            title={pick(item.name, item.nameEn || item.name)}
            subtitle={[item.ethnicGroupName || item.ethnicName, item.inheritors?.slice(0, 2).join('、')]
              .filter(Boolean)
              .join(' · ')}
            tag={item.intangibleHeritage || (item.intangible ? t('art.heritage') : undefined)}
            image={item.coverImage}
            fallbackLabel={item.name}
            onPress={() => navigation.navigate('ArtDetail', { id: item.id })}
          />
        )}
        contentContainerStyle={styles.content}
        onEndReached={() => {
          if (loading || list.length >= total) return
          load(page + 1, false)
        }}
        onEndReachedThreshold={0.3}
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.title}>{t('heritage.title')}</Text>
            {stats ? (
              <Text style={styles.dek}>
                {pick(
                  `${stats.total} 项 · ${stats.withInheritor} 项有传承人 · ${stats.ethnicCount} 个民族`,
                  `${stats.total} items · ${stats.withInheritor} with inheritors · ${stats.ethnicCount} groups`
                )}
              </Text>
            ) : null}
            <Text style={styles.filterLabel}>{t('heritage.level')}</Text>
            <View style={styles.chips}>
              <Chip label={t('common.all')} active={!level} onPress={() => setLevel('')} />
              {(stats?.levels ?? []).map((g) => (
                <Chip
                  key={g.code}
                  label={`${g.label} (${g.count})`}
                  active={level === g.code}
                  onPress={() => setLevel(level === g.code ? '' : g.code)}
                />
              ))}
            </View>
            <Text style={styles.filterLabel}>{t('heritage.category')}</Text>
            <View style={styles.chips}>
              <Chip label={t('common.all')} active={!category} onPress={() => setCategory('')} />
              {(stats?.categories ?? []).slice(0, 12).map((g) => (
                <Chip
                  key={g.code}
                  label={`${g.label} (${g.count})`}
                  active={category === g.code}
                  onPress={() => setCategory(category === g.code ? '' : g.code)}
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
  filterLabel: { fontSize: 12, color: palette.faint, marginBottom: 6, marginTop: spacing.sm },
  chips: { flexDirection: 'row', flexWrap: 'wrap' },
  content: { paddingHorizontal: spacing.md, paddingBottom: 40 },
})

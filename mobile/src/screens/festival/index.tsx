import { useCallback, useEffect, useState } from 'react'
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import ListItem from '../../components/ListItem'
import Chip from '../../components/Chip'
import EmptyView from '../../components/EmptyView'
import { festivalApi } from '../../api/modules'
import type { FestivalListItem } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

const TYPES = [
  { value: '', label: '全部' },
  { value: 'traditional', label: '传统节日' },
  { value: 'religious', label: '宗教节日' },
  { value: 'agricultural', label: '农事节日' },
]
const PAGE_SIZE = 20

export default function FestivalScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [type, setType] = useState('')
  const [list, setList] = useState<FestivalListItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)

  const load = useCallback(
    async (pageNum: number, replace: boolean) => {
      if (loading) return
      setLoading(true)
      try {
        const res = await festivalApi.list({ page: pageNum, size: PAGE_SIZE, type: type || undefined })
        setList((prev) => (replace ? res.data : [...prev, ...res.data]))
        setTotal(res.total)
        setPage(pageNum)
      } catch {
        // 静默
      } finally {
        setLoading(false)
      }
    },
    [loading, type]
  )

  useEffect(() => {
    setList([])
    setPage(0)
    load(0, true)
  }, [type])

  const onEnd = () => {
    if (loading || list.length >= total) return
    load(page + 1, false)
  }

  return (
    <Screen>
      <FlatList
        data={list}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <ListItem
            title={pick(item.name, item.nameEn || item.name)}
            subtitle={item.description}
            tag={item.type === 'religious' ? '宗教' : item.type === 'agricultural' ? '农事' : undefined}
            image={item.coverImage}
            fallbackColor={item.themeColor}
            fallbackLabel={item.name}
            onPress={() => navigation.navigate('FestivalDetail', { id: item.id })}
          />
        )}
        contentContainerStyle={styles.content}
        onEndReached={onEnd}
        onEndReachedThreshold={0.3}
        ListHeaderComponent={
          <View style={styles.header}>
            <View style={styles.titleRow}>
              <Text style={styles.title}>{t('festival.title')}</Text>
              <Pressable onPress={() => navigation.navigate('FestivalCalendar')}>
                <Text style={styles.entry}>{t('festival.calendar')} →</Text>
              </Pressable>
            </View>
            <View style={styles.chips}>
              {TYPES.map((c) => (
                <Chip key={c.value} label={t('common.all') === c.label ? t('common.all') : c.label} active={type === c.value} onPress={() => setType(c.value)} />
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
  header: {
    padding: spacing.md,
  },
  title: {
    fontFamily: type.serif,
    fontSize: 28,
    color: palette.ink,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: spacing.md,
  },
  entry: {
    fontSize: 13,
    color: palette.accent,
  },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: spacing.sm,
  },
  content: {
    paddingHorizontal: spacing.md,
    paddingBottom: 40,
  },
})

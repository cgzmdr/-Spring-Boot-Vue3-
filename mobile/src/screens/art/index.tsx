import { useCallback, useEffect, useState } from 'react'
import { FlatList, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import ListItem from '../../components/ListItem'
import Chip from '../../components/Chip'
import EmptyView from '../../components/EmptyView'
import { artApi } from '../../api/modules'
import type { ArtListItem } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

const CATEGORIES = [
  { value: '', label: '全部' },
  { value: 'music', label: '音乐' },
  { value: 'dance', label: '舞蹈' },
  { value: 'drama', label: '戏剧' },
  { value: 'costume', label: '服饰' },
  { value: 'craft', label: '手工艺' },
  { value: 'architecture', label: '建筑' },
]
const HERITAGES = [
  { value: '', label: '全部' },
  { value: 'world', label: '世界级' },
  { value: 'national', label: '国家级' },
  { value: 'provincial', label: '省级' },
]
const PAGE_SIZE = 20

export default function ArtScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [category, setCategory] = useState('')
  const [heritage, setHeritage] = useState('')
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
          category: category || undefined,
          intangibleHeritage: heritage || undefined,
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
    [loading, category, heritage]
  )

  useEffect(() => {
    setList([])
    setPage(0)
    load(0, true)
  }, [category, heritage])

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
            tag={item.intangible ? '非遗' : undefined}
            image={item.coverImage}
            fallbackColor={item.themeColor}
            fallbackLabel={item.name}
            onPress={() => navigation.navigate('ArtDetail', { id: item.id })}
          />
        )}
        contentContainerStyle={styles.content}
        onEndReached={onEnd}
        onEndReachedThreshold={0.3}
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.title}>{t('art.title')}</Text>
            <FilterGroup label={t('art.category')} options={CATEGORIES} value={category} onChange={setCategory} />
            <FilterGroup label={t('art.heritage')} options={HERITAGES} value={heritage} onChange={setHeritage} />
          </View>
        }
        ListEmptyComponent={loading ? <EmptyView loading /> : <EmptyView />}
      />
    </Screen>
  )
}

function FilterGroup({ label, options, value, onChange }: { label: string; options: { value: string; label: string }[]; value: string; onChange: (v: string) => void }) {
  return (
    <View style={styles.group}>
      <Text style={styles.groupLabel}>{label}</Text>
      <View style={styles.chips}>
        {options.map((c) => (
          <Chip key={c.value || c.label} label={c.label} active={value === c.value} onPress={() => onChange(c.value)} />
        ))}
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  header: {
    padding: spacing.md,
    paddingBottom: spacing.sm,
  },
  title: {
    fontFamily: type.serif,
    fontSize: 28,
    color: palette.ink,
    marginBottom: spacing.md,
  },
  group: {
    marginBottom: spacing.sm,
  },
  groupLabel: {
    fontSize: 12,
    color: palette.faint,
    marginBottom: 6,
  },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  content: {
    paddingHorizontal: spacing.md,
    paddingBottom: 40,
  },
})

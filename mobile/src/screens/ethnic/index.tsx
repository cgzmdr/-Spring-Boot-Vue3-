import { useCallback, useEffect, useState } from 'react'
import { FlatList, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import EthnicCard from '../../components/EthnicCard'
import Chip from '../../components/Chip'
import EmptyView from '../../components/EmptyView'
import { ethnicApi } from '../../api/modules'
import type { EthnicListItem } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

const REGIONS = ['东北', '西北', '西南', '中南', '东南', '内蒙古', '其他']
const FAMILIES = ['汉藏语系', '阿尔泰语系', '南岛语系', '南亚语系', '印欧语系']
const PAGE_SIZE = 20

export default function EthnicScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)

  const [region, setRegion] = useState('')
  const [family, setFamily] = useState('')
  const [sort, setSort] = useState('')
  const [list, setList] = useState<EthnicListItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [refreshing, setRefreshing] = useState(false)

  const load = useCallback(
    async (pageNum: number, replace: boolean) => {
      if (loading) return
      setLoading(true)
      try {
        const sortParam = sort ? `${sort === 'population' ? 'population' : 'pinyin'},${sort === 'population' ? 'desc' : 'asc'}` : 'orderNum,asc'
        const res = await ethnicApi.list({
          page: pageNum,
          size: PAGE_SIZE,
          region: region || undefined,
          languageFamily: family || undefined,
          sort: sortParam,
        })
        setList((prev) => (replace ? res.data : [...prev, ...res.data]))
        setTotal(res.total)
        setPage(pageNum)
      } catch {
        // 列表接口失败静默
      } finally {
        setLoading(false)
        setRefreshing(false)
      }
    },
    [loading, region, family, sort]
  )

  useEffect(() => {
    load(0, true)
  }, [region, family, sort])

  const onRefresh = () => {
    setRefreshing(true)
    load(0, true)
  }

  const onEnd = () => {
    if (loading || list.length >= total) return
    load(page + 1, false)
  }

  const renderItem = ({ item }: { item: EthnicListItem }) => (
    <EthnicCard item={item} />
  )

  return (
    <Screen>
      <FlatList
        data={list}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        numColumns={2}
        columnWrapperStyle={styles.row}
        contentContainerStyle={styles.content}
        onRefresh={onRefresh}
        refreshing={refreshing}
        onEndReached={onEnd}
        onEndReachedThreshold={0.3}
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.title}>{t('ethnic.title')}</Text>
            <FilterGroup label={t('ethnic.region')} options={[t('common.all'), ...REGIONS]} value={region} onChange={(v) => setRegion(v === t('common.all') ? '' : v)} />
            <FilterGroup label={t('ethnic.language')} options={[t('common.all'), ...FAMILIES]} value={family} onChange={(v) => setFamily(v === t('common.all') ? '' : v)} />
            <FilterGroup label={t('ethnic.sort')} options={[t('ethnic.sortOrder'), t('ethnic.sortPopulation'), t('ethnic.sortPinyin')]} value={sort} onChange={(v) => setSort(v === t('ethnic.sortOrder') ? '' : v === t('ethnic.sortPopulation') ? 'population' : 'pinyin')} />
          </View>
        }
        ListEmptyComponent={loading ? <EmptyView loading /> : <EmptyView />}
      />
    </Screen>
  )
}

function FilterGroup({ label, options, value, onChange }: { label: string; options: string[]; value: string; onChange: (v: string) => void }) {
  return (
    <View style={styles.filterGroup}>
      <Text style={styles.filterLabel}>{label}</Text>
      <View style={styles.chips}>
        {options.map((opt) => (
          <Chip key={opt} label={opt} active={value === opt || (value === '' && opt === options[0])} onPress={() => onChange(opt)} />
        ))}
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  header: {
    paddingBottom: spacing.sm,
  },
  title: {
    fontFamily: type.serif,
    fontSize: 28,
    color: palette.ink,
    paddingHorizontal: spacing.md,
    marginVertical: spacing.md,
  },
  filterGroup: {
    marginBottom: spacing.sm,
    paddingHorizontal: spacing.md,
  },
  filterLabel: {
    fontSize: 12,
    color: palette.faint,
    marginBottom: 6,
  },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 0,
  },
  row: {
    paddingHorizontal: spacing.md,
    justifyContent: 'space-between',
    marginBottom: spacing.sm,
  },
  content: {
    paddingBottom: 40,
  },
})

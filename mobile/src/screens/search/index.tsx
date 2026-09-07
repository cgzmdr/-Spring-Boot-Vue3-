import { useEffect, useState } from 'react'
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import SearchBar from '../../components/SearchBar'
import Chip from '../../components/Chip'
import ListItem from '../../components/ListItem'
import EmptyView from '../../components/EmptyView'
import { searchApi } from '../../api/modules'
import type { SearchGroup, SearchResultItem } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { navigateByContentType } from '../../utils/navigate'
import { palette, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

export default function SearchScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)

  const [keyword, setKeyword] = useState('')
  const [hot, setHot] = useState<string[]>([])
  const [result, setResult] = useState<SearchGroup | null>(null)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)

  // 热门词预加载
  useEffect(() => {
    searchApi.hot().then(setHot).catch(() => {})
  }, [])

  const doSearch = async (kw: string) => {
    setKeyword(kw)
    setLoading(true)
    setSearched(true)
    try {
      const res = await searchApi.search(kw)
      setResult(res)
    } catch {
      setResult(null)
    } finally {
      setLoading(false)
    }
  }

  const openItem = (item: SearchResultItem) => {
    navigateByContentType(navigation, item.type, item.id)
  }

  const sections: { key: string; label: string; items: SearchResultItem[] }[] = [
    { key: 'ethnic', label: t('search.groupEthnic'), items: result?.ethnic ?? [] },
    { key: 'festival', label: t('search.groupFestival'), items: result?.festival ?? [] },
    { key: 'art', label: t('search.groupArt'), items: result?.art ?? [] },
    { key: 'topic', label: t('search.groupTopic'), items: result?.topic ?? [] },
  ]

  return (
    <Screen>
      <View style={styles.barWrap}>
        <SearchBar placeholder={t('search.placeholder')} autoFocus onSearch={doSearch} />
      </View>
      <ScrollView showsVerticalScrollIndicator={false}>
        {!searched ? (
          <View style={styles.section}>
            <Text style={styles.sectionLabel}>{t('search.hot')}</Text>
            <View style={styles.hotWrap}>
              {hot.map((h) => (
                <Pressable key={h} onPress={() => doSearch(h)}>
                  <Chip label={h} active={false} onPress={() => doSearch(h)} />
                </Pressable>
              ))}
            </View>
          </View>
        ) : loading ? (
          <EmptyView loading />
        ) : (
          sections.map((sec) =>
            sec.items.length ? (
              <View key={sec.key} style={styles.section}>
                <Text style={styles.sectionLabel}>
                  {sec.label} · {sec.items.length}
                </Text>
                {sec.items.map((item) => (
                  <ListItem
                    key={`${sec.key}-${item.id}`}
                    title={item.title}
                    subtitle={item.description}
                    image={item.coverImage}
                    fallbackLabel={item.title}
                    onPress={() => openItem(item)}
                  />
                ))}
              </View>
            ) : null
          )
        )}
        {searched && !loading && sections.every((s) => !s.items.length) ? (
          <EmptyView text={t('search.noResult')} />
        ) : null}
      </ScrollView>
    </Screen>
  )
}

const styles = StyleSheet.create({
  barWrap: {
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
  },
  section: {
    padding: spacing.md,
  },
  sectionLabel: {
    fontSize: 13,
    fontWeight: '600',
    color: palette.ink,
    marginBottom: spacing.sm,
  },
  hotWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
})

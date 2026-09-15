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
import type { FullTextSearchResult, SearchHit } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { navigateByContentType } from '../../utils/navigate'
import { palette, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

const DOC_TYPE_LABEL: Record<string, [string, string]> = {
  ethnic: ['民族', 'Groups'],
  festival: ['节日', 'Festivals'],
  art: ['艺术', 'Arts'],
  food: ['美食', 'Food'],
  custom: ['风俗', 'Customs'],
  person: ['人物', 'Persons'],
  area: ['自治地方', 'Areas'],
  sport: ['传统体育', 'Sports'],
}

function matchHint(matchBy: string, pick: (zh: string, en: string) => string) {
  switch (matchBy) {
    case 'pinyin':
      return pick('按拼音匹配', 'matched by pinyin')
    case 'abbr':
      return pick('按首字母匹配', 'matched by initials')
    case 'titleEn':
      return pick('按英文名匹配', 'matched by English name')
    case 'title':
      return pick('标题命中', 'in title')
    default:
      return pick('正文命中', 'in content')
  }
}

export default function SearchScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [keyword, setKeyword] = useState('')
  const [typeFilter, setTypeFilter] = useState('all')
  const [hot, setHot] = useState<string[]>([])
  const [result, setResult] = useState<FullTextSearchResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)

  useEffect(() => {
    searchApi.hot().then(setHot).catch(() => {})
  }, [])

  const doSearch = async (kw: string, type = typeFilter) => {
    const q = kw.trim()
    setKeyword(q)
    setTypeFilter(type)
    setLoading(true)
    setSearched(true)
    try {
      const res = await searchApi.full({
        q: q || undefined,
        type: type === 'all' ? undefined : type,
        page: 0,
        size: 30,
      })
      setResult(res)
    } catch {
      setResult(null)
    } finally {
      setLoading(false)
    }
  }

  const openHit = (hit: SearchHit) => {
    navigateByContentType(navigation, hit.docType, hit.docId, hit.url)
  }

  const facets = Object.entries(result?.facets || {})
    .sort((a, b) => b[1] - a[1])
    .map(([key, count]) => ({
      key,
      label: pick(DOC_TYPE_LABEL[key]?.[0] || key, DOC_TYPE_LABEL[key]?.[1] || key),
      count,
    }))

  return (
    <Screen>
      <View style={styles.barWrap}>
        <SearchBar placeholder={t('search.placeholder')} autoFocus onSearch={(kw) => doSearch(kw)} />
      </View>
      <ScrollView showsVerticalScrollIndicator={false}>
        {!searched ? (
          <View style={styles.section}>
            <Text style={styles.sectionLabel}>{t('search.hot')}</Text>
            <View style={styles.hotWrap}>
              {hot.map((h) => (
                <Chip key={h} label={h} active={false} onPress={() => doSearch(h)} />
              ))}
            </View>
          </View>
        ) : loading ? (
          <EmptyView loading />
        ) : (
          <View style={styles.section}>
            {result ? (
              <Text style={styles.meta}>
                {result.total} {t('search.resultCount')} · {result.tookMs} ms
              </Text>
            ) : null}
            <View style={styles.hotWrap}>
              <Chip
                label={t('common.all')}
                active={typeFilter === 'all'}
                onPress={() => doSearch(keyword, 'all')}
              />
              {facets.map((f) => (
                <Chip
                  key={f.key}
                  label={`${f.label} (${f.count})`}
                  active={typeFilter === f.key}
                  onPress={() => doSearch(keyword, f.key)}
                />
              ))}
            </View>
            {(result?.list ?? []).length === 0 ? (
              <EmptyView text={t('search.noResult')} />
            ) : (
              (result?.list ?? []).map((hit) => (
                <Pressable key={`${hit.docType}:${hit.docId}`} onPress={() => openHit(hit)}>
                  <ListItem
                    title={hit.title}
                    subtitle={[
                      pick(DOC_TYPE_LABEL[hit.docType]?.[0] || hit.docType, DOC_TYPE_LABEL[hit.docType]?.[1] || hit.docType),
                      hit.ethnicName,
                      hit.category,
                      matchHint(hit.matchBy, pick),
                    ]
                      .filter(Boolean)
                      .join(' · ')}
                    image={hit.coverImage}
                    fallbackColor={hit.themeColor || undefined}
                    fallbackLabel={hit.title}
                    onPress={() => openHit(hit)}
                  />
                </Pressable>
              ))
            )}
          </View>
        )}
      </ScrollView>
    </Screen>
  )
}

const styles = StyleSheet.create({
  barWrap: {
    paddingTop: spacing.sm,
    paddingBottom: spacing.sm,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: palette.border,
  },
  section: {
    padding: spacing.md,
    paddingBottom: 40,
  },
  sectionLabel: {
    fontFamily: type.serif,
    fontSize: 16,
    color: palette.ink,
    marginBottom: spacing.sm,
  },
  hotWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: spacing.md,
  },
  meta: {
    fontSize: 12,
    color: palette.faint,
    marginBottom: spacing.sm,
  },
})

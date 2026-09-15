import { useEffect, useMemo, useState } from 'react'
import { FlatList, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import ListItem from '../../components/ListItem'
import Chip from '../../components/Chip'
import EmptyView from '../../components/EmptyView'
import { ethnicApi } from '../../api/modules'
import type { LanguageAtlas, LanguageGroupRef } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>
type Mode = 'family' | 'script'

export default function LanguagesScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [data, setData] = useState<LanguageAtlas | null>(null)
  const [mode, setMode] = useState<Mode>('family')
  const [filter, setFilter] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    ;(async () => {
      setLoading(true)
      try {
        setData(await ethnicApi.languageAtlas())
      } catch {
        setData(null)
      } finally {
        setLoading(false)
      }
    })()
  }, [])

  const groups: { key: string; label: string; items: LanguageGroupRef[] }[] = useMemo(() => {
    if (!data) return []
    if (mode === 'family') {
      return data.families
        .filter((f) => !filter || f.family === filter || f.name === filter)
        .map((f) => ({ key: f.name, label: f.name, items: f.groups }))
    }
    return data.scripts
      .filter((s) => !filter || s.name === filter)
      .map((s) => ({
        key: s.name,
        label: `${s.name}${s.nativeScript ? '' : pick('（借用）', ' (borrowed)')}`,
        items: s.groups,
      }))
  }, [data, mode, filter, pick])

  const flat = groups.flatMap((g) =>
    g.items.map((item) => ({ ...item, section: g.label }))
  )

  const filterOptions =
    mode === 'family'
      ? Array.from(new Set((data?.families ?? []).map((f) => f.family)))
      : (data?.scripts ?? []).map((s) => s.name)

  return (
    <Screen>
      <FlatList
        data={flat}
        keyExtractor={(item, i) => `${item.id}-${item.section}-${i}`}
        renderItem={({ item }) => (
          <ListItem
            title={item.name}
            subtitle={[item.section, item.languages.join('、'), item.scripts.join('、')]
              .filter(Boolean)
              .join(' · ')}
            fallbackColor={item.themeColor}
            fallbackLabel={item.name}
            onPress={() => navigation.navigate('EthnicDetail', { id: item.id })}
          />
        )}
        contentContainerStyle={styles.content}
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.title}>{t('languages.title')}</Text>
            {data ? (
              <Text style={styles.dek}>
                {pick(
                  `${data.summary.familyCount} 语系 · ${data.summary.languageCount} 语言 · ${data.summary.scriptCount} 文字`,
                  `${data.summary.familyCount} families · ${data.summary.languageCount} languages · ${data.summary.scriptCount} scripts`
                )}
              </Text>
            ) : null}
            <View style={styles.chips}>
              <Chip
                label={t('languages.byFamily')}
                active={mode === 'family'}
                onPress={() => {
                  setMode('family')
                  setFilter('')
                }}
              />
              <Chip
                label={t('languages.byScript')}
                active={mode === 'script'}
                onPress={() => {
                  setMode('script')
                  setFilter('')
                }}
              />
            </View>
            <View style={styles.chips}>
              <Chip label={t('common.all')} active={!filter} onPress={() => setFilter('')} />
              {filterOptions.slice(0, 16).map((opt) => (
                <Chip
                  key={opt}
                  label={opt}
                  active={filter === opt}
                  onPress={() => setFilter(filter === opt ? '' : opt)}
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
  chips: { flexDirection: 'row', flexWrap: 'wrap', marginBottom: spacing.sm },
  content: { paddingHorizontal: spacing.md, paddingBottom: 40 },
})

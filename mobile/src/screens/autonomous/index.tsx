import { useEffect, useMemo, useState } from 'react'
import { FlatList, StyleSheet, Text, View } from 'react-native'
import { Screen } from '../../components/Screen'
import ListItem from '../../components/ListItem'
import Chip from '../../components/Chip'
import EmptyView from '../../components/EmptyView'
import { autonomousAreaApi } from '../../api/modules'
import type { AutonomousArea, AutonomousAreaDirectory } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, spacing, type } from '../../theme'

type ViewMode = 'level' | 'ethnic' | 'province'

export default function AutonomousScreen() {
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [data, setData] = useState<AutonomousAreaDirectory | null>(null)
  const [view, setView] = useState<ViewMode>('level')
  const [level, setLevel] = useState('')
  const [ethnic, setEthnic] = useState('')
  const [province, setProvince] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    ;(async () => {
      setLoading(true)
      try {
        setData(await autonomousAreaApi.list())
      } catch {
        setData(null)
      } finally {
        setLoading(false)
      }
    })()
  }, [])

  const areas: AutonomousArea[] = useMemo(() => {
    if (!data) return []
    if (view === 'level') {
      if (level) return data.levels.find((l) => l.level === level)?.areas ?? []
      return data.levels.flatMap((l) => l.areas)
    }
    if (view === 'ethnic') {
      if (ethnic) return data.ethnics.find((e) => e.ethnic === ethnic)?.areas ?? []
      return data.ethnics.flatMap((e) => e.areas)
    }
    if (province) return data.provinces.find((p) => p.province === province)?.areas ?? []
    return data.provinces.flatMap((p) => p.areas)
  }, [data, view, level, ethnic, province])

  return (
    <Screen>
      <FlatList
        data={areas}
        keyExtractor={(item, i) => `${item.name}-${item.level}-${i}`}
        renderItem={({ item }) => (
          <ListItem
            title={item.name}
            subtitle={[item.levelLabel, item.province, item.seat, item.ethnicGroups.join('、')]
              .filter(Boolean)
              .join(' · ')}
            tag={item.establishedYear ? String(item.establishedYear) : undefined}
            fallbackLabel={item.name}
          />
        )}
        contentContainerStyle={styles.content}
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.title}>{t('autonomous.title')}</Text>
            {data ? (
              <Text style={styles.dek}>
                {pick(
                  `${data.summary.total} 个 · 自治区 ${data.summary.regionCount} · 自治州 ${data.summary.prefectureCount} · 县旗 ${data.summary.countyCount}`,
                  `${data.summary.total} areas · ${data.summary.regionCount} regions · ${data.summary.prefectureCount} prefectures · ${data.summary.countyCount} counties`
                )}
              </Text>
            ) : null}
            <View style={styles.chips}>
              <Chip label={t('autonomous.byLevel')} active={view === 'level'} onPress={() => setView('level')} />
              <Chip label={t('autonomous.byEthnic')} active={view === 'ethnic'} onPress={() => setView('ethnic')} />
              <Chip label={t('autonomous.byProvince')} active={view === 'province'} onPress={() => setView('province')} />
            </View>
            {view === 'level' ? (
              <View style={styles.chips}>
                <Chip label={t('common.all')} active={!level} onPress={() => setLevel('')} />
                {(data?.levels ?? []).map((l) => (
                  <Chip
                    key={l.level}
                    label={`${l.label} (${l.count})`}
                    active={level === l.level}
                    onPress={() => setLevel(level === l.level ? '' : l.level)}
                  />
                ))}
              </View>
            ) : null}
            {view === 'ethnic' ? (
              <View style={styles.chips}>
                <Chip label={t('common.all')} active={!ethnic} onPress={() => setEthnic('')} />
                {(data?.ethnics ?? []).slice(0, 20).map((e) => (
                  <Chip
                    key={e.ethnic}
                    label={`${e.ethnic} (${e.count})`}
                    active={ethnic === e.ethnic}
                    onPress={() => setEthnic(ethnic === e.ethnic ? '' : e.ethnic)}
                  />
                ))}
              </View>
            ) : null}
            {view === 'province' ? (
              <View style={styles.chips}>
                <Chip label={t('common.all')} active={!province} onPress={() => setProvince('')} />
                {(data?.provinces ?? []).slice(0, 20).map((p) => (
                  <Chip
                    key={p.province}
                    label={`${p.province} (${p.count})`}
                    active={province === p.province}
                    onPress={() => setProvince(province === p.province ? '' : p.province)}
                  />
                ))}
              </View>
            ) : null}
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

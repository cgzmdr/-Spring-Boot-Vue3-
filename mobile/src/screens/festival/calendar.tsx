import { useEffect, useState } from 'react'
import { Pressable, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { ScrollScreen } from '../../components/Screen'
import ListItem from '../../components/ListItem'
import EmptyView from '../../components/EmptyView'
import Chip from '../../components/Chip'
import { festivalApi } from '../../api/modules'
import type { CalendarFestival, FestivalCalendar } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, radius, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

export default function FestivalCalendarScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [data, setData] = useState<FestivalCalendar | null>(null)
  const [loading, setLoading] = useState(true)
  const [month, setMonth] = useState<number | null>(null)

  useEffect(() => {
    ;(async () => {
      setLoading(true)
      try {
        const res = await festivalApi.calendar()
        setData(res)
        const first = res.months.find((m) => m.count > 0)
        setMonth(first?.month ?? new Date().getMonth() + 1)
      } catch {
        setData(null)
      } finally {
        setLoading(false)
      }
    })()
  }, [])

  const open = (f: CalendarFestival) => navigation.navigate('FestivalDetail', { id: f.id })

  const activeMonth = data?.months.find((m) => m.month === month)
  const festivals = activeMonth?.festivals ?? []

  return (
    <ScrollScreen>
      <View style={styles.pad}>
        <Text style={styles.title}>{t('calendar.title')}</Text>
        <Text style={styles.dek}>
          {data ? pick(`${data.year} 年农历换算公历`, `Gregorian calendar · ${data.year}`) : t('common.loading')}
        </Text>

        {data?.today ? (
          <Pressable style={styles.today} onPress={() => open(data.today!)}>
            <Text style={styles.todayLabel}>{t('calendar.today')}</Text>
            <Text style={styles.todayName}>{pick(data.today.name, data.today.nameEn || data.today.name)}</Text>
            <Text style={styles.todayMeta}>
              {data.today.date}
              {data.today.ethnicGroupName ? ` · ${data.today.ethnicGroupName}` : ''}
            </Text>
          </Pressable>
        ) : null}

        {data?.upcoming?.length ? (
          <View style={styles.block}>
            <Text style={styles.blockTitle}>{t('calendar.upcoming')}</Text>
            {data.upcoming.slice(0, 5).map((f) => (
              <ListItem
                key={`${f.id}-${f.date}`}
                title={pick(f.name, f.nameEn || f.name)}
                subtitle={`${f.date}${f.ethnicGroupName ? ` · ${f.ethnicGroupName}` : ''}`}
                tag={f.daysFromToday === 0 ? t('calendar.today') : `+${f.daysFromToday}d`}
                fallbackLabel={f.name}
                onPress={() => open(f)}
              />
            ))}
          </View>
        ) : null}

        {loading ? (
          <EmptyView loading />
        ) : (
          <>
            <View style={styles.chips}>
              {(data?.months ?? []).map((m) => (
                <Chip
                  key={m.month}
                  label={`${m.month}${pick('月', '')}${m.count ? ` (${m.count})` : ''}`}
                  active={month === m.month}
                  onPress={() => setMonth(m.month)}
                />
              ))}
            </View>
            {festivals.length === 0 ? (
              <EmptyView text={t('common.empty')} />
            ) : (
              festivals.map((f) => (
                <ListItem
                  key={`${f.id}-${f.date}`}
                  title={pick(f.name, f.nameEn || f.name)}
                  subtitle={[f.date, f.lunarDate, f.ethnicGroupName].filter(Boolean).join(' · ')}
                  tag={f.dateSource === 'approx' ? t('calendar.approx') : undefined}
                  fallbackLabel={f.name}
                  onPress={() => open(f)}
                />
              ))
            )}
          </>
        )}
      </View>
    </ScrollScreen>
  )
}

const styles = StyleSheet.create({
  pad: { padding: spacing.md },
  title: { fontFamily: type.serif, fontSize: 28, color: palette.ink },
  dek: { marginTop: spacing.xs, fontSize: 13, color: palette.muted, marginBottom: spacing.md },
  today: {
    backgroundColor: palette.ink,
    borderRadius: radius.md,
    padding: spacing.md,
    marginBottom: spacing.md,
  },
  todayLabel: { fontSize: 11, letterSpacing: 1, color: palette.gold },
  todayName: { fontFamily: type.serif, fontSize: 20, color: palette.paper, marginTop: 4 },
  todayMeta: { marginTop: 4, fontSize: 12, color: '#C9C4BA' },
  block: { marginBottom: spacing.md },
  blockTitle: { fontFamily: type.serif, fontSize: 18, color: palette.ink, marginBottom: spacing.sm },
  chips: { flexDirection: 'row', flexWrap: 'wrap', marginBottom: spacing.sm },
})

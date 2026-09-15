import { useCallback, useEffect, useState } from 'react'
import { FlatList, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import ListItem from '../../components/ListItem'
import Chip from '../../components/Chip'
import EmptyView from '../../components/EmptyView'
import { personApi } from '../../api/modules'
import type { PersonDirectory, PersonItem } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

export default function PersonsScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [data, setData] = useState<PersonDirectory | null>(null)
  const [domain, setDomain] = useState('')
  const [roleType, setRoleType] = useState('')
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await personApi.list({
        domain: domain || undefined,
        roleType: roleType || undefined,
      })
      setData(res)
    } catch {
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [domain, roleType])

  useEffect(() => {
    load()
  }, [load])

  const persons = data?.persons ?? []

  const openPerson = (p: PersonItem) => {
    const first = p.projects[0]
    if (first?.id) navigation.navigate('ArtDetail', { id: first.id })
  }

  return (
    <Screen>
      <FlatList
        data={persons}
        keyExtractor={(item, i) => `${item.name}-${item.roleType}-${i}`}
        renderItem={({ item }) => (
          <ListItem
            title={item.name}
            subtitle={[item.ethnicGroupName, item.domain, item.lifespan].filter(Boolean).join(' · ')}
            tag={item.roleLabel}
            fallbackLabel={item.name}
            onPress={() => openPerson(item)}
          />
        )}
        contentContainerStyle={styles.content}
        ListHeaderComponent={
          <View style={styles.header}>
            <Text style={styles.title}>{t('persons.title')}</Text>
            {data ? (
              <Text style={styles.dek}>
                {pick(
                  `${data.summary.personCount} 人 · 传承人 ${data.summary.inheritorCount} · 名家 ${data.summary.masterCount}`,
                  `${data.summary.personCount} people · ${data.summary.inheritorCount} inheritors · ${data.summary.masterCount} masters`
                )}
              </Text>
            ) : null}
            <Text style={styles.filterLabel}>{t('persons.role')}</Text>
            <View style={styles.chips}>
              <Chip label={t('common.all')} active={!roleType} onPress={() => setRoleType('')} />
              {(data?.filters.roles ?? []).map((r) => (
                <Chip
                  key={r.value}
                  label={`${r.label} (${r.count})`}
                  active={roleType === r.value}
                  onPress={() => setRoleType(roleType === r.value ? '' : r.value)}
                />
              ))}
            </View>
            <Text style={styles.filterLabel}>{t('persons.domain')}</Text>
            <View style={styles.chips}>
              <Chip label={t('common.all')} active={!domain} onPress={() => setDomain('')} />
              {(data?.filters.domains ?? []).slice(0, 16).map((d) => (
                <Chip
                  key={d.value}
                  label={`${d.label} (${d.count})`}
                  active={domain === d.value}
                  onPress={() => setDomain(domain === d.value ? '' : d.value)}
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

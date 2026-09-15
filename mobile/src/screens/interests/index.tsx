import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import ListItem from '../../components/ListItem'
import Chip from '../../components/Chip'
import EmptyView from '../../components/EmptyView'
import { recommendApi } from '../../api/modules'
import type { InterestTag, Recommendation } from '../../api/types'
import { useAuthStore } from '../../stores/auth'
import { useLangStore } from '../../stores/lang'
import { navigateByContentType } from '../../utils/navigate'
import { palette, radius, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

const DIM_LABEL: Record<string, [string, string]> = {
  ethnic: ['民族', 'Ethnic'],
  region: ['地域', 'Region'],
  type: ['内容类型', 'Type'],
  topic: ['主题', 'Topic'],
}

export default function InterestsScreen() {
  const navigation = useNavigation<Nav>()
  const token = useAuthStore((s) => s.token)
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [groups, setGroups] = useState<Record<string, InterestTag[]>>({})
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [reco, setReco] = useState<Recommendation | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [tags, recommendation] = await Promise.all([
        recommendApi.tags(),
        recommendApi.recommend({ size: 12 }),
      ])
      setGroups(tags)
      setReco(recommendation)
      if (token) {
        try {
          const mine = await recommendApi.mine()
          setSelected(new Set(mine.map((x) => x.id)))
        } catch {
          // 游客或未设兴趣
        }
      }
    } catch {
      setGroups({})
      setReco(null)
    } finally {
      setLoading(false)
    }
  }, [token])

  useEffect(() => {
    load()
  }, [load])

  const dimensions = useMemo(() => Object.keys(groups), [groups])

  const toggle = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const save = async () => {
    if (!token) {
      navigation.navigate('Login')
      return
    }
    setSaving(true)
    try {
      await recommendApi.saveMine([...selected])
      const recommendation = await recommendApi.recommend({ size: 12 })
      setReco(recommendation)
      Alert.alert(t('common.confirm'), t('interests.saved'))
    } catch {
      Alert.alert(t('interests.saveFailed'))
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <Screen>
        <EmptyView loading />
      </Screen>
    )
  }

  return (
    <Screen>
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <Text style={styles.title}>{t('interests.title')}</Text>
        <Text style={styles.dek}>{t('interests.dek')}</Text>

        {dimensions.map((dim) => (
          <View key={dim} style={styles.block}>
            <Text style={styles.blockTitle}>
              {pick(DIM_LABEL[dim]?.[0] || dim, DIM_LABEL[dim]?.[1] || dim)}
            </Text>
            <View style={styles.chips}>
              {(groups[dim] || []).map((tag) => (
                <Chip
                  key={tag.id}
                  label={pick(tag.name, tag.nameEn || tag.name)}
                  active={selected.has(tag.id)}
                  onPress={() => toggle(tag.id)}
                />
              ))}
            </View>
          </View>
        ))}

        <Pressable style={[styles.saveBtn, saving && styles.saveDisabled]} onPress={save} disabled={saving}>
          <Text style={styles.saveText}>
            {token ? (saving ? t('common.loading') : t('interests.save')) : t('interests.loginToSave')}
          </Text>
        </Pressable>

        <Text style={styles.recoTitle}>{t('interests.forYou')}</Text>
        {reco ? (
          <Text style={styles.recoNote}>
            {reco.basisLabel}
            {reco.dataNote ? ` · ${reco.dataNote}` : ''}
          </Text>
        ) : null}
        {(reco?.list ?? []).map((item) => (
          <ListItem
            key={`${item.docType}:${item.docId}`}
            title={item.title}
            subtitle={[item.ethnicName, item.category, item.reason].filter(Boolean).join(' · ')}
            image={item.coverImage}
            fallbackColor={item.themeColor || undefined}
            fallbackLabel={item.title}
            onPress={() => navigateByContentType(navigation, item.docType, item.docId, item.url)}
          />
        ))}
        {!reco?.list?.length ? <EmptyView /> : null}
      </ScrollView>
    </Screen>
  )
}

const styles = StyleSheet.create({
  content: { padding: spacing.md, paddingBottom: 48 },
  title: { fontFamily: type.serif, fontSize: 28, color: palette.ink },
  dek: { marginTop: spacing.xs, fontSize: 13, lineHeight: 20, color: palette.muted, marginBottom: spacing.md },
  block: { marginBottom: spacing.md },
  blockTitle: { fontSize: 13, fontWeight: '600', color: palette.ink, marginBottom: spacing.sm },
  chips: { flexDirection: 'row', flexWrap: 'wrap' },
  saveBtn: {
    backgroundColor: palette.accent,
    borderRadius: radius.md,
    paddingVertical: 12,
    alignItems: 'center',
    marginBottom: spacing.lg,
  },
  saveDisabled: { opacity: 0.6 },
  saveText: { color: palette.white, fontSize: 15, fontWeight: '600' },
  recoTitle: { fontFamily: type.serif, fontSize: 20, color: palette.ink, marginBottom: spacing.xs },
  recoNote: { fontSize: 12, color: palette.faint, marginBottom: spacing.md },
})

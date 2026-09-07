import { useEffect, useState } from 'react'
import { Pressable, StyleSheet, Text, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { ScrollScreen } from '../../components/Screen'
import SectionRule from '../../components/SectionRule'
import EmptyView from '../../components/EmptyView'
import { ethnicApi } from '../../api/modules'
import type { EthnicBrief } from '../../api/types'
import { useLangStore } from '../../stores/lang'
import { palette, radius, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

export default function UnityScreen() {
  const navigation = useNavigation<Nav>()
  const t = useLangStore((s) => s.t)
  const pick = useLangStore((s) => s.pick)

  const [groups, setGroups] = useState<EthnicBrief[]>([])

  useEffect(() => {
    ethnicApi.all().then(setGroups).catch(() => {})
  }, [])

  return (
    <ScrollScreen>
      <View style={styles.hero}>
        <Text style={styles.heroTitle}>多元一体，{'\n'}中华民族共同体</Text>
        <Text style={styles.heroDek}>五十六个民族，像石榴籽一样紧紧抱在一起。</Text>
      </View>

      <View style={styles.section}>
        <SectionRule index="01" title="中华民族一家亲" />
        <Text style={styles.para}>
          中华民族是一个命运共同体，各民族在长期的历史发展中，形成了你中有我、我中有你、
          谁也离不开谁的多元一体格局。共同书写历史、共同创造文化、共同守护家园。
        </Text>
      </View>

      <View style={styles.section}>
        <SectionRule index="02" title="五十六个民族全家福" />
        <View style={styles.wall}>
          {groups.map((g) => (
            <Pressable key={g.id} style={styles.wallItem} onPress={() => navigation.navigate('EthnicDetail', { id: g.id })}>
              <View style={[styles.wallBlock, { backgroundColor: g.themeColor || palette.coverFallback }]}>
                <Text style={styles.wallChar}>{g.name[0]}</Text>
              </View>
              <Text style={styles.wallName} numberOfLines={1}>{g.name}</Text>
            </Pressable>
          ))}
        </View>
        {!groups.length ? <EmptyView /> : null}
      </View>
    </ScrollScreen>
  )
}

const styles = StyleSheet.create({
  hero: {
    backgroundColor: palette.ink,
    padding: spacing.lg,
  },
  heroTitle: {
    fontFamily: type.serif,
    fontSize: 34,
    lineHeight: 44,
    color: palette.paper,
  },
  heroDek: {
    marginTop: spacing.md,
    fontSize: 14,
    lineHeight: 22,
    color: '#C9C4BA',
  },
  section: {
    marginTop: spacing.lg,
  },
  para: {
    fontSize: 15,
    lineHeight: 26,
    color: palette.ink,
    paddingHorizontal: spacing.md,
  },
  wall: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: spacing.md,
  },
  wallItem: {
    width: '25%',
    alignItems: 'center',
    marginBottom: spacing.md,
  },
  wallBlock: {
    width: 56,
    height: 56,
    borderRadius: radius.sm,
    alignItems: 'center',
    justifyContent: 'center',
  },
  wallChar: {
    fontFamily: type.serif,
    fontSize: 24,
    color: palette.white,
  },
  wallName: {
    marginTop: 4,
    fontSize: 11,
    color: palette.muted,
  },
})

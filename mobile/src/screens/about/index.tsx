import { StyleSheet, Text, View } from 'react-native'
import { ScrollScreen } from '../../components/Screen'
import SectionRule from '../../components/SectionRule'
import { useLangStore } from '../../stores/lang'
import { palette, radius, spacing, type } from '../../theme'

export default function AboutScreen() {
  const t = useLangStore((s) => s.t)

  return (
    <ScrollScreen>
      <View style={styles.hero}>
        <Text style={styles.heroTitle}>{t('app.name')}</Text>
        <Text style={styles.heroSub}>{t('app.slogan')}</Text>
      </View>

      <View style={styles.section}>
        <SectionRule index="01" title="项目目标" />
        <Text style={styles.para}>
          一站式、沉浸式、可探索的中华民族数字文化博物馆。以五十六个民族为脉络，
          聚合节日庆典、传统艺术、民族习俗、特色美食与聚居地知识，
          让每一位访问者都能便捷地触摸中华文化的多样之美。
        </Text>
      </View>

      <View style={styles.section}>
        <SectionRule index="02" title="数据与版权" />
        <Text style={styles.para}>
          站内内容由后台编辑团队整理发布。部分图片与资料来源于公开渠道，
          版权归原作者所有；如涉及侵权，请联系我们删除。
        </Text>
      </View>

      <View style={styles.section}>
        <SectionRule index="03" title="联系我们" />
        <View style={styles.card}>
          <Text style={styles.cardLine}>· 意见反馈：support@example.com</Text>
          <Text style={styles.cardLine}>· 数据合作：partners@example.com</Text>
          <Text style={styles.cardLine}>· 版本：1.0.0</Text>
        </View>
      </View>
    </ScrollScreen>
  )
}

const styles = StyleSheet.create({
  hero: {
    backgroundColor: palette.accent,
    padding: spacing.lg,
  },
  heroTitle: {
    fontFamily: type.serif,
    fontSize: 26,
    color: palette.white,
  },
  heroSub: {
    marginTop: spacing.sm,
    fontSize: 13,
    color: 'rgba(255,255,255,0.85)',
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
  card: {
    marginHorizontal: spacing.md,
    backgroundColor: palette.white,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    borderRadius: radius.md,
    padding: spacing.md,
  },
  cardLine: {
    fontSize: 13,
    color: palette.muted,
    lineHeight: 24,
  },
})

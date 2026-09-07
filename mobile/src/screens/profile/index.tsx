import { useCallback, useEffect, useState } from 'react'
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native'
import { useNavigation, useIsFocused } from '@react-navigation/native'
import type { NativeStackNavigationProp } from '@react-navigation/native-stack'
import { Screen } from '../../components/Screen'
import ListItem from '../../components/ListItem'
import EmptyView from '../../components/EmptyView'
import { meApi } from '../../api/modules'
import { useAuthStore } from '../../stores/auth'
import { useLangStore } from '../../stores/lang'
import { navigateByContentType } from '../../utils/navigate'
import { palette, radius, spacing, type } from '../../theme'
import type { RootStackParamList } from '../../navigation/types'

type Nav = NativeStackNavigationProp<RootStackParamList>

interface FavoriteItem {
  id: string
  entryType: string
  entryId: string
  entryName: string
  coverImage?: string | null
}

const PAGE_SIZE = 20

export default function ProfileScreen() {
  const navigation = useNavigation<Nav>()
  const isFocused = useIsFocused()
  const token = useAuthStore((s) => s.token)
  const user = useAuthStore((s) => s.user)
  const logout = useAuthStore((s) => s.logout)
  const t = useLangStore((s) => s.t)
  const isEn = useLangStore((s) => s.isEn)
  const toggleLang = useLangStore((s) => s.toggle)

  const [favs, setFavs] = useState<FavoriteItem[]>([])
  const [favTotal, setFavTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)

  const loadFavs = useCallback(
    async (pageNum: number, replace: boolean) => {
      if (!token || loading) return
      setLoading(true)
      try {
        const res = await meApi.favorites({ page: pageNum, size: PAGE_SIZE })
        const items = (res.data ?? []) as FavoriteItem[]
        setFavs((prev) => (replace ? items : [...prev, ...items]))
        setFavTotal(res.total)
        setPage(pageNum)
      } catch {
        // 静默
      } finally {
        setLoading(false)
      }
    },
    [token, loading]
  )

  useEffect(() => {
    if (isFocused) {
      setFavs([])
      setPage(0)
      loadFavs(0, true)
    }
  }, [isFocused, token])

  const openFav = (item: FavoriteItem) => {
    navigateByContentType(navigation, item.entryType, item.entryId)
  }

  return (
    <Screen>
      <FlatList
        data={favs}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <ListItem
            title={item.entryName}
            image={item.coverImage}
            fallbackLabel={item.entryName}
            onPress={() => openFav(item)}
          />
        )}
        contentContainerStyle={styles.content}
        onEndReached={() => {
          if (loading || favs.length >= favTotal) return
          loadFavs(page + 1, false)
        }}
        onEndReachedThreshold={0.3}
        ListHeaderComponent={
          <View>
            {/* 用户卡片 */}
            <View style={styles.userCard}>
              {token ? (
                <>
                  <View style={styles.avatar}>
                    <Text style={styles.avatarText}>{user?.nickname?.[0] ?? '我'}</Text>
                  </View>
                  <View style={styles.userInfo}>
                    <Text style={styles.nickname}>{user?.nickname}</Text>
                    <Text style={styles.userMeta}>{user?.email || user?.mobile || ''}</Text>
                  </View>
                </>
              ) : (
                <>
                  <View style={styles.avatar}>
                    <Text style={styles.avatarText}>?</Text>
                  </View>
                  <View style={styles.userInfo}>
                    <Text style={styles.nickname}>{t('profile.guest')}</Text>
                    <Text style={styles.userMeta}>{t('profile.guestTip')}</Text>
                  </View>
                  <Pressable style={styles.loginBtn} onPress={() => navigation.navigate('Login')}>
                    <Text style={styles.loginBtnText}>{t('profile.loginNow')}</Text>
                  </Pressable>
                </>
              )}
            </View>

            {/* 菜单 */}
            <View style={styles.menu}>
              <Pressable style={styles.menuRow} onPress={() => navigation.navigate('About')}>
                <Text style={styles.menuLabel}>{t('profile.about')}</Text>
                <Text style={styles.menuArrow}>→</Text>
              </Pressable>
              <Pressable style={styles.menuRow} onPress={toggleLang}>
                <Text style={styles.menuLabel}>{t('profile.language')}</Text>
                <Text style={styles.menuValue}>{isEn ? 'English' : '中文'}</Text>
              </Pressable>
              {token ? (
                <Pressable style={styles.menuRow} onPress={logout}>
                  <Text style={[styles.menuLabel, styles.logoutText]}>{t('profile.logout')}</Text>
                  <Text style={styles.menuArrow}>→</Text>
                </Pressable>
              ) : null}
            </View>

            {token ? (
              <Text style={styles.favTitle}>{t('profile.favorites')}</Text>
            ) : null}
          </View>
        }
        ListEmptyComponent={
          token && !loading ? <EmptyView text={t('profile.emptyFavorites')} /> : !token ? null : <EmptyView loading />
        }
      />
    </Screen>
  )
}

const styles = StyleSheet.create({
  content: {
    padding: spacing.md,
    paddingBottom: 40,
  },
  userCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: palette.ink,
    borderRadius: radius.md,
    padding: spacing.lg,
    marginBottom: spacing.md,
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: palette.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    fontFamily: type.serif,
    fontSize: 24,
    color: palette.white,
  },
  userInfo: {
    flex: 1,
    marginLeft: spacing.md,
  },
  nickname: {
    fontFamily: type.serif,
    fontSize: 20,
    color: palette.paper,
  },
  userMeta: {
    marginTop: 4,
    fontSize: 12,
    color: '#C9C4BA',
  },
  loginBtn: {
    borderWidth: 1,
    borderColor: palette.gold,
    borderRadius: 99,
    paddingHorizontal: spacing.md,
    paddingVertical: 8,
  },
  loginBtnText: {
    color: palette.gold,
    fontSize: 13,
  },
  menu: {
    backgroundColor: palette.white,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    borderRadius: radius.md,
    marginBottom: spacing.lg,
  },
  menuRow: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: palette.border,
  },
  menuLabel: {
    flex: 1,
    fontSize: 15,
    color: palette.ink,
  },
  menuValue: {
    fontSize: 13,
    color: palette.muted,
  },
  menuArrow: {
    color: palette.faint,
    marginLeft: spacing.sm,
  },
  logoutText: {
    color: palette.accent,
  },
  favTitle: {
    fontFamily: type.serif,
    fontSize: 20,
    color: palette.ink,
    marginBottom: spacing.sm,
  },
})

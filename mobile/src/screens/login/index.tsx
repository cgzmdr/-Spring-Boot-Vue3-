import { useState } from 'react'
import { Alert, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native'
import { useNavigation } from '@react-navigation/native'
import { useAuthStore } from '../../stores/auth'
import { useLangStore } from '../../stores/lang'
import { palette, radius, spacing, type } from '../../theme'

export default function LoginScreen() {
  const navigation = useNavigation()
  const login = useAuthStore((s) => s.login)
  const register = useAuthStore((s) => s.register)
  const t = useLangStore((s) => s.t)

  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [account, setAccount] = useState('')
  const [nickname, setNickname] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    if (busy) return
    if (!account.trim() || !password) {
      Alert.alert(t('login.title'), t('login.error'))
      return
    }
    if (mode === 'register' && nickname.trim().length < 2) {
      Alert.alert(t('login.register'), t('login.nicknamePlaceholder'))
      return
    }
    setBusy(true)
    try {
      if (mode === 'login') {
        await login(account.trim(), password)
        Alert.alert(t('login.success'))
        navigation.goBack()
      } else {
        await register(nickname.trim(), password)
        Alert.alert(t('login.registerSuccess'))
        setMode('login')
        setNickname('')
        setPassword('')
      }
    } catch (e) {
      Alert.alert(t('login.title'), e instanceof Error ? e.message : t('login.error'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.brand}>{t('app.name')}</Text>
        <Text style={styles.subBrand}>{t('app.slogan')}</Text>

        <View style={styles.card}>
          <View style={styles.tabs}>
            <Pressable style={[styles.tab, mode === 'login' && styles.tabActive]} onPress={() => setMode('login')}>
              <Text style={[styles.tabText, mode === 'login' && styles.tabTextActive]}>{t('login.title')}</Text>
            </Pressable>
            <Pressable style={[styles.tab, mode === 'register' && styles.tabActive]} onPress={() => setMode('register')}>
              <Text style={[styles.tabText, mode === 'register' && styles.tabTextActive]}>{t('login.register')}</Text>
            </Pressable>
          </View>

          {mode === 'register' ? (
            <Field
              label={t('login.nickname')}
              placeholder={t('login.nicknamePlaceholder')}
              value={nickname}
              onChangeText={setNickname}
            />
          ) : null}

          <Field
            label={t('login.account')}
            placeholder={t('login.accountPlaceholder')}
            value={account}
            onChangeText={setAccount}
            autoCapitalize="none"
          />

          <Field
            label={t('login.password')}
            placeholder={t('login.passwordPlaceholder')}
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />

          <Pressable style={[styles.submit, busy && styles.submitBusy]} onPress={submit} disabled={busy}>
            <Text style={styles.submitText}>
              {mode === 'login' ? t('login.submit') : t('login.submitRegister')}
            </Text>
          </Pressable>

          <Pressable onPress={() => setMode(mode === 'login' ? 'register' : 'login')}>
            <Text style={styles.switch}>
              {mode === 'login' ? t('login.switchToRegister') : t('login.switchToLogin')}
            </Text>
          </Pressable>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  )
}

function Field({ label, placeholder, value, onChangeText, secureTextEntry, autoCapitalize }: {
  label: string
  placeholder: string
  value: string
  onChangeText: (v: string) => void
  secureTextEntry?: boolean
  autoCapitalize?: 'none' | 'sentences'
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <TextInput
        style={styles.input}
        placeholder={placeholder}
        placeholderTextColor={palette.faint}
        value={value}
        onChangeText={onChangeText}
        secureTextEntry={secureTextEntry}
        autoCapitalize={autoCapitalize ?? 'sentences'}
      />
    </View>
  )
}

const styles = StyleSheet.create({
  flex: {
    flex: 1,
    backgroundColor: palette.paper,
  },
  content: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: spacing.lg,
  },
  brand: {
    fontFamily: type.serif,
    fontSize: 28,
    color: palette.ink,
    textAlign: 'center',
  },
  subBrand: {
    marginTop: spacing.sm,
    fontSize: 13,
    color: palette.faint,
    textAlign: 'center',
    marginBottom: spacing.xl,
  },
  card: {
    backgroundColor: palette.white,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: palette.border,
    borderRadius: radius.lg,
    padding: spacing.lg,
  },
  tabs: {
    flexDirection: 'row',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: palette.border,
    marginBottom: spacing.md,
  },
  tab: {
    flex: 1,
    paddingVertical: spacing.sm,
    alignItems: 'center',
  },
  tabActive: {
    borderBottomWidth: 2,
    borderBottomColor: palette.accent,
  },
  tabText: {
    fontSize: 16,
    color: palette.muted,
  },
  tabTextActive: {
    color: palette.accent,
    fontWeight: '600',
  },
  field: {
    marginBottom: spacing.md,
  },
  fieldLabel: {
    fontSize: 13,
    color: palette.faint,
    marginBottom: 6,
  },
  input: {
    height: 46,
    borderWidth: 1,
    borderColor: palette.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    fontSize: 15,
    color: palette.ink,
    backgroundColor: palette.paper,
  },
  submit: {
    height: 48,
    borderRadius: radius.md,
    backgroundColor: palette.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: spacing.sm,
  },
  submitBusy: {
    opacity: 0.6,
  },
  submitText: {
    fontSize: 16,
    letterSpacing: 2,
    color: palette.white,
  },
  switch: {
    marginTop: spacing.md,
    fontSize: 13,
    color: palette.accent,
    textAlign: 'center',
  },
})

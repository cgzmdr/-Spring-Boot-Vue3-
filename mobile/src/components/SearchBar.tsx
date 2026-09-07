import { useState } from 'react'
import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native'
import { palette, radius, spacing, type } from '../theme'
import { useLangStore } from '../stores/lang'

interface SearchBarProps {
  placeholder?: string
  defaultValue?: string
  autoFocus?: boolean
  onSearch: (keyword: string) => void
}

/** 搜索输入条 */
export default function SearchBar({ placeholder, defaultValue = '', autoFocus = false, onSearch }: SearchBarProps) {
  const [value, setValue] = useState(defaultValue)
  const t = useLangStore((s) => s.t)

  const submit = () => {
    const kw = value.trim()
    if (kw) onSearch(kw)
  }

  return (
    <View style={styles.wrap}>
      <TextInput
        style={styles.input}
        placeholder={placeholder}
        placeholderTextColor={palette.faint}
        value={value}
        autoFocus={autoFocus}
        returnKeyType="search"
        onChangeText={setValue}
        onSubmitEditing={submit}
      />
      <Pressable style={styles.btn} onPress={submit}>
        <Text style={styles.btnText}>{t('common.search')}</Text>
      </Pressable>
    </View>
  )
}

const styles = StyleSheet.create({
  wrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    paddingHorizontal: spacing.md,
  },
  input: {
    flex: 1,
    height: 44,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: palette.border,
    backgroundColor: palette.white,
    paddingHorizontal: spacing.md,
    fontSize: 15,
    color: palette.ink,
  },
  btn: {
    height: 44,
    paddingHorizontal: spacing.md,
    borderRadius: radius.md,
    backgroundColor: palette.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnText: {
    color: palette.white,
    fontSize: 14,
  },
})

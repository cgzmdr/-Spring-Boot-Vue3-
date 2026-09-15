<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { recommendApi } from '@/api/modules'
import type { InterestTag } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useLangStore } from '@/stores/lang'

/**
 * 兴趣标签选择器（方向 D）。
 *
 * 这是推荐系统的**冷启动解法**：新用户没有任何浏览记录时，
 * 只靠行为无法推荐；让用户直接勾选题材，推荐立即变得有意义。
 *
 * 四个维度：民族 / 地域 / 内容类型 / 主题。
 */
const auth = useAuthStore()
const lang = useLangStore()

/** 保存成功后通知父组件刷新推荐 */
const emit = defineEmits<{ (e: 'saved', count: number): void }>()

const groups = ref<Record<string, InterestTag[]>>({})
const selected = ref<Set<string>>(new Set())
const loading = ref(true)
const saving = ref(false)
/** 各维度是否展开（民族 56 个较长，默认收起） */
const expanded = ref<Record<string, boolean>>({})

const DIMENSION_LABEL: Record<string, string> = {
  ethnic: '民族',
  region: '地域',
  type: '内容类型',
  topic: '主题',
}
const DIMENSION_LABEL_EN: Record<string, string> = {
  ethnic: 'Ethnic Groups',
  region: 'Regions',
  type: 'Content Type',
  topic: 'Topics',
}

/** 维度展示顺序 */
const ORDER = ['type', 'ethnic', 'region', 'topic']

const orderedGroups = computed(() =>
  ORDER.filter((d) => groups.value[d]?.length).map((d) => ({
    key: d,
    label: lang.pick(DIMENSION_LABEL[d] || d, DIMENSION_LABEL_EN[d] || d),
    tags: groups.value[d],
  })),
)

/** 每个维度默认最多显示 12 个，其余折叠 */
const VISIBLE_LIMIT = 12

function visibleTags(key: string, tags: InterestTag[]): InterestTag[] {
  if (expanded.value[key] || tags.length <= VISIBLE_LIMIT) return tags
  return tags.slice(0, VISIBLE_LIMIT)
}

function toggle(id: string) {
  const next = new Set(selected.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selected.value = next
}

async function load() {
  loading.value = true
  try {
    groups.value = await recommendApi.tags()
    if (auth.isLoggedIn) {
      const mine = await recommendApi.mine()
      selected.value = new Set(mine.map((t) => t.id))
    }
  } catch {
    ElMessage.error('兴趣标签加载失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!auth.isLoggedIn) {
    ElMessage.warning('登录后即可保存兴趣，获得个性化推荐')
    return
  }
  saving.value = true
  try {
    const n = await recommendApi.saveMine([...selected.value])
    ElMessage.success(`已保存 ${n} 个兴趣标签，推荐将据此调整`)
    emit('saved', n)
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

onMounted(load)

defineExpose({ reload: load })
</script>

<template>
  <div class="interest-picker">
    <div class="ip-head">
      <div>
        <h3>{{ lang.pick('选择你的兴趣', 'Pick your interests') }}</h3>
        <p class="ip-sub">
          {{ lang.pick(
            '勾选感兴趣的题材，我们会据此推荐相关内容；未勾选时按热度推荐。',
            'Choose topics you care about; without selections we fall back to popular content.',
          ) }}
        </p>
      </div>
      <div class="ip-actions">
        <span class="ip-count">{{ selected.size }} {{ lang.pick('已选', 'selected') }}</span>
        <button class="ip-save" :disabled="saving" @click="save">
          {{ saving ? lang.pick('保存中…', 'Saving…') : lang.pick('保存', 'Save') }}
        </button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />

    <div v-else class="ip-groups">
      <section v-for="g in orderedGroups" :key="g.key" class="ip-group">
        <h4>
          {{ g.label }}
          <span class="ip-gn">{{ g.tags.length }}</span>
        </h4>
        <div class="ip-tags">
          <button
            v-for="t in visibleTags(g.key, g.tags)"
            :key="t.id"
            class="ip-tag"
            :class="{ on: selected.has(t.id) }"
            :style="selected.has(t.id) && t.color ? { borderColor: t.color, color: t.color, background: t.color + '14' } : {}"
            :title="t.description || t.name"
            @click="toggle(t.id)"
          >
            {{ lang.pick(t.name, t.nameEn || t.name) }}
          </button>
        </div>
        <button
          v-if="g.tags.length > VISIBLE_LIMIT"
          class="ip-more"
          @click="expanded[g.key] = !expanded[g.key]"
        >
          {{ expanded[g.key] ? lang.pick('收起', 'Show less') : lang.pick(`展开全部 ${g.tags.length} 个`, `Show all ${g.tags.length}`) }}
        </button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.interest-picker {
  padding: 22px 0;
}
.ip-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}
.ip-head h3 {
  margin: 0 0 6px;
  font-size: 17px;
  font-family: var(--serif);
}
.ip-sub {
  margin: 0;
  font-size: 12.5px;
  color: var(--muted);
  line-height: 1.6;
}
.ip-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ip-count {
  font-size: 12.5px;
  color: var(--muted);
}
.ip-save {
  padding: 7px 20px;
  border: 1px solid var(--accent);
  background: var(--accent);
  color: #fff;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.15s ease;
}
.ip-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ip-groups {
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding-top: 18px;
}
.ip-group h4 {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--muted);
}
.ip-gn {
  margin-left: 6px;
  font-size: 11px;
  opacity: 0.7;
}
.ip-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.ip-tag {
  padding: 5px 13px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: none;
  font: inherit;
  font-size: 13px;
  color: var(--ink-2);
  cursor: pointer;
  transition: all 0.15s ease;
}
.ip-tag:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.ip-tag.on {
  border-color: var(--accent);
  color: var(--accent);
  font-weight: 600;
}
.ip-more {
  margin-top: 10px;
  padding: 0;
  border: 0;
  background: none;
  font: inherit;
  font-size: 12px;
  color: var(--muted);
  cursor: pointer;
}
.ip-more:hover {
  color: var(--accent);
  text-decoration: underline;
}
</style>

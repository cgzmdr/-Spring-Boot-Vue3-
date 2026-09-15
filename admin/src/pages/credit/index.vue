<template>
  <div class="page-container">
    <div class="page-header">
      <h2>图片署名</h2>
      <el-tag
        v-if="showOnlyPending"
        type="warning"
        >仅看待核</el-tag
      >
    </div>

    <el-alert
      type="warning"
      :closable="false"
      class="hint"
    >
      <template #title>
        <b>版权元数据已丢失</b>：图片采集后被重命名为 cover.webp，采集清单目录已删除、media_asset 表为空，
        无法把现有图片对应回原始来源页。因此 <b>当前 574 张全部标为「来源待核」</b>。
        <br />
        核实方式：用 Wikimedia Commons 的「反向图片搜索」按图片内容找回来源页，
        再把作者、许可、来源链接填入本页 —— 填好后 C 端自动显示真实署名。
        <br />
        <b>标记为「已核实」时至少需填作者或许可</b>，否则系统会拒绝（避免出现显示不出署名的「假核实」）。
      </template>
    </el-alert>

    <el-form
      :inline="true"
      class="search-bar"
      @submit.prevent
    >
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="路径 / 说明 / 作者"
          clearable
          style="width: 220px"
          @keyup.enter="onSearch"
          @clear="onSearch"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select
          v-model="query.status"
          clearable
          placeholder="全部"
          style="width: 170px"
          @change="onSearch"
        >
          <el-option
            v-for="o in CREDIT_STATUS_OPTIONS"
            :key="o.value"
            :label="o.label"
            :value="o.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          @click="onSearch"
          >查询</el-button
        >
        <el-button
          type="warning"
          @click="showPending"
          >只看待核</el-button
        >
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table
      :data="list"
      v-loading="loading"
      border
      stripe
    >
      <el-table-column
        label="图片"
        width="90"
      >
        <template #default="{ row }">
          <img
            :src="imageUrl(row.imagePath)"
            class="thumb"
            alt=""
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="caption"
        label="说明"
        min-width="180"
        show-overflow-tooltip
      >
        <template #default="{ row }">{{ row.caption || '-' }}</template>
      </el-table-column>
      <el-table-column
        label="状态"
        width="130"
      >
        <template #default="{ row }">
          <StatusTag
            :label="STATUS_MAP[row.creditStatus] || row.creditStatus"
            :tag="row.creditStatus === 'verified' ? 'success' : row.creditStatus === 'unverified' ? 'warning' : 'info'"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="author"
        label="作者"
        width="160"
        show-overflow-tooltip
      >
        <template #default="{ row }">{{ row.author || '-' }}</template>
      </el-table-column>
      <el-table-column
        prop="license"
        label="许可"
        width="120"
      >
        <template #default="{ row }">{{ row.license || '-' }}</template>
      </el-table-column>
      <el-table-column
        label="来源"
        width="100"
      >
        <template #default="{ row }">
          <el-link
            v-if="row.sourceUrl"
            type="primary"
            :href="row.sourceUrl"
            target="_blank"
            rel="noopener noreferrer"
            >打开</el-link
          >
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="100"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openEdit(row as ImageCredit)"
            >核实</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <div class="result-count">共 {{ total }} 条</div>
    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next, sizes, total"
        :total="total"
        :current-page="page + 1"
        :page-size="size"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <!-- 核实对话框 -->
    <el-dialog
      v-model="editVisible"
      title="核实图片署名"
      width="620px"
    >
      <div
        v-if="form"
        class="edit-body"
      >
        <img
          :src="imageUrl(form.imagePath)"
          class="preview"
          alt=""
        />
        <div class="path">{{ form.imagePath }}</div>

        <el-form
          :model="form"
          label-width="100px"
          style="margin-top: 14px"
        >
          <el-form-item label="说明">
            <el-input v-model="form.caption" />
          </el-form-item>
          <el-form-item label="核实状态">
            <el-select
              v-model="form.creditStatus"
              style="width: 100%"
            >
              <el-option
                v-for="o in CREDIT_STATUS_OPTIONS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="作者">
            <el-input
              v-model="form.author"
              placeholder="从来源页抄录的作者 / 摄影者"
            />
          </el-form-item>
          <el-form-item label="许可">
            <el-input
              v-model="form.license"
              placeholder="如 CC BY-SA 4.0 / CC0 / Public Domain"
            />
          </el-form-item>
          <el-form-item label="许可链接">
            <el-input
              v-model="form.licenseUrl"
              placeholder="https://creativecommons.org/licenses/…"
            />
          </el-form-item>
          <el-form-item label="来源链接">
            <el-input
              v-model="form.sourceUrl"
              placeholder="来源文件页 URL"
            />
          </el-form-item>
          <el-form-item label="来源站点">
            <el-input
              v-model="form.sourceSite"
              placeholder="如 Wikimedia Commons"
            />
          </el-form-item>
          <el-form-item label="需署名">
            <!-- el-switch 不接受 null，null 视为「需要署名」的默认值 -->
            <el-switch :model-value="form.attributionRequired !== false" @update:model-value="(v) => { if (form) form.attributionRequired = v as boolean }" />
            <span class="form-tip">CC BY / CC BY-SA 为是；CC0 / 公有领域为否</span>
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="2"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="saving"
          @click="handleSave"
          >保存</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getImageCredit,
  listImageCredits,
  updateImageCredit,
  CREDIT_STATUS_OPTIONS,
  type ImageCredit
} from '@/api/modules/bseries'
import StatusTag from '@/components/StatusTag.vue'
import { resolveImageUrl } from '@/utils/format'

const STATUS_MAP: Record<string, string> = Object.fromEntries(
  CREDIT_STATUS_OPTIONS.map((o) => [o.value, o.label])
)

const loading = ref(false)
const list = ref<ImageCredit[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(10)
const query = ref<{ keyword: string; status: string }>({ keyword: '', status: '' })

const editVisible = ref(false)
const saving = ref(false)
const form = ref<ImageCredit | null>(null)

const showOnlyPending = computed(() => query.value.status === 'unverified')

/** 图片直接指向后端静态服务（与 C 端一致） */
function imageUrl(path: string): string {
  return resolveImageUrl(path)
}

async function loadData() {
  loading.value = true
  try {
    const res = await listImageCredits({
      keyword: query.value.keyword || undefined,
      status: query.value.status || undefined,
      page: page.value,
      size: size.value
    })
    list.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 0
  loadData()
}
function showPending() {
  query.value.status = 'unverified'
  page.value = 0
  loadData()
}
function resetQuery() {
  query.value = { keyword: '', status: '' }
  page.value = 0
  loadData()
}
function onPageChange(p: number) {
  page.value = p - 1
  loadData()
}
function onSizeChange(sz: number) {
  size.value = sz
  page.value = 0
  loadData()
}

async function openEdit(row: ImageCredit) {
  form.value = await getImageCredit(String(row.id))
  editVisible.value = true
}

async function handleSave() {
  if (!form.value) return
  saving.value = true
  try {
    await updateImageCredit(String(form.value.id), form.value)
    ElMessage.success('已保存')
    editVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
.search-bar {
  padding: 12px 0;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 12px;
}
.result-count {
  margin-top: 12px;
  font-size: 13px;
  color: #6b7280;
}
.hint {
  margin-bottom: 12px;
}
.thumb {
  width: 60px;
  height: 44px;
  object-fit: cover;
  border-radius: 2px;
  display: block;
}
.edit-body .preview {
  width: 100%;
  max-height: 240px;
  object-fit: contain;
  background: #f3f4f6;
  border-radius: 4px;
}
.edit-body .path {
  font-size: 12px;
  color: #6b7280;
  margin-top: 6px;
  word-break: break-all;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}
</style>

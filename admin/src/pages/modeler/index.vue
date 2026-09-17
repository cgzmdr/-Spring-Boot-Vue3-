<template>
  <div class="modeler-page">
    <!-- 顶部工具栏 -->
    <div class="modeler-toolbar">
      <div class="tb-left">
        <el-radio-group v-model="activeTab" size="small">
          <el-radio-button value="bpmn">BPMN 流程</el-radio-button>
          <el-radio-button value="form">Camunda Form</el-radio-button>
          <el-radio-button value="engine">引擎</el-radio-button>
        </el-radio-group>

        <el-tag v-if="engine.healthy" type="success" effect="plain" size="small" class="ml">
          引擎在线（嵌入式） · Camunda {{ engine.version || '7' }}
        </el-tag>
        <el-tag v-else type="danger" effect="dark" size="small" class="ml">引擎离线</el-tag>
      </div>

      <div class="tb-right">
        <template v-if="activeTab === 'bpmn'">
          <el-button size="small" :icon="DocumentAdd" @click="newProcess">新建流程</el-button>
          <el-button
            size="small"
            :icon="FolderOpened"
            :disabled="!currentProcess"
            @click="loadProcess(currentProcess!.id)"
          >
            重新载入
          </el-button>
          <el-button
            size="small"
            type="primary"
            :icon="Check"
            :disabled="!currentProcess || bpmnDirty === false"
            :loading="saving"
            @click="saveProcess"
          >
            保存
          </el-button>
          <el-button
            size="small"
            type="success"
            :icon="Upload"
            :disabled="!currentProcess"
            :loading="deploying"
            @click="deployProcess"
          >
            部署到引擎
          </el-button>
        </template>

        <template v-else-if="activeTab === 'form'">
          <el-button size="small" :icon="DocumentAdd" @click="newForm">新建表单</el-button>
          <el-button
            size="small"
            type="primary"
            :icon="Check"
            :disabled="!currentForm"
            :loading="saving"
            @click="saveForm"
          >
            保存
          </el-button>
          <el-button
            size="small"
            type="success"
            :icon="Upload"
            :disabled="!currentForm"
            :loading="deploying"
            @click="deployForm"
          >
            保存并生效
          </el-button>
        </template>

        <template v-else>
          <el-button size="small" :icon="Refresh" :loading="loadingEngine" @click="loadEngine">
            刷新
          </el-button>
          <el-button
            size="small"
            type="warning"
            :icon="Upload"
            :loading="deploying"
            @click="redeployBuiltin"
          >
            重新部署内置流程
          </el-button>
        </template>
      </div>
    </div>

    <!-- ============================ BPMN ============================ -->
    <div v-show="activeTab === 'bpmn'" class="modeler-body">
      <!-- 左侧：流程定义列表 -->
      <div class="side-panel">
        <div class="panel-title">流程定义</div>
        <el-scrollbar class="panel-scroll">
          <div
            v-for="p in processes"
            :key="p.id"
            class="list-item"
            :class="{ active: currentProcess?.id === p.id }"
            @click="loadProcess(p.id)"
          >
            <div class="li-main">
              <div class="li-name">
                {{ p.name }}
                <el-tag v-if="p.enabled" size="small" type="success" effect="dark">启用</el-tag>
              </div>
              <div class="li-sub">
                {{ p.processKey }} · v{{ p.version }}
                <span v-if="p.camundaDefinitionVersion" class="li-engine">
                  引擎 v{{ p.camundaDefinitionVersion }}
                </span>
                <span v-else class="li-warn">未部署</span>
              </div>
            </div>
            <div class="li-actions" @click.stop>
              <el-tooltip content="设为启用" placement="top">
                <el-button
                  link
                  size="small"
                  :type="p.enabled ? 'success' : 'info'"
                  :icon="Select"
                  @click="toggleProcessEnabled(p)"
                />
              </el-tooltip>
              <el-tooltip content="另存新版本" placement="top">
                <el-button link size="small" :icon="CopyDocument" @click="duplicateProcess(p.id)" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button
                  link
                  size="small"
                  type="danger"
                  :icon="Delete"
                  :disabled="p.enabled"
                  @click="removeProcess(p)"
                />
              </el-tooltip>
            </div>
          </div>
          <el-empty v-if="!processes.length" description="暂无流程定义" :image-size="60" />
        </el-scrollbar>
      </div>

      <!-- 中：BPMN 画布 -->
      <div class="canvas-wrap">
        <div ref="bpmnHostRef" class="canvas-host" />
        <div v-if="!currentProcess" class="canvas-empty">
          <el-empty description="请选择左侧流程定义，或新建一个流程">
            <el-button type="primary" @click="newProcess">新建流程</el-button>
          </el-empty>
        </div>
      </div>

      <!-- 右：属性 / 校验 -->
      <div class="side-panel right">
        <el-tabs v-model="bpmnRightTab" class="right-tabs">
          <el-tab-pane label="属性" name="props">
            <el-form v-if="currentProcess" label-width="90px" size="small" class="prop-form">
              <el-form-item label="流程名称">
                <el-input v-model="processForm.name" @change="markBpmnDirty" />
              </el-form-item>
              <el-form-item label="流程标识">
                <el-input v-model="processForm.processKey" @change="markBpmnDirty" />
              </el-form-item>
              <el-form-item label="业务类型">
                <el-select v-model="processForm.entryType" style="width: 100%">
                  <el-option label="民族" value="ethnic" />
                  <el-option label="节日" value="festival" />
                  <el-option label="艺术" value="art" />
                  <el-option label="专题" value="topic" />
                  <el-option label="通用（*）" value="*" />
                </el-select>
              </el-form-item>
              <el-form-item label="备注">
                <el-input v-model="processForm.remark" type="textarea" :rows="2" />
              </el-form-item>
              <el-form-item label="引擎版本">
                <span class="text-muted">
                  {{ currentProcess.camundaDefinitionVersion ?? '未部署' }}
                </span>
              </el-form-item>
            </el-form>
            <el-empty v-else description="未选择流程" :image-size="60" />
          </el-tab-pane>

          <el-tab-pane :label="`校验 (${lintIssues.length})`" name="lint">
            <el-alert
              v-if="!lintIssues.length"
              type="success"
              :closable="false"
              title="未发现建模问题"
              show-icon
            />
            <div v-else class="lint-list">
              <div
                v-for="(issue, i) in lintIssues"
                :key="i"
                class="lint-item"
                :class="`is-${issue.category}`"
                @click="focusElement(issue.elementId)"
              >
                <el-tag size="small" :type="issue.category === 'error' ? 'danger' : 'warning'">
                  {{ issue.category === 'error' ? '错误' : '警告' }}
                </el-tag>
                <span class="lint-msg">{{ issue.message }}</span>
                <span v-if="issue.elementId" class="lint-el">{{ issue.elementId }}</span>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="元素" name="elements">
            <div v-if="selectedElement" class="prop-form">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="类型">{{ selectedElement.type }}</el-descriptions-item>
                <el-descriptions-item label="ID">{{ selectedElement.id }}</el-descriptions-item>
                <el-descriptions-item label="名称">
                  <el-input
                    v-model="elementName"
                    size="small"
                    @change="applyElementName"
                  />
                </el-descriptions-item>
              </el-descriptions>
              <el-button
                v-if="isUserTask"
                size="small"
                type="primary"
                class="mt"
                @click="assignFormToTask"
              >
                绑定 Camunda Form
              </el-button>
              <el-form-item v-if="isUserTask" label="候选组" class="mt-lg">
                <el-input v-model="candidateGroup" size="small" placeholder="reviewer / content_admin / editor" />
              </el-form-item>
              <el-button v-if="isUserTask" size="small" @click="applyCandidateGroup">
                应用候选组
              </el-button>
            </div>
            <el-empty v-else description="在画布中选择一个元素" :image-size="60" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- ============================ Form ============================ -->
    <div v-show="activeTab === 'form'" class="modeler-body">
      <div class="side-panel">
        <div class="panel-title">Camunda Form</div>
        <el-scrollbar class="panel-scroll">
          <div
            v-for="f in forms"
            :key="f.id"
            class="list-item"
            :class="{ active: currentForm?.id === f.id }"
            @click="loadForm(f.id)"
          >
            <div class="li-main">
              <div class="li-name">
                {{ f.name }}
                <el-tag v-if="f.enabled" size="small" type="success" effect="dark">启用</el-tag>
              </div>
              <div class="li-sub">
                {{ FORM_PURPOSE_MAP[f.purpose] || f.purpose }} · v{{ f.version }}
              </div>
              <div class="li-sub mono">{{ f.formId }}</div>
            </div>
            <div class="li-actions" @click.stop>
              <el-tooltip content="删除" placement="top">
                <el-button
                  link
                  size="small"
                  type="danger"
                  :icon="Delete"
                  :disabled="f.enabled"
                  @click="removeForm(f)"
                />
              </el-tooltip>
            </div>
          </div>
          <el-empty v-if="!forms.length" description="暂无表单定义" :image-size="60" />
        </el-scrollbar>
      </div>

      <div class="canvas-wrap">
        <div ref="formHostRef" class="canvas-host form-host" />
        <div v-if="!currentForm" class="canvas-empty">
          <el-empty description="请选择左侧表单，或新建一个表单">
            <el-button type="primary" @click="newForm">新建表单</el-button>
          </el-empty>
        </div>
      </div>
      <div class="side-panel right">
        <div class="panel-title">表单属性</div>
        <el-form v-if="currentForm" label-width="90px" size="small" class="prop-form">
          <el-form-item label="表单名称">
            <el-input v-model="formMeta.name" />
          </el-form-item>
          <el-form-item label="formId">
            <el-input v-model="formMeta.formId" placeholder="与 BPMN 中 formId 一致" />
          </el-form-item>
          <el-form-item label="用途">
            <el-select v-model="formMeta.purpose" style="width: 100%">
              <el-option
                v-for="o in FORM_PURPOSE_OPTIONS"
                :key="o.value"
                :label="o.label"
                :value="o.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="业务类型">
            <el-select v-model="formMeta.entryType" style="width: 100%">
              <el-option label="通用（*）" value="*" />
              <el-option label="民族" value="ethnic" />
              <el-option label="节日" value="festival" />
              <el-option label="艺术" value="art" />
              <el-option label="专题" value="topic" />
            </el-select>
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="formMeta.enabled" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="formMeta.remark" type="textarea" :rows="2" />
          </el-form-item>
          <el-divider content-position="left">预览</el-divider>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="表单将被审核页面用同一渲染器（form-js）渲染，所见即所得"
          />
        </el-form>
        <el-empty v-else description="未选择表单" :image-size="60" />
      </div>
    </div>

    <!-- ============================ 引擎 ============================ -->
    <div v-show="activeTab === 'engine'" class="engine-panel">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span class="card-title">Camunda 7 引擎状态（嵌入式）</span></template>
            <el-descriptions v-if="engine.healthy" :column="1" border size="small">
              <el-descriptions-item label="状态">
                <el-tag type="success" size="small">在线</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="部署模式">
                <el-tag type="info" size="small" effect="plain">嵌入式（随应用进程启动，无外部集群）</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="引擎实例">{{ engine.engineName || 'default' }}</el-descriptions-item>
              <el-descriptions-item label="引擎版本">{{ engine.version }}</el-descriptions-item>
              <el-descriptions-item label="已部署流程定义">{{ engine.processDefinitions ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="运行中实例">{{ engine.runningInstances ?? '-' }}</el-descriptions-item>
              <el-descriptions-item label="待办用户任务">{{ engine.openTasks ?? '-' }}</el-descriptions-item>
            </el-descriptions>
            <el-alert
              v-else
              type="error"
              :closable="false"
              show-icon
              :title="`引擎不可用：${engine.error || '未知错误'}`"
              description="Camunda 7 引擎内嵌于本应用进程，正常情况下不会离线。若出现此提示，请检查应用日志中 ACT_* 引擎表的初始化情况。"
            />
          </el-card>
        </el-col>

        <el-col :span="12">
          <el-card shadow="never">
            <template #header><span class="card-title">引擎上已部署的流程</span></template>
            <el-table :data="engineProcesses" size="small" border>
              <el-table-column prop="processDefinitionId" label="流程标识" min-width="160" />
              <el-table-column prop="name" label="名称" min-width="120" />
              <el-table-column prop="version" label="版本" width="70" />
              <el-table-column prop="processDefinitionKey" label="Key" width="150" />
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 新建流程对话框 -->
    <el-dialog v-model="newProcessVisible" title="新建 BPMN 流程" width="460px">
      <el-form label-width="90px">
        <el-form-item label="流程标识" required>
          <el-input v-model="newProcessForm.processKey" placeholder="ethnic-content-review" />
        </el-form-item>
        <el-form-item label="流程名称" required>
          <el-input v-model="newProcessForm.name" placeholder="民族内容审批流程" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="newProcessForm.entryType" style="width: 100%">
            <el-option label="民族" value="ethnic" />
            <el-option label="节日" value="festival" />
            <el-option label="艺术" value="art" />
            <el-option label="专题" value="topic" />
            <el-option label="通用（*）" value="*" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newProcessVisible = false">取消</el-button>
        <el-button type="primary" @click="createProcess">创建</el-button>
      </template>
    </el-dialog>

    <!-- 新建表单对话框 -->
    <el-dialog v-model="newFormVisible" title="新建 Camunda Form" width="460px">
      <el-form label-width="90px">
        <el-form-item label="formId" required>
          <el-input v-model="newFormForm.formId" placeholder="ethnic-approval-form" />
        </el-form-item>
        <el-form-item label="表单名称" required>
          <el-input v-model="newFormForm.name" placeholder="民族-审核员审批表单" />
        </el-form-item>
        <el-form-item label="用途">
          <el-select v-model="newFormForm.purpose" style="width: 100%">
            <el-option
              v-for="o in FORM_PURPOSE_OPTIONS"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newFormVisible = false">取消</el-button>
        <el-button type="primary" @click="createForm">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Check,
  CopyDocument,
  Delete,
  DocumentAdd,
  FolderOpened,
  Refresh,
  Select,
  Upload
} from '@element-plus/icons-vue'

// bpmn-js：与 Camunda Modeler 同源的建模器，产出可直接部署到嵌入式 Camunda 7 引擎的 BPMN
import BpmnModeler from 'bpmn-js/lib/Modeler'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'

// bpmnlint：在浏览器里做建模规范校验（缺 bpmn:process id / 没有可执行流程等）
import lintModule from 'bpmn-js-bpmnlint'
import bpmnlintConfig from 'bpmnlint/config/recommended'
import 'bpmn-js-bpmnlint/dist/assets/css/bpmn-js-bpmnlint.css'

// form-js 设计器：产出 form-js schema（Camunda Form），与审核页面同一个渲染器
import { FormEditor } from '@bpmn-io/form-js-editor'
// 注意：编辑器包只导出 editor / editor-base / dragula / properties-panel 四份样式，
// 基础表单控件样式需从 viewer 包引入（两边共用同一套 fjs-* class）
import '@bpmn-io/form-js-editor/dist/assets/form-js-editor.css'
import '@bpmn-io/form-js-editor/dist/assets/form-js-editor-base.css'
import '@bpmn-io/form-js-editor/dist/assets/dragula.css'
import '@bpmn-io/form-js-editor/dist/assets/properties-panel.css'
import '@bpmn-io/form-js-viewer/dist/assets/form-js.css'
import '@bpmn-io/form-js-viewer/dist/assets/form-js-base.css'

import {
  deployFormBinding,
  deployProcessBinding,
  duplicateProcessBinding,
  getEngineTopology,
  getFormBinding,
  getFormTemplate,
  getProcessBinding,
  getProcessTemplate,
  listEngineProcessDefinitions,
  listFormBindings,
  listProcessBindings,
  redeployBuiltin as redeployBuiltinApi,
  saveFormBinding,
  saveProcessBinding,
  updateFormBinding,
  updateProcessBinding,
  deleteProcessBinding,
  deleteFormBinding,
  type EngineTopology,
  type FormBinding,
  type ProcessBinding
} from '@/api/modules/workflow'
import { FORM_PURPOSE_MAP, FORM_PURPOSE_OPTIONS } from '@/constants'

type Tab = 'bpmn' | 'form' | 'engine'
const activeTab = ref<Tab>('bpmn')

const loadingEngine = ref(false)
const saving = ref(false)
const deploying = ref(false)

const engine = ref<EngineTopology>({ healthy: false })
const engineProcesses = ref<
  { processDefinitionId: string; name?: string; version: number; processDefinitionKey: number }[]
>([])

// -------------------------------------------------------------- BPMN 状态
const bpmnHostRef = ref<HTMLElement | null>(null)
const processes = ref<ProcessBinding[]>([])
const currentProcess = ref<ProcessBinding | null>(null)
const bpmnDirty = ref(false)
const bpmnRightTab = ref('props')
const lintIssues = ref<{ category: string; message: string; elementId?: string }[]>([])
const selectedElement = ref<{ id: string; type: string; businessObject?: { name?: string } } | null>(null)
const elementName = ref('')
const candidateGroup = ref('')

const processForm = reactive({
  name: '',
  processKey: '',
  entryType: 'ethnic',
  remark: ''
})

let bpmnModeler: BpmnModeler | null = null

// -------------------------------------------------------------- Form 状态
const formHostRef = ref<HTMLElement | null>(null)
const forms = ref<FormBinding[]>([])
const currentForm = ref<FormBinding | null>(null)

const formMeta = reactive({
  name: '',
  formId: '',
  purpose: 'approval',
  entryType: '*',
  enabled: true,
  remark: ''
})

let formEditor: FormEditor | null = null

// -------------------------------------------------------------- 对话框
const newProcessVisible = ref(false)
const newProcessForm = reactive({
  processKey: 'custom-content-review',
  name: '内容审批流程',
  entryType: 'ethnic'
})
const newFormVisible = ref(false)
const newFormForm = reactive({
  formId: 'custom-approval-form',
  name: '自定义审批表单',
  purpose: 'approval'
})

const isUserTask = ref(false)

// ============================================================================
// BPMN Modeler
// ============================================================================

/** 初始化 bpmn-js 建模器（含 lint 与属性监听） */
async function initBpmnModeler() {
  if (!bpmnHostRef.value || bpmnModeler) return

  bpmnModeler = new BpmnModeler({
    container: bpmnHostRef.value,
    // 目标平台为 Camunda 7（嵌入式引擎）：用户任务的分配属性是 camunda:candidateGroups /
    // camunda:assignee，服务任务用 camunda:class 指向 JavaDelegate。
    // 注意：bpmn-js 18 起键盘绑定是隐式的，传 keyboard.bindTo 会告警且被忽略。
    additionalModules: [lintModule]
  })

  const eventBus = bpmnModeler.get('eventBus') as {
    on: (e: string, fn: (arg: unknown) => void) => void
  }

  // 任何建模动作都标记为「未保存」，避免误关丢改动
  eventBus.on('commandStack.changed', () => {
    bpmnDirty.value = true
    runLint()
  })

  eventBus.on('selection.changed', (e: unknown) => {
    const evt = e as { newSelection?: unknown[] }
    const el = (evt.newSelection || [])[0] as
      | { id: string; type: string; businessObject?: { name?: string; get?: (k: string) => unknown } }
      | undefined
    if (!el || el.id === undefined) {
      selectedElement.value = null
      isUserTask.value = false
      return
    }
    selectedElement.value = { id: el.id, type: el.type, businessObject: el.businessObject }
    elementName.value = el.businessObject?.name || ''
    isUserTask.value = el.type === 'bpmn:UserTask'
    if (isUserTask.value) {
      const bo = el.businessObject as { get?: (k: string) => unknown } | undefined
      // 优先读 Camunda 7 属性；同时兼容早期用 Camunda 8 建模保存的历史流程（zeebe:*），
      // 这样老流程打开后仍能显示候选组，保存时会自动改写成 camunda:*。
      const groups = bo?.get?.('camunda:candidateGroups') ?? bo?.get?.('zeebe:candidateGroups')
      candidateGroup.value = typeof groups === 'string' ? groups : ''
    }
  })
}

/** 载入 BPMN XML 到画布 */
async function importBpmn(xml: string) {
  if (!bpmnModeler) return
  try {
    await bpmnModeler.importXML(xml)
    bpmnDirty.value = false
    const canvas = bpmnModeler.get('canvas') as { zoom: (v: string) => void }
    canvas.zoom('fit-viewport')
    runLint()
  } catch (e) {
    ElMessage.error(`BPMN 载入失败：${(e as Error).message}`)
  }
}

/** 读取画布上的 BPMN XML */
async function getBpmnXml(): Promise<string> {
  if (!bpmnModeler) return ''
  const { xml } = await bpmnModeler.saveXML({ format: true })
  return xml || ''
}

/** 运行 bpmnlint 校验，结果展示在右侧「校验」面板 */
function runLint() {
  if (!bpmnModeler) return
  try {
    const linting = bpmnModeler.get('linting') as {
      lint: () => Promise<{ rule: string; category: string; message: string; elementId?: string }[]>
    }
    linting
      .lint()
      .then((reports) => {
        lintIssues.value = reports.map((r) => ({
          category: r.category === 'error' ? 'error' : 'warn',
          message: r.message,
          elementId: r.elementId
        }))
      })
      .catch(() => {
        lintIssues.value = []
      })
  } catch {
    lintIssues.value = []
  }
}

/** 点击校验项时定位到对应元素 */
function focusElement(elementId?: string) {
  if (!elementId || !bpmnModeler) return
  try {
    const canvas = bpmnModeler.get('canvas') as { scrollToElement?: (el: unknown) => void }
    const registry = bpmnModeler.get('elementRegistry') as {
      get: (id: string) => unknown
    }
    const selection = bpmnModeler.get('selection') as { select: (el: unknown) => void }
    const el = registry.get(elementId)
    if (el) {
      selection.select(el)
      canvas.scrollToElement?.(el)
    }
  } catch {
    /* 忽略定位失败 */
  }
}

function markBpmnDirty() {
  bpmnDirty.value = true
}

function applyElementName() {
  if (!bpmnModeler || !selectedElement.value) return
  try {
    const modeling = bpmnModeler.get('modeling') as {
      updateProperties: (el: unknown, props: Record<string, unknown>) => void
    }
    const registry = bpmnModeler.get('elementRegistry') as { get: (id: string) => unknown }
    const el = registry.get(selectedElement.value.id)
    if (el) {
      modeling.updateProperties(el, { name: elementName.value })
    }
  } catch {
    /* 忽略 */
  }
}

/**
 * 用户任务与 Camunda Form 的绑定说明（Camunda 7 嵌入式模式）。
 *
 * Camunda 8 时代表单是引擎资源，需要在 userTask 上写 zeebe:formDefinition formId；
 * Camunda 7 嵌入式模式下，表单 schema 存在本地 form_binding 表、由前端 form-js 渲染，
 * 后端按「当前环节（stage）」查对应 purpose 的表单
 * （见 WorkflowServiceImpl#formPurposeOfStage：审批=approval / 审查=inspection / 修改=revision）。
 *
 * 因此这里不再往 BPMN 里写表单引用 —— 只要用户任务的 **elementId** 是下列约定值之一，
 * 审核页就会自动加载对应表单。给出提示即可，避免写入引擎不认的 zeebe 扩展。
 */
function assignFormToTask() {
  const purposeMap: Record<string, { purpose: string; formId: string }> = {
    Task_ReviewerApprove: { purpose: '审批', formId: 'ethnic-approval-form' },
    Task_ContentInspect: { purpose: '审查', formId: 'ethnic-inspection-form' },
    Task_EditorRevise: { purpose: '修改', formId: 'ethnic-revision-form' }
  }
  const id = selectedElement.value?.id || ''
  const hit = purposeMap[id]
  ElMessageBox.alert(
    hit
      ? `该任务（${id}）已按 elementId 自动关联「${hit.purpose}」表单：${hit.formId}。\n\n` +
        'Camunda 7 嵌入式模式下表单由本地表单库按环节提供，无需在 BPMN 中绑定 formId；' +
        '如需修改表单内容，请到「Camunda Form」页签编辑对应表单。'
      : `当前任务 elementId 为 ${id || '（未选中）'}，不是内置审批流程的约定环节，` +
        '因此不会自动加载表单。内置流程的约定 elementId 为：\n' +
        '· Task_ReviewerApprove → 审批表单\n' +
        '· Task_ContentInspect → 审查表单\n' +
        '· Task_EditorRevise → 修改表单',
    '表单绑定说明',
    { confirmButtonText: '知道了' }
  ).catch(() => {
    /* 用户关闭 */
  })
}

/** 设置用户任务的候选组（决定哪个角色能看到待办） */
function applyCandidateGroup() {
  if (!bpmnModeler || !selectedElement.value) return
  try {
    const modeling = bpmnModeler.get('modeling') as {
      updateProperties: (el: unknown, props: Record<string, unknown>) => void
    }
    const registry = bpmnModeler.get('elementRegistry') as { get: (id: string) => unknown }
    const el = registry.get(selectedElement.value.id)
    if (el) {
      // Camunda 7 用 camunda:candidateGroups（Camunda 8 的 zeebe:candidateGroups 会被引擎忽略，
      // 导致待办找不到处理人）。同时清掉可能残留的 zeebe 属性，避免两套配置并存造成困惑。
      modeling.updateProperties(el, {
        'camunda:candidateGroups': candidateGroup.value,
        'zeebe:candidateGroups': undefined
      })
      ElMessage.success('已应用候选组（camunda:candidateGroups）')
    }
  } catch {
    /* 忽略 */
  }
}

// ============================================================================
// Form Editor
// ============================================================================

/** 初始化 form-js 设计器 */
function initFormEditor() {
  if (!formHostRef.value || formEditor) return
  formEditor = new FormEditor({ container: formHostRef.value })
}

/** 载入 form-js schema 到设计器 */
async function importFormSchema(schemaJson: string) {
  // 表单页签由 v-show 控制显示：切换瞬间容器可能还是 display:none，
  // 此时 form-js 计算不出画布尺寸、渲染会失败。等 DOM 更新 + 一帧后再初始化。
  await nextTick()
  initFormEditor()
  if (!formEditor) return
  try {
    const schema = JSON.parse(schemaJson)
    // form-js 设计器要求 schema 顶层带 id（部署到 Camunda 时同样必需）
    if (!schema.id && formMeta.formId) {
      schema.id = formMeta.formId
    }
    if (!schema.type) schema.type = 'default'
    await formEditor.importSchema(schema)
    // 容器从 display:none 切到可见后，editor 内部布局需要重算一次
    await nextTick()
    try {
      const eventBus = (formEditor as unknown as { get?: (n: string) => unknown }).get?.('eventBus')
      ;(eventBus as { fire?: (e: string) => void } | undefined)?.fire?.('form.layoutChanged')
    } catch {
      /* 不同版本 API 差异，忽略 */
    }
  } catch (e) {
    ElMessage.error(`表单载入失败：${(e as Error).message}`)
  }
}

/** 读取设计器中的 schema（并补全 id / type，保证可直接部署） */
function getFormSchema(): string {
  if (!formEditor) return ''
  const schema = formEditor.saveSchema() as Record<string, unknown>
  if (!schema.id) schema.id = formMeta.formId
  if (!schema.type) schema.type = 'default'
  if (!schema.schemaVersion) schema.schemaVersion = 19
  return JSON.stringify(schema, null, 2)
}

// ============================================================================
// 数据加载
// ============================================================================

/** 引擎拓扑：取不到时给出「离线」默认值，避免模板里读 undefined.healthy 导致整页渲染崩溃 */
async function loadEngine() {
  loadingEngine.value = true
  try {
    engine.value = await getEngineTopology()
  } catch {
    engine.value = { healthy: false, error: '无法连接 Camunda 引擎' }
  }
  try {
    engineProcesses.value = ((await listEngineProcessDefinitions()) || []) as typeof engineProcesses.value
  } catch {
    engineProcesses.value = []
  } finally {
    loadingEngine.value = false
  }
}

async function loadProcesses() {
  try {
    processes.value = (await listProcessBindings()) || []
  } catch {
    processes.value = []
  }
}

async function loadForms() {
  try {
    forms.value = (await listFormBindings()) || []
  } catch {
    forms.value = []
  }
}

async function loadProcess(id: string) {
  if (bpmnDirty.value) {
    try {
      await ElMessageBox.confirm('当前流程有未保存的改动，切换将丢失。确定继续？', '提示', {
        type: 'warning'
      })
    } catch {
      return
    }
  }
  const binding = await getProcessBinding(id)
  currentProcess.value = binding
  processForm.name = binding.name
  processForm.processKey = binding.processKey
  processForm.entryType = binding.entryType
  processForm.remark = binding.remark || ''

  await nextTick()
  await initBpmnModeler()
  await importBpmn(binding.bpmnXml)
  selectedElement.value = null
  isUserTask.value = false
}

async function loadForm(id: string) {
  const binding = await getFormBinding(id)
  currentForm.value = binding
  formMeta.name = binding.name
  formMeta.formId = binding.formId
  formMeta.purpose = binding.purpose
  formMeta.entryType = binding.entryType
  formMeta.enabled = binding.enabled
  formMeta.remark = binding.remark || ''

  await nextTick()
  await importFormSchema(binding.schemaJson)
}

// ============================================================================
// 新建 / 保存 / 部署
// ============================================================================

function newProcess() {
  newProcessForm.processKey = 'custom-content-review'
  newProcessForm.name = '内容审批流程'
  newProcessForm.entryType = 'ethnic'
  newProcessVisible.value = true
}

async function createProcess() {
  if (!newProcessForm.processKey.trim() || !newProcessForm.name.trim()) {
    ElMessage.warning('请填写流程标识与名称')
    return
  }
  // 用后端模板生成一份合法的、可部署的最小 BPMN
  const template = await getProcessTemplate({
    processKey: newProcessForm.processKey.trim(),
    processName: newProcessForm.name.trim(),
    entryType: newProcessForm.entryType
  })
  const created = await saveProcessBinding({
    processKey: newProcessForm.processKey.trim(),
    name: newProcessForm.name.trim(),
    entryType: newProcessForm.entryType,
    bpmnXml: template,
    enabled: false,
    remark: '在 OA 内置 Modeler 中创建'
  })
  newProcessVisible.value = false
  ElMessage.success('已创建流程定义（未启用），可在画布中编辑后保存并部署')
  await loadProcesses()
  await loadProcess(created.id)
}

async function saveProcess() {
  if (!currentProcess.value || !bpmnModeler) return
  const xml = await getBpmnXml()
  saving.value = true
  try {
    const updated = await updateProcessBinding(currentProcess.value.id, {
      name: processForm.name,
      entryType: processForm.entryType,
      bpmnXml: xml,
      remark: processForm.remark
    })
    currentProcess.value = updated
    bpmnDirty.value = false
    ElMessage.success('已保存')
    await loadProcesses()
  } finally {
    saving.value = false
  }
}

async function deployProcess() {
  if (!currentProcess.value) return
  deploying.value = true
  try {
    if (bpmnDirty.value) {
      await saveProcess()
    }
    const updated = await deployProcessBinding(currentProcess.value.id)
    currentProcess.value = updated
    ElMessage.success(`部署成功：引擎版本 v${updated.camundaDefinitionVersion}`)
    await Promise.all([loadProcesses(), loadEngine()])
  } catch (e) {
    // 引擎的校验错误已在拦截器中弹出，这里不再重复
    void e
  } finally {
    deploying.value = false
  }
}

async function toggleProcessEnabled(p: ProcessBinding) {
  try {
    await updateProcessBinding(p.id, { enabled: !p.enabled })
    ElMessage.success(p.enabled ? '已停用' : '已启用（同业务类型的其他版本已自动停用）')
    await loadProcesses()
  } catch {
    /* 拦截器已提示 */
  }
}

async function duplicateProcess(id: string) {
  const copy = await duplicateProcessBinding(id)
  ElMessage.success(`已另存为 v${copy.version}`)
  await loadProcesses()
  await loadProcess(copy.id)
}

async function removeProcess(p: ProcessBinding) {
  await ElMessageBox.confirm(`确定删除流程「${p.name}」v${p.version}？`, '删除确认', {
    type: 'warning'
  })
  await deleteProcessBinding(p.id)
  ElMessage.success('已删除')
  if (currentProcess.value?.id === p.id) {
    currentProcess.value = null
  }
  await loadProcesses()
}

function newForm() {
  newFormForm.formId = 'custom-approval-form'
  newFormForm.name = '自定义审批表单'
  newFormForm.purpose = 'approval'
  newFormVisible.value = true
}

async function createForm() {
  if (!newFormForm.formId.trim() || !newFormForm.name.trim()) {
    ElMessage.warning('请填写 formId 与名称')
    return
  }
  const template = await getFormTemplate(newFormForm.purpose, newFormForm.formId.trim())
  const created = await saveFormBinding({
    formId: newFormForm.formId.trim(),
    name: newFormForm.name.trim(),
    purpose: newFormForm.purpose,
    entryType: '*',
    schemaJson: template,
    enabled: false,
    remark: '在 OA 内置表单设计器中创建'
  })
  newFormVisible.value = false
  ElMessage.success('已创建表单（未启用），可在设计器中编辑后保存并部署')
  await loadForms()
  await loadForm(created.id)
}

async function saveForm() {
  if (!currentForm.value || !formEditor) return
  const schemaJson = getFormSchema()
  saving.value = true
  try {
    const updated = await updateFormBinding(currentForm.value.id, {
      name: formMeta.name,
      purpose: formMeta.purpose,
      entryType: formMeta.entryType,
      schemaJson,
      enabled: formMeta.enabled,
      remark: formMeta.remark
    })
    currentForm.value = updated
    ElMessage.success('已保存')
    await loadForms()
  } finally {
    saving.value = false
  }
}

async function deployForm() {
  if (!currentForm.value) return
  deploying.value = true
  try {
    await saveForm()
    await deployFormBinding(currentForm.value.id)
    // Camunda 7 嵌入式模式下表单是本地资源（form_binding + 前端 form-js 渲染），
    // 后端按环节提供，所以「部署」等价于「保存并立即生效」——
    // 不再像 Camunda 8 那样需要把 .form 资源部署到远端引擎。
    ElMessage.success('表单已生效（嵌入式模式：本地表单库，审批页面立即使用新版）')
  } catch (e) {
    void e
  } finally {
    deploying.value = false
  }
}

async function removeForm(f: FormBinding) {
  await ElMessageBox.confirm(`确定删除表单「${f.name}」v${f.version}？`, '删除确认', {
    type: 'warning'
  })
  await deleteFormBinding(f.id)
  ElMessage.success('已删除')
  if (currentForm.value?.id === f.id) {
    currentForm.value = null
  }
  await loadForms()
}

async function redeployBuiltin() {
  deploying.value = true
  try {
    const result = await redeployBuiltinApi()
    const failures = (result?.failures as string[] | undefined) || []
    if (failures.length) {
      ElMessage.warning(`部署完成，但有 ${failures.length} 项失败：${failures.join('; ')}`)
    } else {
      ElMessage.success('内置流程与表单已重新部署到 Camunda')
    }
    await Promise.all([loadProcesses(), loadForms(), loadEngine()])
  } finally {
    deploying.value = false
  }
}

// ============================================================================
// 生命周期
// ============================================================================

watch(activeTab, async (tab) => {
  await nextTick()
  if (tab === 'bpmn' && !bpmnModeler) {
    await initBpmnModeler()
    if (currentProcess.value) {
      await importBpmn(currentProcess.value.bpmnXml)
    }
  } else if (tab === 'form') {
    // 表单容器在切过来之前是 display:none，必须等可见后再初始化/重绘，
    // 否则 form-js 量不出高度、画布为空。
    await importFormSchema(
      currentForm.value?.schemaJson || (await buildBlankSchema(formMeta.purpose))
    )
    if (!currentForm.value && forms.value.length) {
      await loadForm(forms.value[0].id)
    }
  } else if (tab === 'engine') {
    await loadEngine()
  }
})

/** 构造一份空白表单 schema（无选中表单时也能打开设计器） */
async function buildBlankSchema(purpose: string): Promise<string> {
  try {
    return await getFormTemplate(purpose || 'approval', formMeta.formId || '')
  } catch {
    return JSON.stringify({
      id: formMeta.formId || 'custom-form',
      schemaVersion: 19,
      type: 'default',
      components: []
    })
  }
}

onMounted(async () => {
  await Promise.all([loadProcesses(), loadForms(), loadEngine()])
  await nextTick()
  await initBpmnModeler()
  // 默认打开业务上最重要的流程：民族内容审批
  const preferred =
    processes.value.find((p) => p.processKey === 'ethnic-content-review' && p.enabled) ||
    processes.value[0]
  if (preferred) {
    await loadProcess(preferred.id)
  }
  // 表单页签同理：预选「审核员审批」表单，切过去就有内容可编辑
  const preferredForm =
    forms.value.find((f) => f.formId === 'ethnic-approval-form' && f.enabled) || forms.value[0]
  if (preferredForm) {
    await loadForm(preferredForm.id)
  }
})

onBeforeUnmount(() => {
  try {
    bpmnModeler?.destroy()
  } catch {
    /* 忽略 */
  }
  try {
    formEditor?.destroy()
  } catch {
    /* 忽略 */
  }
})
</script>

<style scoped lang="scss">
.modeler-page {
  display: flex;
  flex-direction: column;
  // 建模区需要占满可用高度：减去布局 header(56) 与 main padding
  height: calc(100vh - 88px);
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
}

.modeler-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafbfc;
  flex-shrink: 0;

  .tb-left,
  .tb-right {
    display: flex;
    align-items: center;
    gap: 8px;

    .ml {
      margin-left: 6px;
    }
  }
}

.modeler-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.side-panel {
  width: 260px;
  flex-shrink: 0;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  background: #fcfcfd;

  &.right {
    width: 300px;
    border-right: none;
    border-left: 1px solid #e5e7eb;
    overflow-y: auto;
  }

  .panel-title {
    padding: 10px 12px;
    font-size: 13px;
    font-weight: 600;
    color: #374151;
    border-bottom: 1px solid #eef0f3;
  }

  .panel-scroll {
    flex: 1;
    min-height: 0;
  }
}

.list-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 6px;
  padding: 10px 12px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: #f5f8ff;
  }

  &.active {
    background: #eef4ff;
    border-left: 3px solid #409eff;
    padding-left: 9px;
  }

  .li-main {
    min-width: 0;
    flex: 1;
  }

  .li-name {
    font-size: 13px;
    color: #111827;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .li-sub {
    font-size: 12px;
    color: #6b7280;
    margin-top: 2px;
    word-break: break-all;

    &.mono {
      font-family: monospace;
      color: #9ca3af;
    }

    .li-engine {
      color: #16a34a;
      margin-left: 6px;
    }

    .li-warn {
      color: #d97706;
      margin-left: 6px;
    }
  }

  .li-actions {
    display: flex;
    align-items: center;
    flex-shrink: 0;
  }
}

.canvas-wrap {
  flex: 1;
  min-width: 0;
  position: relative;
  background: #fff;

  .canvas-host {
    width: 100%;
    height: 100%;

    &.form-host {
      padding: 12px;
      overflow: auto;
    }
  }

  .canvas-empty {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fff;
    z-index: 5;
  }
}

.right-tabs {
  padding: 0 12px;

  :deep(.el-tabs__header) {
    margin-bottom: 10px;
  }
}

.prop-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;
  }
}

.lint-list {
  .lint-item {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    padding: 8px;
    border-radius: 4px;
    background: #fffbeb;
    margin-bottom: 6px;
    cursor: pointer;
    font-size: 12px;

    &.is-error {
      background: #fef2f2;
    }

    &:hover {
      filter: brightness(0.97);
    }

    .lint-msg {
      flex: 1;
      color: #374151;
    }

    .lint-el {
      font-family: monospace;
      color: #9ca3af;
    }
  }
}

.engine-panel {
  flex: 1;
  overflow-y: auto;
  padding: 16px;

  .card-title {
    font-weight: 600;
  }
}

.text-muted {
  color: #9ca3af;
}

.mt {
  margin-top: 10px;
}

.mt-lg {
  margin-top: 16px;
}

// bpmn-js / form-js 画布容器高度修正
:deep(.djs-container) {
  outline: none;
}

:deep(.fjs-editor-container) {
  height: 100%;
}
</style>

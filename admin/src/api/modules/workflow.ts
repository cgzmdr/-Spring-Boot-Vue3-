import { del, get, post, put } from '../request'

/** 内容类型 */
export type WorkflowEntryType = 'ethnic' | 'festival' | 'art' | 'topic'

/** 当前环节 */
export type WorkflowStage =
  | 'pending_review' // 待审核员审批
  | 'pending_inspect' // 待内容管理员审查
  | 'revising' // 待内容编辑修改
  | 'published' // 已上线（审查通过，本轮结束）
  | 'stopped' // 已终止

/** 实例状态 */
export type WorkflowInstanceStatus = 'running' | 'completed' | 'withdrawn' | 'superseded'

/** 意见环节 */
export type OpinionStage = 'submit' | 'approve' | 'inspect' | 'offline' | 'revise' | 'publish'

/** 意见结论 */
export type OpinionDecision = 'submitted' | 'approved' | 'rejected' | 'issue' | 'resolved'

/** 审批 / 审查意见 */
export interface WorkflowOpinion {
  id: string
  instanceId: string
  entryType: string
  entryId: string
  contentVersion: number
  stage: OpinionStage
  stageLabel: string
  decision?: OpinionDecision | null
  decisionLabel?: string | null
  opinion?: string | null
  operatorId?: string | null
  operatorName?: string | null
  operatorRole?: string | null
  /** Camunda 7 用户任务 ID（字符串；Camunda 8 时代为数字 key） */
  camundaTaskKey?: string | null
  taskName?: string | null
  createdAt?: string
}

/** 工作流实例 */
export interface WorkflowInstance {
  id: string
  entryType: string
  entryId: string
  entryTitle?: string | null
  contentVersion: number
  processDefinitionId?: string | null
  processDefinitionVersion?: number | null
  /** Camunda 7 流程实例 ID（字符串，形如 ethnic-content-review:<版本>:<uuid>） */
  processInstanceKey?: string | null
  businessKey?: string | null
  status: WorkflowInstanceStatus
  statusLabel: string
  currentStage: WorkflowStage
  currentStageLabel: string
  submitterId?: string | null
  submitterName?: string | null
  currentAssigneeId?: string | null
  /** Camunda 7 当前用户任务 ID（字符串） */
  currentTaskKey?: string | null
  currentTaskName?: string | null
  roundNo?: number | null
  startedAt?: string
  finishedAt?: string | null
  updatedAt?: string
}

/** 待办任务 */
export interface WorkflowTask {
  instanceId: string
  entryType: string
  entryTypeLabel: string
  entryId: string
  entryTitle?: string | null
  contentVersion: number
  status: WorkflowInstanceStatus
  currentStage: WorkflowStage
  currentStageLabel: string
  /** Camunda 7 当前用户任务 ID（字符串） */
  taskKey?: string | null
  taskName?: string | null
  elementId?: string | null
  processDefinitionId?: string | null
  processDefinitionVersion?: number | null
  /** Camunda 7 流程实例 ID（字符串） */
  processInstanceKey?: string | null
  submitterName?: string | null
  startedAt?: string
  updatedAt?: string
  /** 当前环节的 Camunda Form schema（JSON 字符串） */
  formSchema?: string | null
  formId?: string | null
  /** 当前登录人是否可处理 */
  actionable: boolean
  /** 该内容全部历史意见（含前序所有版本） */
  opinions: WorkflowOpinion[]
}

/** 内容审批全过程 */
export interface WorkflowTimeline {
  entryType: string
  entryId: string
  entryTitle?: string | null
  contentStatus?: string | null
  contentVersion: number
  instances: WorkflowInstance[]
  opinions: WorkflowOpinion[]
  activeInstance?: WorkflowInstance | null
  queriedAt?: string
}

/** BPMN 流程定义 */
export interface ProcessBinding {
  id: string
  processKey: string
  name: string
  entryType: string
  version: number
  bpmnXml: string
  camundaDefinitionId?: string | null
  camundaDefinitionVersion?: number | null
  enabled: boolean
  remark?: string | null
  createdAt?: string
  updatedAt?: string
}

/** Camunda Form 定义 */
export interface FormBinding {
  id: string
  formId: string
  name: string
  purpose: 'approval' | 'inspection' | 'revision' | string
  entryType: string
  version: number
  schemaJson: string
  enabled: boolean
  remark?: string | null
  createdAt?: string
  updatedAt?: string
}

/**
 * Camunda 引擎状态。
 *
 * <p>Camunda 8 时代这是「集群拓扑」（gatewayVersion / clusterSize / partitions /
 * brokers）；迁移到 Camunda 7 嵌入式引擎后，引擎就在本进程内、没有网关与分区概念，
 * 因此改为返回引擎实例信息与实时计数。</p>
 */
export interface EngineTopology {
  healthy: boolean
  /** 固定为 embedded（引擎内嵌于应用进程） */
  mode?: string
  engineName?: string
  version?: string
  processDefinitions?: number
  runningInstances?: number
  openTasks?: number
  error?: string
}

export interface ProcessBindingSaveRequest {
  processKey?: string
  name?: string
  entryType?: string
  bpmnXml?: string
  enabled?: boolean
  remark?: string
}

export interface FormBindingSaveRequest {
  formId?: string
  name?: string
  purpose?: string
  entryType?: string
  schemaJson?: string
  enabled?: boolean
  remark?: string
}

// ============================================================ 待办与查询

/** 我的待办 */
export function listWorkflowTasks(params: {
  stage?: WorkflowStage | ''
  entryType?: WorkflowEntryType | ''
  mineOnly?: boolean
}): Promise<WorkflowTask[]> {
  return get<WorkflowTask[]>('/api/workflow/tasks', params as Record<string, unknown>)
}

/** 待办任务详情 */
export function getWorkflowTask(instanceId: string): Promise<WorkflowTask> {
  return get<WorkflowTask>(`/api/workflow/instances/${instanceId}`)
}

/** 按内容查当前活跃待办 */
export function getActiveTask(entryType: string, entryId: string): Promise<WorkflowTask | null> {
  return get<WorkflowTask | null>(`/api/workflow/entries/${entryType}/${entryId}/task`)
}

/** 内容审批全过程（含逐版本实例与全量意见） */
export function getWorkflowTimeline(entryType: string, entryId: string): Promise<WorkflowTimeline> {
  return get<WorkflowTimeline>(`/api/workflow/entries/${entryType}/${entryId}/timeline`)
}

/** 审批/审查意见列表 */
export function listWorkflowOpinions(entryType: string, entryId: string): Promise<WorkflowOpinion[]> {
  return get<WorkflowOpinion[]>(`/api/workflow/entries/${entryType}/${entryId}/opinions`)
}

// ============================================================ 环节动作

/** 提交审批 */
export function submitForReview(
  entryType: string,
  entryId: string,
  note?: string
): Promise<WorkflowInstance> {
  return post<WorkflowInstance>(`/api/workflow/entries/${entryType}/${entryId}/submit`, { note })
}

/** 通用完成当前环节 */
export function completeWorkflowTask(
  instanceId: string,
  body: {
    decision?: string
    opinion?: string
    rejectReason?: string
    variables?: Record<string, unknown>
  }
): Promise<void> {
  return post<void>(`/api/workflow/instances/${instanceId}/complete`, body)
}

/** 审核员审批通过（必填审批意见） */
export function approveWorkflowTask(instanceId: string, opinion: string): Promise<void> {
  return post<void>(`/api/workflow/instances/${instanceId}/approve`, { opinion })
}

/** 审核员退回（必填审批意见） */
export function rejectWorkflowTask(instanceId: string, opinion: string): Promise<void> {
  return post<void>(`/api/workflow/instances/${instanceId}/reject`, { opinion, rejectReason: opinion })
}

/** 内容管理员审查通过（内容保持在线） */
export function inspectPass(instanceId: string, opinion: string): Promise<void> {
  return post<void>(`/api/workflow/instances/${instanceId}/inspect/pass`, { opinion })
}

/** 内容管理员审查发现问题并暂时下线 */
export function inspectIssue(instanceId: string, reason: string): Promise<void> {
  return post<void>(`/api/workflow/instances/${instanceId}/inspect/issue`, { reason })
}

/** 内容编辑修改完成，重新提交审批 */
export function reviseAndResubmit(
  instanceId: string,
  revisionNote: string,
  needReapproval = true
): Promise<void> {
  return post<void>(`/api/workflow/instances/${instanceId}/revise`, {
    opinion: revisionNote,
    variables: { needReapproval }
  })
}

/** 撤回提交 */
export function withdrawWorkflow(instanceId: string, reason?: string): Promise<void> {
  return post<void>(`/api/workflow/instances/${instanceId}/withdraw`, { reason })
}

// ============================================================ Modeler：流程定义

export function listProcessBindings(entryType?: string): Promise<ProcessBinding[]> {
  return get<ProcessBinding[]>('/api/workflow/processes', entryType ? { entryType } : undefined)
}

export function getProcessBinding(id: string): Promise<ProcessBinding> {
  return get<ProcessBinding>(`/api/workflow/processes/${id}`)
}

export function saveProcessBinding(data: ProcessBindingSaveRequest): Promise<ProcessBinding> {
  return post<ProcessBinding>('/api/workflow/processes', data)
}

export function updateProcessBinding(
  id: string,
  data: ProcessBindingSaveRequest
): Promise<ProcessBinding> {
  return put<ProcessBinding>(`/api/workflow/processes/${id}`, data)
}

export function deleteProcessBinding(id: string): Promise<void> {
  return del<void>(`/api/workflow/processes/${id}`)
}

export function deployProcessBinding(id: string): Promise<ProcessBinding> {
  return post<ProcessBinding>(`/api/workflow/processes/${id}/deploy`)
}

export function duplicateProcessBinding(id: string): Promise<ProcessBinding> {
  return post<ProcessBinding>(`/api/workflow/processes/${id}/duplicate`)
}

export function getProcessTemplate(params: {
  processKey?: string
  processName?: string
  entryType?: string
}): Promise<string> {
  return get<string>('/api/workflow/processes/template', params as Record<string, unknown>)
}

// ============================================================ Modeler：Camunda Form

export function listFormBindings(params?: {
  purpose?: string
  entryType?: string
}): Promise<FormBinding[]> {
  return get<FormBinding[]>('/api/workflow/forms', params as Record<string, unknown>)
}

export function getFormBinding(id: string): Promise<FormBinding> {
  return get<FormBinding>(`/api/workflow/forms/${id}`)
}

export function getFormByFormId(formId: string): Promise<FormBinding> {
  return get<FormBinding>(`/api/workflow/forms/by-form-id/${formId}`)
}

export function saveFormBinding(data: FormBindingSaveRequest): Promise<FormBinding> {
  return post<FormBinding>('/api/workflow/forms', data)
}

export function updateFormBinding(id: string, data: FormBindingSaveRequest): Promise<FormBinding> {
  return put<FormBinding>(`/api/workflow/forms/${id}`, data)
}

export function deleteFormBinding(id: string): Promise<void> {
  return del<void>(`/api/workflow/forms/${id}`)
}

/** 把表单部署到 Camunda 引擎（流程引用的表单必须已部署） */
export function deployFormBinding(id: string): Promise<Record<string, unknown>> {
  return post<Record<string, unknown>>(`/api/workflow/forms/${id}/deploy`)
}

export function getFormTemplate(purpose: string, formId?: string): Promise<string> {
  return get<string>('/api/workflow/forms/template', { purpose, formId })
}

// ============================================================ 引擎信息

export function getEngineTopology(): Promise<EngineTopology> {
  return get<EngineTopology>('/api/workflow/engine/topology')
}

export function listEngineProcessDefinitions(): Promise<
  { processDefinitionId: string; processDefinitionKey: number; name?: string; version: number }[]
> {
  return get('/api/workflow/engine/process-definitions')
}

export function redeployBuiltin(): Promise<Record<string, unknown>> {
  return post<Record<string, unknown>>('/api/workflow/engine/redeploy-builtin')
}

/** 部署前校验 BPMN（引擎错误会直接回显） */
export function validateBpmn(resourceName: string, bpmnXml: string): Promise<Record<string, unknown>> {
  return post<Record<string, unknown>>(
    `/api/workflow/engine/validate-bpmn?resourceName=${encodeURIComponent(resourceName)}`,
    bpmnXml
  )
}

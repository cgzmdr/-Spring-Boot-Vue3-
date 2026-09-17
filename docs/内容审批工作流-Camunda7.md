# 内容审批工作流（Camunda 7 嵌入式引擎）

> 面向后台 OA 的内容审核闭环。以「民族」为例实现，流程模型可复用到节日 / 艺术 / 专题等所有内容类型。
>
> 相关代码：`backend/src/main/java/com/czdr/work/{service,controller,model}/…`、
> `admin/src/pages/{todo,modeler,review,ethnic}/…`

---

## 0. 引擎形态：内嵌，不是外部集群

**这是本模块与早期版本最重要的区别。**

早期实现使用 Camunda 8（Zeebe）。Camunda 8 在架构上是「Zeebe broker + gateway +
Elasticsearch 二次存储」的**分布式系统**，官方的 Spring Boot starter 只是瘦客户端，
必须额外部署并运维一整套集群，「生产部署方便」这个诉求无法满足。

现在改为 **Camunda 7 嵌入式引擎**（`camunda-bpm-spring-boot-starter:7.24.0`）：

| 关注点 | Camunda 8（旧） | Camunda 7 嵌入式（现在） |
| --- | --- | --- |
| 引擎位置 | 独立进程/集群，HTTP/gRPC 连接 | **与应用同一个 JVM**，以依赖库形式加载 |
| 数据存储 | 引擎自带 + Elasticsearch 二次存储 | **复用业务同一个 PostgreSQL**（`ACT_*` 表） |
| 部署组件 | broker + gateway + ES + 应用 | **仅应用本身** |
| 读写一致性 | 查询走二次存储，**有可见性延迟** | 本地 SQL，**强一致、与业务写同事务** |
| 用户任务 ID / 流程实例 ID | `Long`（2^53 量级 key） | **`String`**（如 `ethnic-content-review:1:<uuid>`） |
| 流程流转 | job worker 异步拉取 | `JavaDelegate`（可由 `asyncBefore` 转异步作业） |
| 表单 | 引擎资源（`.form` 需部署到引擎） | 本地 `form_binding` + 前端 form-js 渲染 |

**为什么能把项目迁过来**：Camunda 官方兼容性矩阵明确列出
[7.24.x 支持 Spring Boot 3.5.x / 4.0.x](https://docs.camunda.org/manual/7.24/user-guide/spring-boot-integration/version-compatibility/)，
而本项目是 Spring Boot 4.0.7，因此可以直接用官方 starter，无需降级框架。

---

## 1. 业务闭环

原来的审核只有「一张 `content_review` 流水表 + 一个 status 字段」，只能表达
「待审 / 通过 / 驳回」，既没有版本概念、也没有审批意见，更没有内容管理员复核与下线。
新的闭环：

```
内容编辑提交审批
      │
      ▼
① 审核员审批 ──（退回，留审批意见）──▶ 内容编辑修改 ──┐
      │（通过，留审批意见）                          │
      ▼                                              │
   内容上线 ◀────────────────────────────────────────┘
      │                                        （修改完成，二次审批）
      ▼
② 内容管理员审查 ──（无问题，留审查意见）──▶ 本轮闭环结束，内容保持在线
      │
      └─（发现问题，留审查意见）─▶ 内容暂时下线
                                      │
                                      ▼
                              ③ 内容编辑修改
                              （可查看前序全部审批意见与审查意见）
                                      │
                                      └──▶ 回到 ① 审核员审批（第二次）
                                             → 重新上线 → 再次审查 → ……
```

**关键点**：每一次流转都会在 `workflow_opinion` 留下一条意见（含操作人、角色、环节、结论、
所属内容版本），因此第 3 步的「内容编辑查看前面的审批意见和审查意见」是天然的查询，
不需要额外传递上下文。

---

## 2. 职责划分：引擎 vs 本地表

| 关注点 | 承担者 | 说明 |
| --- | --- | --- |
| 流转编排（谁在什么时候该处理） | **Camunda 7 引擎** | 用户任务、排他网关、服务任务；支持在线建模与改版 |
| 用户任务分派 | **`camunda:candidateGroups`** | 用**角色名**（reviewer / content_admin / editor）表达，与本地 RBAC 一致 |
| 用户任务表单 | **本地 `form_binding` + frontend form-js** | 后端按「环节」查 purpose 对应的 schema，**不经引擎** |
| 业务数据（内容版本、意见） | **本地 PostgreSQL** | `workflow_instance` / `workflow_opinion` |
| 内容状态副作用（上线/下线） | **本地 `JavaDelegate`** | 服务任务 `WorkflowPublishDelegate` / `WorkflowOfflineDelegate` |
| 审核态快照（列表页/统计） | **`content_review`** | 由工作流事件同步维护，保持向后兼容 |

**为什么上线/下线用 serviceTask + JavaDelegate 而不是网关分支直接改状态：**
排他网关只做条件判断，不具备「带重试的副作用」语义。放 delegate 里，失败会自动重试
并形成 incident，可在后台看到。同时上线/下线是必须留痕、必须可重入的动作，天然适合幂等处理。

> **`asyncBefore` 的取舍**：两个服务任务都标了 `camunda:asyncBefore="true"`。
> 这样 delegate 抛异常时**不会**把调用方（审核员点「通过」的 HTTP 请求）一起打挂，
> 而是生成带重试次数的 incident。代价是：用户任务完成后，流程会短暂停在一个待执行作业上
> —— 此时查不到新任务、实例也没结束。这不是错误状态，本地实例保持 `running`，
> 下一次读取（待办列表 / 详情页 / 兜底复查）会自然刷新到新环节。

---

## 3. 数据模型

### 3.1 `workflow_instance`（一次「提交审批」一行）

| 字段 | 含义 |
| --- | --- |
| `entry_type` / `entry_id` | 业务内容（ethnic / festival / art / topic） |
| `content_version` | **第几版内容**（内容表每次提交审批 +1） |
| `process_instance_key` | Camunda 7 流程实例 ID（**varchar，字符串**） |
| `business_key` | `entryType:entryId:v版本` |
| `status` | `running` / `completed` / `withdrawn` / `superseded` |
| `current_stage` | `pending_review` / `pending_inspect` / `revising` / `published` / `stopped` |
| `current_task_key` / `current_task_name` | 当前待办的 Camunda 用户任务（**key 为 varchar**） |
| `round_no` | 第几轮流转 |

唯一约束 `uk_workflow_instance_active (entry_type, entry_id, content_version) WHERE status='running'`，
保证同一内容同一版本不会出现两个并发流程。

> ⚠️ **迁移注意**：`process_instance_key` / `current_task_key` / `camunda_task_key`
> 三列在 V18 里是 `bigint`（Camunda 8 的长整型 key），
> **V19 迁移已把它们改成 `varchar(64)`**（Camunda 7 的字符串 ID）。
> 未执行 V19 会让字符串 ID 写入 bigint 列直接报类型错误。

### 3.2 `workflow_opinion`（append-only 意见流水）

`stage`：`submit` 提交 / `approve` 审批 / `inspect` 审查 / `offline` 下线 / `revise` 修改 / `publish` 上线
`decision`：`submitted` / `approved` / `rejected` / `issue` / `resolved`

带 `content_version`，所以「按版本回看审批意见」是一次普通查询。

### 3.3 内容表新增 `content_version`

`ethnic_group` / `festival` / `art` / `topic` 各加一列 `content_version integer NOT NULL DEFAULT 0`。

### 3.4 `process_binding` / `form_binding`

网页版 Modeler 的持久化：一个 `process_key` + `version` 对应一份 BPMN XML
（及其在引擎上的部署版本）；一个 `form_id` + `version` 对应一份 form-js schema。

> **表单不再部署到引擎**。Camunda 7 嵌入式模式下表单是纯本地资源，
> 后端按环节（`formPurposeOfStage`）提供 schema，前端 form-js 渲染。

### 3.5 `content_review` 扩展

状态取值扩展为 `pending` / `approved` / `rejected` / `offline` / `revising`，
并新增 `instance_id` / `content_version` / `last_opinion` 三列指向工作流。

---

## 4. 后端接口

所有接口挂在 `/workflow`（`WorkflowController`），已有 `admin/reviews/*` 同步改造。

### 提交与流转

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| POST | `/entries/{entryType}/{entryId}/submit` | 内容编辑提交审批 | 登录 |
| POST | `/instances/{id}/approve` | 审核员审批通过（**必填审批意见**） | `review:approve` |
| POST | `/instances/{id}/reject` | 审核员退回修改（**必填审批意见**） | `review:reject` |
| POST | `/instances/{id}/inspect/pass` | 内容管理员审查通过（保持在线） | `review:inspect` |
| POST | `/instances/{id}/inspect/issue` | 审查发现问题 → 暂时下线（**必填审查意见**） | `review:offline` |
| POST | `/instances/{id}/revise` | 内容编辑修改完成 → 二次审批 | 登录 |
| POST | `/instances/{id}/withdraw` | 提交人撤回 | 登录 |
| POST | `/instances/{id}/complete` | 通用入口（按当前环节分派） | 登录 |

### 待办与查询

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/tasks?stage=&entryType=&mineOnly=` | 我的待办 |
| GET | `/instances/{id}` | 待办详情（含**全量历史意见** + 当前环节表单 schema） |
| GET | `/entries/{entryType}/{entryId}/task` | 按内容查活跃待办 |
| GET | `/entries/{entryType}/{entryId}/timeline` | 审批全过程（逐版本实例 + 全量意见） |
| GET | `/entries/{entryType}/{entryId}/opinions` | 意见列表 |

### 网页版 Modeler

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST/PUT/DELETE | `/processes[/{id}]` | BPMN 流程定义 CRUD |
| POST | `/processes/{id}/deploy` | 部署到引擎 |
| POST | `/processes/{id}/duplicate` | 另存新版本 |
| GET | `/processes/template` | 生成可部署的 BPMN 模板（**Camunda 7 命名空间**） |
| GET/POST/PUT/DELETE | `/forms[/{id}]` | Camunda Form CRUD（纯本地） |
| POST | `/forms/{id}/deploy` | 表单「保存并生效」（本地，不部署到引擎） |
| GET | `/forms/template?purpose=&formId=` | 生成 form-js schema 模板 |
| POST | `/engine/validate-bpmn` | 部署前校验（引擎错误直接回显） |
| GET | `/engine/topology` | **引擎状态**（嵌入式：引擎名/版本/流程数/实例数/待办数） |
| POST | `/engine/redeploy-builtin` | 重新部署内置流程 |

---

## 5. 权限

新增权限点，已写入 `RbacDataInitializer`：

| 权限 | 说明 | 授予角色 |
| --- | --- | --- |
| `review:inspect` | 内容审查（出审查意见 / 通过） | content_admin |
| `review:offline` | 审查发现问题并暂时下线 | content_admin |
| `workflow:list` | 工作流定义查看 | reviewer / content_admin |
| `workflow:save` | 工作流建模与保存 | content_admin |
| `workflow:deploy` | 部署到引擎 | content_admin |
| `review:list` | 查看审批过程与意见 | **editor 也新增**（内容编辑必须能看到前序意见） |

---

## 6. 前端页面

| 页面 | 路由 | 作用 |
| --- | --- | --- |
| **我的待办** | `/todo` | 按环节筛选待办，各角色的统一入口 |
| **审核处理** | `/todo/detail?instanceId=` | 闭环进度条 + **表单渲染** + 审批/审查操作 + 意见时间线 + 版本历史 |
| **内容审核** | `/review` | 审核态快照列表（含「已暂时下线」「待修改」），可跳转到处理页 |
| **民族管理** | `/ethnic` | 列表新增「审批环节」「版本」列 + 「提交审批」/「去处理」入口 |
| **编辑民族** | `/ethnic/edit` | 审批状态提示条 + **前序审批/审查意见** + 「保存并提交审批」 |
| **流程建模** | `/modeler` | 网页版建模器：BPMN + Camunda Form + **引擎状态（嵌入式）** |

### Camunda Form 的一致性做法

审核页用 `@bpmn-io/form-js-viewer` 的 `Form` 渲染 `form_binding.schema_json`，
Modeler 用 `@bpmn-io/form-js-editor` 编辑同一份 schema。二者同源，
**Modeler 里改完表单 → 保存 → 审核页立刻用新版表单**，不需要改前端代码。

`CamundaFormRenderer.vue` 里用 `:deep()` 覆写 form-js 的 `fjs-*` 样式
（输入框圆角、边框色、聚焦色、错误色），使其与项目其它 Element Plus 表单观感一致。

### 表单如何与环节关联（Camunda 7 的做法）

Camunda 8 时代表单是引擎资源，需要在 `userTask` 上写 `zeebe:formDefinition formId`，
且引用了不存在的 formId 会直接抛 `FORM_NOT_FOUND` incident 卡住流程。

Camunda 7 嵌入式模式下改为**按环节约定 elementId**：

| 用户任务 elementId | 环节 | 表单 purpose | 默认 formId |
| --- | --- | --- | --- |
| `Task_ReviewerApprove` | 待审核员审批 | `approval` | `ethnic-approval-form` |
| `Task_ContentInspect` | 待内容管理员审查 | `inspection` | `ethnic-inspection-form` |
| `Task_EditorRevise` | 待内容编辑修改 | `revision` | `ethnic-revision-form` |

因此**只要保持这三个 elementId 不变**，表单就会自动对上；改表单内容只需在
「Camunda Form」页签编辑，无需动 BPMN。

### 网页版 Modeler 能力

- **BPMN 页签**：bpmn-js 画布（拖拽建模、属性面板、`bpmnlint` 实时校验）、
  左侧流程定义列表（启用/停用、另存新版本、删除）、保存/部署到引擎、
  用户任务设置候选组（写 `camunda:candidateGroups`；打开旧流程时会兼容读取
  历史 `zeebe:candidateGroups`，保存时自动改写为 Camunda 7 属性）。
- **Form 页签**：form-js 设计器（拖拽组件、属性面板）、左侧表单列表、保存并生效。
- **引擎页签**：嵌入式引擎状态（引擎名/版本/已部署流程数/运行实例数/待办数）、
  引擎上已部署的流程定义、一键重新部署内置流程。

---

## 7. 内置流程与表单

启动时 `WorkflowDataInitializer` 会自动：
1. 部署 `processes/ethnic-content-review.bpmn`；
2. 把流程绑定写进 `process_binding`（含 BPMN XML 原文，保证 Modeler 打开有内容）；
3. 把三个 Camunda Form 的 form-js schema 写进 `form_binding`。

内置资源：

| 资源 | 标识 | 说明 |
| --- | --- | --- |
| `ethnic-content-review.bpmn` | `ethnic-content-review` | 民族内容审批流程 |
| 表单 | `ethnic-approval-form` | 审核员审批（审批意见 + 通过/退回） |
| 表单 | `ethnic-inspection-form` | 内容管理员审查（审查意见 + 无问题/发现问题） |
| 表单 | `ethnic-revision-form` | 内容编辑修改（修改说明 + 是否二次审批） |

引擎初始化失败（例如 `ACT_*` 表未建好、BPMN XML 写错）时只告警不阻断启动 ——
内容管理、检索等功能不依赖流程引擎；可通过 `app.camunda.deploy-on-startup=false` 关闭启动期部署。

### BPMN 里的 Camunda 7 写法要点

| 关注点 | Camunda 8 写法 | Camunda 7 写法 |
| --- | --- | --- |
| 命名空间 | `xmlns:zeebe="http://camunda.org/schema/zeebe/1.0"` | `xmlns:camunda="http://camunda.org/schema/1.0/bpmn"` |
| 候选组 | `zeebe:candidateGroups` | `camunda:candidateGroups` |
| 服务任务 | `<zeebe:taskDefinition type="..."/>` + job worker | `camunda:class="…JavaDelegate"`（可选 `camunda:asyncBefore`） |
| 条件表达式 | FEEL：`= approved = true` | **JUEL：`${approved == true}`** |
| 表单 | `<zeebe:formDefinition formId="…"/>` | 不需要（本地按环节提供） |

> ⚠️ **JUEL 的空值陷阱**：`${approved == true}` 在变量 `approved` **不存在**时会抛
> `PropertyNotFound`。因此 `WorkflowServiceImpl#submit` 会预置
> `approved=false` / `hasIssue=false` / `needReapproval=true` 三个变量，
> 保证流程任意时刻求值都不会因缺变量失败。

---

## 8. 对账与一致性（重要变化）

### Camunda 8 时代为什么需要复杂的对账

Camunda 8 的 REST 查询读的是**二次存储**（Elasticsearch 导出器），相对引擎写入有延迟。
用户任务刚完成时，新任务可能还没落库、流程实例也还没进终态，因此当时需要：
- 提交路径内**短轮询**（最多 12 × 250ms = 3s，且是在事务内 sleep）；
- `WorkflowSettlementScheduler` 每 30s 全量对账作为正确性兜底。

### 迁移后为什么可以大幅简化

嵌入式 Camunda 7 的用户任务与运行时数据**就在业务同一个 PostgreSQL 库**，
查询是本地 SQL、强一致，且 `complete()` 与业务写在同一事务里。因此：

1. **提交路径不再轮询**：一次查询即可拿到确定结论，不存在「二次存储还没追上」的窗口，
   也不会把实例误判为「已结束」。相关常量 `ENGINE_SETTLE_ATTEMPTS` /
   `ENGINE_SETTLE_INTERVAL_MS` 与事务内 sleep 已全部删除。
2. **`WorkflowSettlementScheduler` 降级为兜底**：
   它不再是正确性的必要环节，只处理异常残留——
   - 本地标记 `running`、但引擎运行时表已无该实例的 → 本地收尾；
   - 从未成功启动流程的残留行 → 收尾，避免永久占用唯一约束。
   频率也从 30s 放宽到 60s。

> 换言之：**即使兜底任务完全不运行，审批主流程依然正确。**

### 查询用户任务的简化

Camunda 8 版本里 `queryEngineUserTasks` 有一段绕行：因为本地 8.9 自管集群的
`/v2/user-tasks/search` 无法解析超出 int 范围的 `filter.processInstanceKey`
（返回 `Request property [filter.processInstanceKey] cannot be parsed`），
而 Zeebe 的 key 都是 2^53 量级，所以只能「全量拉取 `state=CREATED` 再本地比对」。
Camunda 7 的 `TaskQuery.processInstanceIdIn(...)` 直接下推到本地 SQL，
这类绕行与随之而来的「无分页导致静默截断」风险一并消失。

---

## 9. 验证

| 脚本 | 覆盖 | 结果 |
| --- | --- | --- |
| `backend/scripts/workflow-e2e-verify.mjs` | 完整闭环：提交 → 审批 → 上线 → 审查 → 下线 → 修改 → 二审 → 重新上线 → 再审查 → 通过 → 二次提交 | **52 / 52 通过** |
| `admin/scripts/ui-smoke-verify.mjs` | Chrome CDP 真实渲染：待办、Modeler、审核处理页、民族列表、内容审核页 | 见脚本输出 |

运行方式：

```bash
# 后端（Camunda 7 已内嵌，只需 PostgreSQL + Redis）
cd backend && node scripts/workflow-e2e-verify.mjs

# 前端（需先启动 admin dev server 于 5273）
cd admin && node scripts/ui-smoke-verify.mjs
```

> **注意 E2E 脚本对数据隔离的假设**：脚本断言「两轮审批意见都在」等固定条数，
> 依赖 `workflow_opinion` / `workflow_instance` 从零开始累积。
> 在同一库上反复运行会累计历史数据导致计数类断言失败（其余断言仍通过）。
> 需要干净复现时，先清理业务侧工作流历史（`workflow_opinion` / `workflow_instance` /
> `content_review`）与引擎侧历史（`act_hi_*`），
> 并把目标民族的 `content_version` 归零。

### 引擎侧可自查的证据

流程是否真的按预期跑过，可以直接查引擎表（这正是嵌入式的好处——就在业务库里）：

```sql
-- 历史流程实例（含业务键与撤回原因）
SELECT proc_def_id_, business_key_, start_time_, end_time_, delete_reason_
FROM act_hi_procinst ORDER BY start_time_ DESC LIMIT 10;

-- 历史用户任务（任务链是否完整流转）
SELECT name_, task_def_key_, assignee_, start_time_, end_time_ IS NOT NULL AS done
FROM act_hi_taskinst ORDER BY start_time_ DESC LIMIT 20;

-- 是否有卡住/失败的异步作业（应为 0）
SELECT count(*) AS pending, count(*) FILTER (WHERE retries_ = 0) AS failed FROM act_ru_job;
```

---

## 10. 部署检查清单

1. **应用数据库迁移**（按序执行；V19 必做，否则字符串 ID 写不进 bigint 列）：
   ```bash
   psql -h localhost -U postgres -d 56_app -f backend/src/main/resources/db/migration/V18__workflow_camunda.sql
   psql -h localhost -U postgres -d 56_app -f backend/src/main/resources/db/migration/V19__camunda7_embedded.sql
   ```
2. **确认无需再启动任何 Camunda 组件**：`ACT_*` 表由引擎在首次启动时自动创建
   （`camunda.bpm.database.schema-update=true`）。若生产要求 DDL 由 DBA 执行，
   可改为 `false` 并预先用引擎自带的 `sql/create/*.sql` 建表。
3. **启动应用**，检查日志出现：
   - `ENGINE-00001 Process Engine default created.`
   - `Camunda 内置流程资源部署完成: processes=1, failures=0`
   - `ENGINE-14014 Starting up the JobExecutor`
4. 打开「流程建模 → 流程建模」页，切到「引擎」页签，确认
   状态在线、已部署流程定义 ≥ 1、无异常。
5. 确认 `camunda.bpm.auto-deployment-enabled=false`（避免与
   `WorkflowDataInitializer` 重复部署同一份 BPMN）。
6. 角色分配：确保对应账号拥有 `reviewer` / `content_admin` / `editor` 角色
   （权限点已由 `RbacDataInitializer` 自动绑定）。

### 与部署相关的配置项

```yaml
camunda:
  bpm:
    auto-deployment-enabled: false   # 由 WorkflowDataInitializer 显式部署
    database:
      schema-update: true            # 自动建/升级 ACT_* 表
      type: postgres
    job-execution:
      enabled: true                  # asyncBefore 服务任务需要作业执行器
      core-pool-size: 3
      max-pool-size: 10
    history-level: audit             # 记录实例/活动/任务/变量，够回溯前序意见
    metrics:
      enabled: false
```

> **遥测**：Camunda 7 的 starter **没有** telemetry 配置项
> （不在 `spring-configuration-metadata.json` 中），也无法注入 `TelemetryReporter`。
> 本项目用 `CamundaConfig`（`ApplicationRunner`）在启动后调用
> `ManagementService#toggleTelemetry(false)` 关闭匿名上报，幂等且失败不影响启动。

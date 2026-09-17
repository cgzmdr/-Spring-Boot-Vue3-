-- ============================================================================
-- V18 · Camunda 8 工作流（民族内容审批闭环）
--
-- 背景
--   原先「审核」是一张 content_review 流水表 + 一个 status 字段：
--     · 无版本概念 —— 第二次修改后找不到「上一版审批意见」；
--     · 无多人多环节 —— 只有「审核员通过/驳回」，没有内容管理员复核与下线；
--     · 无审批意见 —— 只有 reject_reason 一个驳回原因，通过时不落任何意见。
--
--   业务要求（民族为例）的完整闭环：
--     内容编辑提交 → ① 审核员审批（留审批意见）→ 上线
--       → 内容管理员审查（留审查意见）
--           · 无问题 → 本轮结束，内容保持在线
--           · 有问题 → 暂时下线 → 内容编辑修改（可查看前序审批/审查意见）
--                      → ② 审核员二次审批 → 重新上线 → 内容管理员再审查 → ……
--
--   这与 Camunda 8 的「用户任务 + 版本化流程定义」天然对应，因此：
--     · 流程引擎（Zeebe/Camunda 8.9）负责「流转」：用户任务、网关、定时器；
--     · 本地表负责「业务数据」：内容版本、逐条意见、流程实例↔内容版本的绑定。
--
-- 设计要点
--   1. workflow_instance 一次「提交」一行：绑定 内容(entry_type,entry_id) + 内容版本
--      (content_version) + Camunda 流程实例(process_instance_key) + 当前环节(current_stage)。
--      「第几轮」「上一版意见」都由这张表的历史行推导，不再依赖单一 status 字段。
--   2. workflow_opinion 追加式意见流水（append-only）：审批意见 / 审查意见 / 下线说明 /
--      修改说明 都落这里，带 task_key 与任务名，可直接回答「前面的审批意见和审查意见是什么」。
--   3. content_version 是「第几次提交审批」的计数，内容表每次提交审批自增；
--      同一版本内的多次流转共享该版本号，便于按版本回看。
--   4. process_binding / decision_binding 是 网页版 Modeler 的落库表：
--      一个业务类型 + 一个版本号 对应一个已部署的 BPMN/DMN 资源，支持在线建模/上传/部署/回滚。
--
-- 与 content_review 的关系：
--   content_review 保留为「审核态快照」表（列表页/统计仍可直接查），由工作流事件同步维护，
--   状态取值扩展为 pending/approved/rejected/offline，语义 = 内容当前处于哪一步。
--
-- 幂等：CREATE TABLE IF NOT EXISTS / ADD COLUMN IF NOT EXISTS。
-- 应用：psql -h localhost -U postgres -d 56_app -f V18__workflow_camunda.sql
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1) 内容版本号：内容表每次「提交审批」自增，作为审批意见的归属版本
-- ---------------------------------------------------------------------------
ALTER TABLE ethnic_group ADD COLUMN IF NOT EXISTS content_version integer NOT NULL DEFAULT 0;
ALTER TABLE festival     ADD COLUMN IF NOT EXISTS content_version integer NOT NULL DEFAULT 0;
ALTER TABLE art          ADD COLUMN IF NOT EXISTS content_version integer NOT NULL DEFAULT 0;
ALTER TABLE topic        ADD COLUMN IF NOT EXISTS content_version integer NOT NULL DEFAULT 0;

COMMENT ON COLUMN ethnic_group.content_version IS '内容版本号：每提交一次审批 +1，审批/审查意见按该版本归档';

-- ---------------------------------------------------------------------------
-- 2) 工作流实例：一次「提交审批」= 一个 Camunda 流程实例 + 一个内容版本
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS workflow_instance (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 业务内容
    entry_type            varchar(32)  NOT NULL,          -- ethnic / festival / art / topic
    entry_id              uuid         NOT NULL,
    entry_title           varchar(200),                   -- 冗余标题，待办列表免二次查询
    content_version       integer      NOT NULL DEFAULT 1,-- 第几版内容
    -- Camunda 8
    process_definition_id varchar(255),                   -- 如 ethnic-content-review
    process_definition_version integer,                   -- 流程定义版本（Camunda 侧）
    process_instance_key  bigint,                          -- 流程实例 key
    business_key          varchar(120),                    -- 约定 entryType:entryId:v版本
    -- 状态机
    --   running       流程在跑（审批中 / 审查中）
    --   completed     本轮闭环结束（内容已上线且审查通过）
    --   withdrawn     提交人撤回
    --   superseded    被新一轮提交取代（内容被下线后重新提交的场景）
    status                varchar(20)  NOT NULL DEFAULT 'running',
    -- 当前环节：pending_review 待审核员审批 / pending_inspect 待内容管理员审查 /
    --           revising 待内容编辑修改 / published 已上线（审查通过）/ stopped 已终止
    current_stage         varchar(32)  NOT NULL DEFAULT 'pending_review',
    -- 参与人
    submitter_id          uuid,
    submitter_name        varchar(64),
    current_assignee_id   uuid,                            -- 当前待办负责人（可空=候选组认领）
    current_task_key      bigint,                          -- Camunda 用户任务 key
    current_task_name     varchar(200),
    round_no              integer      NOT NULL DEFAULT 1, -- 第几轮流转（审批/审查/修改各算一次进入）
    -- 时间
    started_at            timestamptz  NOT NULL DEFAULT now(),
    finished_at           timestamptz,
    created_at            timestamptz  NOT NULL DEFAULT now(),
    updated_at            timestamptz  NOT NULL DEFAULT now()
);

COMMENT ON TABLE  workflow_instance IS '内容审批工作流实例：一条内容的一次提交审批（对应一个 Camunda 流程实例 + 一个内容版本）';
COMMENT ON COLUMN workflow_instance.current_stage IS '当前环节：pending_review 待审批 / pending_inspect 待审查 / revising 待修改 / published 已上线 / stopped 已终止';

-- 同一内容同一版本只允许一个「活跃」实例
CREATE UNIQUE INDEX IF NOT EXISTS uk_workflow_instance_active
    ON workflow_instance (entry_type, entry_id, content_version)
    WHERE status = 'running';

CREATE INDEX IF NOT EXISTS idx_workflow_instance_entry   ON workflow_instance (entry_type, entry_id, content_version DESC);
CREATE INDEX IF NOT EXISTS idx_workflow_instance_status  ON workflow_instance (status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_workflow_instance_assignee ON workflow_instance (current_assignee_id, status);
CREATE INDEX IF NOT EXISTS idx_workflow_instance_pikey   ON workflow_instance (process_instance_key);

-- ---------------------------------------------------------------------------
-- 3) 审批 / 审查意见流水（append-only）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS workflow_opinion (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    instance_id   uuid         NOT NULL REFERENCES workflow_instance (id) ON DELETE CASCADE,
    entry_type    varchar(32)  NOT NULL,
    entry_id      uuid         NOT NULL,
    content_version integer    NOT NULL DEFAULT 1,
    -- 环节：submit 提交 / approve 审批 / inspect 审查 / offline 下线 / revise 修改说明 / publish 上线
    stage         varchar(32)  NOT NULL,
    -- 结论：approved 通过 / rejected 驳回(退回) / issue 发现问题 / resolved 已修正 / submitted 已提交
    decision      varchar(32),
    opinion       text,                                    -- 意见正文（审批意见 / 审查意见）
    operator_id   uuid,
    operator_name varchar(64),
    operator_role varchar(32),                             -- reviewer / content_admin / editor
    camunda_task_key bigint,                               -- 对应 Camunda 用户任务 key
    task_name     varchar(200),
    created_at    timestamptz  NOT NULL DEFAULT now()
);

COMMENT ON TABLE workflow_opinion IS '审批/审查意见流水（append-only），按内容版本归档，用于「查看前面的审批意见和审查意见」';

CREATE INDEX IF NOT EXISTS idx_workflow_opinion_instance ON workflow_opinion (instance_id, created_at);
CREATE INDEX IF NOT EXISTS idx_workflow_opinion_entry    ON workflow_opinion (entry_type, entry_id, content_version, created_at);
CREATE INDEX IF NOT EXISTS idx_workflow_opinion_stage    ON workflow_opinion (entry_type, entry_id, stage);

-- ---------------------------------------------------------------------------
-- 4) 网页版 Modeler 的流程/表单绑定（在线建模 → 部署 → 按业务类型解析）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS process_binding (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- 业务标识，如 ethnic-content-review / festival-content-review
    process_key    varchar(120) NOT NULL,
    name           varchar(200) NOT NULL,
    -- 业务类型：ethnic / festival / art / topic / *（通用兜底）
    entry_type     varchar(32)  NOT NULL,
    -- 版本：允许同一业务类型保留多个可切换的流程定义
    version        integer      NOT NULL DEFAULT 1,
    -- BPMN XML 原文（网页 Modeler 保存/上传），部署后回填 Camunda 侧信息
    bpmn_xml       text         NOT NULL,
    camunda_definition_id      varchar(255),
    camunda_definition_version integer,
    -- 是否启用（同一 entry_type 下只能有一个 enabled）
    enabled        boolean      NOT NULL DEFAULT true,
    remark         varchar(500),
    created_by     uuid,
    updated_by     uuid,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT process_binding_unique UNIQUE (process_key, version)
);

COMMENT ON TABLE process_binding IS '业务类型 → Camunda 流程定义绑定（网页版 BPMN Modeler 的持久化）';

CREATE INDEX IF NOT EXISTS idx_process_binding_entry ON process_binding (entry_type, enabled, version DESC);

-- 表单绑定：Camunda Form（form-js schema），按 form_id 解析
CREATE TABLE IF NOT EXISTS form_binding (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Camunda Form 的 formId，与 BPMN 中 zeebe:userTaskForm / formId 一致
    form_id        varchar(120) NOT NULL,
    name           varchar(200) NOT NULL,
    -- 用途：approval 审批表单 / inspection 审查表单 / revision 修改表单
    purpose        varchar(32)  NOT NULL DEFAULT 'approval',
    entry_type     varchar(32)  NOT NULL DEFAULT '*',
    version        integer      NOT NULL DEFAULT 1,
    -- form-js schema JSON 原文
    schema_json    text         NOT NULL,
    enabled        boolean      NOT NULL DEFAULT true,
    remark         varchar(500),
    created_by     uuid,
    updated_by     uuid,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT form_binding_unique UNIQUE (form_id, version)
);

COMMENT ON TABLE form_binding IS 'Camunda Form（form-js schema）绑定：审批/审查/修改表单，供前端 FormRenderer 渲染';

CREATE INDEX IF NOT EXISTS idx_form_binding_purpose ON form_binding (purpose, entry_type, enabled, version DESC);

-- ---------------------------------------------------------------------------
-- 5) updated_at 触发器
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'set_updated_at') THEN
        DROP TRIGGER IF EXISTS trg_workflow_instance_updated ON workflow_instance;
        CREATE TRIGGER trg_workflow_instance_updated BEFORE UPDATE ON workflow_instance
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();

        DROP TRIGGER IF EXISTS trg_process_binding_updated ON process_binding;
        CREATE TRIGGER trg_process_binding_updated BEFORE UPDATE ON process_binding
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();

        DROP TRIGGER IF EXISTS trg_form_binding_updated ON form_binding;
        CREATE TRIGGER trg_form_binding_updated BEFORE UPDATE ON form_binding
            FOR EACH ROW EXECUTE FUNCTION set_updated_at();
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 6) content_review 状态取值扩展 + 与工作流对齐
--    offline  = 内容管理员审查发现问题后暂时下线
--    revising = 已退回内容编辑修改
-- ---------------------------------------------------------------------------
ALTER TABLE content_review DROP CONSTRAINT IF EXISTS content_review_status_check;
ALTER TABLE content_review ADD CONSTRAINT content_review_status_check
    CHECK (status::text = ANY (ARRAY['pending','approved','rejected','offline','revising']::text[]));

-- 审核记录指向最新的工作流实例（可空，兼容历史数据）
ALTER TABLE content_review ADD COLUMN IF NOT EXISTS instance_id uuid;
ALTER TABLE content_review ADD COLUMN IF NOT EXISTS content_version integer;
ALTER TABLE content_review ADD COLUMN IF NOT EXISTS last_opinion text;

CREATE INDEX IF NOT EXISTS idx_content_review_entry ON content_review (entry_type, entry_id);

-- ---------------------------------------------------------------------------
-- 7) 历史数据回填：已发布内容补一个「已结束」的实例，保证列表/详情不空窗
-- ---------------------------------------------------------------------------
INSERT INTO workflow_instance
    (entry_type, entry_id, entry_title, content_version, status, current_stage,
     round_no, started_at, finished_at)
SELECT 'ethnic', e.id, e.name, GREATEST(e.content_version, 1), 'completed',
       CASE WHEN e.status = 'published' THEN 'published' ELSE 'stopped' END,
       1, e.created_at, e.updated_at
FROM ethnic_group e
WHERE NOT EXISTS (
    SELECT 1 FROM workflow_instance w WHERE w.entry_type = 'ethnic' AND w.entry_id = e.id
);

-- 校验：
-- SELECT entry_type, status, current_stage, count(*) FROM workflow_instance GROUP BY 1,2,3 ORDER BY 1;
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name='workflow_opinion' ORDER BY ordinal_position;

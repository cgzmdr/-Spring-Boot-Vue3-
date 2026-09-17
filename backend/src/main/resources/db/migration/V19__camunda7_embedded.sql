-- ============================================================================
-- V19：内容审批工作流 —— 引擎由 Camunda 8 迁移到 Camunda 7（嵌入式）
--
-- 背景：原先工作流使用 Camunda 8（Zeebe 自管集群）。Camunda 8 的流转标识
-- （流程实例 key、用户任务 key）是 2^53 量级的**长整型**，因此 V18 里
-- workflow_instance.process_instance_key / current_task_key 与
-- workflow_opinion.camunda_task_key 都建成了 bigint。
--
-- 迁移到 Camunda 7 后，引擎以依赖库形式跑在本应用 JVM 内、复用同一个业务库，
-- 其流程实例 ID 与任务 ID 都是**字符串**（流程实例 ID 形如 <processKey>-<uuid>，
-- 任务 ID 形如 <processKey>-<uuid> 或 UUID）。因此这三列必须由 bigint 改为 varchar，
-- 否则字符串 ID 写入 bigint 列会直接报类型错误（invalid input syntax for type bigint）。
--
-- 幂等性：使用 information_schema 判断当前类型，仅在仍是 bigint 时执行转换，
-- 可重复执行。已存在的历史 bigint 值（若有）会被转换成其十进制字符串，保持可读。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. workflow_instance.process_instance_key：bigint -> varchar(64)
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'workflow_instance'
          AND column_name = 'process_instance_key'
          AND data_type = 'bigint'
    ) THEN
        -- 先丢弃依赖该列的索引，转换完成后再重建
        DROP INDEX IF EXISTS idx_workflow_instance_pikey;

        ALTER TABLE workflow_instance
            ALTER COLUMN process_instance_key TYPE varchar(64)
            USING process_instance_key::text;

        RAISE NOTICE 'workflow_instance.process_instance_key 已由 bigint 转换为 varchar(64)';
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 2. workflow_instance.current_task_key：bigint -> varchar(64)
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'workflow_instance'
          AND column_name = 'current_task_key'
          AND data_type = 'bigint'
    ) THEN
        ALTER TABLE workflow_instance
            ALTER COLUMN current_task_key TYPE varchar(64)
            USING current_task_key::text;

        RAISE NOTICE 'workflow_instance.current_task_key 已由 bigint 转换为 varchar(64)';
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 3. workflow_opinion.camunda_task_key：bigint -> varchar(64)
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'workflow_opinion'
          AND column_name = 'camunda_task_key'
          AND data_type = 'bigint'
    ) THEN
        ALTER TABLE workflow_opinion
            ALTER COLUMN camunda_task_key TYPE varchar(64)
            USING camunda_task_key::text;

        RAISE NOTICE 'workflow_opinion.camunda_task_key 已由 bigint 转换为 varchar(64)';
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 4. 重建流程实例索引（varchar 列；Camunda 7 下按字符串等值查询）
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_workflow_instance_pikey
    ON workflow_instance (process_instance_key);

-- ---------------------------------------------------------------------------
-- 5. 列注释更新为 Camunda 7 语义
-- ---------------------------------------------------------------------------
COMMENT ON COLUMN workflow_instance.process_instance_key
    IS 'Camunda 7 流程实例 ID（字符串，形如 <processKey>-<uuid>）';
COMMENT ON COLUMN workflow_instance.current_task_key
    IS 'Camunda 7 当前用户任务 ID（字符串）';
COMMENT ON COLUMN workflow_opinion.camunda_task_key
    IS 'Camunda 7 用户任务 ID（字符串，审批留痕用）';

-- ---------------------------------------------------------------------------
-- 6. 历史数据说明（不在这里自动执行清理）
--
--    Camunda 8 时代的运行时实例（ACT_* 表）并不存在于 Camunda 7 引擎中，
--    因此迁移后本地标记为 running 的旧实例在引擎里查不到对应运行时实例。
--    应用启动后 WorkflowSettlementScheduler（每 60 秒兜底复查）会自动把这些
--    残留实例收尾为 superseded，无需在此强行 UPDATE ——
--    保留自动收尾可以避免「迁移瞬间把正在审批的流程误判为终止」。
--
--    若希望迁移后立即清理（例如确认旧流程已全部作废），可手动执行：
--
--    UPDATE workflow_instance
--    SET status = 'superseded', current_stage = 'stopped',
--        current_task_key = NULL, current_task_name = NULL,
--        current_assignee_id = NULL,
--        finished_at = COALESCE(finished_at, now()), updated_at = now()
--    WHERE status = 'running';
-- ---------------------------------------------------------------------------

COMMENT ON TABLE workflow_instance
    IS '内容审批工作流实例：一条内容的一次提交审批（对应一个 Camunda 流程实例 + 一个内容版本）';

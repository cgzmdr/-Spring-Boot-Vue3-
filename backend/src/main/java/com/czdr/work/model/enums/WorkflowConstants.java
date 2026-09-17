package com.czdr.work.model.enums;

/**
 * 内容审批工作流环节与状态常量。
 * <p>集中定义，避免散落的魔法字符串导致「环节名」与 Camunda 模型不一致。</p>
 *
 * @author cz
 */
public final class WorkflowConstants {

    private WorkflowConstants() {
    }

    // ==================== 业务内容类型 ====================

    public static final String ENTRY_ETHNIC = "ethnic";
    public static final String ENTRY_FESTIVAL = "festival";
    public static final String ENTRY_ART = "art";
    public static final String ENTRY_TOPIC = "topic";

    // ==================== 工作流实例状态 ====================

    /** 流程在跑（审批中 / 审查中 / 待修改） */
    public static final String INSTANCE_RUNNING = "running";
    /** 本轮闭环结束（已上线且审查通过） */
    public static final String INSTANCE_COMPLETED = "completed";
    /** 提交人撤回 */
    public static final String INSTANCE_WITHDRAWN = "withdrawn";
    /** 被新一轮提交取代 */
    public static final String INSTANCE_SUPERSEDED = "superseded";

    // ==================== 当前环节 ====================

    /** 待审核员审批 */
    public static final String STAGE_PENDING_REVIEW = "pending_review";
    /** 待内容管理员审查 */
    public static final String STAGE_PENDING_INSPECT = "pending_inspect";
    /** 内容已发现问题，待内容编辑修改 */
    public static final String STAGE_REVISING = "revising";
    /** 已上线（内容管理员审查通过） */
    public static final String STAGE_PUBLISHED = "published";
    /** 已终止 */
    public static final String STAGE_STOPPED = "stopped";

    // ==================== 意见环节 ====================

    public static final String OPINION_SUBMIT = "submit";
    public static final String OPINION_APPROVE = "approve";
    public static final String OPINION_INSPECT = "inspect";
    public static final String OPINION_OFFLINE = "offline";
    public static final String OPINION_REVISE = "revise";
    public static final String OPINION_PUBLISH = "publish";

    // ==================== 意见结论 ====================

    public static final String DECISION_SUBMITTED = "submitted";
    public static final String DECISION_APPROVED = "approved";
    public static final String DECISION_REJECTED = "rejected";
    public static final String DECISION_ISSUE = "issue";
    public static final String DECISION_RESOLVED = "resolved";

    // ==================== 内容状态（ethnic_group.status 等） ====================

    public static final String CONTENT_DRAFT = "draft";
    public static final String CONTENT_PENDING = "pending";
    public static final String CONTENT_PUBLISHED = "published";
    public static final String CONTENT_REJECTED = "rejected";
    /** 内容管理员审查发现问题后暂时下线 */
    public static final String CONTENT_OFFLINE = "offline";

    // ==================== 审核态快照（content_review.status） ====================

    public static final String REVIEW_PENDING = "pending";
    public static final String REVIEW_APPROVED = "approved";
    public static final String REVIEW_REJECTED = "rejected";
    public static final String REVIEW_OFFLINE = "offline";
    public static final String REVIEW_REVISING = "revising";

    // ==================== 角色 ====================

    public static final String ROLE_REVIEWER = "reviewer";
    public static final String ROLE_CONTENT_ADMIN = "content_admin";
    public static final String ROLE_EDITOR = "editor";

    // ==================== Camunda 变量名 ====================

    public static final String VAR_ENTRY_TYPE = "entryType";
    public static final String VAR_ENTRY_ID = "entryId";
    public static final String VAR_ENTRY_TITLE = "entryTitle";
    public static final String VAR_CONTENT_VERSION = "contentVersion";
    public static final String VAR_APPROVED = "approved";
    public static final String VAR_HAS_ISSUE = "hasIssue";
    public static final String VAR_APPROVAL_OPINION = "approvalOpinion";
    public static final String VAR_INSPECTION_OPINION = "inspectionOpinion";
    public static final String VAR_REJECT_REASON = "rejectReason";
    public static final String VAR_OFFLINE_REASON = "offlineReason";
    public static final String VAR_REVISION_NOTE = "revisionNote";

    // ==================== 任务定义类型（BPMN serviceTask） ====================

    /** 内容上线 */
    public static final String JOB_PUBLISH = "content-publish";
    /** 内容暂时下线 */
    public static final String JOB_OFFLINE = "content-offline";

    // ==================== 业务键分隔符 ====================

    public static final String BUSINESS_KEY_SEPARATOR = ":";

    /** 组装业务键：entryType:entryId:v版本 */
    public static String businessKey(String entryType, Object entryId, Integer contentVersion) {
        return entryType + BUSINESS_KEY_SEPARATOR + entryId
                + BUSINESS_KEY_SEPARATOR + "v" + (contentVersion == null ? 1 : contentVersion);
    }
}

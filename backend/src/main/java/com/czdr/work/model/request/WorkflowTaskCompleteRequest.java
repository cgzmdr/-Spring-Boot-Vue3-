package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 完成审批/审查/修改任务的请求体。
 * <p>字段尽量宽松（可空），由具体环节决定哪些必填，便于前端按 Camunda Form 动态渲染后回传。</p>
 *
 * @author cz
 */
@Schema(description = "完成工作流任务请求")
public record WorkflowTaskCompleteRequest(
        @Schema(description = "结论：approved 通过 / rejected 退回 / issue 发现问题 / resolved 已修正", example = "approved")
        String decision,
        @Schema(description = "意见正文（审批意见 / 审查意见 / 修改说明），也可用 opinion 字段")
        String opinion,
        @Schema(description = "驳回/退回原因（可空，留空时取 opinion）")
        String rejectReason,
        @Schema(description = "暂不处理原因（可选）")
        String comment,
        @Schema(description = "Camunda Form 提交的表单数据，键为 form 字段 key")
        Map<String, Object> variables
) {

    /** 取意见正文：opinion 优先，其次 rejectReason / comment */
    public String resolveOpinion() {
        if (opinion != null && !opinion.isBlank()) {
            return opinion.trim();
        }
        if (rejectReason != null && !rejectReason.isBlank()) {
            return rejectReason.trim();
        }
        if (comment != null && !comment.isBlank()) {
            return comment.trim();
        }
        // 兜底：从 Camunda Form 数据里取常见字段名
        if (variables != null) {
            for (String key : new String[]{"opinion", "approvalOpinion", "inspectionOpinion", "revisionNote"}) {
                Object v = variables.get(key);
                if (v instanceof String s && !s.isBlank()) {
                    return s.trim();
                }
            }
        }
        return null;
    }

    /** 从 form 数据中推断结论（未显式传 decision 时） */
    public String resolveDecision(String fallback) {
        if (decision != null && !decision.isBlank()) {
            return decision.trim();
        }
        if (variables != null) {
            Object v = variables.get("decision");
            if (v instanceof String s && !s.isBlank()) {
                return s.trim();
            }
            Object approved = variables.get("approved");
            if (approved instanceof Boolean b) {
                return b ? "approved" : "rejected";
            }
            if (approved instanceof String s && !s.isBlank()) {
                return "true".equalsIgnoreCase(s) ? "approved" : "rejected";
            }
        }
        return fallback;
    }
}

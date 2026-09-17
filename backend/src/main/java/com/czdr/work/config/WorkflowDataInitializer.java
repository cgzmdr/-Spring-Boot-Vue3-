package com.czdr.work.config;

import com.czdr.work.model.entity.FormBinding;
import com.czdr.work.service.WorkflowDefinitionService;
import com.czdr.work.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时把内置 Camunda 流程与 Camunda Form 初始化到嵌入式引擎与本地表。
 *
 * <p>三件事：</p>
 * <ol>
 *   <li>把 classpath 下的 BPMN 资源部署到内嵌的 Camunda 7 引擎（幂等：每次部署生成新版本）；</li>
 *   <li>把流程绑定写进 process_binding；</li>
 *   <li>把三个 Camunda Form（审批 / 审查 / 修改）的 form-js schema 写进 form_binding，
 *       前端 FormRenderer 据此渲染，保证 UI 与 Element Plus 风格一致。</li>
 * </ol>
 *
 * <p><b>Camunda 7 嵌入式</b>：引擎就在本进程内、与业务共用同一个 PostgreSQL 库，
 * 因此这里不再有「远端集群不可用」的场景；仍保留 try/catch 是为了让
 * 「引擎表未初始化 / 流程 XML 写错」这类问题只告警不阻断整站启动 ——
 * 内容管理、检索等功能不依赖流程引擎。</p>
 *
 * @author cz
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class WorkflowDataInitializer implements ApplicationRunner {

    private final WorkflowService workflowService;
    private final WorkflowDefinitionService definitionService;

    @Value("${app.camunda.deploy-on-startup:true}")
    private boolean deployOnStartup;

    @Value("${app.camunda.init-forms:true}")
    private boolean initForms;

    @Override
    public void run(ApplicationArguments args) {
        if (initForms) {
            try {
                initFormBindings();
            } catch (Exception e) {
                log.error("初始化 Camunda Form 绑定失败（不影响启动）: {}", e.getMessage(), e);
            }
        }
        if (!deployOnStartup) {
            log.info("已关闭启动期 Camunda 部署（app.camunda.deploy-on-startup=false）");
            return;
        }
        try {
            workflowService.deployBuiltinResources();
        } catch (Exception e) {
            log.error("""
                    启动期部署 Camunda 流程失败 —— 审批功能将不可用。
                    Camunda 7 引擎为内嵌模式，请先确认引擎表已建好
                    （camunda.bpm.database.schema-update=true 会自动创建 ACT_* 表），
                    随后可在 OA「流程建模」页手动点「部署」重试。
                    原因: {}""", e.getMessage());
        }
    }

    /**
     * 初始化三个 Camunda Form：审批（reviewer）/ 审查（content_admin）/ 修改（editor）。
     * 已存在同 formId 的启用版本时跳过，保留管理员在网页 Modeler 里的修改。
     */
    private void initFormBindings() {
        seedForm("ethnic-approval-form", "民族-审核员审批表单", "approval");
        seedForm("ethnic-inspection-form", "民族-内容管理员审查表单", "inspection");
        seedForm("ethnic-revision-form", "民族-内容编辑修改表单", "revision");
    }

    private void seedForm(String formId, String name, String purpose) {
        FormBinding existing = definitionService.findFormByFormId(formId);
        if (existing != null) {
            log.debug("Camunda Form 已存在，跳过初始化: {}", formId);
            return;
        }
        definitionService.saveForm(new com.czdr.work.model.request.FormBindingSaveRequest(
                formId,
                name,
                purpose,
                "*",
                definitionService.buildStarterFormSchema(purpose, formId),
                true,
                "系统内置表单，可在「Camunda Modeler → 表单设计器」中调整"));
        log.info("初始化 Camunda Form: {}（{}）", formId, name);
    }
}

package com.czdr.work.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Camunda 7 嵌入式引擎的装配。
 *
 * <h3>关闭匿名遥测</h3>
 * Camunda 7 的 starter 配置项里<b>没有</b> telemetry 开关
 * （见 starter 的 {@code spring-configuration-metadata.json}：只有 database /
 * job-execution / history-level / webapp / metrics 等），
 * 引擎内部也没有可注入的 {@code TelemetryReporter}（7.19+ 已移除该扩展点）。
 * 7.24 提供的公开入口是 {@link ManagementService#toggleTelemetry(boolean)}，
 * 因此在应用启动后调用一次把遥测关掉，避免周期性向 Camunda 上报匿名使用统计。
 *
 * <p>关闭状态会写入引擎库（{@code ACT_GE_PROPERTY} 的 telemetry 相关键），
 * 因此只需执行一次；重复执行是幂等的，这里仍在每次启动时确认一次，
 * 保证「被手动打开」后下次启动会自动纠正。</p>
 *
 * <h3>为什么不引入 camunda-bpm-spring-boot-starter-webapp</h3>
 * 后台 UI 完全由本项目 admin 前端承担，鉴权走 Sa-Token。
 * 引入官方 webapp 会额外注册一整套 Spring Security 过滤器链与静态资源，
 * 与现有鉴权冲突，因此 build.gradle 中已排除，这里也无需任何 webapp 配置。
 *
 * <p>流程的启动期部署由 {@link WorkflowDataInitializer} 显式完成
 * （因为还要回填 {@code process_binding}），故配置里关闭了 starter 的
 * {@code auto-deployment-enabled}，避免同一份 BPMN 被部署两次。</p>
 *
 * @author cz
 */
@Slf4j
@Component
@Order(90)
@RequiredArgsConstructor
public class CamundaConfig implements ApplicationRunner {

    private final ProcessEngine processEngine;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ManagementService managementService = processEngine.getManagementService();
            if (Boolean.TRUE.equals(managementService.isTelemetryEnabled())) {
                managementService.toggleTelemetry(false);
                log.info("Camunda 7 匿名遥测上报已关闭（embedded 模式）");
            } else {
                log.debug("Camunda 7 遥测上报已处于关闭状态");
            }
        } catch (Exception e) {
            // 遥测开关失败不应影响启动
            log.warn("关闭 Camunda 遥测失败（不影响启动）: {}", e.getMessage());
        }
    }
}

package com.czdr.work.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.strategy.SaStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author cz
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfigure implements WebMvcConfigurer {

    private final StpInterfaceImpl stpInterface;

    /**
     * C 端只读、且当前实现为委派给 {@code /admin/**} 的接口白名单。
     * <p>这些路径在 C 端是公开展示数据（浏览量、反馈提交等），后台自身仍通过
     * {@code @SaCheckPermission} 做写操作鉴权；此处仅放行「无需登录即可读」的部分，
     * 避免 C 端未登录用户访问时被 Sa-Token 拦截为 1003。
     * <p>新增条目时务必确认对应接口不返回任何敏感字段。
     */
    private static final String[] ADMIN_PUBLIC_PATHS = {
            // 批量查询浏览量（C 端列表页展示浏览数）
            "/admin/view-counts",
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(ADMIN_PUBLIC_PATHS);
    }

    @PostConstruct
    public void initSaStrategy() {
        SaManager.setStpInterface(stpInterface);
        SaStrategy.instance.hasElement = (elementList, element) ->
                elementList.contains("*") || elementList.contains(element);
    }
}

package com.czdr.work.config;

import com.easy.query.core.basic.jdbc.types.handler.JdbcTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author cz
 */
@Configuration
public class EasyQueryJsonbAutoConfiguration {

    @Bean
    public Map<String, JdbcTypeHandler> easyQueryJdbcTypeHandlerMap() {
        return Map.of("pgsqlStringSupportJsonb", new PgSQLStringSupportJsonbTypeHandler());
    }
}

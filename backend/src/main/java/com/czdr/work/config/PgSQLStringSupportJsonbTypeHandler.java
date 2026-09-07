package com.czdr.work.config;

import com.easy.query.core.basic.jdbc.executor.internal.props.JdbcProperty;
import com.easy.query.core.basic.jdbc.executor.internal.merge.result.StreamResultSet;
import com.easy.query.core.basic.jdbc.types.EasyParameter;
import com.easy.query.core.basic.jdbc.types.handler.JdbcTypeHandler;
import com.easy.query.sql.starter.config.JdbcTypeHandlerReplaceConfigurer;
import org.postgresql.util.PGobject;

import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.Set;

/**
 * @author cz
 */
public class PgSQLStringSupportJsonbTypeHandler implements JdbcTypeHandler, JdbcTypeHandlerReplaceConfigurer {

    @Override
    public Object getValue(JdbcProperty jdbcProperty, StreamResultSet streamResultSet) throws SQLException {
        return streamResultSet.getString(jdbcProperty.getJdbcIndex());
    }

    @Override
    public void setParameter(EasyParameter parameter) throws SQLException {
        JDBCType jdbcType = parameter.getSQLParameter().getJdbcType();
        if (jdbcType == JDBCType.JAVA_OBJECT) {
            setJsonParameter(parameter);
        } else if (isJsonOrJsonArray(parameter)) {
            setJsonParameter(parameter);
        } else {
            parameter.getPs().setString(parameter.getIndex(), (String) parameter.getValue());
        }
    }

    private void setJsonParameter(EasyParameter parameter) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue((String) parameter.getValue());
        parameter.getPs().setObject(parameter.getIndex(), pgObject);
    }

    private boolean isJsonOrJsonArray(EasyParameter parameter) {
        Object value = parameter.getValue();
        if (value == null) {
            return false;
        }
        String s = value.toString().trim();
        return s.startsWith("{") || s.startsWith("[");
    }

    @Override
    public boolean replace() {
        return true;
    }

    @Override
    public Set<Class<?>> allowTypes() {
        return Set.of(String.class);
    }
}

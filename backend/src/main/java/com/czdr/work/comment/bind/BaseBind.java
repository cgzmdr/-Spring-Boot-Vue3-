package com.czdr.work.comment.bind;

import java.util.Map;

/**
 * @author cz
 */ // 定义基础绑定基类接口：将字段名与对应的处理方法绑定
public interface BaseBind<T> {
    Map<String, T> customize();
}

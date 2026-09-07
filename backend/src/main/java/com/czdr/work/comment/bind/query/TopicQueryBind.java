package com.czdr.work.comment.bind.query;

import com.czdr.work.comment.bind.BaseBind;
import com.czdr.work.model.entity.proxy.TopicProxy;
import com.czdr.work.model.request.TopicQueryInfoRequest;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author cz
 */
@RequiredArgsConstructor
public class TopicQueryBind implements BaseBind<Consumer<TopicProxy>> {
    private final TopicQueryInfoRequest request;

    @Override
    public Map<String, Consumer<TopicProxy>> customize() {
        HashMap<String, Consumer<TopicProxy>> map = new HashMap<>();
        map.put("keyword", proxy -> proxy.or(() -> {
            proxy.title().like(request.getKeyword());
            proxy.subtitle().like(request.getKeyword());
        }));
        return map;
    }
}

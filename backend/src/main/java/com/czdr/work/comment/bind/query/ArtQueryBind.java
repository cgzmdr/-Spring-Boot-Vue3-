package com.czdr.work.comment.bind.query;

import com.czdr.work.comment.bind.BaseBind;
import com.czdr.work.model.entity.proxy.ArtProxy;
import com.czdr.work.model.request.ArtQueryInfoRequest;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author cz
 */
@RequiredArgsConstructor
public class ArtQueryBind implements BaseBind<Consumer<ArtProxy>> {
    private final ArtQueryInfoRequest request;

    @Override
    public Map<String, Consumer<ArtProxy>> customize() {
        HashMap<String, Consumer<ArtProxy>> map = new HashMap<>();
        map.put("category", proxy -> proxy.category().eq(request.getCategory()));
        map.put("ethnicGroupId", proxy -> proxy.ethnicGroupId().eq(request.getEthnicGroupId()));
        map.put("intangibleHeritage", proxy -> proxy.intangibleHeritage().eq(request.getIntangibleHeritage()));
        map.put("keyword", proxy -> proxy.name().like(request.getKeyword()));
        return map;
    }
}

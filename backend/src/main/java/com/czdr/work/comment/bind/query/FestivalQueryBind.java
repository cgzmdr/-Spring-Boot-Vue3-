package com.czdr.work.comment.bind.query;

import com.czdr.work.comment.bind.BaseBind;
import com.czdr.work.model.entity.proxy.FestivalProxy;
import com.czdr.work.model.request.FestivalQueryInfoRequest;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author cz
 */
@RequiredArgsConstructor
public class FestivalQueryBind implements BaseBind<Consumer<FestivalProxy>> {
    private final FestivalQueryInfoRequest request;

    @Override
    public Map<String, Consumer<FestivalProxy>> customize() {
        HashMap<String, Consumer<FestivalProxy>> map = new HashMap<>();
        map.put("ethnicGroupId", proxy -> proxy.ethnicGroupId().eq(request.getEthnicGroupId()));
        map.put("type", proxy -> proxy.type().eq(request.getType()));
        map.put("month", proxy -> proxy.solarDate().month().eq(request.getMonth()));
        map.put("keyword", proxy -> proxy.name().like(request.getKeyword()));
        return map;
    }
}

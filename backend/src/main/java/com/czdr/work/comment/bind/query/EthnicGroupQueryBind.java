package com.czdr.work.comment.bind.query;

import com.czdr.work.comment.bind.BaseBind;
import com.czdr.work.model.entity.proxy.EthnicGroupProxy;
import com.czdr.work.model.more.Population;
import com.czdr.work.model.request.EthnicQueryInfoRequest;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
/**
 * @author cz
 */
@RequiredArgsConstructor
public class EthnicGroupQueryBind implements BaseBind<Consumer<EthnicGroupProxy>> {
    private final EthnicQueryInfoRequest request;
    @Override
    public Map<String, Consumer<EthnicGroupProxy>> customize() {
        Map<String, Consumer<EthnicGroupProxy>> map = new HashMap<>();
        map.put("region", ethnicGroupProxy -> {
           ethnicGroupProxy.region().asAny().toStr().like(request.getRegion());
        });
        map.put("languageFamily", ethnicGroupProxy -> {
           ethnicGroupProxy.languageFamily().like(request.getLanguageFamily());
        });
        map.put("population", ethnicGroupProxy -> {
            Population population = request.getPopulation();
            if (population != null) {
                ethnicGroupProxy.population().gt(population.getMin());
                ethnicGroupProxy.population().lt(population.getMax());
            }
        });
        map.put("keyword", ethnicGroupProxy -> {
            ethnicGroupProxy.name().like(request.getKeyword());
        });
        return map;
    }
}

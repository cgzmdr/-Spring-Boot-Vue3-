package com.czdr.work.comment.bind.query;

import com.czdr.work.comment.bind.BaseBind;
import com.czdr.work.model.entity.proxy.UserAuthProxy;
import com.czdr.work.model.request.UserQueryInfoRequest;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author cz
 */
@RequiredArgsConstructor
public class UserQueryBind implements BaseBind<Consumer<UserAuthProxy>> {
    private final UserQueryInfoRequest request;
    @Override
    public Map<String, Consumer<UserAuthProxy>> customize() {
        HashMap<String, Consumer<UserAuthProxy>> map = new HashMap<>();
        map.put("nickname", proxy -> {
            proxy.nickname().like(request.getNickname().trim());
        });
        map.put("mobile", proxy -> {
            proxy.mobile().like(request.getMobile());
        });
        map.put("email", proxy -> {
            proxy.email().like(request.getEmail());
        });
        map.put("roleName", proxy -> {
            proxy.roles().filter(roleProxy -> {
                roleProxy.name().like(request.getRoleName());
            });
        });
        map.put("code", proxy -> {
            proxy.roles().filter(roleProxy -> {
                roleProxy.code().like(request.getRoleName());
            });
        });
        map.put("description", proxy -> {
            proxy.roles().filter(roleProxy -> {
                roleProxy.description().like(request.getRoleName());
            });
        });
        return map;
    }
}

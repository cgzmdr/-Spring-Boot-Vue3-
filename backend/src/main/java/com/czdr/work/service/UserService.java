package com.czdr.work.service;

import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.request.PasswordChangeRequest;
import com.czdr.work.model.request.UserQueryInfoRequest;
import com.czdr.work.model.request.UserUpdateInfoRequest;

import java.util.List;

/**
 * @author cz
 */
public interface UserService {
    void create(String nickname,String password);

    UserAuth findById(String loginId);

    String login(String account, String password);

    void update(String id, UserUpdateInfoRequest request);

    /** 修改密码：必须通过 旧密码 / 验证码 / 密保答案 三者之一完成验证 */
    void changePassword(String loginId, PasswordChangeRequest request);

    List<UserAuth> find(UserQueryInfoRequest request);
}

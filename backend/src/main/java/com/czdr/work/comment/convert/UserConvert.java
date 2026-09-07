package com.czdr.work.comment.convert;

import com.czdr.work.model.entity.Role;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.resource.UserInfoResource;
import com.czdr.work.model.resource.UserQueryInfoResource;

import java.util.List;

/**
 * @author cz
 */
public class UserConvert {
    public static UserInfoResource toInfoModel(UserAuth userAuth){
        return new UserInfoResource(
                userAuth.getId(),
                userAuth.getNickname(),
                userAuth.getAvatar(),
                // 界面语言偏好（登录后前端恢复该语言）
                userAuth.getLang() == null || userAuth.getLang().isBlank() ? "zh" : userAuth.getLang(),
                // 返回角色编码（如 super_admin），供前端按编码做权限/菜单过滤
                userAuth.getRoles().stream().map(Role::getCode).toArray(String[]::new),
                // 绑定信息（空串表示未绑定，便于前端直接展示）
                userAuth.getMobile() == null ? "" : userAuth.getMobile(),
                userAuth.getEmail() == null ? "" : userAuth.getEmail(),
                userAuth.getCreatedAt(),
                // 密保问题：未设置时返回 null，前端据此展示「设置密保」入口
                userAuth.getSecurityQuestion() == null || userAuth.getSecurityQuestion().isBlank() ? null : userAuth.getSecurityQuestion()
        );
    }
    public static UserQueryInfoResource toQueryInfoModel(UserAuth userAuth){
        List<Role> roles = userAuth.getRoles();
        return new UserQueryInfoResource(
                userAuth.getId(),
                userAuth.getAccount(),
                userAuth.getNickname(),
                userAuth.getAvatar(),
                userAuth.getMobile(),
                userAuth.getEmail(),
                userAuth.getStatus(),
                userAuth.getCreatedAt(),
                userAuth.getUpdatedAt(),
                roles == null ? new Role[]{} : roles.toArray(new Role[]{})
        );
    }
}

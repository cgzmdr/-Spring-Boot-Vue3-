package com.czdr.work.service.impl;

import com.czdr.work.comment.bind.query.UserQueryBind;
import com.czdr.work.comment.bind.update.UserUpdateBind;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.config.AesCbcEncryptor;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.entity.Role;
import com.czdr.work.model.entity.UserRole;
import com.czdr.work.model.entity.proxy.RoleProxy;
import com.czdr.work.model.entity.proxy.UserAuthProxy;
import com.czdr.work.model.enums.WhereStatus;
import com.czdr.work.model.request.PasswordChangeRequest;
import com.czdr.work.model.request.UserQueryInfoRequest;
import com.czdr.work.model.request.UserUpdateInfoRequest;
import com.czdr.work.service.MailService;
import com.czdr.work.service.UserService;
import com.czdr.work.util.RegexValidatorUtil;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final EasyEntityQuery entityQuery;
    private final AesCbcEncryptor aesCbcEncryptor;
    private final MailService mailService;
    private Map<String, Object> map;

    @Override
    public void create(String nickname, String password) {
        UUID uuid = UUID.randomUUID();
        String passwordHash;
        try {
            passwordHash = aesCbcEncryptor.encrypt(password);
        }catch (Exception e){
            e.fillInStackTrace();
            throw new BusinessException(ErrorCode.SERVER_ERROR, "密码加密失败");
        }
        UserAuth userAuth = new UserAuth(uuid, nickname, passwordHash);
        userAuth.setAccount(nickname);
        try {
            entityQuery.insertable(userAuth).executeRows();
        }catch (Exception e){
            e.fillInStackTrace();
            throw new BusinessException(ErrorCode.SERVER_ERROR, "注册失败，请稍后重试");
        }
        // 注册用户默认绑定 user 角色（浏览 + 互动）
        Role userRole = entityQuery.queryable(Role.class)
                .where(r -> r.code().eq("user"))
                .firstOrNull();
        if (userRole != null) {
            entityQuery.insertable(new UserRole(uuid, userRole.getId())).executeRows();
        }
    }

    @Override
    public UserAuth findById(String loginId) {
        UUID uuid = UUID.fromString(loginId);
        return entityQuery.queryable(UserAuth.class)
                .where(u->{
                    u.id().eq(uuid);
                })
                .include(UserAuthProxy::roles)
                .firstNotNull();
    }

    @Override
    public String login(String account, String password) {
        // 登录账号支持 昵称(用户名)、邮箱、手机号 任一匹配（account 列无实际业务含义，不参与登录）
        UserAuth userAuth = entityQuery.queryable(UserAuth.class)
                .where(u -> {
                    u.or(() -> {
                        u.nickname().eq(account);
                        u.email().eq(account);
                        u.mobile().eq(account);
                    });
                })
                .firstOrNull();
        if (userAuth == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        String storedHash = userAuth.getPasswordHash();
        if (storedHash == null || storedHash.isBlank()) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        String decrypt;
        try {
            decrypt = aesCbcEncryptor.decrypt(storedHash);
        } catch (Exception e) {
            e.fillInStackTrace();
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        if (!decrypt.equals(password)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "账号或密码错误");
        }
        if ("disabled".equals(userAuth.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }
        // 记录活跃时间：后台公告的「活跃用户」受众依赖该字段
        userAuth.setLastActiveAt(LocalDateTime.now());
        entityQuery.updatable(userAuth).executeRows();
        return userAuth.getId().toString();
    }

    @Override
    public void update(String id, UserUpdateInfoRequest request) {
        UserAuth userAuth = findById(id);
        Map<String, Runnable> binds = new UserUpdateBind(userAuth, request, aesCbcEncryptor).customize();
        request.toMap().forEach((field, value) -> {
            // 空字符串字段视为"未提交"；avatar 特殊处理：空串表示清除头像
            if (field.equals("avatar")) {
                if (value == null) {
                    return;
                }
            } else if (value == null || value.toString().isBlank()) {
                return;
            }
            Runnable action = binds.get(field);
            if (action != null) {
                action.run();
            }
        });
        userAuth.setUpdatedAt(LocalDate.now());
        entityQuery.updatable(userAuth).executeRows();
    }

    @Override
    public void changePassword(String loginId, PasswordChangeRequest request) {
        UserAuth userAuth = findById(loginId);

        String newPassword = request.newPassword();
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能为空");
        }
        RegexValidatorUtil.ValidationResult validate = RegexValidatorUtil.validatePassword(newPassword);
        if (!validate.valid()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码需为 8 位以上，且包含大写字母、小写字母、数字和特殊字符");
        }

        boolean hasOld = request.oldPassword() != null && !request.oldPassword().isBlank();
        boolean hasCode = request.code() != null && !request.code().isBlank();
        boolean hasSecurity = request.securityAnswer() != null && !request.securityAnswer().isBlank();
        int methods = (hasOld ? 1 : 0) + (hasCode ? 1 : 0) + (hasSecurity ? 1 : 0);
        if (methods != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择且仅选择一种验证方式（旧密码 / 验证码 / 密保答案）");
        }

        if (hasOld) {
            String storedHash = userAuth.getPasswordHash();
            try {
                if (storedHash == null || storedHash.isBlank() || !aesCbcEncryptor.decrypt(storedHash).equals(request.oldPassword())) {
                    throw new BusinessException(ErrorCode.LOGIN_FAILED, "原密码不正确");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.LOGIN_FAILED, "原密码校验失败");
            }
        }

        if (hasCode) {
            String account = request.account() == null ? "" : request.account().trim();
            if (account.isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "请输入接收验证码的账号（邮箱 / 手机号）");
            }
            if (!mailService.verifyCode(account, request.code())) {
                throw new BusinessException(ErrorCode.CODE_INVALID);
            }
        }

        if (hasSecurity) {
            String storedAnswer = userAuth.getSecurityAnswer();
            if (userAuth.getSecurityQuestion() == null || userAuth.getSecurityQuestion().isBlank()
                    || storedAnswer == null || storedAnswer.isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "尚未设置密保问题，请先设置后再使用该方式");
            }
            try {
                if (!aesCbcEncryptor.decrypt(storedAnswer).equals(request.securityAnswer().trim())) {
                    throw new BusinessException(ErrorCode.LOGIN_FAILED, "密保答案不正确");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.LOGIN_FAILED, "密保答案校验失败");
            }
        }

        // 校验通过后更新密码
        try {
            userAuth.setPasswordHash(aesCbcEncryptor.encrypt(newPassword));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR, "密码加密失败");
        }
        userAuth.setUpdatedAt(LocalDate.now());
        entityQuery.updatable(userAuth).executeRows();
    }

    @Override
    public List<UserAuth> find(UserQueryInfoRequest request) {
        Map<String, Consumer<UserAuthProxy>> binds = new UserQueryBind(request).customize();
        return entityQuery.queryable(UserAuth.class)
                .where(u->{
                    if (request.getStatus().equals(WhereStatus.OR)){
                        u.or(()->{
                            applyBindings(request, u, binds);
                        });
                    }else if (request.getStatus().equals(WhereStatus.AND)){
                        u.and(()->{
                            applyBindings(request, u, binds);
                        });
                    }
                })
                .include(
                        UserAuthProxy::roles,
                        roleProxyRoleEntityQueryable ->
                                roleProxyRoleEntityQueryable.include(RoleProxy::permissions)
                )
                .orderBy(UserAuthProxy::id)
                .toList();
    }

    private static void applyBindings(UserQueryInfoRequest request, UserAuthProxy u, Map<String, Consumer<UserAuthProxy>> binds) {
        request.toMap().forEach((field, value) -> {
            if (value == null || value.toString().isBlank()) {
                return;
            }
            Consumer<UserAuthProxy> runnable = binds.get(field);
            if (runnable != null) {
                runnable.accept(u);
            }
        });
    }
}

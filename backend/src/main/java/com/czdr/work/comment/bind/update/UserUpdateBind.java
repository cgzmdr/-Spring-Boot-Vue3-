package com.czdr.work.comment.bind.update;

import com.czdr.work.comment.bind.BaseBind;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.config.AesCbcEncryptor;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.request.UserUpdateInfoRequest;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author cz
 */
@RequiredArgsConstructor
public class UserUpdateBind implements BaseBind<Runnable> {
    private final UserAuth entity;
    private final UserUpdateInfoRequest request;
    private final AesCbcEncryptor aesCbcEncryptor;

    @Override
    public Map<String, Runnable> customize() {
        HashMap<String, Runnable> map = new HashMap<>();
        map.put("nickname", () -> entity.setNickname(request.getNickname()));
        map.put("avatar", () -> entity.setAvatar(request.getAvatar()));
        map.put("email", () -> entity.setEmail(request.getEmail()));
        map.put("mobile", () -> entity.setMobile(request.getMobile()));
        map.put("status", () -> entity.setStatus(request.getStatus()));
        map.put("lang", () -> {
            String lang = request.getLang();
            if (lang != null && !"zh".equals(lang) && !"en".equals(lang)) {
                lang = "zh";
            }
            entity.setLang(lang == null || lang.isBlank() ? "zh" : lang);
        });
        map.put("password", () -> {
            try {
                entity.setPasswordHash(aesCbcEncryptor.encrypt(request.getPassword()));
            } catch (Exception e) {
                throw new BusinessException(404, "密码加密失败");
            }
        });
        map.put("securityQuestion", () -> entity.setSecurityQuestion(request.getSecurityQuestion()));
        map.put("securityAnswer", () -> {
            try {
                entity.setSecurityAnswer(aesCbcEncryptor.encrypt(request.getSecurityAnswer()));
            } catch (Exception e) {
                throw new BusinessException(404, "密保答案加密失败");
            }
        });
        return map;
    }
}

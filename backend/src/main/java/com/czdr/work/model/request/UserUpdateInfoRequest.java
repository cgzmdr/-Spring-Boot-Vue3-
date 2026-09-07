package com.czdr.work.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author cz
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "修改用户信息请求")
public class UserUpdateInfoRequest {
    @Schema(description = "用户昵称")
    private String nickname;
    @Schema(description = "头像（图片 URL 或 Data URL）")
    private String avatar;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "手机号")
    private String mobile;
    //disabled
    @Schema(description = "用户状态（active/disabled）")
    private String status;
    @Schema(description = "界面语言偏好（zh / en）")
    private String lang;
    @Schema(description = "新密码")
    private String password;
    @Schema(description = "密保问题（明文问题文本）")
    private String securityQuestion;
    @Schema(description = "密保答案（明文，后端加密存储）")
    private String securityAnswer;

    public Map<String, Object> toMap(){
        HashMap<String, Object> map = new HashMap<>();
        map.put("nickname", this.nickname);
        map.put("avatar", this.avatar);
        map.put("email", this.email);
        map.put("mobile", this.mobile);
        map.put("status", this.status);
        map.put("lang", this.lang);
        map.put("password", this.password);
        map.put("securityQuestion", this.securityQuestion);
        map.put("securityAnswer", this.securityAnswer);
        return map;
    }
}

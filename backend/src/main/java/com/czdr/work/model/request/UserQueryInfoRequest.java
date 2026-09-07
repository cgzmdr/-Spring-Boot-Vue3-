package com.czdr.work.model.request;

import com.czdr.work.model.enums.WhereStatus;
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
@Schema(description = "用户列表查询条件")
public class UserQueryInfoRequest {
    @Schema(description = "用户昵称（模糊匹配）")
    private String nickname;
    @Schema(description = "手机号（模糊匹配）")
    private String mobile;
    @Schema(description = "邮箱（模糊匹配）")
    private String email;
    @Schema(description = "角色名称（模糊匹配）")
    private String roleName;
    @Schema(description = "角色编码（模糊匹配）")
    private String code;
    @Schema(description = "角色描述（模糊匹配）")
    private String description;
    @Schema(description = "多条件连接方式（AND/OR）")
    private WhereStatus status;
    public Map<String,Object> toMap(){
        Map<String,Object> map = new HashMap<>();
        map.put("nickname",nickname);
        map.put("mobile",mobile);
        map.put("email",email);
        map.put("roleName",roleName);
        map.put("code",code);
        map.put("description",description);
        return map;
    }
}

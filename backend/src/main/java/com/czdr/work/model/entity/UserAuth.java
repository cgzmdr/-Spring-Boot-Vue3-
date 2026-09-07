package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.RoleProxy;
import com.czdr.work.model.entity.proxy.UserAuthProxy;
import com.easy.query.core.annotation.*;
import com.easy.query.core.enums.RelationTypeEnum;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "user_account")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class UserAuth implements ProxyEntityAvailable<UserAuth,UserAuthProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String account;
    public String passwordHash;
    public String nickname;
    public String avatar;
    public String mobile;
    public String email;
    public String status;
    /** 界面语言偏好（zh / en），登录后随用户持久化 */
    public String lang;
    /** 密保问题（明文问题文本，为空表示未设置） */
    public String securityQuestion;
    /** 密保答案（AES 加密存储） */
    public String securityAnswer;
    public LocalDate createdAt;
    public LocalDate updatedAt;

    @Navigate(
            value = RelationTypeEnum.ManyToMany,
            mappingClass = UserRole.class,
            selfMappingProperty = "userId",
            targetMappingProperty = "roleId",
            selfProperty = "id",
            targetProperty = "id"
    )
    public List<Role> roles;

    @Navigate(
            value = RelationTypeEnum.OneToMany,
            selfProperty = "id",
            targetProperty = "userId"
    )
    public List<Favorite> favorites;

    @Navigate(
            value = RelationTypeEnum.OneToMany,
            selfProperty = "id",
            targetProperty = "userId"
    )
    public List<LikeRecord> likes;

    public UserAuth(UUID id, String nickname, String passwordHash) {
        this.id = id;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.account = "";
        this.avatar = "";
        this.mobile = "";
        this.email = "";
        this.status = "active";
        this.lang = "zh";
        this.securityQuestion = "";
        this.securityAnswer = "";
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }
}

package com.czdr.work.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.convert.UserConvert;
import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.entity.UserAuth;
import com.czdr.work.model.request.PasswordChangeRequest;
import com.czdr.work.model.request.RegisterRequest;
import com.czdr.work.model.request.UserQueryInfoRequest;
import com.czdr.work.model.request.UserUpdateInfoRequest;
import com.czdr.work.model.resource.AuthResource;
import com.czdr.work.model.resource.RegisterResource;
import com.czdr.work.model.resource.UserInfoResource;
import com.czdr.work.model.resource.UserQueryInfoResource;
import com.czdr.work.service.MailService;
import com.czdr.work.service.UserService;
import com.czdr.work.util.RegexValidatorUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * @author cz
 */
@Slf4j
@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name = "认证与用户 Auth", description = "认证与用户模块：注册 / 登录 / 登出 / 当前用户信息 / 验证码 / 修改信息 / 用户列表查询")
public class AuthController {

    private final UserService userService;

    private final MailService mailService;

    @Operation(summary = "用户注册", description = "使用昵称与密码注册账号（昵称、手机号、邮箱均可作为登录账号）")
    @PostMapping("users")
    Result<RegisterResource> user(@RequestBody RegisterRequest request){
        String nickname = request.nickname();
        String password = request.password();
        RegexValidatorUtil.ValidationResult validateNickname = RegexValidatorUtil.validateNickname(nickname);
        if(!validateNickname.valid()){
            return Result.error(ErrorCode.PARAM_ERROR, validateNickname.message());
        }
        RegexValidatorUtil.ValidationResult validatePassword = RegexValidatorUtil.validatePassword(password);
        if(!validatePassword.valid()){
            return Result.error(ErrorCode.PARAM_ERROR, validatePassword.message());
        }
        try {
            userService.create(nickname,password);
        } catch (BusinessException e) {
            return Result.error(e.getCode(),e.getMessage());
        }
        RegisterResource resource = new RegisterResource(nickname,LocalDate.now());

        return Result.success(resource);
    };

    @Operation(summary = "当前用户信息", description = "获取当前登录用户的信息（需登录）")
    @SaCheckLogin
    @GetMapping("me")
    Result<UserInfoResource> me(){
        String loginId = StpUtil.getLoginIdAsString();
        UserAuth userAuth = userService.findById(loginId);
        UserInfoResource infoModel = UserConvert.toInfoModel(userAuth);
        return Result.success(infoModel);
    };

    @Operation(summary = "发送验证码", description = "向指定账号（邮箱/手机号）发送验证码（预留接口）")
    @GetMapping("code")
    void code(@RequestParam("account") @Parameter(description = "账号（邮箱 / 手机号）") String account){
        RegexValidatorUtil.AccountType accountType = RegexValidatorUtil.resolveAccountType(account);
        String code = createCode(6);
        log.info(code);
        switch(accountType){
            case EMAIL: mailService.sendMail(account,"走进多彩 56 个民族世界",code);
            break;
            case MOBILE: mailService.sendMail(account,"走进多彩 56 个民族世界",code);
            break;
        }
    }

    @Operation(summary = "校验验证码", description = "校验指定账号收到的验证码是否正确")
    @GetMapping("check")
    Result<?> check(
            @RequestParam("account") @Parameter(description = "账号（邮箱 / 手机号）") String account,
            @RequestParam("code") @Parameter(description = "验证码") String code
    ){
        boolean b = mailService.verifyCode(account, code);
        if(b){
            return Result.success("校验成功");
        }else {
            return Result.error(ErrorCode.CODE_INVALID);
        }
    }

    @Operation(summary = "用户登录", description = "使用账号（邮箱/手机号/昵称）与密码登录，返回登录凭证")
    @PostMapping("login")
    Result<String> login(@RequestBody AuthResource resource){
        String account = resource.account();
        String password = resource.password();
        if (account == null || account.isBlank()) {
            return Result.error(ErrorCode.PARAM_ERROR, "账号不能为空");
        }
        if (password == null || password.isBlank()) {
            return Result.error(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        String id = userService.login(account,password);
        StpUtil.login(id);
        return Result.success(StpUtil.getTokenValue());
    }

    @Operation(summary = "修改用户信息", description = "按 ID 修改用户信息（需登录）")
    @SaCheckLogin
    @PutMapping("{id}")
    Result<?> update(@PathVariable @Parameter(description = "用户 ID") String id, @RequestBody UserUpdateInfoRequest request){
        try {
            userService.update(id, request);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
        return Result.success(null);
    }

    @Operation(summary = "修改密码", description = "修改当前登录用户密码，需通过旧密码 / 验证码 / 密保答案之一完成验证（需登录）")
    @SaCheckLogin
    @PostMapping("password")
    Result<?> changePassword(@RequestBody PasswordChangeRequest request){
        try {
            userService.changePassword(StpUtil.getLoginIdAsString(), request);
        } catch (BusinessException e) {
            return Result.error(e.getCode(), e.getMessage());
        }
        return Result.success(null);
    }

    @Operation(summary = "用户列表查询", description = "后台管理：按条件查询用户列表（需 user:list 权限）")
    @SaCheckPermission("user:list")
    @PostMapping
    Result<List<UserQueryInfoResource>> find(@RequestBody UserQueryInfoRequest request){
        List<UserAuth> userAuths = userService.find(request);
        List<UserQueryInfoResource> list = userAuths.stream().map(UserConvert::toQueryInfoModel).toList();
        return Result.success(list);
    }

    @Operation(summary = "用户登出", description = "登出并销毁当前登录会话（需登录）")
    @SaCheckLogin
    @PostMapping("logout")
    void logout(){
        StpUtil.logout();
    }

    @Operation(summary = "刷新 Token", description = "滑动续期刷新登录态并返回最新凭证（需登录）")
    @SaCheckLogin
    @PostMapping("refresh")
    Result<String> refresh(){
        String tokenValue = StpUtil.getTokenValue();
        StpUtil.updateLastActiveToNow();
        return Result.success(tokenValue);
    }

    private String createCode(int length){
        Random random = new Random();
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            codeBuilder.append(random.nextInt(10));
        }
        return codeBuilder.toString();
    }
}

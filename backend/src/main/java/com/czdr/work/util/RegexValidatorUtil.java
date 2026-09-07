package com.czdr.work.util;

import java.util.regex.Pattern;

/**
 * @author cz
 */

/**
 * 通用正则表达式校验工具类 (Java 21 增强版)
 */
public final class RegexValidatorUtil {

    private RegexValidatorUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 账户类型枚举
     */
    public enum AccountType {
        MOBILE, EMAIL, UNKNOWN
    }

    /**
     * 校验结果载体
     */
    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() {
            return new ValidationResult(true, "校验通过");
        }
        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }

    // ================= 预编译 Pattern (生产环境必备) =================
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    // 昵称校验：4-16位，允许中文、英文、数字、下划线
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9_]{4,16}$");
    private static final String REGEX_STRONG_PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"; // 强密码
    // ================= 新增功能 =================
    /**
     * 自定义正则校验
     * @param regex 正则表达式
     * @param input 待校验字符串
     * @return 校验结果
     */
    public static ValidationResult validate(String regex, String input) {
        if (regex == null || input == null) {
            return ValidationResult.fail("正则表达式和待校验字符串不能为空");
        }
        boolean matches = Pattern.matches(regex, input);
        return matches ? ValidationResult.success() : ValidationResult.fail("不符合指定的正则规则");
    }
    /**
     * 校验用户昵称
     * @param nickname 待校验昵称
     * @return 校验结果
     */
    public static ValidationResult validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return ValidationResult.fail("昵称不能为空");
        }
        boolean matches = NICKNAME_PATTERN.matcher(nickname).matches();
        return matches ? ValidationResult.success() : ValidationResult.fail("昵称长度需为4-16位，且仅支持中英文、数字和下划线");
    }

    /**
     * 智能判断账户类型 (利用 Java 21 Switch 表达式)
     * @param account 待判断的账户字符串
     * @return AccountType 枚举
     */
    public static AccountType resolveAccountType(String account) {
        if (account == null || account.isBlank()) {
            return AccountType.UNKNOWN;
        }
        // 使用 switch 表达式，逻辑清晰且无冗余代码
        return switch (account) {
            case String s when MOBILE_PATTERN.matcher(s).matches() -> AccountType.MOBILE;
            case String s when EMAIL_PATTERN.matcher(s).matches() -> AccountType.EMAIL;
            default -> AccountType.UNKNOWN;
        };
    }

    // ================= 原有基础功能 =================

    public static ValidationResult validateMobile(String mobile) {
        if (mobile == null) {
            return ValidationResult.fail("手机号不能为空");
        }
        return MOBILE_PATTERN.matcher(mobile).matches() ? ValidationResult.success() : ValidationResult.fail("手机号格式不正确");
    }

    public static ValidationResult validateEmail(String email) {
        if (email == null) {
            return ValidationResult.fail("邮箱不能为空");
        }
        return EMAIL_PATTERN.matcher(email).matches() ? ValidationResult.success() : ValidationResult.fail("邮箱格式不正确");
    }
    public static ValidationResult validatePassword(String password) {
        return validate(REGEX_STRONG_PASSWORD, password);
    }
    // ================= 测试入口 =================
    public static void main(String[] args) {
        // 测试昵称校验
        System.out.println(validateNickname("Java21_User")); // ValidationResult[valid=true, message=校验通过]
        System.out.println(validateNickname("ab"));          // ValidationResult[valid=false, message=昵称长度需为4-16位...]
        System.out.println(validateNickname("User@Name"));   // ValidationResult[valid=false, message=昵称长度需为4-16位...]

        // 测试账户类型判断
        System.out.println(resolveAccountType("13800138000"));      // MOBILE
        System.out.println(resolveAccountType("test@example.com")); // EMAIL
        System.out.println(resolveAccountType("admin"));            // UNKNOWN
    }
}
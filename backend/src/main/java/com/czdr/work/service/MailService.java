package com.czdr.work.service;

/**
 * @author cz
 */
public interface MailService {

    /**
     * 发送「验证码」邮件：使用 dist/sendemail.html 模板渲染 {{code}}，
     * 并把验证码写入 Redis 供 {@link #verifyCode} 校验。仅用于校验类邮件（注册/改密）。
     */
    void sendMail(String to, String subject, String content);

    /**
     * 发送「通知 / 公告」HTML 邮件：与验证码通道完全隔离
     * ——不套验证码模板、不写验证码缓存（否则站内通知会被当成验证码）。
     * 失败由调用方捕获并降级（站内通知已落库，不因邮件失败而失败）。
     */
    void sendHtml(String to, String subject, String html);

    boolean verifyCode(String account, String code);
}

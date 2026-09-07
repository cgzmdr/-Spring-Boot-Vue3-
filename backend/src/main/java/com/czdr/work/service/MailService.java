package com.czdr.work.service;

/**
 * @author cz
 */
public interface MailService {
    void sendMail(String to, String subject, String content);
    boolean verifyCode(String account,String code);
}

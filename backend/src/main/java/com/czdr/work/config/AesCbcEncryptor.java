package com.czdr.work.config;

import com.czdr.work.util.AesCbcUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author cz
 */
@Component
public class AesCbcEncryptor {

    @Value("${app.crypto.aes-key}")
    private String key;

    @Value("${app.crypto.aes-iv}")
    private String iv;

    public String encrypt(String data) throws Exception {
        return AesCbcUtil.encrypt(data, key, iv);
    }

    public String decrypt(String data) throws Exception {
        return AesCbcUtil.decrypt(data, key, iv);
    }
}

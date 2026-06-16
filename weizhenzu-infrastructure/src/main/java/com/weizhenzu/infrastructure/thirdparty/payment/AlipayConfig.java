package com.weizhenzu.infrastructure.thirdparty.payment;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置
 *
 * @author weizhenzu
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    private String appId;
    private String privateKey;
    private String publicKey;
    private String gateway;
    private String charset = "UTF-8";
    private String signType = "RSA2";
    private String notifyUrl;
    private String returnUrl;

    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                gateway, appId, privateKey, "json", charset, publicKey, signType);
    }
}

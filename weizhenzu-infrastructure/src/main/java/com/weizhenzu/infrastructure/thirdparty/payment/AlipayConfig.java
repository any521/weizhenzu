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

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 应用私钥
     */
    private String privateKey;

    /**
     * 支付宝公钥
     */
    private String publicKey;

    /**
     * 网关地址
     */
    private String gateway;

    /**
     * 字符集
     */
    private String charset = "UTF-8";

    /**
     * 签名类型
     */
    private String signType = "RSA2";

    /**
     * 数据格式
     */
    private String format = "json";

    /**
     * 收款账号（沙箱默认与 app-id 相同）
     */
    private String sellerId;

    /**
     * 异步通知 URL
     */
    private String notifyUrl;

    /**
     * 同步跳转 URL
     */
    private String returnUrl;

    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                gateway, appId, privateKey, format, charset, publicKey, signType);
    }
}

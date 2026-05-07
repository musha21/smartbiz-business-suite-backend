package com.example.smartBiz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payhere")
public class PayHereConfig {

    private String merchantId;
    private String merchantSecret;
    private boolean sandbox = true;
    private String notifyUrl;
    private String returnUrl;
    private String cancelUrl;
    private String currency = "LKR";

    /**
     * Returns the PayHere checkout URL based on sandbox flag.
     */
    public String getCheckoutUrl() {
        return sandbox
                ? "https://sandbox.payhere.lk/pay/checkout"
                : "https://www.payhere.lk/pay/checkout";
    }
}

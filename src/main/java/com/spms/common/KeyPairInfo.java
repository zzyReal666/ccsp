package com.spms.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.security.KeyStore;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyPairInfo implements Serializable {

    /** 密钥类型 */
    private String secretKeyType;

    /**
     * 密钥索引
     */
    private int secretKeyIndex;

    /** 密钥用途 */
    private int secretKeyUsage;

    /** 密钥长度 */
    private Integer secretKeyModuleLength;

    /** 私钥 */
    private String privateKey;

    /** 公钥 */
    private String publicKey;
    /**
     * 开始时间
     */
    private String lifeCycleStartTime;

    /**
     * 结束时间
     */
    private String lifeCycleEndTime;
    /**
     * ECC 密钥存储文件
     */
    private KeyStore keyStore;



}

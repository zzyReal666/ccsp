package com.spms.dbhsm.stockDataProcess.algorithm;

import com.spms.common.spi.typed.TypedSPI;

import java.util.Map;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/5/21 15:34
 */
public interface AlgorithmSPI extends TypedSPI {

    //加密
    String encrypt(String data, String key, Map<String, String> props);

    //解密
    String decrypt(String data, String key, Map<String, String> props);
}

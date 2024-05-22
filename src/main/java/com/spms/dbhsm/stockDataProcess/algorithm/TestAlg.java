package com.spms.dbhsm.stockDataProcess.algorithm;

import java.util.Map;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/5/21 15:48
 */
public class TestAlg implements AlgorithmSPI{
    @Override
    public String encrypt(String data, String key, Map<String, String> props) {

        return "encrypt" + "-"+data + "-" + key;
    }

    @Override
    public String decrypt(String data, String key, Map<String, String> props) {
        return "decrypt" + "-"+data + "-" + key;
    }

    @Override
    public String getType() {
        return "TestAlg";
    }
}

package com.spms.dbhsm.secretKey.domain;

import lombok.Data;

/**
 * @author 18853
 */
@Data
public class KeyResponse {
    private String code;
    private String message;
    private Data data;


    @lombok.Data
    public static class Data {
        private String keyCode;
        private String expirationTime;
        private String symmetricKey;

    }
}

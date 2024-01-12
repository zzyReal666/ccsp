package com.spms.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * @project ccsp
 * @description 达梦数据库错误码映射
 * @author 18853
 * @date 2023/12/26 15:59:47
 * @version 1.0
 */
public class DMErrorCode {
    /** 对象已存性 */
    public static String OBJECT_ALREADY_EXISTS = "-2124";

    public static Map<String, String> dmErrorCodeMap = new HashMap();
    static {
        dmErrorCodeMap.put(OBJECT_ALREADY_EXISTS, "对象已存在！");
    }

    public static String getErrorMessage(String errorCode) {
        return dmErrorCodeMap.get(errorCode);
    }

    public static void main(String[] args) {
        System.out.println(getErrorMessage("1111"));
        System.out.println(getErrorMessage(OBJECT_ALREADY_EXISTS));
    }
}

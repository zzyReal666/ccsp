package com.spms.common;

/**
 * @project ccsp
 * @description 数据库加密网关字符串处理
 * @author 18853
 * @date 2023/12/20 14:49:22
 * @version 1.0
 */
public class DBStringUtil {

    /**
     * 去掉路径后缀
     * @param filePath
     * @return
     */
    public static String removeFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf(".");
        if (lastDotIndex != -1) {
            return filePath.substring(0, lastDotIndex);
        } else {
            return filePath;
        }
    }


}

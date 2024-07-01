package com.spms.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zzypersonally@gmail.com
 * @description 各个文件上传的路径枚举
 * @since 2024/2/29 18:01
 */
@Getter
@AllArgsConstructor
public enum PathEnum {


    //数据源
    DATA_SOURCE("/ZA-${namespace}/metadata/${database}/data_sources/units/${dbname}/versions/0"),
    //加密器
    ENCRYPTOR("/ZA-${namespace}/metadata/${database}/rules/encrypt/encryptors/${encryptorName}"),
    //新增、修改加密规则
    RULE("/ZA-${namespace}/metadata/${database}/rules/encrypt/tables/${tableName}"),
    //属性
    PROPERTY("/ZA-${namespace}/props/versions/0"),
    //活跃版本号
    ACTIVE_VERSION("/active_version"),
    //版本
    VERSION("/versions/0");


    private final String value;


    //根据value获取
    public static PathEnum getEnumByValue(String value) {
        for (PathEnum pathEnum : PathEnum.values()) {
            if (pathEnum.getValue().equals(value)) {
                return pathEnum;
            }
        }
        throw new IllegalArgumentException("没有找到对应的PathEnum枚举: " + value);
    }


}

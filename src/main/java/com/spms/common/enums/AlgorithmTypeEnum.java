package com.spms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/6/7 08:37
 */
@Getter
@AllArgsConstructor
public enum AlgorithmTypeEnum {
    SGD_SM4_ECB("401"),
    TestAlg("000");
    private final String code;

    // 根据值获取枚举
    public static AlgorithmTypeEnum getEnumByCode(String code) {
        AlgorithmTypeEnum[] values = values();
        for (AlgorithmTypeEnum e : values) {
            if (code.equals(e.getCode())) {
                return e;
            }
        }
        throw new IllegalArgumentException("AlgorithmTypeEnum code not found: " + code);
    }

    // 根据名字获取枚举
    public static AlgorithmTypeEnum getEnumByName(String name) {
        AlgorithmTypeEnum[] values = values();
        for (AlgorithmTypeEnum e : values) {
            if (name.equals(e.name())) {
                return e;
            }
        }
        throw new IllegalArgumentException("AlgorithmTypeEnum name not found: " + name);
    }

    // 根据值获取名称
    public static String getNameByCode(String code) {
        AlgorithmTypeEnum[] var1 = values();
        for (AlgorithmTypeEnum e : var1) {
            if (code.equals(e.getCode())) {
                return e.name();
            }
        }
        throw new IllegalArgumentException("AlgorithmTypeEnum code not found: " + code);
    }


    // 根据名字获取值
    public static String getCodeByName(String name) {
        AlgorithmTypeEnum[] var1 = values();
        for (AlgorithmTypeEnum e : var1) {
            if (name.equals(e.name())) {
                return e.getCode();
            }
        }
        throw new IllegalArgumentException("AlgorithmTypeEnum name not found: " + name);
    }

}

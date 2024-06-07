package com.spms.common.enums;

import lombok.Getter;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/5/21 15:34
 */
@Getter
public enum DatabaseTypeEnum {
    Oracle("0", "Oracle"),
    SQLServer("1", "SQLServer"),
    MySQL("2", "MySql"),
    PostgresSQL("3", "PostgresSQL"),
    ClickHouse("6", "ClickHouse"),
    KingBase("7", "KingBase");

    private final String code;
    private final String name;

    DatabaseTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    //  根据值获取名称
    public static String getNameByCode(String code) {
        DatabaseTypeEnum[] var1 = values();
        for (DatabaseTypeEnum e : var1) {
            if (code.equals(e.getCode())) {
                return e.getName();
            }
        }
        return code;
    }

    //根据名称获取值
    public static String getCodeByName(String name) {
        DatabaseTypeEnum[] var1 = values();
        for (DatabaseTypeEnum e : var1) {
            if (name.equals(e.getName())) {
                return e.getCode();
            }
        }
        return name;
    }

    //根据值获取枚举
    public static DatabaseTypeEnum getEnumByCode(String code) {
        DatabaseTypeEnum[] values = values();
        for (DatabaseTypeEnum e : values) {
            if (code.equals(e.getCode())) {
                return e;
            }
        }
        return null;
    }

    //根据名字获取枚举
    public static DatabaseTypeEnum getEnumByName(String name) {
        DatabaseTypeEnum[] values = values();
        for (DatabaseTypeEnum e : values) {
            if (name.equals(e.getCode())) {
                return e;
            }
        }
        return null;
    }


}

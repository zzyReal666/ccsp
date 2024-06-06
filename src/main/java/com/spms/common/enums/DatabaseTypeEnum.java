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
    ClickHouse("6", "ClickHouse");

    private final String code;
    private final String name;

    DatabaseTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(String code) {
        DatabaseTypeEnum[] var1 = values();
        for (DatabaseTypeEnum e : var1) {
            if (code.equals(e.getCode())) {
                return e.getName();
            }
        }
        return null;
    }
}

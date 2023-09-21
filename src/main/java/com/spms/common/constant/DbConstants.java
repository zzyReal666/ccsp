package com.spms.common.constant;

/**
 * @project ccsp
 * @description 数据库加密网关常量类
 * @author 18853
 * @date 2023/9/20 08:53:42
 * @version 1.0
 */
public class DbConstants {

    public static final String DB_TYPE_ORACLE = "0";
    public static final String DB_TYPE_SQLSERVER = "1";
    public static final String DB_TYPE_MYSQL = "2";

    /**
     * 数据库实例是否唯一返回码
     */
    public static final String DATABASE_INSTANCE_UNIQUE = "0";
    public static final String DATABASE_INSTANCE_NOT_UNIQUE = "1";
    //
    /** Oracle 数据库实例类型 1 SID取值 ":" , 2 服务名取值 "/"  */
    public static final String DB_EXAMPLE_TYPE_SID = "1";
}

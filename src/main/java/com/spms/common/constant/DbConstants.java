package com.spms.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * @project ccsp
 * @description 数据库加密网关常量类
 * @author 18853
 * @date 2023/9/20 08:53:42
 * @version 1.0
 */
public class DbConstants {

    /** 查询数据库用户SQL */
    public static final String DB_SQL_SQLSERVER_USER_QUERY = "select name,principal_id,create_date from sys.database_principals u " +
            "where u.type_desc != 'APPLICATION_ROLE' and u.type_desc != 'DATABASE_ROLE' " +
            "and u.name != 'dbo' and u.name != 'guest'  and u.name != 'INFORMATION_SCHEMA' " +
            "and u.name != 'sys' ";
    public static final String DB_SQL_ORACLE_USER_QUERY = "select  * from all_users where INHERITED='NO'";
    public static final String DB_SQL_MYSQL_USER_QUERY = "SELECT user FROM  mysql.user group by user";

    /**数据库驱动*/
    public static final String DB_ORACLE_CLASS_NAME =  "oracle.jdbc.driver.OracleDriver";
    public static final String DB_SQLSERVER_CLASS_NAME =  "com.microsoft.sqlserver.jdbc.SQLServerDataSource";
    public static final String DB_MYSQL_CLASS_NAME =  "com.mysql.cj.jdbc.Driver";
    /**字典类型*/
    public static final String DBHSM_DB_TYPE = "dbhsm_db_type";
    //密码卡类型
    public static String cryptoCardType = "cryptoCardType";
    //三未信安
    public static final int HSM_CARD_SANSEC = 1;
    //渔翁信息
    public static final int HSM_CARD_FISEC = 2;
    //中安云科 SC10密码卡
    public static final int HSM_CARD_ZASEC_SC10 = 3;
    //中安云科 SC68密码卡
    public static final int HSM_CARD_ZASEC_SC68 = 4;
    //中安云科 SC11密码卡
    public static final int HSM_CARD_ZASEC_SC11 = 5;
    //中安云科 SC12密码卡
    public static final int HSM_CARD_ZASEC_SC12 = 6;
    //中安云科 SC12密码卡虚拟卡
    public static final int HSM_CARD_ZASEC_SC12V = 7;
    //中安云科 SC20密码卡
    public static final int HSM_CARD_ZASEC_SC20 = 8;
    //中安云科 SC30密码卡
    public static final int HSM_CARD_ZASEC_SC30 = 11 ;

    public static final String DB_TYPE_ORACLE = "0";
    public static final String DB_TYPE_ORACLE_DESC = "Oracle";
    public static final String DB_TYPE_SQLSERVER = "1";
    public static final String DB_TYPE_SQLSERVER_DESC = "SQL Server";
    public static final String DB_TYPE_MYSQL = "2";
    public static final String DB_TYPE_MYSQL_DESC = "MySql";
    public static final String DB_COLUMN_NAME = "columnName";

    /** 加密状态  1未加密 2已加密 3已解密 */
    public static final Integer NOT_ENCRYPTED = 1;
    public static final Integer ENCRYPTED = 2;

    /**
     * 是否唯一返回码
     */
    /* 存在  */
    public static final String DBHSM_GLOBLE_UNIQUE = "0";
    /* 不存在  */
    public static final String DBHSM_GLOBLE_NOT_UNIQUE = "1";

    //
    /** Oracle 数据库实例类型 1 SID取值 ":" , 2 服务名取值 "/"  */
    public static final String DB_EXAMPLE_TYPE_SID = "1";

    /**是否为web端创建的用户0：是 1：否 */
    public static final Integer IS_SELF_BUILT = 0;
    public static final Integer IS_NOT_SELF_BUILT = 1;


    public static final Integer SECRET_KEY_TYPE_SYM = 3;

    /** 密钥来源1：密码卡；2：KMIP  */
    public static final Integer KEY_SOURCE_SECRET_CARD = 1;
    public static final Integer KEY_SOURCE_KMIP = 2;

    //KMIP配置路径
    public static final String KMIP_INI_PATH="/opt/config_file/jsonfile";
    //KMIP配置文件相关
    public static final String KMC1 = "KMC1";
    public static final String AUTHENTICATION = "Authentication";
    //KMIP 响应状态
    public static final String SUCCESS_STR = "Success";

    /**获取用户创建模式 0：创建CDB容器中的公共用户 1：创建无容器数据库用户*/
    public static final int USER_CREATE_MODE_CDB =1;
    /** web端创建的用户标记*/
    public static final Integer CREATED_ON_WEB_SEDE = 0;
    /**标记为创建的是普通用户*/
    public static final Integer ORDINARY_USERS = 1;

    /** 查询表 */
    public static final String DB_SQL_SQLSERVER_TABLE_QUERY = "select name from sysobjects where xtype='U'" ;


    /**
     *  是否建立加密规则 1：是 0：否
     * */
    public static final Integer ESTABLISH_RULES_YES = 1;
    /** 加密算法 */
    public static final String SGD_SM4 = "0";
    public static final String SGD_SM4_FPE_10 = "10";
    public static final String SGD_SM4_FPE_62 = "62";

    /*****************  数据库字段类型  ********************/
    public static int DATA_TYPE_INT = 0x00000001;
    public static int DATA_TYPE_INT64 = 0x00000002;
    public static int DATA_TYPE_NUMBER = 0x00000004;
    public static int DATA_TYPE_CHAR = 0x00000008;
    public static int DATA_TYPE_VARCHAR = 0x00000010;
    public static int DATA_TYPE_VARCHAR2 = 0x00000020;
    public static int DATA_TYPE_DATE = 0x00000040;
    public static int DATA_TYPE_TIMESTAMP = 0x00000080;
    public static int DATA_TYPE_RAW = 0x00000100;

    /*****************  策略版本  ********************/
    public static int POLICY_VERSION = 0x1;

    /*****************  策略模式  ********************/
    public static int POLICY_TYPE_PUSH = 0x0100;
    public static int POLICY_TYPE_PULL = 0x0200;

    /*****************  策略类型  ********************/
    public static int POLICY_TYPE_ENC_DEC = 0x1;
    public static int POLICY_TYPE_SIGN_VERIFY = 0x2;
    public static int POLICY_TYPE_MAC = 0x4;

    /*****************  策略处理方式  ********************/
    public static int POLICY_TYPE_DB = 0x1000;
    public static int POLICY_TYPE_FILE = 0x2000;

    /*****************  数据库类型  ********************/
    public static int DB_TYPE_ORACLE_api = 0x10;
    public static int DB_TYPE_MSSQL_api = 0x20;
    public static int DB_TYPE_MYSQL_api = 0x30;
    public static int DB_TYPE_MARIADB_api = 0x40;
    public static int DB_TYPE_POSTGRESQL_api = 0x50;
    public static int DB_TYPE_DB2_api = 0x60;

    public static int SGD_SM4_ECB = 0x00000401;
    public static int SGD_SM4_CBC = 0x00000402;
    public static int SGD_SM4_CFB = 0x00000404;
    public static int SGD_SM4_OFB = 0x00000408;
    public static int SGD_SM4_MAC = 0x00000410;
    public static int SGD_SM4_CTR = 0x00000420;

    public static int SGD_SM3 = 0x00000001;
    public static int SGD_SHA1 = 0x00000002;
    public static int SGD_SHA256 = 0x00000004;
    public static Map<String, Integer> mapStrToInt = new HashMap();

    static {
        mapStrToInt.put("INT", DATA_TYPE_INT);
        mapStrToInt.put("INT64", DATA_TYPE_INT64);
        mapStrToInt.put("NUMBER", DATA_TYPE_NUMBER);
        mapStrToInt.put("CHAR", DATA_TYPE_CHAR);
        mapStrToInt.put("VARCHAR", DATA_TYPE_VARCHAR);
        mapStrToInt.put("VARCHAR2", DATA_TYPE_VARCHAR2);
        mapStrToInt.put("DATE", DATA_TYPE_DATE);
        mapStrToInt.put("TIMESTAMP", DATA_TYPE_TIMESTAMP);
        mapStrToInt.put("RAW", DATA_TYPE_RAW);


    }

    public static int getColumnTypeToInt(String type) {
        type = type.toUpperCase();
        if (mapStrToInt.containsKey(type)) {
            return mapStrToInt.get(type);
        }
        return 0;
    }
}

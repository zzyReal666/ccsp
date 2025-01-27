package com.spms.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 18853
 * @version 1.0
 * @project ccsp
 * @description 数据库加密网关常量类
 * @date 2023/9/20 08:53:42
 */
public class DbConstants {

    /**
     * 查询数据库用户SQL
     */
    public static final String DB_SQL_SQLSERVER_USER_QUERY = "select name,principal_id,create_date from sys.database_principals u " +
            "where u.type_desc != 'APPLICATION_ROLE' and u.type_desc != 'DATABASE_ROLE' " +
            "and u.name != 'dbo' and u.name != 'guest'  and u.name != 'INFORMATION_SCHEMA' " +
            "and u.name != 'sys' ";
    public static final String DB_SQL_ORACLE_USER_QUERY = "select  * from all_users where INHERITED='NO'";
    public static final String DB_SQL_MYSQL_USER_QUERY = "SELECT user FROM  mysql.user group by user";

    /**
     * 数据库驱动
     */
    public static final String DB_ORACLE_CLASS_NAME = "oracle.jdbc.driver.OracleDriver";
    public static final String DB_SQLSERVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDataSource";
    public static final String DB_MYSQL_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
    /**
     * 字典类型
     */
    public static final String DBHSM_DB_TYPE = "dbhsm_db_type";
    public static final String DB_SQL_POSTGRESQL_USER_QUERY = "select  * from pg_user";
    public static final String DB_SQL_DM_USER_QUERY = "select * from dba_users";

    /**
     * 存量数据加密解密 加密true，解密false
     */
    public static final Boolean STOCK_DATA_ENCRYPTION = true;
    public static final Boolean STOCK_DATA_DECRYPTION = false;
    /**
     * 0解密 1加密
     */
    public static final int DEC_FLAG = 0;
    public static final int ENC_FLAG = 1;
    /**
     * 数据库加密网关web端口key
     */
    public static final String DBENC_WEB_PORT = "dbencWebPort";
    /**
     * 查询表空间
     */
    public static final String DB_SQL_ORACLE_TABLESPACE_QUERY = "select tablespace_name from dba_data_files where tablespace_name!='UNDOTBS1' and tablespace_name!='SYSTEM' and tablespace_name!='SYSAUX'";
    public static final String DB_SQL_DM_TABLESPACE_QUERY = "select tablespace_name from dba_data_files where tablespace_name!='SYSTEM' and tablespace_name!='TEMP' and tablespace_name!='ROLL'";
    //系统主密钥
    public static final String SYSTEM_MASTER_KEY = "systemMasterKey";
    //系统主密钥是否是定制
    public static final String SYSTEM_MASTER_KEY_IS_CUSTOMIZED = "SMKIsCustomized";
    //定制信息
    public static final String CUSTOMIZED_INFO = "customizedInfo";
    public static final String DBENC_SYSTEM_MASTER_KEY = "dbencSystemMasterKey";
    public static final String AUTHORIZATION = "AUTHORIZATION";
    public static final String SECRET_SERVICE_TYPE_JIT = "1";
    public static final String SECRET_SERVICE_TYPE_KMIP = "0";
    public static final int SECRET_KEY_USAGE_ENC = 8;

    /**
     * 吉大正元获取最新版本kek
     * 加密方式
     * 0=证书
     * 1=公钥
     */
    public static final int JIT_ENCRYPTED_TYPE_PUBLICKEY = 1;
    public static final String JIT_ALGORITHM_TYPE_SM2 = "SM2";
    public static final String JIT_SUCCESS_CODE = "0";
    public static final String TRUE_STRING = "true";

    /**
     * 请求成功
     */
    public static int SUCCESS = 200;
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
    public static final int HSM_CARD_ZASEC_SC30 = 11;


    public static final String DB_TYPE = "dbhsm_db_type";
    public static final String DB_TYPE_ORACLE = "0";
    public static final String DB_TYPE_ORACLE_DESC = "Oracle";
    public static final String DB_TYPE_SQLSERVER = "1";
    public static final String DB_TYPE_SQLSERVER_DESC = "SQL Server";
    public static final String DB_TYPE_MYSQL = "2";
    public static final String DB_TYPE_MYSQL_DESC = "MySql";
    public static final String DB_TYPE_POSTGRESQL = "3";
    public static final String DB_TYPE_POSTGRESQL_DESC = "PostgreSQL";
    public static final String DB_TYPE_DM = "4";
    public static final String DB_TYPE_DM_DESC = "DM";
    public static final String DB_TYPE_DB2 = "5";
    public static final String DB_TYPE_DB2_DESC = "DB2";
    public static final String DB_TYPE_CLICKHOUSE = "6";
    public static final String DB_TYPE_CLICKHOUSE_DESC = "ClickHouse";

    public static final String DB_TYPE_KB = "7";
    public static final String DB_TYPE_KING_BASE_DESC = "KingBase";

    public static final String DB_TYPE_HB = "8";
    public static final String DB_TYPE_HBASE_DESC = "Hbase";
    public static final String DB_COLUMN_NAME = "columnName";
    public static final String DB_COLUMN_TYPE = "columnType";
    public static final String DB_COLUMN_KEY = "Key";
    /**
     * 加密状态  1未加密 2已加密 3加密中
     */
    public static final Integer NOT_ENCRYPTED = 1;
    public static final Integer ENCRYPTED = 2;
    public static final Integer ENCRYPTING = 3;

    public static final Integer DECRYPTING = 4;


    /*插件模式 0：前端插件  1：后端插件*/
    public static final Integer FG_PLUG = 0;

    public static final Integer BE_PLUG = 1;

    public static final Integer DL_PLUG = 2;
    /**
     * 是否唯一返回码
     */
    /**
     * 存在
     */
    public static final String DBHSM_GLOBLE_UNIQUE = "0";
    /**
     * 不存在
     */
    public static final String DBHSM_GLOBLE_NOT_UNIQUE = "1";

    //
    /**
     * Oracle 数据库实例类型 1 SID取值 ":" , 2 服务名取值 "/"
     */
    public static final String DB_EXAMPLE_TYPE_SID = "1";

    /**
     * 是否为web端创建的用户0：是 1：否
     */
    public static final Integer IS_SELF_BUILT = 0;
    public static final Integer IS_NOT_SELF_BUILT = 1;


    public static final Integer SECRET_KEY_TYPE_SYM = 3;

    /**
     * 密钥来源1：密码卡；2：KMIP  3：大容量密钥
     */
    public static final Integer KEY_SOURCE_SECRET_CARD = 1;
    public static final Integer KEY_SOURCE_KMIP = 2;
    public static final Integer KEY_SOURCE_JIT = 3;
    /**
     * 密钥生成方式软实现
     * <p>
     * 0：软实现
     * 1：硬实现
     * 2：大容量
     * 3: 软实现(密钥加密后存数据库)
     */
    // public static final int SOFT_SECRET_KEY = 0;
    public static final int HARD_SECRET_KEY = 1;
    public static final int BULK_SECRET_KEY = 2;
    public static final int SOFT_SECRET_KEY = 3;
    //KMIP配置路径
    public static final String KMIP_INI_PATH = "/opt/config_file/jsonfile";
    //KMIP配置文件相关
    public static final String KMC1 = "KMC1";
    public static final String AUTHENTICATION = "Authentication";
    //KMIP 响应状态
    public static final String SUCCESS_STR = "Success";

    /**
     * 获取用户创建模式 0：创建CDB容器中的公共用户 1：创建无容器数据库用户
     */
    public static final int USER_CREATE_MODE_CDB = 1;
    /**
     * web端创建的用户标记
     */
    public static final Integer CREATED_ON_WEB_SEDE = 0;
    /**
     * 标记为创建的是普通用户
     */
    public static final Integer ORDINARY_USERS = 1;

    /**
     * 查询表
     */
    public static final String DB_SQL_SQLSERVER_TABLE_QUERY = "select name from sysobjects where xtype='U'";

    public static final String STRATEGY_URI = "prod-api/dbhsm/api/datahsm/v1/strategy/get";

    public static final String ENC_MODE = "enc";

    public static final String DEC_MODE = "dec";

    public static final String DOWN = "down";
    public static final String UP = "up";
    public static final String CONTINUE = "continue";


    /**
     * 是否建立加密规则 1：是 0：否
     */
    public static final Integer ESTABLISH_RULES_YES = 1;
    /**
     * 加密算法 注意：增加算法后要在algMapping（）中进行添加相应的算法名称
     */
    public static final String SGD_SM4 = "0";
    public static final String SGD_SM4_FPE_10 = "10";
    public static final String SGD_SM4_FPE_62 = "62";

    /**
     * 算法标识（0,10,62）映射为算法名称
     */
    public static String algMapping(String algFlag) {
        Map<String, String> map = new HashMap<>();
        map.put(SGD_SM4, "SGD_SM4");
        map.put(SGD_SM4_FPE_10, "SGD_SM4_FPE_10");
        map.put(SGD_SM4_FPE_62, "SGD_SM4_FPE_62");
        return map.get(algFlag) == null ? algFlag : map.get(algFlag);
    }

    /**
     * 算法标识（0,10,62）映射为视图名称 区分sm4和fpe拼接使用
     */
    public static String algMappingStrOrFpe(String algFlag) {
        Map<String, String> map = new HashMap<>();
        map.put(SGD_SM4, "string");
        map.put(SGD_SM4_FPE_10, "fpe");
        map.put(SGD_SM4_FPE_62, "fpe");
        return map.get(algFlag) == null ? algFlag : map.get(algFlag);
    }

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

    public static final String jsonPath = "jsonPath";
    /**
     * 三合一配置文件路径
     */
    public static final String SPMS_CONFIG_FILE_PATH = "spms_config_file_path";
    //系统配置SysData
    public static final String SysData = "SysData";

    public static String SYSDATA_ALGORITHM_TYPE_SYK = "CardKeySYK";

    public static final String CRYPTO_CARD_TYPE_NO_CARD = "0";
}

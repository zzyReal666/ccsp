package com.spms.common.constant;

/**
 * @project ccsp
 * @description 数据库加密网关常量类
 * @author 18853
 * @date 2023/9/20 08:53:42
 * @version 1.0
 */
public class DbConstants {

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
    public static final String DB_TYPE_SQLSERVER = "1";
    public static final String DB_TYPE_MYSQL = "2";

    /**
     * 是否唯一返回码
     */
    public static final String DBHSM_GLOBLE_UNIQUE = "0";
    public static final String DBHSM_GLOBLE_NOT_UNIQUE = "1";

    //
    /** Oracle 数据库实例类型 1 SID取值 ":" , 2 服务名取值 "/"  */
    public static final String DB_EXAMPLE_TYPE_SID = "1";


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
}

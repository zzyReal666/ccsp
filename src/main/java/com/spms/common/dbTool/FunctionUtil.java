package com.spms.common.dbTool;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.StringUtils;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Base64;
import org.zayksoft.zkgm.SM2Utils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @project ccsp
 * @description 加解密函数工具类
 * @author 18853
 * @date 2023/10/26 11:38:38
 * @version 1.0
 */
@Slf4j
public class FunctionUtil {
    /**
     * 创建mysql加密解密函数
     *
     * CREATE FUNCTION StringEncrypt RETURNS STRING SONAME "mysqldll.dll";
     *
     * CREATE FUNCTION StringDecrypt RETURNS STRING SONAME "mysqldll.dll";
     *
     * */
    public static void createMysqlStringEncryptDecryptFunction(Connection connection) throws SQLException {
        String stringEncryptFunction = "CREATE FUNCTION StringEncrypt RETURNS STRING SONAME 'mysqldll.dll'";
        String stringDecryptFunction = "CREATE FUNCTION StringDecrypt RETURNS STRING SONAME 'mysqldll.dll'";
        String fpeEncryptFunction = "CREATE FUNCTION FpeStringEncrypt RETURNS STRING SONAME 'mysqldll.dll'";
        String fpeDecryptFunction = "CREATE FUNCTION FpeStringDecrypt RETURNS STRING SONAME 'mysqldll.dll'";
        PreparedStatement preparedStatement = null;
        try {
            log.info("CREATE MYSQL STRINGENCRYPT  Function INFO：\n" + stringEncryptFunction);
            preparedStatement = connection.prepareStatement(stringEncryptFunction);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            log.info("创建加解密函数失败！加密函数StringEncrypt已存在？" + e.getMessage());
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        try {
            log.info("CREATE MYSQL  STRINGDECRYPT FUNCTION INFO：\n" + stringDecryptFunction);
            preparedStatement = connection.prepareStatement(stringDecryptFunction);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            log.info("创建加解密函数失败！解密函数StringDecrypt已存在？" + e.getMessage());
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }

        try{
            log.info("CREATE MYSQL  FpeStringEncrypt FUNCTION INFO：\n" + fpeEncryptFunction);
            preparedStatement = connection.prepareStatement(fpeEncryptFunction);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            log.info("创建加解密函数失败！加密函数FpeStringEncrypt已存在？" + e.getMessage());
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        try{
            log.info("CREATE MYSQL  FpeStringDecrypt FUNCTION INFO：\n" + fpeDecryptFunction);
            preparedStatement = connection.prepareStatement(fpeDecryptFunction);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            log.info("创建加解密函数失败！解密函数FpeStringDecrypt已存在？" + e.getMessage());
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    public static void createEncryptDecryptFunction(Connection connection, DbhsmDbInstance dbhsmDbInstance) throws ZAYKException, SQLException {
        String sql ="";
        String dbType= dbhsmDbInstance.getDatabaseType();
        switch (dbType) {
            case DbConstants.DB_TYPE_ORACLE:
                break;
            case DbConstants.DB_TYPE_SQLSERVER:
                break;
            case DbConstants.DB_TYPE_MYSQL:
                //mysql创建加解密函数
                FunctionUtil.createMysqlStringEncryptDecryptFunction(connection);
                break;
            case DbConstants.DB_TYPE_POSTGRESQL:
                break;
            default:
                throw new ZAYKException("暂不支持的数据库类型： " + dbType);
        }

    }

    /**
     * 创建加解密方法 fpe方法
     * @param connection
     */
    public static void createSqlServerFunction(Connection connection) {
        try {
            //创建加解密方法 fpe方法
            ProcedureUtil.transSQLServerStringEncryptEX(connection);
            ProcedureUtil.transSQLServerStringDecryptEX(connection);
            ProcedureUtil.funcSQLServerFuncFpeEncryptEx(connection);
            ProcedureUtil.funcSQLServerFuncFpeDecryptEx(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * ALTER TABLE tuser1.TABLE1_SM4_ECB
     * MODIFY(
     * CHANGE DECIMAL(20,6) ENCRYPT WITH SM4_ECB MANUAL BY 1111111122222222 USER (tuser1)
     * );
     *
     * MODIFY + (加密列配置)
     * 加密列配置 = 加密列定义 + ENCRYPT WITH + 加密算法 + MANUAL BY + 密钥 + USER + (可见用户)
     *
     * 注：
     * CHANGE为列名，后接列定义。配置加密列时，保持列定义与原表一致
     * 1111…为指定的密钥
     * DES_ECB为算法，支持的算法见2.3加载自定义算法
     * tuser1为可见用户，除此用户外，其他用户均不可见该列数据
     * */
    public static void encOrdecColumnsSqlToDM(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, int encOrDecFlag) {
        Statement stmt=null;
        String schema = dbhsmEncryptColumnsAdd.getDbUserName();
        String tableName = dbhsmEncryptColumnsAdd.getDbTable();
        String columnDefinitions = dbhsmEncryptColumnsAdd.getColumnDefinitions();
        String alg = dbhsmEncryptColumnsAdd.getEncryptionAlgorithm();
        String encStr = "",decStr = "",encOrDecFlagStr;
        if(DbConstants.ENC_FLAG==encOrDecFlag) {
            String secretKey;
            secretKey = dbhsmEncryptColumnsAdd.getSecretKey();
            encStr = StringUtils.format("{} ENCRYPT WITH {} MANUAL BY \"{}\" USER ({}) ", columnDefinitions, alg, secretKey, schema);
            encOrDecFlagStr="加密";
        }else {
            decStr = dbhsmEncryptColumnsAdd.getColumnDefinitions();
            encOrDecFlagStr = "解密";
        }
        String alterSql = (encOrDecFlag == DbConstants.ENC_FLAG ? encStr : decStr);
        StringBuilder sql = new StringBuilder();
        sql.append(StringUtils.format("ALTER TABLE {}.\"{}\" MODIFY(", schema, tableName));
        sql.append(alterSql);
        sql.append(");");
        //执行sql
        log.info("执行"+encOrDecFlagStr+"配置sql:{}", sql);
        try {
            stmt = conn.createStatement();
            stmt.execute(sql.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            if (stmt != null){
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main1(String[] args) {
        // Base64编码的字符串
        //String base64String = "SGVsbG8gd29ybGQ=";
        String base64String = "cEpHUTNtOU4yRktWb1d5Wg==";
        String authentication = "bjcz:Jitbjcz2023";
        String authentication1 = "oa:123456";
        byte[] encode = Base64.encode(authentication.getBytes(StandardCharsets.UTF_8));
        byte[] encode1 = Base64.encode(authentication1.getBytes(StandardCharsets.UTF_8));
        System.out.println(new String(encode));
        System.out.println(new String(encode1));
        // 解码
        byte[] decodedBytes = org.bouncycastle.util.encoders.Base64.decode(base64String);

        System.out.println(new String(decodedBytes));;
    }

    public static void main(String[] args) throws Exception {
        String plainText = "message digest";

        //String sm2Pubk = "043c246100a7d242540d6bc94be497b09b22ccb72c25b3aafd80586ef995fd26c80ccb2f5be4db45020aabdc6acd2c4a101411fe154cd04468f3ccda789b99f5f1";
        //String pubkS = new String(org.zayksoft.util.encoders.Base64.encode(Util.hexToByte(sm2Pubk)));
        String pubkS = "MFkwEwYHKoZIzj0CAQYIKoEcz1UBgi0DQgAEgBIQBz1ayFZoRnw1mbdSEDm7fnA3Zt+x1nrolIib6cyS69+AcQCWWkYhGxpqlFf7SigE7jOVpM+RcGOsWI2pGw==";
        pubkS = new String(Base64.encode(pubkS.getBytes()));
        System.out.println("加密: ");
        byte[] cipherText = SM2Utils.encrypt(org.zayksoft.util.encoders.Base64.decode(pubkS.getBytes()), "1234567812345678".getBytes());
        System.out.println("");
        System.out.println("解密: ");
        //String sm2Prik = "67254fdb1cf67b4497d5a7b4eb31f60876b2f8cc1d014d068fb9d83493599d64";
        //String prikS = new String(org.zayksoft.util.encoders.Base64.encode(Util.hexToByte(sm2Prik)));
        String prikS = "MIGTAgEAMBMGByqGSM49AgEGCCqBHM9VAYItBHkwdwIBAQQgCulB6/CHlSV7STb+bIUheSfdw5hgHyvhR7R4fIDliiGgCgYIKoEcz1UBgi2hRANCAASAEhAHPVrIVmhGfDWZt1IQObt+cDdm37HWeuiUiJvpzJLr34BxAJZaRiEbGmqUV/tKKATuM5Wkz5FwY6xYjakb";
        plainText = new String(SM2Utils.decrypt(org.zayksoft.util.encoders.Base64.decode(prikS.getBytes()), cipherText));
        System.out.println(plainText);
    }

}

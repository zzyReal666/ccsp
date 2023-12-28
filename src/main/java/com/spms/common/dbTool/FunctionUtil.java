package com.spms.common.dbTool;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.StringUtils;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;

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
        String schema = dbhsmEncryptColumnsAdd.getDbUserName();
        String tableName = dbhsmEncryptColumnsAdd.getDbTable();
        String columnDefinitions = dbhsmEncryptColumnsAdd.getColumnDefinitions();
        String alg = dbhsmEncryptColumnsAdd.getEncryptionAlgorithm();
        String encStr = "",decStr = "",encOrDecFlagStr;
        if(DbConstants.ENC_FLAG==encOrDecFlag) {
            String secretKey = dbhsmEncryptColumnsAdd.getSecretKey();
            encStr = StringUtils.format("{} ENCRYPT WITH {} MANUAL BY '{}' USER ({}) ", columnDefinitions, alg, secretKey, schema);
            encOrDecFlagStr="加密";
        }else {
            decStr = dbhsmEncryptColumnsAdd.getColumnDefinitions();
            encOrDecFlagStr = "解密";
        }
        String alterSql = (encOrDecFlag == DbConstants.ENC_FLAG ? encStr : decStr);
        StringBuilder sql = new StringBuilder();
        sql.append(StringUtils.format("ALTER TABLE {}.{} MODIFY(", schema, tableName));
        sql.append(alterSql);
        sql.append(");");
        //执行sql
        log.info("执行"+encOrDecFlagStr+"配置sql:{}", sql);
        try {
            Statement stmt = conn.createStatement();
            stmt.execute(sql.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

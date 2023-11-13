package com.spms.common.dbTool;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
        String stringDecryptFunction =  "CREATE FUNCTION StringDecrypt RETURNS STRING SONAME 'mysqldll.dll'";
        PreparedStatement preparedStatement = null;
        try {
            log.info("CREATE MYSQL STRINGENCRYPT  Function INFO：\n" + stringEncryptFunction);
            preparedStatement = connection.prepareStatement(stringEncryptFunction);
            preparedStatement.execute();

            log.info("CREATE MYSQL  STRINGDECRYPT FUNCTION INFO：\n" + stringDecryptFunction);
            preparedStatement = connection.prepareStatement(stringDecryptFunction);
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException(e);
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
}

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
import java.sql.*;

/**
 * @author 18853
 * @version 1.0
 * @project ccsp
 * @description 加解密函数工具类
 * @date 2023/10/26 11:38:38
 */
@Slf4j
public class FunctionUtil {
    /**
     * 创建mysql加密解密函数
     * <p>
     * CREATE FUNCTION StringEncrypt RETURNS STRING SONAME "mysqldll.dll";
     * <p>
     * CREATE FUNCTION StringDecrypt RETURNS STRING SONAME "mysqldll.dll";
     */
    public static void createMysqlStringEncryptDecryptFunction(Connection connection) throws SQLException {

        String osName = System.getProperty("os.name");
        String libName = "mysqldll.dll";
        if (osName.toLowerCase().startsWith("linux")) {
            libName = "libmysqlext.so";
        }

        String stringEncryptFunction = "CREATE FUNCTION StringEncrypt RETURNS STRING SONAME '" + libName + "'";
        String stringDecryptFunction = "CREATE FUNCTION StringDecrypt RETURNS STRING SONAME '" + libName + "'";
        String fpeEncryptFunction = "CREATE FUNCTION FpeStringEncrypt RETURNS STRING SONAME '" + libName + "'";
        String fpeDecryptFunction = "CREATE FUNCTION FpeStringDecrypt RETURNS STRING SONAME '" + libName + "'";
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

        try {
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
        try {
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
        String sql = "";
        String dbType = dbhsmDbInstance.getDatabaseType();
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
     *
     * @param connection
     */
    public static void createSqlServerFunction(Connection connection) {
        try {
            //创建加解密方法 fpe方法
            ProcedureUtil.transSQLServerStringEncryptEX(connection);
            ProcedureUtil.transSQLServerStringDecryptEX(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ALTER TABLE tuser1.TABLE1_SM4_ECB
     * MODIFY(
     * CHANGE DECIMAL(20,6) ENCRYPT WITH SM4_ECB MANUAL BY 1111111122222222 USER (tuser1)
     * );
     * <p>
     * MODIFY + (加密列配置)
     * 加密列配置 = 加密列定义 + ENCRYPT WITH + 加密算法 + MANUAL BY + 密钥 + USER + (可见用户)
     * <p>
     * 注：
     * CHANGE为列名，后接列定义。配置加密列时，保持列定义与原表一致
     * 1111…为指定的密钥
     * DES_ECB为算法，支持的算法见2.3加载自定义算法
     * tuser1为可见用户，除此用户外，其他用户均不可见该列数据
     */
    public static void encOrdecColumnsSqlToDM(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, int encOrDecFlag) throws ZAYKException {
        Statement stmt = null;
        String schema = dbhsmEncryptColumnsAdd.getDbUserName();
        String tableName = dbhsmEncryptColumnsAdd.getDbTable();
        String columnDefinitions = dbhsmEncryptColumnsAdd.getColumnDefinitions();
        String alg = dbhsmEncryptColumnsAdd.getEncryptionAlgorithm();
        String encStr = "", decStr = "", encOrDecFlagStr;
        if (DbConstants.ENC_FLAG == encOrDecFlag) {
            String secretKey;
            secretKey = dbhsmEncryptColumnsAdd.getSecretKey();
            if (StringUtils.isEmpty(secretKey)) {
                throw new ZAYKException("密钥不存在！");
            }
            encStr = StringUtils.format("{} ENCRYPT WITH {} MANUAL BY \"{}\" USER ({}) ", columnDefinitions, alg, secretKey, schema);
            encOrDecFlagStr = "加密";
        } else {
            decStr = dbhsmEncryptColumnsAdd.getColumnDefinitions();
            encOrDecFlagStr = "解密";
        }
        String alterSql = (encOrDecFlag == DbConstants.ENC_FLAG ? encStr : decStr);
        StringBuilder sql = new StringBuilder();
        sql.append(StringUtils.format("ALTER TABLE {}.\"{}\" MODIFY(", schema, tableName));
        sql.append(alterSql);
        sql.append(");");
        //执行sql
        log.info("执行" + encOrDecFlagStr + "配置sql:{}", sql);
        try {
            stmt = conn.createStatement();
            stmt.execute(sql.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ZAYKException(e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取达梦口令策略
     *
     * @param conn
     * @throws ZAYKException
     */
    public static int getPwdPolicyToDM(Connection conn) throws ZAYKException {
        Statement stmt = null;
        int value = 2;
        String pwdPolicySql = "SELECT value FROM V$PARAMETER WHERE NAME= 'PWD_POLICY'";
        //执行sql
        log.info("获取口令策略sql:{}", pwdPolicySql);
        try {
            stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(pwdPolicySql);
            while (resultSet.next()) {
                value = Integer.parseInt(resultSet.getString("value"));
                log.info("口令策略:{}", value);
            }
            return value;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ZAYKException(e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * 获取达梦口令策略INI 参数 PWD_MIN_LEN 设置的值
     *
     * @param conn
     * @throws ZAYKException
     */
    public static int getPwdMinLenToDM(Connection conn) throws ZAYKException {
        Statement stmt = null;
        int value = 2;
        String pwdMinLenSql = "SELECT value FROM V$PARAMETER WHERE NAME= 'PWD_MIN_LEN'";
        //执行sql
        log.info("获取口令策略INI 参数 PWD_MIN_LEN 设置的值:{}", pwdMinLenSql);
        try {
            stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(pwdMinLenSql);
            while (resultSet.next()) {
                value = Integer.parseInt(resultSet.getString("value"));
                log.info("INI 参数 PWD_MIN_LEN 值:{}", value);
            }
            return value;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ZAYKException(e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                try {
                    conn.close();
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

        System.out.println(new String(decodedBytes));
        ;
    }

    public static void main(String[] args) throws Exception {
        // JDBC连接字符串
        String url = "jdbc:sqlserver://;serverName=192.168.6.64;databaseName=master";
        String user = "sa";
        String password = "server@2020";

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        Connection connection = null;
        PreparedStatement statement = null;

        try {
            // 连接到SQL Server
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("连接成功!");

            // 创建一个Statement对象
            statement = connection.prepareStatement("CREATE FUNCTION dbo.func_string_encrypt_ex(\n" +
                    "@policy_id NVARCHAR(MAX),@policy_url NVARCHAR(MAX),@user_ipaddr NVARCHAR(MAX),\n" +
                    "@db_instance_name NVARCHAR(MAX),@db_name NVARCHAR(MAX),@db_table_name NVARCHAR(MAX),\n" +
                    "@db_column_name NVARCHAR(MAX),@db_user_name NVARCHAR(MAX),\n" +
                    "@rawstring NVARCHAR(max), @rawstringlen INT,\n" +
                    "@offset INT, @length INT, @encryptstringlen INT\n" +
                    ")\n" +
                    "RETURNS NVARCHAR(max)\n" +
                    "AS\n" +
                    "EXTERNAL NAME\n" +
                    "libsqlextdll.[libsqlserver.SqlExtPolicyFunc].SqlStringEncryptEx ");
            boolean execute = statement.execute();
            System.out.println(execute);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭资源

            try {
                if (statement != null) statement.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

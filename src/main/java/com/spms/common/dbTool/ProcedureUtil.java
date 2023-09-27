package com.spms.common.dbTool;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @date 2022/12/13
 * @dec 存储过程工具类
 */
@Slf4j
public class ProcedureUtil {

    //创建用户时创建存储过程
    public static void cOciTransStringEncrypt(Connection conn, String username) throws SQLException {
        log.info("创建存储过程start：cOciTransStringEncrypt");
        PreparedStatement statement = null;
        StringBuffer transSql = new StringBuffer("CREATE OR REPLACE PROCEDURE " + username + ".c_oci_trans_string_encrypt_p(\n");
        transSql.append("  pid IN VARCHAR2,\n");
        transSql.append("  purl IN VARCHAR2,\n");
        transSql.append("  ipaddr IN VARCHAR2,\n");
        transSql.append("  instancename IN VARCHAR2,\n");
        transSql.append("  dbname IN VARCHAR2,\n");
        transSql.append("  tablename IN VARCHAR2,\n");
        transSql.append("  columnname IN VARCHAR2,\n");
        transSql.append("  username IN VARCHAR2,\n");
        transSql.append("  ivalue IN VARCHAR2,\n");
        transSql.append("  offset  IN NATURAL,\n");
        transSql.append("  length  IN NATURAL,\n");
        transSql.append("  ovalue OUT VARCHAR2)\n");
        transSql.append("AS EXTERNAL\n");
        transSql.append("  LIBRARY liboraextapi\n");
        transSql.append("  NAME \"oci_trans_string_encrypt\"\n");
        transSql.append("  LANGUAGE C\n");
        transSql.append("  WITH CONTEXT\n");
        transSql.append("  PARAMETERS(\n");
        transSql.append("  CONTEXT,                          ---上下文指针\n");
        transSql.append("  pid STRING,          pid LENGTH UNSIGNED INT,\n");
        transSql.append("  purl STRING,         purl LENGTH UNSIGNED INT,\n");
        transSql.append("  ipaddr STRING,       ipaddr LENGTH UNSIGNED INT,\n");
        transSql.append("  instancename STRING, instancename LENGTH UNSIGNED INT,\n");
        transSql.append("  dbname STRING,       dbname LENGTH UNSIGNED INT,\n");
        transSql.append("  tablename STRING,    tablename LENGTH UNSIGNED INT,\n");
        transSql.append("  columnname STRING,   columnname LENGTH UNSIGNED INT,\n");
        transSql.append("  username STRING,     username LENGTH UNSIGNED INT,\n");
        transSql.append("  ivalue STRING,                    ---传入参数ivalue字符串\n");
        transSql.append("  ivalue LENGTH UNSIGNED INT,       ---参数ivalue字符串的传入长度\n");
        transSql.append("  offset  UNSIGNED INT,             ---偏移量\n");
        transSql.append("  length  UNSIGNED INT,               ---需要加密的长度，0为加密全部\n");
        transSql.append("  ovalue STRING,                    ---传出参数ovalue\n");
        transSql.append("  ovalue MAXLEN UNSIGNED INT);      ---传出参数ovalue的最大容纳长度\n");

        try {
            log.info("创建存储过程cOciTransStringEncrypt，SQL：{} " + transSql.toString());
            statement = conn.prepareStatement(transSql.toString());
            statement.execute();
            log.info("创建存储过程end：cOciTransStringEncrypt 命令提交完成");
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    //创建用户时创建fpe存储过程
    public static void cOciTransFPEEncrypt(Connection conn, String username) throws SQLException {
        log.info("创建存储过程start：cOciTransFPEEncrypt");
        PreparedStatement statement = null;
        StringBuffer transSql = new StringBuffer("CREATE OR REPLACE PROCEDURE " + username + ".c_oci_trans_fpe_encrypt_p(\n");
        transSql.append("pid IN VARCHAR2,\n");
        transSql.append("  purl IN VARCHAR2,\n");
        transSql.append("  ipaddr IN VARCHAR2,\n");
        transSql.append("  instancename IN VARCHAR2,\n");
        transSql.append("  dbname IN VARCHAR2,\n");
        transSql.append("  tablename IN VARCHAR2,\n");
        transSql.append("  columnname IN VARCHAR2,\n");
        transSql.append("  username IN VARCHAR2,\n");
        transSql.append("  ivalue IN VARCHAR2,\n");
        transSql.append("  offset  IN NATURAL,\n");
        transSql.append("  length  IN NATURAL,\n");
        transSql.append("  ovalue OUT VARCHAR2,\n");
        transSql.append("  radix IN PLS_INTEGER)\n");
        transSql.append("AS EXTERNAL\n");
        transSql.append("  LIBRARY liboraextapi\n");
        transSql.append("  NAME \"oci_trans_fpe_encrypt\"\n");
        transSql.append("  LANGUAGE C\n");
        transSql.append("  WITH CONTEXT\n");
        transSql.append("  PARAMETERS(\n");
        transSql.append("  CONTEXT,                          ---上下文指针\n");
        transSql.append("  pid STRING,          pid LENGTH UNSIGNED INT,\n");
        transSql.append("  purl STRING,         purl LENGTH UNSIGNED INT,\n");
        transSql.append("  ipaddr STRING,       ipaddr LENGTH UNSIGNED INT,\n");
        transSql.append("  instancename STRING, instancename LENGTH UNSIGNED INT,\n");
        transSql.append("  dbname STRING,       dbname LENGTH UNSIGNED INT,\n");
        transSql.append("  tablename STRING,    tablename LENGTH UNSIGNED INT,\n");
        transSql.append("  columnname STRING,   columnname LENGTH UNSIGNED INT,\n");
        transSql.append("  username STRING,     username LENGTH UNSIGNED INT,\n");
        transSql.append("  ivalue STRING,                    ---传入参数ivalue字符串\n");
        transSql.append("  ivalue LENGTH UNSIGNED INT,       ---参数ivalue字符串的传入长度\n");
        transSql.append("  offset  UNSIGNED INT,\n");
        transSql.append("  length  UNSIGNED INT,               ---需要加密的长度，0为加密全部\n");
        transSql.append("  ovalue STRING,                    ---传出参数ovalue\n");
        transSql.append("  ovalue MAXLEN UNSIGNED INT,       ---传出参数ovalue的最大容纳长度\n");
        transSql.append("  radix INT);\n");

        try {
            log.info("创建存储过程cOciTransStringEncrypt，SQL：{} " + transSql.toString());
            statement = conn.prepareStatement(transSql.toString());
            statement.execute();
            log.info("创建存储过程end：cOciTransStringEncrypt 命令提交完成");
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    //创建用户时创建该解密存储过程
    public static void cOciTransStringDecryptP(Connection conn, String username) throws SQLException {
        log.info("创建存储过程start：cOciTransStringDecryptP");
        PreparedStatement statement = null;
        StringBuffer transSql = new StringBuffer("CREATE OR REPLACE PROCEDURE " + username + ".c_oci_trans_string_decrypt_p(\n");
        transSql.append("  pid IN VARCHAR2,\n");
        transSql.append("  purl IN VARCHAR2,\n");
        transSql.append("  ipaddr IN VARCHAR2,\n");
        transSql.append("  instancename IN VARCHAR2,\n");
        transSql.append("  dbname IN VARCHAR2,\n");
        transSql.append("  tablename IN VARCHAR2,\n");
        transSql.append("  columnname IN VARCHAR2,\n");
        transSql.append("  username IN VARCHAR2,\n");
        transSql.append("  ivalue IN VARCHAR2,\n");
        transSql.append("  offset IN NATURAL,\n");
        transSql.append("  length IN NATURAL,\n");
        transSql.append("  ovalue OUT VARCHAR2)\n");
        transSql.append("AS EXTERNAL\n");
        transSql.append("  LIBRARY liboraextapi\n");
        transSql.append("  NAME \"oci_trans_string_decrypt\"\n");
        transSql.append("  LANGUAGE C\n");
        transSql.append("  WITH CONTEXT\n");
        transSql.append("  PARAMETERS(\n");
        transSql.append("  CONTEXT,                          ---上下文指针\n");
        transSql.append("  pid STRING,          pid LENGTH UNSIGNED INT,\n");
        transSql.append("  purl STRING,         purl LENGTH UNSIGNED INT,\n");
        transSql.append("  ipaddr STRING,       ipaddr LENGTH UNSIGNED INT,\n");
        transSql.append("  instancename STRING, instancename LENGTH UNSIGNED INT,\n");
        transSql.append("  dbname STRING,       dbname LENGTH UNSIGNED INT,\n");
        transSql.append("  tablename STRING,    tablename LENGTH UNSIGNED INT,\n");
        transSql.append("  columnname STRING,   columnname LENGTH UNSIGNED INT,\n");
        transSql.append("  username STRING,     username LENGTH UNSIGNED INT,\n");
        transSql.append("  ivalue STRING,                    ---传入参数ivalue字符串\n");
        transSql.append("  ivalue LENGTH UNSIGNED INT,       ---参数ivalue字符串的传入长度\n");
        transSql.append("  offset    UNSIGNED INT,\n");
        transSql.append("  length     UNSIGNED INT,\n");
        transSql.append("  ovalue STRING,                    ---传出参数ovalue\n");
        transSql.append("  ovalue MAXLEN UNSIGNED INT);      ---传出参数ovalue的最大容纳长度\n");
        try {
            log.info("创建存储过程cOciTransStringDecryptP，SQL：{} ", transSql.toString());
            statement = conn.prepareStatement(transSql.toString());
            statement.execute();
            log.info("创建存储过程end：cOciTransStringDecryptP 命令提交完成");
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    //创建用户时创建该解密方法
    public static void cOciTransStringDecryptF(Connection conn, String username) throws SQLException {
        log.info("创建函数start：cOciTransStringDecryptF");
        PreparedStatement statement = null;
        StringBuffer transSql = new StringBuffer("CREATE OR REPLACE FUNCTION " + username + ".c_oci_trans_string_decrypt_f(\n");
        transSql.append("  pid IN VARCHAR2,\n");
        transSql.append("  purl IN VARCHAR2,\n");
        transSql.append("  ipaddr IN VARCHAR2,\n");
        transSql.append("  instancename IN VARCHAR2,\n");
        transSql.append("  dbname IN VARCHAR2,\n");
        transSql.append("  tablename IN VARCHAR2,\n");
        transSql.append("  columnname IN VARCHAR2,\n");
        transSql.append("  username IN VARCHAR2,\n");
        transSql.append("  ivalue IN VARCHAR2,\n");
        transSql.append("  offset  IN NATURAL,\n");
        transSql.append("  length IN NATURAL)\n");
        transSql.append("RETURN VARCHAR2\n");
        transSql.append("AS\n");
        transSql.append("   ovalue varchar2(500);\n");
        transSql.append("begin\n");
        transSql.append("    c_oci_trans_string_decrypt_p(pid, purl, ipaddr, instancename,\n");
        transSql.append("    dbname, tablename, columnname, username,\n");
        transSql.append("     ivalue, offset, length, ovalue);\n");
        transSql.append("    return ovalue;\n");
        transSql.append("end;\n");
        try {
            log.info("创建函数cOciTransStringDecryptFSQL：{} ", transSql.toString());
            statement = conn.prepareStatement(transSql.toString());
            statement.execute();
            log.info("创建函数end：cOciTransStringDecryptF 命令提交完成");
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    public static void cOciTransFPEDecrypt(Connection connection, String username) throws SQLException {
        log.info("创建函数start：c_oci_trans_fpe_decrypt_p");
        PreparedStatement statement = null;
        StringBuffer transSql = new StringBuffer();
        transSql.append("create or replace PROCEDURE " + username + ".c_oci_trans_fpe_decrypt_p(\n");
        transSql.append("  pid IN VARCHAR2,\n");
        transSql.append("  purl IN VARCHAR2,\n");
        transSql.append("  ipaddr IN VARCHAR2,\n");
        transSql.append("  instancename IN VARCHAR2,\n");
        transSql.append("  dbname IN VARCHAR2,\n");
        transSql.append("  tablename IN VARCHAR2,\n");
        transSql.append("  columnname IN VARCHAR2,\n");
        transSql.append("  username IN VARCHAR2,\n");
        transSql.append("  ivalue IN VARCHAR2,\n");
        transSql.append("  offset  IN NATURAL,\n");
        transSql.append("  length  IN NATURAL,\n");
        transSql.append("  ovalue OUT VARCHAR2,\n");
        transSql.append("  radix IN PLS_INTEGER)\n");
        transSql.append("AS EXTERNAL\n");
        transSql.append("  LIBRARY liboraextapi\n");
        transSql.append("  NAME \"oci_trans_fpe_decrypt\"\n");
        transSql.append("  LANGUAGE C\n");
        transSql.append("  WITH CONTEXT\n");
        transSql.append("  PARAMETERS(\n");
        transSql.append("  CONTEXT,                          ---上下文指针\n");
        transSql.append("  pid STRING,          pid LENGTH UNSIGNED INT,\n");
        transSql.append("  purl STRING,         purl LENGTH UNSIGNED INT,\n");
        transSql.append("  ipaddr STRING,       ipaddr LENGTH UNSIGNED INT,\n");
        transSql.append("  instancename STRING, instancename LENGTH UNSIGNED INT,\n");
        transSql.append("  dbname STRING,       dbname LENGTH UNSIGNED INT,\n");
        transSql.append("  tablename STRING,    tablename LENGTH UNSIGNED INT,\n");
        transSql.append("  columnname STRING,   columnname LENGTH UNSIGNED INT,\n");
        transSql.append("  username STRING,     username LENGTH UNSIGNED INT,\n");
        transSql.append("  ivalue STRING,                    ---传入参数ivalue字符串\n");
        transSql.append("  ivalue LENGTH UNSIGNED INT,       ---参数ivalue字符串的传入长度\n");
        transSql.append("  offset  UNSIGNED INT,\n");
        transSql.append("  length  UNSIGNED INT,\n");
        transSql.append("  ovalue STRING,                    ---传出参数ovalue\n");
        transSql.append("  ovalue MAXLEN UNSIGNED INT,       ---传出参数ovalue的最大容纳长度\n");
        transSql.append("  radix INT);\n");
        try {
            log.info("创建函数 c_oci_trans_fpe_decrypt_p：{} ", transSql.toString());
            statement = connection.prepareStatement(transSql.toString());
            statement.execute();
            log.info("创建函数end：c_oci_trans_fpe_decrypt_p 命令提交完成");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    //创建用户时创建该解密方法
    public static void cOciTransFpeDecryptF(Connection conn, String username) throws SQLException {
        log.info("创建函数start：cOciTransStringDecryptF");
        PreparedStatement statement = null;
        StringBuffer transSql = new StringBuffer("CREATE OR REPLACE FUNCTION " + username + ".c_oci_trans_fpe_decrypt_f(\n");
        transSql.append(" pid IN VARCHAR2,\n");
        transSql.append(" purl IN VARCHAR2,\n");
        transSql.append(" ipaddr IN VARCHAR2,\n");
        transSql.append(" instancename IN VARCHAR2,\n");
        transSql.append(" dbname IN VARCHAR2,\n");
        transSql.append(" tablename IN VARCHAR2,\n");
        transSql.append(" columnname IN VARCHAR2,\n");
        transSql.append(" username IN VARCHAR2,\n");
        transSql.append(" ivalue IN VARCHAR2,\n");
        transSql.append(" offset  IN NATURAL,\n");
        transSql.append(" length IN NATURAL,\n");
        transSql.append(" radix IN PLS_INTEGER)\n");
        transSql.append("RETURN VARCHAR2\n");
        transSql.append("AS\n");
        transSql.append("  ovalue varchar2(500);\n");
        transSql.append("begin\n");
        transSql.append("  c_oci_trans_fpe_decrypt_p(pid, purl, ipaddr, instancename,\n");
        transSql.append("  dbname, tablename, columnname, username,\n");
        transSql.append("  ivalue, offset, length, ovalue, radix);\n");
        transSql.append("  return ovalue;\n");
        transSql.append("end;\n");
        try {
            log.info("创建函数cOciTransFpeDecryptF：{} ", transSql.toString());
            statement = conn.prepareStatement(transSql.toString());
            statement.execute();
            log.info("创建函数end：cOciTransFpeDecryptF 命令提交完成");
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    /**
     * SQLserver数据库字段加密方法
     *
     * @param conn
     */
    public static void transSQLServerStringEncrypt(Connection conn) throws SQLException {

        /**
         * create or alter FUNCTION func_string_encrypt (
         * @rawstring NVARCHAR(50), @rawstringlen INT,
         * @offset INT, @length INT, @encryptstringlen INT
         * )
         * RETURNS NVARCHAR(50)
         * AS
         * EXTERNAL NAME
         * libsqlextdll.[libsqlserver.SqlExtFunc].Sqlstringencrypt
         * 分别对应 [程序集名称].[namespace.class].[方法/函数]
         * */

        StringBuilder transSQL = new StringBuilder();

        transSQL.append("create FUNCTION func_string_encrypt (");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@rawstring NVARCHAR(50), @rawstringlen INT,");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@offset INT, @length INT, @encryptstringlen INT");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append(")");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("RETURNS NVARCHAR(50)");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("AS");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("EXTERNAL NAME");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("libsqlextdll.[libsqlserver.SqlExtFunc].Sqlstringencrypt ");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("/* 分别对应 [程序集名称].[namespace.class].[方法/函数] */");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement statement = null;
        try {
            log.info("创建函数cOciTransStringDecryptFSQL：{} ", transSQL.toString());
            statement = conn.prepareStatement(transSQL.toString());
            statement.execute();
            log.info("创建函数end：cOciTransStringDecryptF 命令提交完成");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * SQLserver数据库字段解密方法
     *
     * @param conn
     */
    public static void transSQLServerStringDecrypt(Connection conn) throws SQLException {

        /**
         * create or alter FUNCTION func_string_decrypt(
         * @rawstring NVARCHAR(50), @rawstringlen INT,
         * @offset INT, @length INT, @encryptstringlen INT)
         * RETURNS NVARCHAR(50)
         * AS
         * EXTERNAL NAME
         *
         * GO
         * */

        StringBuilder transSQL = new StringBuilder();

        transSQL.append("create FUNCTION func_string_decrypt(");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@rawstring NVARCHAR(50), @rawstringlen INT,");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@offset INT, @length INT, @encryptstringlen INT)");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("RETURNS NVARCHAR(50)");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("AS");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("EXTERNAL NAME");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("libsqlextdll.[libsqlserver.SqlExtFunc].Sqlstringencrypt ");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement statement = null;
        try {
            log.info("创建函数 func_string_decrypt：{} ", transSQL.toString());
            statement = conn.prepareStatement(transSQL.toString());
            statement.execute();
            log.info("创建函数end：cOciTransStringDecryptF 命令提交完成");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }


    /***
     * 创建SQLserver程序集
     * @param connection
     * @param databaseServerName
     */
    public static void transSQLServerAssembly(Connection connection, String databaseServerName,String libsqlextPath) throws SQLException {
        /***
         *
         * 创建程序集
         * USE db_test1
         * GO
         *
         * create ASSEMBLY libsqlextdll
         * FROM 'E:\sqlServerDll\libsqlserverdll.dll'
         * WITH permission_set = UnSafe;
         * GO
         *
         */
        StringBuilder transSQL = new StringBuilder();
        transSQL.append("USE " + databaseServerName);
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("create ASSEMBLY libsqlextdll");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("FROM '" + libsqlextPath + "libsqlserverdll.dll'");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("WITH permission_set = UnSafe;");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement preparedStatement = null;
        try {
            log.info("sql server create lib:" + transSQL.toString());
            preparedStatement = connection.prepareStatement(transSQL.toString());
            preparedStatement.execute();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

}

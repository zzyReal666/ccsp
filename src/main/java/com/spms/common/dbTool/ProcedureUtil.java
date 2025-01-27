package com.spms.common.dbTool;

import com.spms.common.DBStringUtil;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
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
    public static void cOciTransStringEncrypt(Connection conn,String dbaUser, String username) throws SQLException {
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
        transSql.append("  LIBRARY " + dbaUser + ".liboraextapi\n");
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
    public static void cOciTransFPEEncrypt(Connection conn,String dbaUser,String username) throws SQLException {
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
        transSql.append("  LIBRARY " + dbaUser + ".liboraextapi\n");
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
    public static void cOciTransStringDecryptP(Connection conn,String dbaUser, String username) throws SQLException {
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
        transSql.append("  LIBRARY " + dbaUser + ".liboraextapi\n");
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
        transSql.append("   ovalue varchar2(32767);\n");
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

    public static void cOciTransFPEDecrypt(Connection connection,String dbaUser, String username) throws SQLException {
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
        transSql.append("  LIBRARY " + dbaUser + ".liboraextapi\n");
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
    public static void transSQLServerStringEncryptEX(Connection conn) throws SQLException {

        /**
         * create or alter FUNCTION func_string_encrypt_ex (
         * @policy_id NVARCHAR(50),@policy_url NVARCHAR(200),@user_ipaddr NVARCHAR(50),
         * @db_instance_name NVARCHAR(50),@db_table_name NVARCHAR(50),
         * @db_column_name NVARCHAR(50),@db_user_name NVARCHAR(50),
         * @rawstring NVARCHAR(50), @rawstringlen INT,
         * @offset INT, @length INT, @encryptstringlen INT
         * )
         * RETURNS NVARCHAR(50)
         * AS
         * EXTERNAL NAME
         * libsqlextdll.[libsqlserver.SqlExtPolicyFunc].SqlStringEncryptEx
         * 分别对应 [程序集名称].[namespace.class].[方法/函数]
         * GO
         *
         * */

        StringBuilder transSQL = new StringBuilder();

        transSQL.append("CREATE FUNCTION dbo.func_string_encrypt_ex(");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@policy_id NVARCHAR(MAX),@policy_url NVARCHAR(MAX),@user_ipaddr NVARCHAR(MAX),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@db_instance_name NVARCHAR(MAX),@db_name NVARCHAR(MAX),@db_table_name NVARCHAR(MAX),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@db_column_name NVARCHAR(MAX),@db_user_name NVARCHAR(MAX),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@rawstring NVARCHAR(max), @rawstringlen INT,");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@offset INT, @length INT, @encryptstringlen INT");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append(")");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("RETURNS NVARCHAR(max)");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("AS");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("EXTERNAL NAME");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("libsqlextdll.[libsqlserver.SqlExtPolicyFunc].SqlStringEncryptEx ");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement statement = null;
        try {
            log.info("创建函数func_string_encrypt_ex：{} ", transSQL.toString());
            statement = conn.prepareStatement(transSQL.toString());
            boolean execute = statement.execute();
            log.info("创建func_string_encrypt_ex函数返回：{}",execute);
            conn.commit();
            log.info("创建函数end：func_string_encrypt_ex 命令提交完成");
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
    public static void transSQLServerStringDecryptEX(Connection conn) throws SQLException {

        /**
         * create or alter FUNCTION func_string_decrypt_ex (
         * @policy_id NVARCHAR(50),@policy_url NVARCHAR(200),@user_ipaddr NVARCHAR(50),
         * @db_instance_name NVARCHAR(50),@db_name NVARCHAR(50), @db_table_name NVARCHAR(50),
         * @db_column_name NVARCHAR(50),@db_user_name NVARCHAR(50),
         * @rawstring NVARCHAR(50), @rawstringlen INT,
         * @offset INT, @length INT, @encryptstringlen INT
         * )
         * RETURNS NVARCHAR(50)
         * AS
         * EXTERNAL NAME libsqlextdll.[libsqlserver.SqlExtPolicyFunc].SqlStringDecryptEx
         * 分别对应 [程序集名称].[namespace.class].[方法/函数]
         *GO
         * */

        StringBuilder transSQL = new StringBuilder();

        transSQL.append("CREATE FUNCTION dbo.func_string_decrypt_ex(");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@policy_id NVARCHAR(MAX),@policy_url NVARCHAR(MAX),@user_ipaddr NVARCHAR(MAX),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@db_instance_name NVARCHAR(MAX),@db_name NVARCHAR(MAX), @db_table_name NVARCHAR(MAX),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@db_column_name NVARCHAR(MAX),@db_user_name NVARCHAR(MAX),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@rawstring NVARCHAR(MAX), @rawstringlen INT,");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@offset INT, @length INT, @encryptstringlen INT");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append(")");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("RETURNS NVARCHAR(max)");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("AS");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("EXTERNAL NAME libsqlextdll.[libsqlserver.SqlExtPolicyFunc].SqlStringDecryptEx");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement statement = null;
        try {
            log.info("创建函数 func_string_decrypt_ex：{} ", transSQL.toString());
            statement = conn.prepareStatement(transSQL.toString());
            boolean execute = statement.execute();
            log.info("创建func_string_decrypt_ex函数返回：{}",execute);
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * SQLserver FPE加密函数带策略
     *
     * @param conn
     */
    public static void funcSQLServerFuncFpeEncryptEx(Connection conn) throws SQLException {

        /**
         * --创建动态策略的fpe加密函数
         * create or alter FUNCTION func_fpe_encrypt_ex (
         * @policy_id NVARCHAR(50),@policy_url NVARCHAR(200),@user_ipaddr NVARCHAR(50),
         * @db_instance_name NVARCHAR(50),@db_name NVARCHAR(50), @db_table_name NVARCHAR(50),
         * @db_column_name NVARCHAR(50),@db_user_name NVARCHAR(50),
         * @rawstring NVARCHAR(50), @rawstringlen INT,
         * @offset INT, @length INT, @encryptstringlen INT, @radix INT
         * )
         * RETURNS NVARCHAR(50)
         * AS
         * EXTERNAL NAME libsqlextdll.[libsqlserver.SqlExtPolicyFunc].SqlfpeStringEncryptEx
         * 分别对应 [程序集名称].[namespace.class].[方法/函数]
         *GO
         * */

        StringBuilder transSQL = new StringBuilder();

        transSQL.append("create or alter FUNCTION func_fpe_encrypt_ex (");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@policy_id NVARCHAR(50),@policy_url NVARCHAR(200),@user_ipaddr NVARCHAR(50),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@db_instance_name NVARCHAR(50),@db_name NVARCHAR(50), @db_table_name NVARCHAR(50),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@db_column_name NVARCHAR(50),@db_user_name NVARCHAR(50),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@rawstring NVARCHAR(max), @rawstringlen INT,");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@offset INT, @length INT, @encryptstringlen INT, @radix INT");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append(")");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("RETURNS NVARCHAR(max)");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("AS");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("EXTERNAL NAME libsqlextdll.[libsqlserver.SqlExtPolicyFunc].SqlfpeStringEncryptEx");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("/*分别对应 [程序集名称].[namespace.class].[方法/函数]*/");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement statement = null;
        try {
            log.info("创建函数 func_fpe_encrypt_ex：{} ", transSQL.toString());
            statement = conn.prepareStatement(transSQL.toString());
            statement.execute();
            log.info("创建函数end：func_fpe_encrypt_ex 命令提交完成");
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
    public static void funcSQLServerFuncFpeDecryptEx(Connection conn) throws SQLException {

        /**
         * create or alter FUNCTION func_fpe_decrypt_ex (
         * @policy_id NVARCHAR(50),@policy_url NVARCHAR(200),@user_ipaddr NVARCHAR(50),
         * @db_instance_name NVARCHAR(50),@db_name NVARCHAR(50), @db_table_name NVARCHAR(50),
         * @db_column_name NVARCHAR(50),@db_user_name NVARCHAR(50),
         * @rawstring NVARCHAR(50), @rawstringlen INT,
         * @offset INT, @length INT, @encryptstringlen INT, @radix INT
         * )
         * RETURNS NVARCHAR(50)
         * AS
         * EXTERNAL NAME libsqlextdll.[libsqlserver.SqlExtPolicyFunc].SqlfpeStringDecryptEx
         * 分别对应 [程序集名称].[namespace.class].[方法/函数]
         *GO
         * */

        StringBuilder transSQL = new StringBuilder();

        transSQL.append("create or alter FUNCTION func_fpe_decrypt_ex (");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@policy_id NVARCHAR(50),@policy_url NVARCHAR(200),@user_ipaddr NVARCHAR(50),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@db_instance_name NVARCHAR(50),@db_name NVARCHAR(50), @db_table_name NVARCHAR(50),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@db_column_name NVARCHAR(50),@db_user_name NVARCHAR(50),");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@rawstring NVARCHAR(max), @rawstringlen INT,");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("@offset INT, @length INT, @encryptstringlen INT, @radix INT");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append(")");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("RETURNS NVARCHAR(max)");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("AS");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("EXTERNAL NAME libsqlextdll.[libsqlserver.SqlExtPolicyFunc].SqlfpeStringDecryptEx");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("/*分别对应 [程序集名称].[namespace.class].[方法/函数] */");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement statement = null;
        try {
            log.info("创建函数 func_fpe_decrypt_ex：{} ", transSQL.toString());
            statement = conn.prepareStatement(transSQL.toString());
            statement.execute();
            log.info("创建函数end：func_fpe_decrypt_ex 命令提交完成");
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

        transSQL.append("FROM '" + libsqlextPath + "'");
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

    public static void pgextFuncStringEncrypt(Connection connection,DbhsmDbUser dbhsmDbUser) throws SQLException {
        /***
         *
         * 创建PostgreSQL加密函数
         * CREATE OR REPLACE FUNCTION
         *  testuser1.pgext_func_string_encrypt( --testuser1 架构
         * 	text, --策略唯一标识类型
         * 	text, --策略下载地址类型
         * 	text, --ip
         * 	text, --实例名
         * 	text, --库名
         * 	text, --表名
         * 	text, --列名
         * 	text, --用户名
         * 	text, --加密数据
         * 	integer) --偏移量
         *      RETURNS text
         *      AS 'D:\pgsql\postgreSQL', 'pgext_func_string_encrypt'
         *      LANGUAGE C STRICT;
         *
         */
        String encLibapiPath = DBStringUtil.removeFileExtension(dbhsmDbUser.getEncLibapiPath());
        StringBuilder transSQL = new StringBuilder();
        transSQL.append("CREATE OR REPLACE FUNCTION ");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("\""+dbhsmDbUser.getDbSchema() + "\".pgext_func_string_encrypt (");
        transSQL.append("text,--策略唯一标识类型");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--策略下载地址类型");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--ip");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--实例名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--库名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--表名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--列名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--用户名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--加密数据");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("integer, --偏移量");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("integer) --加密长度");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("RETURNS varchar");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("AS '" + encLibapiPath +"', 'pgext_func_string_encrypt'");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append(" LANGUAGE C STRICT;");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement preparedStatement = null;
        try {
            log.info("PostgreSQL create pgext_func_string_encrypt:\n" + transSQL);
            preparedStatement = connection.prepareStatement(transSQL.toString());
            preparedStatement.execute();
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException(e.getMessage());
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    public static void pgextFuncStringDecrypt(Connection connection, DbhsmDbUser dbhsmDbUser) throws SQLException {
        /***
         * 创建PostgreSQL解密函数

         *
         *CREATE OR REPLACE FUNCTION
         * testuser1.pgext_func_string_decrypt(  -testuser1 架构
         * 	text, --策略唯一标识类型
         * 	text, --策略下载地址类型
         * 	text, --ip
         * 	text, --实例名
         * 	text, --库名
         * 	text, --表名
         * 	text, --列名
         * 	text, --用户名
         * 	text, --加密数据
         * 	integer) --偏移量
         * RETURNS text
         * AS 'D:\pgsql\postgreSQL', 'pgext_func_string_decrypt'
         * LANGUAGE C STRICT;
         *
         */
        String encLibapiPath = DBStringUtil.removeFileExtension(dbhsmDbUser.getEncLibapiPath());
        StringBuilder transSQL = new StringBuilder();
        transSQL.append("CREATE OR REPLACE FUNCTION ");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("\""+dbhsmDbUser.getDbSchema() + "\".pgext_func_string_decrypt(");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--策略唯一标识类型");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--策略下载地址类型");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--ip");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--实例名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--库名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--表名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--列名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--用户名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--解密数据");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("integer, --偏移量");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("integer) --解密长度");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("RETURNS varchar");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("AS '" + encLibapiPath +"', 'pgext_func_string_decrypt'");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append(" LANGUAGE C STRICT;");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement preparedStatement = null;
        try {
            log.info("PostgreSQL create pgext_func_string_encrypt:" + transSQL);
            preparedStatement = connection.prepareStatement(transSQL.toString());
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

    public static void pgextFuncFPEEncrypt(Connection connection, DbhsmDbUser dbhsmDbUser) throws SQLException {
        /***
         * 创建PostgreSQL FPE格式保留算法
         * CREATE OR REPLACE FUNCTION testuser1.pgext_func_fpe_encrypt( --testuser1 架构
         * 	text, --策略唯一标识类型
         * 	text, --策略下载地址类型
         * 	text, --ip
         * 	text, --实例名
         * 	text, --库名
         * 	text, --表名
         * 	text, --列名
         * 	text, --用户名
         * 	text, --加密数据
         * 	integer, --偏移量
         * 	integer, --加密长度
         * integer) --算法radix
         *      RETURNS text
         *      AS 'D:\pgsql\postgreSQL', 'pgext_func_fpe_encrypt'
         *      LANGUAGE C STRICT;
         *
         */
        String encLibapiPath = DBStringUtil.removeFileExtension(dbhsmDbUser.getEncLibapiPath());
        StringBuilder transSQL = new StringBuilder();
        transSQL.append("CREATE OR REPLACE FUNCTION ");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("\""+dbhsmDbUser.getDbSchema() + "\".pgext_func_fpe_encrypt(");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--策略唯一标识类型");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--策略下载地址类型");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--ip");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--实例名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--库名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--表名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--列名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--用户名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--加密数据");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("integer,--偏移量");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("integer,--加密长度");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("integer) --算法radix");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("RETURNS text");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("AS '" + encLibapiPath +"', 'pgext_func_fpe_encrypt'");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append(" LANGUAGE C STRICT;");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement preparedStatement = null;
        try {
            log.info("PostgreSQL create pgext_func_fpe_encrypt:" + transSQL);
            preparedStatement = connection.prepareStatement(transSQL.toString());
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

    public static void pgextFuncFPEDecrypt(Connection connection, DbhsmDbUser dbhsmDbUser) throws SQLException {
        /***
         * 创建PostgreSQL FPE格式保留算法
         * CREATE OR REPLACE FUNCTION testuser1.pgext_func_fpe_decrypt( --testuser1 架构
         * 	text, --策略唯一标识类型
         * 	text, --策略下载地址类型
         * 	text, --ip
         * 	text, --实例名
         * 	text, --库名
         * 	text, --表名
         * 	text, --列名
         * 	text, --用户名
         * 	text, --解密数据
         * 	integer, --偏移量
         * 	integer, --加密长度
         * integer) --算法radix
         *      RETURNS text
         *      AS 'D:\pgsql\postgreSQL', 'pgext_func_fpe_decrypt'
         *      LANGUAGE C STRICT;
         */
        String encLibapiPath = DBStringUtil.removeFileExtension(dbhsmDbUser.getEncLibapiPath());
        StringBuilder transSQL = new StringBuilder();
        transSQL.append("CREATE OR REPLACE FUNCTION ");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("\""+dbhsmDbUser.getDbSchema() + "\".pgext_func_fpe_decrypt(");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--策略唯一标识类型");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--策略下载地址类型");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--ip");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--实例名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--库名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--表名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--列名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--用户名");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("text,--解密数据");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("integer,--偏移量");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("integer,--加密长度");
        transSQL.append(System.getProperty("line.separator"));
        transSQL.append("integer) --算法radix");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("RETURNS text");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append("AS '" + encLibapiPath+"', 'pgext_func_fpe_decrypt'");
        transSQL.append(System.getProperty("line.separator"));

        transSQL.append(" LANGUAGE C STRICT;");
        transSQL.append(System.getProperty("line.separator"));

        PreparedStatement preparedStatement = null;
        try {
            log.info("PostgreSQL create pgext_func_fpe_decrypt:" + transSQL);
            preparedStatement = connection.prepareStatement(transSQL.toString());
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
}

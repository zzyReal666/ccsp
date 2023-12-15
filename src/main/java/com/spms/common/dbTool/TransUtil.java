package com.spms.common.dbTool;

import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;

/**
 * @author zhouwenhao
 * @date 2022/12/13
 * @dec 触发器工具类
 */
@Slf4j
public class TransUtil {
    /**
     * 创建列执行
     */
    public static void transEncryptColumns(Connection conn, DbhsmEncryptColumnsAdd encryptColumns) throws Exception {
        Statement statement = null;
        log.info("创建Oracle触发器start");

        StringBuffer transSql = new StringBuffer("CREATE OR REPLACE TRIGGER " + encryptColumns.getDbUserName() + ".tr_" + encryptColumns.getDbUserName() + "_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns() + "\n");
        transSql.append("    BEFORE INSERT\n");
        transSql.append("ON " + encryptColumns.getDbUserName() + "." + encryptColumns.getDbTable() + " -- utest 是用户名，test01 是表格名称\n");
        transSql.append("    FOR EACH ROW\n");
        transSql.append("BEGIN\n");
        transSql.append("    if (:NEW."+ encryptColumns.getEncryptColumns()+" is not NULL) then -- COLUMN1是需要加密处理的列名称\n");
        transSql.append("    c_oci_trans_string_encrypt_p(--c_oci_trans_string_encrypt_p 是储过程名称\n");
        transSql.append("    '" + encryptColumns.getId() + "',--策略唯一标识\n");
        transSql.append("    'http://" + encryptColumns.getIpAndPort() + "/prod-api/dbhsm/api/datahsm/v1/strategy/get', --'http://192.168.6.31:8080/TestCurl/PullPolicy',策略下载地址\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'IP_ADDRESS'),--客户端IP\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'INSTANCE_NAME'),--库实例名\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'DB_NAME'),--库名\n");
        transSql.append("    '" + encryptColumns.getDbTable() + "',--表名\n");
        transSql.append("    '" + encryptColumns.getEncryptColumns() + "',--列名，以列名 name 为例\n");
        transSql.append("    USER(),--用户名\n");
        transSql.append("    :NEW." + encryptColumns.getEncryptColumns().toUpperCase() + ",0,0,:NEW." + encryptColumns.getEncryptColumns().toUpperCase() + ");--变换的列，此处为变换 name 列6,8分别为偏移量和加密长度\n");
        transSql.append("end if;\n");
        transSql.append("END;\n");
        try {
            log.info("创建Oracle触发器{}", transSql);
            statement = conn.createStatement();
            statement.execute(transSql.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    public static void transFPEEncryptColumns(Connection conn, DbhsmEncryptColumnsAdd encryptColumns) throws Exception {
        log.info("创建Oracle触发器fpestart");
        //获取网口ip
        StringBuffer transSql = new StringBuffer("CREATE OR REPLACE TRIGGER " + encryptColumns.getDbUserName() + ".tr_fpe_" + encryptColumns.getDbUserName() + "_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns() + "\n");
        transSql.append("    BEFORE INSERT\n");
        transSql.append("ON " + encryptColumns.getDbUserName() + "." + encryptColumns.getDbTable() + " -- utest 是用户名，test01 是表格名称\n");
        transSql.append("    FOR EACH ROW\n");
        transSql.append("BEGIN\n");
        transSql.append("    if (:NEW."+ encryptColumns.getEncryptColumns()+" is not NULL) then -- COLUMN1是需要加密处理的列名称\n");
        transSql.append("    c_oci_trans_fpe_encrypt_p(--c_oci_trans_string_encrypt_p 是储过程名称\n");
        transSql.append("    '" + encryptColumns.getId() + "',--策略唯一标识\n");
        transSql.append("    'http://" + encryptColumns.getIpAndPort() + "/prod-api/dbhsm/api/datahsm/v1/strategy/get', --'http://192.168.6.31:8080/TestCurl/PullPolicy',策略下载地址\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'IP_ADDRESS'),--客户端IP\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'INSTANCE_NAME'),--库实例名\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'DB_NAME'),--库名\n");
        transSql.append("    '" + encryptColumns.getDbTable() + "',--表名\n");
        transSql.append("    '" + encryptColumns.getEncryptColumns() + "',--列名，以列名 name 为例\n");
        transSql.append("    USER(),--用户名\n");
        transSql.append("    :NEW." + encryptColumns.getEncryptColumns().toUpperCase() + "," + (ObjectUtils.isEmpty(encryptColumns.getEncryptionOffset()) ? 0 : encryptColumns.getEncryptionOffset() - 1) + "," + (ObjectUtils.isEmpty(encryptColumns.getEncryptionLength()) ? 0 : encryptColumns.getEncryptionLength()-(encryptColumns.getEncryptionOffset() - 1)) + ", :NEW." + encryptColumns.getEncryptColumns().toUpperCase() + ",--变换的列，此处为变换 name 列6,8分别为偏移量和加密长度\n");
        transSql.append("    " + encryptColumns.getEncryptionAlgorithm() + ");\n");
        transSql.append("end if;\n");
        transSql.append("END;\n");
        Statement statement = null;
        try {
            log.info("创建Oracle触发器fpestart{}", transSql);
            statement = conn.createStatement();
            statement.execute(transSql.toString());

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    /**
     * 创建SqlServer数据库列加密的触发器
     *
     * @param conn
     * @param zaDatabaseEncryptColumns
     * @throws Exception
     */
    public static void transEncryptColumnsToSqlServer(Connection conn, DbhsmEncryptColumnsAdd zaDatabaseEncryptColumns) throws Exception {

        Map<String, String> schemaMap = DBUtil.findSchemaByTable(conn, zaDatabaseEncryptColumns.getDbTable());
        String schemaName = "dbo";
        if (schemaMap.containsKey("schemaName")) {
            schemaName = schemaMap.get("schemaName");
        }

        /**
         * USE [dbtest]
         * GO
         * --Object:  Trigger [dbo].[tr_Table_1_name]    Script Date: 2023/2/14 13:41:38
         *SET ANSI_NULLS OFF-- 保持关闭，否则触发器无法判断null
                * GO
                * SET QUOTED_IDENTIFIER ON
         *GO
                * CREATE OR ALTER TRIGGER [dbo].[tr_tablename_column_sm4_encrypt]--自定义名称 dbo架构名 sm4/fpe算法名 encrypt/
        decrypt 加/解密
                * ON dbtest.dbo.Table1-- 触发器生效的表
                * AFTER INSERT-- 插入时触发
                * AS
                * declare c cursor for select[name] from inserted --name 加密列
                * declare
                * @policy_id NVARCHAR(50), @policy_url NVARCHAR(200), @user_ipaddr NVARCHAR(50),
         *@db_instance_name NVARCHAR(50), @db_name NVARCHAR(50), @db_table_name NVARCHAR(50),
         *@db_column_name NVARCHAR(50), @db_user_name NVARCHAR(50),
         *@rawstring nvarchar(50),@rawstringlen int,
         *@offset int,@length int,
         *@encryptdata nvarchar(50),@encryptlen int
         *open c
         *fetch next from c into @rawstring
         *IF(UPDATE([name]))AND( @rawstring <>null) ##防止影响其他列的修改,name为加密列，rawstring为插入的数据
         *BEGIN
                * SET NOCOUNT ON;
         *set @policy_id ='433880302541213696'
                * set @policy_url ='http://192.168.7.133:10013/prod-api/dbhsm/api/datahsm/v1/strategy/get'
                * select @user_ipaddr =client_net_address FROM sys.dm_exec_connections WHERE session_id = @ @SPID
        --客户端ip
                * set @db_instance_name =CAST(@ @ServerName as char)--实例名
                * set @db_name =db_name()-- 数据库名
                * set @db_table_name ='table1'-- 加密表
                * set @db_column_name ='name'-- 加密列
                * set @db_user_name =suser_name()
                * set @rawstringlen =LEN( @rawstring)/2
                * set @offset =0
                * set @length =@rawstringlen
         *set @encryptlen =@rawstringlen
         *
         *set @encryptdata =dbtest.dbo.func_string_encrypt_ex( @policy_id,@policy_url,@user_ipaddr,
         *@db_instance_name,@db_name,@db_table_name,@db_column_name,@db_user_name,
         *@rawstring,@rawstringlen,@offset,@length,@encryptlen)  --数据库.架构.加密函数（）
         *
         *update[dbo].[Table1]set name = @encryptdata where[name] =
        @rawstring --[dbo].[Table_1] 触发器生效的表, name 加密列, name 加密列
         *END
                * CLOSE c
                * DEALLOCATE c
         */
        StringBuffer transSql = new StringBuffer();
        String alg = zaDatabaseEncryptColumns.getEncryptionAlgorithm();
        // 替换成表名和字段名
        transSql.append("CREATE OR ALTER  TRIGGER "+ schemaName + ".tr_" + zaDatabaseEncryptColumns.getDbTable() + "_" + zaDatabaseEncryptColumns.getEncryptColumns()+"_" + DbConstants.algMapping(alg) + "_encrypt");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("   ON " + zaDatabaseEncryptColumns.getDatabaseServerName()+ "." +schemaName + "." + zaDatabaseEncryptColumns.getDbTable() + "");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("   AFTER INSERT");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("AS");
        transSql.append(System.getProperty("line.separator"));

        //加密字段
        transSql.append("    declare c cursor for select " + zaDatabaseEncryptColumns.getEncryptColumns() + " from inserted");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("declare @policy_id NVARCHAR(50), @policy_url NVARCHAR(200), @user_ipaddr NVARCHAR(50),");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("@db_instance_name NVARCHAR(50), @db_name NVARCHAR(50), @db_table_name NVARCHAR(50),");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("@db_column_name NVARCHAR(50), @db_user_name NVARCHAR(50),");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("@rawstring nvarchar(max),@rawstringlen int,");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("   @offset int,@length int,");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("   @encryptdata nvarchar(max),@encryptlen int"+(DbConstants.SGD_SM4.equals(alg)?"":",@radix int"));
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" open c");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" fetch next from c into @rawstring");
        transSql.append(System.getProperty("line.separator"));

        //替换成加密列的字段
        transSql.append("IF(UPDATE(" + zaDatabaseEncryptColumns.getEncryptColumns() + ")) AND (@rawstring is not null) /*防止影响其他列的修改*/");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("BEGIN");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" SET NOCOUNT ON;");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" set  @policy_id ='"+ zaDatabaseEncryptColumns.getId() + "'");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" set @policy_url ='http://" + zaDatabaseEncryptColumns.getIpAndPort() + "/prod-api/dbhsm/api/datahsm/v1/strategy/get'");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("select @user_ipaddr =client_net_address FROM sys.dm_exec_connections WHERE session_id = @@SPID");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("set @db_instance_name =CAST(@@ServerName as char)");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("set @db_name =db_name()");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("set @db_table_name ='"+zaDatabaseEncryptColumns.getDbTable() +"'");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("set @db_column_name ='"+zaDatabaseEncryptColumns.getEncryptColumns() +"'");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("set @db_user_name =suser_name()");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("set @rawstringlen =LEN(@rawstring)");
        transSql.append(System.getProperty("line.separator"));

        if (!DbConstants.SGD_SM4.equals(alg) && DbConstants.ESTABLISH_RULES_YES.equals(zaDatabaseEncryptColumns.getEstablishRules())) {
            transSql.append(" set @offset = "+(zaDatabaseEncryptColumns.getEncryptionOffset()-1));
            transSql.append(System.getProperty("line.separator"));

            transSql.append(" set @length = "+(zaDatabaseEncryptColumns.getEncryptionLength()-(zaDatabaseEncryptColumns.getEncryptionOffset()-1)));
            transSql.append(System.getProperty("line.separator"));
        } else {
            transSql.append(" set @offset = 0");
            transSql.append(System.getProperty("line.separator"));

            transSql.append(" set @length = @rawstringlen");
            transSql.append(System.getProperty("line.separator"));
        }
        transSql.append(" set @encryptlen = @rawstringlen");
        transSql.append(System.getProperty("line.separator"));

        if (!DbConstants.SGD_SM4.equals(alg)) {
            transSql.append(" set @radix =" + Integer.parseInt(alg));
        }
        transSql.append(" set @encryptdata = " + zaDatabaseEncryptColumns.getDatabaseServerName()+ "." +schemaName + ".func_"+DbConstants.algMappingStrOrFpe(alg)+"_encrypt_ex( @policy_id,@policy_url,@user_ipaddr,");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("@db_instance_name,@db_name,@db_table_name,@db_column_name,@db_user_name,");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("@rawstring,@rawstringlen,@offset,@length,@encryptlen"+(DbConstants.SGD_SM4.equals(alg)?"":", @radix")+")");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" update " + schemaName + "." + zaDatabaseEncryptColumns.getDbTable() + " set " + zaDatabaseEncryptColumns.getEncryptColumns() + " = @encryptdata where " + zaDatabaseEncryptColumns.getEncryptColumns() + " = @rawstring");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("END");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("CLOSE c");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("DEALLOCATE c");
        transSql.append(System.getProperty("line.separator"));

        PreparedStatement preparedStatement = null;
        try {
            log.info("sql server create TRIGGER:" + transSql.toString());
            preparedStatement = conn.prepareStatement(transSql.toString());
            preparedStatement.execute();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    /**
     * 创建Mysql触发器
     *
     * @param conn
     * @param encryptColumns
     */
    public static void transEncryptColumnsToMySql(Connection conn, DbhsmEncryptColumnsAdd encryptColumns) throws Exception {
        /**
         create trigger tri_insert_name before insert
         on testdb.tests
         for each row
         set NEW.name = StringEncrypt(
         '87815597982879744',
         --策略唯一标识
         'http://192.168.7.106:8001/prod-api/dbhsm/api/datahsm/v1/strategy/get',
         --策略下载地址
         'ip_address',
         --ip
         CAST(Database() AS CHAR),
         --实例名
         CAST(Database() AS CHAR),
         --库名
         'TABLE1',
         --表名
         'NAME',
         --列名，以列名 name 为例
         CAST(User() AS CHAR),
         --用户名
         NEW.name,0,0);

         */
        Long length = 0L;
        int offset = 0;
        String algorithm = encryptColumns.getEncryptionAlgorithm();

        if (DbConstants.ESTABLISH_RULES_YES.equals(encryptColumns.getEstablishRules())) {
            offset = encryptColumns.getEncryptionOffset();
            length = encryptColumns.getEncryptionLength();
        }
        String funName = DbConstants.SGD_SM4.equals(algorithm)?"StringEncrypt(":"FpeStringEncrypt(";
        log.info("创建Mysql触发器start");
        //获取网口ip
        StringBuffer transSql = new StringBuffer("CREATE TRIGGER tri_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns() + " before insert");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("on " + encryptColumns.getDatabaseServerName() + "." + encryptColumns.getDbTable() );
        transSql.append(System.getProperty("line.separator"));
        transSql.append("for each row");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("set NEW." + encryptColumns.getEncryptColumns() + " = "+funName );
        transSql.append(System.getProperty("line.separator"));
        transSql.append("'" + encryptColumns.getId() + "',");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("'http://" + encryptColumns.getIpAndPort() + "/prod-api/dbhsm/api/datahsm/v1/strategy/get',");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("'ip_address',#--IP");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("CAST(Database() AS CHAR),");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("CAST(Database() AS CHAR),");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("'" + encryptColumns.getDbTable() + "',");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("'" + encryptColumns.getEncryptColumns() + "',");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("CAST(User() AS CHAR),");
        transSql.append(System.getProperty("line.separator"));

        if (DbConstants.SGD_SM4.equals(algorithm)) {
            transSql.append("NEW." + encryptColumns.getEncryptColumns() + "," + "0,0);\n");
        } else {
            if (DbConstants.ESTABLISH_RULES_YES.equals(encryptColumns.getEstablishRules())) {
                transSql.append("NEW." + encryptColumns.getEncryptColumns() + "," + //加密列
                        (encryptColumns.getEncryptionOffset() -1 ) + "," + //偏移量
                        (encryptColumns.getEncryptionLength()-(encryptColumns.getEncryptionOffset() -1 )) +","+algorithm+");\n");
            } else {
                transSql.append("NEW." + encryptColumns.getEncryptColumns() + "," + "0,0,"+algorithm+"); \n");
            }
        }
        transSql.append(System.getProperty("line.separator"));

        Statement statement = null;

        try {
            log.info("exec sql:" + transSql);
            statement = conn.createStatement();
            statement.execute(transSql.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    /**
     * 创建PostgreSql触发器(需要使用用户创建) 1、定义触发器函数
     *
     * @param conn
     * @param
     */
    public static void transEncryptFunToPostgreSql(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, DbhsmDbUser user ) throws Exception {
        /**
         create or replace function testuser1.tr_sm4_username_tablename_name()
         returns trigger as
         $tr_username_schema_tablename_name$
         begin
         NEW.name := testuser1.pgext_func_string_encrypt(
         '87815597982879744',
         --策略唯一标识
         'http://192.168.7.106:8001/prod-api/dbhsm/api/datahsm/v1/strategy/get',
         --策略下载地址
         CAST(inet_client_addr() as char),
         --ip
         CAST(current_catalog as char),
         --实例名
         CAST(current_catalog as char),
         --库名
         'TABLE1',
         --表名
         'NAME',
         --列名，以列名 name 为例
         CAST(user as char),
         --用户名
         NEW.name,0);
         --NEW.name 加密列， --0 offset;
         return NEW;
         end;
         $tr_username_schema_tablename_name$ language 'plpgsql';

         */
        Statement statement = null;
        String  alg = dbhsmEncryptColumnsAdd.getEncryptionAlgorithm();
        //String userSchema = "'"+user.getDbSchema()+"'";
        String userSchema = user.getDbSchema();
        String funName = "\""+userSchema + "\".tr_" + DbConstants.algMapping(alg) + "_" + user.getUserName() + "_" + dbhsmEncryptColumnsAdd.getDbTable()+ "_" + dbhsmEncryptColumnsAdd.getEncryptColumns();
        String funName$ = "tr_" + user.getUserName() + "_"  + userSchema + "_"  + dbhsmEncryptColumnsAdd.getDbTable() + "_" + dbhsmEncryptColumnsAdd.getEncryptColumns();
        try {
            // 1、定义触发器函数
            log.info("创建PostgreSql触发器函数start");
            //函数名是动态的
            StringBuffer transFun = new StringBuffer("create or replace function " + funName + "()");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("returns trigger as");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("$" + funName$ + "$");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("begin");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("NEW." + dbhsmEncryptColumnsAdd.getEncryptColumns() + " := \""+ userSchema +"\".pgext_func_"+DbConstants.algMappingStrOrFpe(alg)+"_encrypt(");
            transFun.append("'" + dbhsmEncryptColumnsAdd.getId() + "',");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("'http://" + dbhsmEncryptColumnsAdd.getIpAndPort()+ "/prod-api/dbhsm/api/datahsm/v1/strategy/get',");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("CAST(inet_client_addr() as text),");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("CAST(current_catalog as text),");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("CAST(current_catalog as text),");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("'" + dbhsmEncryptColumnsAdd.getDbTable() + "',");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("'" + dbhsmEncryptColumnsAdd.getEncryptColumns() + "',");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("CAST(user AS text),");
            transFun.append(System.getProperty("line.separator"));

            if (DbConstants.SGD_SM4.equals(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm())) {
                transFun.append("NEW." + dbhsmEncryptColumnsAdd.getEncryptColumns() + ",0,0);\n");
            } else {
                if (DbConstants.ESTABLISH_RULES_YES.equals(dbhsmEncryptColumnsAdd.getEstablishRules())) {
                    transFun.append("NEW." + dbhsmEncryptColumnsAdd.getEncryptColumns() + "," + //加密列
                            (dbhsmEncryptColumnsAdd.getEncryptionOffset() -1 ) + "," + //偏移量
                            (dbhsmEncryptColumnsAdd.getEncryptionLength()-(dbhsmEncryptColumnsAdd.getEncryptionOffset() -1 )) +"," +//加密长度
                            dbhsmEncryptColumnsAdd.getEncryptionAlgorithm() + ");\n");
                } else {
                    transFun.append("NEW." + dbhsmEncryptColumnsAdd.getEncryptColumns() + ",0,0,"+ dbhsmEncryptColumnsAdd.getEncryptionAlgorithm() +");\n");
                }
            }
            transFun.append(System.getProperty("line.separator"));
            transFun.append("return NEW;");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("end");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("$" + funName$ + "$ language 'plpgsql'");
            transFun.append(System.getProperty("line.separator"));

            log.info("exec sql:" + transFun);
            statement = conn.createStatement();
            statement.execute(transFun.toString());

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    /**
     * 创建PostgreSql触发器(需要使用用户创建)
     *
     * @param conn
     * @param
     */
    public static void transEncryptColumnsToPostgreSql(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, DbhsmDbUser user) throws Exception {
        /**
         *
         * 2、创建触发器，设置所触发的条件和执行的函数
         * create trigger tr_encrypt_name --触发器名称
         * before insert or update of "name" on testuser1."table1"
         * --name为加密列
         * for each row
         * execute procedure tr_func_string_encrypt(); --触发器函数
         */
        Statement statement = null;
        String funName = "tr_" + DbConstants.algMapping(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm()) + "_" + user.getUserName() + "_" + dbhsmEncryptColumnsAdd.getDbTable()+ "_" + dbhsmEncryptColumnsAdd.getEncryptColumns();
        String transName = "tr_" + DbConstants.algMapping(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm()) + "_" + dbhsmEncryptColumnsAdd.getDbTable()+ "_" + dbhsmEncryptColumnsAdd.getEncryptColumns() ;
        String userSchema = "\""+user.getDbSchema()+"\"";
        try {
            // 1、创建触发器，设置所触发的条件和执行的函数
            log.info("创建PostgreSql触发器start");

            StringBuffer transSql = new StringBuffer("create trigger " + transName);
            transSql.append(System.getProperty("line.separator"));
            transSql.append("before insert on " + userSchema + ".\"" + dbhsmEncryptColumnsAdd.getDbTable() + "\"");
            transSql.append(System.getProperty("line.separator"));
            transSql.append("for each row");
            transSql.append(System.getProperty("line.separator"));
            transSql.append("execute procedure " + userSchema + "." + funName + "()");

            log.info("exec sql:" + transSql);
            statement = conn.createStatement();
            statement.execute(transSql.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }

    }
}

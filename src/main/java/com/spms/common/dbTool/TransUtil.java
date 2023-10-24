package com.spms.common.dbTool;

import com.ccsp.common.core.utils.StringUtils;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
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
        transSql.append("    BEFORE INSERT OR UPDATE\n");
        transSql.append("OF " + encryptColumns.getEncryptColumns() + "--  是需要加密处理的列名称\n");
        transSql.append("ON " + encryptColumns.getDbUserName() + "." + encryptColumns.getDbTable() + " -- utest 是用户名，test01 是表格名称\n");
        transSql.append("    FOR EACH ROW\n");
        transSql.append("BEGIN\n");
        transSql.append("    c_oci_trans_string_encrypt_p(--c_oci_trans_string_encrypt_p 是储过程名称\n");
        transSql.append("    '" + encryptColumns.getId() + "',--策略唯一标识\n");
        transSql.append("    'http://" + encryptColumns.getIpAndPort() + "/api/datahsm/v1/strategy/get', --'http://192.168.6.31:8080/TestCurl/PullPolicy',策略下载地址\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'IP_ADDRESS'),--客户端IP\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'INSTANCE_NAME'),--库实例名\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'DB_NAME'),--库名\n");
        transSql.append("    '" + encryptColumns.getDbTable() + "',--表名\n");
        transSql.append("    '" + encryptColumns.getEncryptColumns() + "',--列名，以列名 name 为例\n");
        transSql.append("    USER(),--用户名\n");
        transSql.append("    :NEW." + encryptColumns.getEncryptColumns().toUpperCase() + ",0,0,:NEW." + encryptColumns.getEncryptColumns().toUpperCase() + ");--变换的列，此处为变换 name 列6,8分别为偏移量和加密长度\n");
        transSql.append("END;\n");
        try {
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
        log.info("创建Oracle触发器start");
        //获取网口ip
        StringBuffer transSql = new StringBuffer("CREATE OR REPLACE TRIGGER " + encryptColumns.getDbUserName() + ".tr_fpe_" + encryptColumns.getDbUserName() + "_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns() + "\n");
        transSql.append("    BEFORE INSERT OR UPDATE\n");
        transSql.append("OF " + encryptColumns.getEncryptColumns() + "--  是需要加密处理的列名称\n");
        transSql.append("ON " + encryptColumns.getDbUserName() + "." + encryptColumns.getDbTable() + " -- utest 是用户名，test01 是表格名称\n");
        transSql.append("    FOR EACH ROW\n");
        transSql.append("BEGIN\n");
        transSql.append("    c_oci_trans_fpe_encrypt_p(--c_oci_trans_string_encrypt_p 是储过程名称\n");
        transSql.append("    '" + encryptColumns.getId() + "',--策略唯一标识\n");
        transSql.append("    'http://" + encryptColumns.getIpAndPort() + "/api/datahsm/v1/strategy/get', --'http://192.168.6.31:8080/TestCurl/PullPolicy',策略下载地址\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'IP_ADDRESS'),--客户端IP\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'INSTANCE_NAME'),--库实例名\n");
        transSql.append("    SYS_CONTEXT('USERENV', 'DB_NAME'),--库名\n");
        transSql.append("    '" + encryptColumns.getDbTable() + "',--表名\n");
        transSql.append("    '" + encryptColumns.getEncryptColumns() + "',--列名，以列名 name 为例\n");
        transSql.append("    USER(),--用户名\n");
        transSql.append("    :NEW." + encryptColumns.getEncryptColumns().toUpperCase() + "," + (ObjectUtils.isEmpty(encryptColumns.getEncryptionOffset()) ? 0 : encryptColumns.getEncryptionOffset() - 1) + "," + (ObjectUtils.isEmpty(encryptColumns.getEncryptionLength()) ? 0 : encryptColumns.getEncryptionLength()) + ", :NEW." + encryptColumns.getEncryptColumns().toUpperCase() + ",--变换的列，此处为变换 name 列6,8分别为偏移量和加密长度\n");
        transSql.append("    " + encryptColumns.getEncryptionAlgorithm() + ");\n");
        transSql.append("END;\n");
        Statement statement = null;
        try {
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
         * USE [db_test1]
         * GO
         * SET ANSI_NULLS OFF
         * GO
         * SET QUOTED_IDENTIFIER ON
         * GO
         *
         * CREATE TRIGGER [dbo].[tr_test_insert]
         *    ON  [dbo].[table1]
         *    AFTER INSERT,UPDATE
         * AS
         *     declare c cursor for select ENCRYPTSTRING from inserted
         *  declare @rawstring nvarchar(50),@rawstringlen int,
         *    @offset int,@length int,
         *    @encryptdata nvarchar(50),@encryptlen int
         *  open c
         *  fetch next from c into @rawstring
         * IF(UPDATE(ENCRYPTSTRING)) AND (@rawstring <> null) --防止影响其他列的修改
         *BEGIN
         * SET NOCOUNT ON;
         *set @rawstringlen =DATALENGTH( @rawstring)/2
         * set @offset =0
         * set @length =@rawstringlen
         *set @encryptlen =@rawstringlen
         *set @encryptdata =dbo.func_string_encrypt( @rawstring,@rawstringlen,@offset,@length,@encryptlen)
         *update dbo.table1 set ENCRYPTSTRING = @encryptdata where ENCRYPTSTRING = @rawstring
         *END
         * CLOSE c
         * DEALLOCATE c
         */
        StringBuffer transSql = new StringBuffer();

        // 替换成表名和字段名
        transSql.append("CREATE   TRIGGER tr_" + zaDatabaseEncryptColumns.getDbTable() + "_" + zaDatabaseEncryptColumns.getEncryptColumns());
        transSql.append(System.getProperty("line.separator"));

        transSql.append("   ON " + schemaName + "." + zaDatabaseEncryptColumns.getDbTable() + "");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("   AFTER INSERT,UPDATE");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("AS");
        transSql.append(System.getProperty("line.separator"));

        //加密字段
        transSql.append("    declare c cursor for select " + zaDatabaseEncryptColumns.getEncryptColumns() + " from inserted");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" declare @rawstring nvarchar(50),@rawstringlen int,");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("   @offset int,@length int,");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("   @encryptdata nvarchar(50),@encryptlen int");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" open c");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" fetch next from c into @rawstring");
        transSql.append(System.getProperty("line.separator"));

        //替换成加密列的字段
        transSql.append("IF(UPDATE(" + zaDatabaseEncryptColumns.getEncryptColumns() + ")) AND (@rawstring IS NOT null) /*防止影响其他列的修改*/");
        transSql.append(System.getProperty("line.separator"));

        transSql.append("BEGIN");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" SET NOCOUNT ON;");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" set @rawstringlen = DATALENGTH(@rawstring)/2");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" set @offset = 0");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" set @length = @rawstringlen");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" set @encryptlen = @rawstringlen");
        transSql.append(System.getProperty("line.separator"));

        transSql.append(" set @encryptdata = dbo.func_string_encrypt(@rawstring, @rawstringlen, @offset, @length, @encryptlen)");
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
         'http://192.168.7.106:8001/api/datahsm/v1/strategy/get',
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

        log.info("创建Mysql触发器start");
        //获取网口ip
        StringBuffer transSql = new StringBuffer("CREATE TRIGGER tri_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns() + " before insert");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("on " + encryptColumns.getDatabaseServerName() + "." + encryptColumns.getDbTable() );
        transSql.append(System.getProperty("line.separator"));
        transSql.append("for each row");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("set NEW." + encryptColumns.getEncryptColumns() + " = StringEncrypt(" );
        transSql.append(System.getProperty("line.separator"));
        transSql.append("'" + encryptColumns.getId() + "',#--策略唯一标识");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("'http://" + encryptColumns.getIpAndPort() + "/api/datahsm/v1/strategy/get', #--'http://192.168.6.31:8080/api/datahsm/v1/strategy/get',策略下载地址");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("'ip_address',#--IP");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("CAST(Database() AS CHAR),#--实例名");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("CAST(Database() AS CHAR),#--库名");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("'" + encryptColumns.getDbTable() + "',#--表名");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("'" + encryptColumns.getEncryptColumns() + "',#--列名");
        transSql.append(System.getProperty("line.separator"));
        transSql.append("CAST(User() AS CHAR),#--用户名");
        transSql.append(System.getProperty("line.separator"));

        if (DbConstants.SGD_SM4.equals(algorithm)) {
            transSql.append("NEW." + encryptColumns.getEncryptColumns() + "," + "0,0) #---- 加密列 --偏移量 --加密长度\n");
        } else {
            if (DbConstants.ESTABLISH_RULES_YES.equals(encryptColumns.getEstablishRules())) {
                transSql.append("NEW." + encryptColumns.getEncryptColumns() + "," + //加密列
                        (encryptColumns.getEncryptionOffset() -1 ) + "," + //偏移量
                        encryptColumns.getEncryptionLength() +") #---- 加密列 --偏移量 --加密长度\n");
            } else {
                transSql.append("NEW." + encryptColumns.getEncryptColumns() + "," + "0,0) #---- 加密列 --偏移量 --加密长度\n");
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
     * 创建PostgreSql触发器(需要使用用户创建)
     *
     * @param conn
     * @param funName
     */
    public static void transEncryptFunToPostgreSql(Connection conn, String funName, List<DbhsmEncryptColumns> encryptColumns,String ip) throws Exception {
        /**
         * 1、定义触发器函数
         * create or replace function tr_func_string_encrypt()
         * returns trigger as
         * $tr_func_string_encrypt$
         * begin
         * NEW.name := testuser1.pgext_func_string_encrypt(NEW.name);
         * return NEW;
         * end;
         * $tr_func_string_encrypt$ language 'plpgsql';
         *
         */
        Statement statement = null;
        try {
            // 1、定义触发器函数
            log.info("创建PostgreSql触发器函数start");
            //函数名是动态的
            StringBuffer transFun = new StringBuffer("create or replace function " + funName + "()");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("returns trigger as");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("$" + funName + "$");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("begin");
            transFun.append(System.getProperty("line.separator"));

            if (StringUtils.isNotEmpty(encryptColumns)){
                for (DbhsmEncryptColumns encryptColumns1 : encryptColumns){
                    transFun.append("NEW." + encryptColumns1.getEncryptColumns() + " := "+ encryptColumns1.getDbUserName() +".pgext_func_string_encrypt(");
                    transFun.append("'" + encryptColumns1.getId() + "',#--策略唯一标识");
                    transFun.append(System.getProperty("line.separator"));
                    transFun.append("'http://" + ip + "/api/datahsm/v1/strategy/get', #--'http://192.168.6.31:8080/api/datahsm/v1/strategy/get',策略下载地址");
                    transFun.append(System.getProperty("line.separator"));
                    transFun.append("CAST(inet_client_addr() as char),#--IP");
                    transFun.append(System.getProperty("line.separator"));
                    transFun.append("CAST(current_catalog as char),#--实例名");
                    transFun.append(System.getProperty("line.separator"));
                    transFun.append("CAST(current_catalog as char),#--库名");
                    transFun.append(System.getProperty("line.separator"));
                    transFun.append("'" + encryptColumns1.getDbTable() + "',#--表名");
                    transFun.append(System.getProperty("line.separator"));
                    transFun.append("'" + encryptColumns1.getEncryptColumns() + "',#--列名");
                    transFun.append(System.getProperty("line.separator"));
                    transFun.append("CAST(user AS CHAR),#--用户名");
                    transFun.append(System.getProperty("line.separator"));

                    if (DbConstants.SGD_SM4.equals(encryptColumns1.getEncryptionAlgorithm())) {
                        transFun.append("NEW." + encryptColumns1.getEncryptColumns() + "," + "0) #---- 加密列 --偏移量\n");
                    } else {
                        if (DbConstants.ESTABLISH_RULES_YES.equals(encryptColumns1.getEstablishRules())) {
                            transFun.append("NEW." + encryptColumns1.getEncryptColumns() + "," + //加密列
                                    (encryptColumns1.getEncryptionOffset() -1 ) + "," + //偏移量
                                    encryptColumns1.getEncryptionLength() +"," +//加密长度
                                    encryptColumns1.getEncryptionAlgorithm() + ") #---- 加密列 --偏移量 --加密长度  --算法\n");
                        } else {
                            transFun.append("NEW." + encryptColumns1.getEncryptColumns() + "," + "0) #---- 加密列 --偏移量\n");
                        }
                    }
                    transFun.append(System.getProperty("line.separator"));
                }
            }

            transFun.append("return NEW;");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("end");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("$" + funName + "$ language 'plpgsql'");
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
     * @param encryptColumn
     */
    public static void transEncryptColumnsToPostgreSql(Connection conn, DbhsmEncryptColumnsAdd encryptColumn,String schema,String funName) throws Exception {
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
        try {
            // 1、创建触发器，设置所触发的条件和执行的函数
            log.info("创建PostgreSql触发器start");

            StringBuffer transSql = new StringBuffer("create trigger tri_" + schema + "_" + encryptColumn.getDbTable() + "_" + encryptColumn.getEncryptColumns() + " --触发器名称");
            transSql.append(System.getProperty("line.separator"));
            transSql.append("before insert or update of \"" + encryptColumn.getEncryptColumns() + "\" on " + schema + ".\"" + encryptColumn.getDbTable() + "\"");
            transSql.append(System.getProperty("line.separator"));
            transSql.append("for each row");
            transSql.append(System.getProperty("line.separator"));
            transSql.append("execute procedure " + schema + "." + funName + "()");

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

package com.spms.common.dbTool;

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
        transSql.append("    :NEW." + encryptColumns.getEncryptColumns().toUpperCase() + "," + (ObjectUtils.isEmpty(encryptColumns.getEncryptionOffset()) ? 0 : encryptColumns.getEncryptionOffset()-1) + "," + (ObjectUtils.isEmpty(encryptColumns.getEncryptionLength()) ? 0 : encryptColumns.getEncryptionLength()) + ", :NEW." + encryptColumns.getEncryptColumns().toUpperCase() + ",--变换的列，此处为变换 name 列6,8分别为偏移量和加密长度\n");
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
//        transSql.append("SET ANSI_NULLS OFF");
//        transSql.append(System.getProperty("line.separator"));
//        transSql.append("GO");
//        transSql.append(System.getProperty("line.separator"));
//        transSql.append("SET QUOTED_IDENTIFIER ON");
//        transSql.append(System.getProperty("line.separator"));
//        transSql.append("GO");
//        transSql.append(System.getProperty("line.separator"));
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
}
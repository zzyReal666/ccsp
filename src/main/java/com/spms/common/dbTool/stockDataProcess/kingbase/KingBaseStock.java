package com.spms.common.dbTool.stockDataProcess.kingbase;

import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.Statement;

/**
 * <p> description: 执行kingBase加密 </p>
 *
 * <p> Powered by wzh On 2024-06-19 11:52 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */
@Slf4j
public class KingBaseStock {

    public static void kingBaseSqlStockEncOrDec(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd) throws Exception {
        //1.定义kingBase触发器
        transEncryptColumnsFunToKingBase(conn,dbhsmEncryptColumnsAdd);
        //2.创建kingBase触发器
        transEncryptColumnsToKingBase(conn,dbhsmEncryptColumnsAdd);
        //3.删除kingBase触发器
        delTrFunStockKingBase(conn,dbhsmEncryptColumnsAdd);
    }

    public static void delTrFunStockKingBase(Connection conn, DbhsmEncryptColumnsAdd encryptColumns) throws Exception {
        String funName = encryptColumns.getDbUserName() + ".trfunc_za_" + DbConstants.algMapping(encryptColumns.getEncryptionAlgorithm()) + "_" + encryptColumns.getDbUserName() + "_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns();

        try {
            String sql ="drop function "+funName+" CASCADE;";
            Statement statement = conn.createStatement();
            statement.execute(sql);
            log.info("3、kingBase 级联删除触发器函数：\n" + sql);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    private static void transEncryptColumnsFunToKingBase(Connection conn, DbhsmEncryptColumnsAdd encryptColumns) throws Exception {
        Statement statement = null;
        log.info("定义KingBase触发器start");

        String alg = encryptColumns.getEncryptionAlgorithm();
        String funName$ = encryptColumns.getDbUserName() + ".tr_za_" + DbConstants.algMappingStrOrFpe(alg) + "_" + encryptColumns.getDbUserName() + "_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns();
        String funName = encryptColumns.getDbUserName() + ".trfunc_za_" + DbConstants.algMappingStrOrFpe(alg) + "_" + encryptColumns.getDbUserName() + "_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns();
        StringBuffer transSql = new StringBuffer("create or replace function \n");
        transSql.append(funName + "\n");
        transSql.append("    returns trigger as $" + funName$ + "$\n");
        transSql.append("BEGIN\n");
        transSql.append("    NEW." + encryptColumns.getEncryptColumns() + ":= " + encryptColumns.getDbUserName() + ".kbext_func_" + DbConstants.algMappingStrOrFpe(alg) + "_encrypt(\n");
        transSql.append("    '" + encryptColumns.getId() + "',--策略唯一标识\n");
        transSql.append("    'http://" + encryptColumns.getIpAndPort() + "/api/datahsm/v1/strategy/get', --'http://192.168.7.106:8001/api/datahsm/v1/strategy/get',策略下载地址\n");
        transSql.append("    CAST(inet_client_addr() as text),--IP\n");
        transSql.append("    CAST(current_catalog as text),--实例名\n");
        transSql.append("    CAST(current_catalog as text),--库名\n");
        transSql.append("    '" + encryptColumns.getDbTable() + "',--表名\n");
        transSql.append("    '" + encryptColumns.getEncryptColumns() + "',--列名\n");
        transSql.append("    CAST(user as text),--用户名\n");

        if (DbConstants.SGD_SM4.equals(encryptColumns.getEncryptionAlgorithm())) {
            transSql.append("    NEW." + encryptColumns.getEncryptColumns().toUpperCase() + ",0,0:);\n");
        } else {
            if (DbConstants.ESTABLISH_RULES_YES.equals(encryptColumns.getEstablishRules())) {
                transSql.append("NEW." + encryptColumns.getEncryptColumns() + "," + //加密列
                        (encryptColumns.getEncryptionOffset() - 1) + "," + //偏移量
                        (encryptColumns.getEncryptionLength() - (encryptColumns.getEncryptionOffset() - 1)) + "," +//加密长度
                        encryptColumns.getEncryptionAlgorithm() + ");\n");
            } else {
                transSql.append("NEW." + encryptColumns.getEncryptColumns() + ",0,0," + encryptColumns.getEncryptionAlgorithm() + ");\n");
            }
        }

        transSql.append("    return NEW;\n");
        transSql.append("end; $" + funName$ + "$\n");
        transSql.append("language 'plsql';\n");
        try {
            log.info("定义KingBase触发器{}", transSql);
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
    private static void transEncryptColumnsToKingBase(Connection conn, DbhsmEncryptColumnsAdd encryptColumns) throws Exception {
        /**
         * create trigger schema.tr_za_string_schema_tablename_clmname  --触发器名称
         * before insert on "schema"."tablename"
         * for each row
         * execute procedure schema.trfunc_za_string_schema_tablename_clmname(); --触发器函数
         */
        Statement statement = null;
        String funName = encryptColumns.getDbUserName() + ".trfunc_za_" + DbConstants.algMapping(encryptColumns.getEncryptionAlgorithm()) + "_" + encryptColumns.getDbUserName() + "_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns();
        String transName =encryptColumns.getDbUserName() + ".tr_za_" +  DbConstants.algMapping(encryptColumns.getEncryptionAlgorithm()) + "_" + encryptColumns.getDbUserName() + "_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns();
        String userSchema = "\"" + encryptColumns.getDbUserName() + "\"";
        try {
            // 1、创建触发器，设置所触发的条件和执行的函数
            log.info("创建KingBase触发器start");

            StringBuffer transSql = new StringBuffer("create trigger " + transName);
            transSql.append(System.lineSeparator());
            transSql.append("before insert on " + userSchema + ".\"" + encryptColumns.getDbTable() + "\"");
            transSql.append(System.lineSeparator());
            transSql.append("for each row");
            transSql.append(System.lineSeparator());
            transSql.append("execute procedure ");
            transSql.append(System.lineSeparator());
            transSql.append(funName + "();");

            log.info("创建KingBase触发器:{}", transSql);
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

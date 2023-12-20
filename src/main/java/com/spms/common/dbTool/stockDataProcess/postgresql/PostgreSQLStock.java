package com.spms.common.dbTool.stockDataProcess.postgresql;

import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @project ccsp
 * @description PostgreSQL 存量数据加解密
 * @author 18853
 * @date 2023/11/29 10:56:16
 * @version 1.0
 */
@Slf4j
public class PostgreSQLStock {

    /**
     * PostgreSQL 存量数据加解密
     */
    public static void postgreSQLStockEncOrDec(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, DbhsmDbUser user, Boolean encOrdec) throws Exception {
        // 1、定义触发器函数
        trFunStockPostgreSQL(conn, dbhsmEncryptColumnsAdd, user,encOrdec);
        //2 创建PostgreSql 存量数据加密触发器
        trStockPostgreSql(conn, dbhsmEncryptColumnsAdd, user);
        //3、PostgreSql 更新数据存储过程
        proceduresStockPostgreSql(conn, dbhsmEncryptColumnsAdd, user);
        //4、PostgreSql 级联删除触发器函数
        delTrFunStockPostgreSql(conn, dbhsmEncryptColumnsAdd, user);

    }

    /**
     * 1、触发器函数  加密存量数据
     * create or replace function testuser1.tr_string_stock_testuser1_table1_name()
     * returns trigger as $$
     * begin
     *   NEW.name := testuser1.pgext_func_string_encrypt(
     *  '443970420010520576', --确保与配置加密列所设置的pid相同
     *  'http://192.168.6.88:10013/prod-api/dbhsm/api/datahsm/v1/strategy/get',  --确保与配置加密列所设置的url相同
     *   CAST(inet_client_addr() as text),
     *  --ip
     *   CAST(current_catalog as text),
     *  --实例名
     *   CAST(current_catalog as text),
     *  --库名
     *  'TABLE1',
     *   --表名
     *  'name',
     *   --列名，以列名 name 为例
     *  CAST(user as text),
     *   --用户名
     *  OLD.name,0,0);
     *   --OLD.name 加密列， --0 offset --0 加密长度，0为默认加密全部;
     *  return NEW;
     * end; $$
     * language 'plpgsql';
     */
    public static void trFunStockPostgreSQL(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, DbhsmDbUser user,Boolean encOrdec) throws Exception {
        Statement statement = null;
        String  alg = dbhsmEncryptColumnsAdd.getEncryptionAlgorithm();
        String userSchema = "\""+user.getDbSchema()+"\"";
        String encOrdecStr = encOrdec ? "_encrypt(" : "_decrypt(";
        String funName = userSchema + ".tr_" + DbConstants.algMappingStrOrFpe(alg) + "_stock_" + user.getUserName() + "_" + dbhsmEncryptColumnsAdd.getDbTable()+ "_" + dbhsmEncryptColumnsAdd.getEncryptColumns();
        log.info("trFunStockPostgreSQL PID:{}",dbhsmEncryptColumnsAdd.getId());
        try{
            // 1、定义触发器函数
            log.info("1、创建PostgreSql存量数据触发器函数start");
            //函数名是动态的
            StringBuffer transFun = new StringBuffer("create or replace function " + funName + "()");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("returns trigger as $$");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("begin");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("NEW." + dbhsmEncryptColumnsAdd.getEncryptColumns() + " := "+ userSchema +".pgext_func_"+DbConstants.algMappingStrOrFpe(alg)+encOrdecStr);
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
            transFun.append("'"+user.getUserName()+"',");
            transFun.append(System.getProperty("line.separator"));

            if (DbConstants.SGD_SM4.equals(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm())) {
                transFun.append("OLD." + dbhsmEncryptColumnsAdd.getEncryptColumns() + ",0,0);\n");
            } else {
                if (DbConstants.ESTABLISH_RULES_YES.equals(dbhsmEncryptColumnsAdd.getEstablishRules())) {
                    //加密列
                    transFun.append("OLD." + dbhsmEncryptColumnsAdd.getEncryptColumns() + "," +
                            //偏移量
                            (dbhsmEncryptColumnsAdd.getEncryptionOffset() -1 ) + "," +
                            //加密长度
                            (dbhsmEncryptColumnsAdd.getEncryptionLength()-(dbhsmEncryptColumnsAdd.getEncryptionOffset() -1 )) +"," +
                            dbhsmEncryptColumnsAdd.getEncryptionAlgorithm() + ");\n");
                } else {
                    transFun.append("OLD." + dbhsmEncryptColumnsAdd.getEncryptColumns() + ",0,0,"+ dbhsmEncryptColumnsAdd.getEncryptionAlgorithm() +");\n");
                }
            }
            transFun.append(System.getProperty("line.separator"));
            transFun.append("return NEW;");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("end; $$");
            transFun.append(System.getProperty("line.separator"));
            transFun.append("language 'plpgsql'");
            transFun.append(System.getProperty("line.separator"));
            log.info("exec sql:" + transFun);
            statement = conn.createStatement();
            statement.execute(transFun.toString());
            //conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("pgSql存量"+(encOrdec ? "加密" : "解密")+"异常，加密列信息"+dbhsmEncryptColumnsAdd);
            throw new Exception(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * 2 创建PostgreSql 存量数据加密触发器
     * create trigger tr_string_stock_table1_name --触发器名称
     * before update of name on testuser1.table1
     * for each row
     * execute procedure testuser1.tr_string_stock_testuser1_table1_name(); --架构.触发器函数
     */
    public static void trStockPostgreSql(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, DbhsmDbUser user) throws Exception {

        Statement statement = null;
        String transName = "tr_" + DbConstants.algMappingStrOrFpe(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm()) + "_stock_" + dbhsmEncryptColumnsAdd.getDbTable()+ "_" + dbhsmEncryptColumnsAdd.getEncryptColumns() ;
        String funName = "tr_" + DbConstants.algMappingStrOrFpe(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm()) + "_stock_" + user.getUserName() + "_" + dbhsmEncryptColumnsAdd.getDbTable()+ "_" + dbhsmEncryptColumnsAdd.getEncryptColumns();
        String userSchema = "\""+user.getDbSchema()+"\"";
        try {
            // 1、创建触发器，设置所触发的条件和执行的函数
            log.info("2、创建PostgreSql存量数据触发器start");

            StringBuffer transSql = new StringBuffer("create trigger " + transName);
            transSql.append(System.getProperty("line.separator"));
            transSql.append("before update of \"" + dbhsmEncryptColumnsAdd.getEncryptColumns() + "\" on " + userSchema + ".\"" + dbhsmEncryptColumnsAdd.getDbTable() + "\"");
            transSql.append(System.getProperty("line.separator"));
            transSql.append("for each row");
            transSql.append(System.getProperty("line.separator"));
            transSql.append("execute procedure " + userSchema + "." + funName + "()");

            log.info("exec sql:" + transSql);
            statement = conn.createStatement();
            statement.execute(transSql.toString());
            //conn.commit();
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
     * 3、PostgreSql 更新数据存储过程
     *
     * @param conn
     * @param
     */
    public static void proceduresStockPostgreSql(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, DbhsmDbUser user) throws Exception {
        Statement statement = conn.createStatement();
        String encryptColumns = dbhsmEncryptColumnsAdd.getEncryptColumns();
        String dbTable = "\"" +  dbhsmEncryptColumnsAdd.getDbTable() +"\"";
        String dbSchema = "\"" + user.getDbSchema() +"\"";
        log.info("3、ostgreSql 更新数据存储过程start：\n" );
        try {
            String sql = "do $$\n" +
                    "  declare \n" +
                    "    encData text;\n" +
                    "    slc cursor for select "+encryptColumns+" from "+dbSchema+"."+dbTable+";\n" +
                    "  begin\n" +
                    "    open slc;\n" +
                    "  loop\n" +
                    "  fetch next from slc into encData;\n" +
                    "  exit when not found;\n" +
                    "    update "+dbSchema+"."+dbTable+" set "+encryptColumns+" = encData where CURRENT OF slc;\n" +
                    "  end loop;\n" +
                    "  close slc;\n" +
                    "end $$;";
            log.info("PostgreSql 更新数据存储过程：\n" + sql);
            statement.execute(sql);
            log.info("Stored procedure executed successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }finally {
            if (statement != null) {
                statement.close();
            }
        }

    }

    /**
     * 4、PostgreSql 级联删除触发器函数
     *
     * @param conn
     * @param
     */
    public static void delTrFunStockPostgreSql(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, DbhsmDbUser user) throws Exception {
        String dbSchema = "\"" + user.getDbSchema() +"\"";
        String funName = dbSchema + ".tr_" + DbConstants.algMappingStrOrFpe(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm()) + "_stock_" + user.getUserName() + "_" + dbhsmEncryptColumnsAdd.getDbTable()+ "_" + dbhsmEncryptColumnsAdd.getEncryptColumns();

        try {
            String sql ="drop function "+funName+" CASCADE;";
            Statement statement = conn.createStatement();
            statement.execute(sql);
            log.info("4、PostgreSql 级联删除触发器函数：\n" + sql);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }

    }
}

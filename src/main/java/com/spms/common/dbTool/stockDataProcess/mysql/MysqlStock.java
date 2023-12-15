package com.spms.common.dbTool.stockDataProcess.mysql;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.StringUtils;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @project ccsp
 * @description mysql 存量数据加解密
 * @author 18853
 * @version 1.0
 */
@Slf4j
public class MysqlStock {

    // 系统换行符
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * MySQL 存量数据加解密
     */
    public static void mysqlStockEncOrDec(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, Boolean encOrdec) throws Exception {
        // 1、创建存量数据加密触发器
        trFunStockMySQL(conn, dbhsmEncryptColumnsAdd,encOrdec);
        //2 创建Mysql 游标更新列
        cursorProcMysql(conn, dbhsmEncryptColumnsAdd);
        //3、Mysql 删除触发器
        delTrFunStockMysql(conn, dbhsmEncryptColumnsAdd);

    }

    /**
     * 1、 #创建存量数据加密触发器 -SM4-CTR
     * create trigger tri_update_name before update
     * on testdb.tests
     * for each row
     * set NEW.name = StringEncrypt(
     * --StringEncrypt 算法
     * '440788057390714880',
     * 'http://192.168.6.88:10013/api/datahsm/v1/strategy/get',
     * 'ip_address',
     * CAST(Database() AS CHAR),
     * CAST(Database() AS CHAR),
     * 'TESTS',
     * 'NAME',
     * CAST(User() AS CHAR),
     * OLD.name,0,0);
     * --OLD.name 加密列， --0 offset --0 加密长度，0为默认加密全部
     *
     * encOrdec ：加密true，解密false
     */
    public static void trFunStockMySQL(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, Boolean encOrdec) throws Exception {
        Statement statement = null;
        String  alg = dbhsmEncryptColumnsAdd.getEncryptionAlgorithm();
        String algName;
        if (encOrdec) {
            algName=(DbConstants.SGD_SM4.equals(alg) ? "StringEncrypt(":"FpeStringEncrypt(") ;
        }else {
            algName =(DbConstants.SGD_SM4.equals(alg) ? "StringDecrypt(":"FpeStringDecrypt(") ;
        }

        String encryptColumns = dbhsmEncryptColumnsAdd.getEncryptColumns();
        String dbName = dbhsmEncryptColumnsAdd.getDatabaseServerName();
        String dbTable = dbhsmEncryptColumnsAdd.getDbTable();
        Integer offset = dbhsmEncryptColumnsAdd.getEncryptionOffset();
        Long encryptionLength = dbhsmEncryptColumnsAdd.getEncryptionLength();
        Integer establishRules = dbhsmEncryptColumnsAdd.getEstablishRules();

        try{
            // 1、定义触发器
            log.info("1、创建Mysql存量数据触发器start");
            //函数名是动态的
            StringBuffer transFun = new StringBuffer(StringUtils.format("create trigger tri_update_{} before update",encryptColumns));
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("on {}.{}", dbName,dbTable));
            transFun.append(LINE_SEPARATOR);
            transFun.append("for each row");
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("set NEW.{} = {}",encryptColumns,algName));
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("'{}',", dbhsmEncryptColumnsAdd.getId()));
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("'http://{}/prod-api/dbhsm/api/datahsm/v1/strategy/get',",dbhsmEncryptColumnsAdd.getIpAndPort()));
            transFun.append(LINE_SEPARATOR);
            transFun.append("'ip_address',");
            transFun.append(LINE_SEPARATOR);
            transFun.append("CAST(Database() AS CHAR),");
            transFun.append(LINE_SEPARATOR);
            transFun.append("CAST(Database() AS CHAR),");
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("'{}',",dbTable));
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("'{}',",encryptColumns));
            transFun.append(LINE_SEPARATOR);
            transFun.append("CAST(User() AS CHAR),");
            transFun.append(LINE_SEPARATOR);

            if (DbConstants.SGD_SM4.equals(alg)) {
                transFun.append(StringUtils.format("OLD.{},0,0);\n",encryptColumns));

            } else {
                if (DbConstants.ESTABLISH_RULES_YES.equals(establishRules)) {
                    //加密列
                    transFun.append("OLD." + encryptColumns + "," +
                            //偏移量
                            (offset -1 ) + "," +
                            //加密长度
                            (encryptionLength-(offset -1 )) +"," +
                            alg + ");\n");
                } else {
                    transFun.append("OLD." + encryptColumns + ",0,0,"+ alg +");\n");
                }
            }
            log.info("创建Mysql存量数据触发器函数 sql:\n" + transFun);
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
     * 2、游标更新列
     * #关闭安全模式，解锁批量处理
     * SET SQL_SAFE_UPDATES = 0;
     * delimiter $
     * create procedure cursorProc()
     * begin
     *   DECLARE encData NVARCHAR(8000);
     *   DECLARE slc CURSOR FOR select name from testdb.table1;  #提取name加密列
     *   DECLARE done INT DEFAULT false; #结束标志变量
     *   DECLARE CONTINUE HANDLER FOR NOT FOUND set done = true;
     *   open slc;
     *   while not done DO
     *     FETCH slc INTO encData;
     * 	update testdb.table1 set name = encData where name = encData; #更新name加密列
     *   end while;
     *   close slc;
     * end $
     * call cursorProc();
     */
    public static void cursorProcMysql(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd) throws Exception {
        Statement statement = null;
        String encryptColumns = dbhsmEncryptColumnsAdd.getEncryptColumns();
        String dbName = dbhsmEncryptColumnsAdd.getDatabaseServerName();
        String dbTable = dbhsmEncryptColumnsAdd.getDbTable();
        try{
            // 2、定义触发器函数
            log.info("2、游标更新列start");
            statement = conn.createStatement();
            statement.execute("SET SQL_SAFE_UPDATES = 0;");
            log.info("SET SQL_SAFE_UPDATES = 0;");
            StringBuffer transFun = new StringBuffer("create procedure cursorProc()");
            transFun.append(LINE_SEPARATOR);
            transFun.append("begin");
            transFun.append(LINE_SEPARATOR);
            transFun.append("  DECLARE done INT DEFAULT false;");
            transFun.append(LINE_SEPARATOR);
            transFun.append("  DECLARE encData VARCHAR(8000);");
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("  DECLARE slc CURSOR FOR select {} from {}.{};",encryptColumns,dbName,dbTable));
            transFun.append(LINE_SEPARATOR);
            transFun.append("  DECLARE CONTINUE HANDLER FOR NOT FOUND set done = true;");
            transFun.append(LINE_SEPARATOR);
            transFun.append("  open slc;");
            transFun.append(LINE_SEPARATOR);
            transFun.append("  while not done DO");
            transFun.append(LINE_SEPARATOR);
            transFun.append("    FETCH slc INTO encData;");
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("  update {}.{} set {} = encData where {} = encData;",dbName,dbTable,encryptColumns,encryptColumns));
            transFun.append(LINE_SEPARATOR);
            transFun.append("  end while;");
            transFun.append(LINE_SEPARATOR);
            transFun.append(" close slc;");
            transFun.append(LINE_SEPARATOR);
            transFun.append("end");
            transFun.append(LINE_SEPARATOR);
            log.info("游标更新列sql:\n" + transFun);
            statement.execute(transFun.toString());
            log.info("call cursorProc();");
            statement.execute("call cursorProc()");
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
     * 3 删除触发器 删除存储过程
     * @param conn
     * @param dbhsmEncryptColumnsAdd
     * @throws Exception
     */
    public static void delTrFunStockMysql(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd) throws Exception {
        String funName = "tri_update_" + dbhsmEncryptColumnsAdd.getEncryptColumns();

        try {
            String sql ="drop TRIGGER  IF EXISTS "+funName;
            String procedureSql = "DROP PROCEDURE IF EXISTS cursorProc";
            Statement statement = conn.createStatement();
            log.info("3、Mysql 删除触发器：" + sql);
            boolean execute = statement.execute(sql);
            log.info("删除触发器返回：{}" ,execute);

            log.info("删除cursorProc：\n" + procedureSql);
            boolean execute1 = statement.execute(procedureSql);
            log.info("删除cursorProc返回：{}，" ,execute1);

        } catch (Exception e) {
            e.printStackTrace();
            throw new ZAYKException("Mysql 删除触发器或删除cursorProc失败：" + e.getMessage());
        }

    }
}


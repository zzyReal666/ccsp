package com.spms.common.dbTool.stockDataProcess.oracle;

import com.ccsp.common.core.utils.StringUtils;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.Statement;

/**
 * @project ccsp
 * @description oracle 存量数据加解密
 * @author 18853
 * @version 1.0
 */
@Slf4j
public class OraclelStock {

    // 系统换行符
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /**
     * Oracle 存量数据加解密
     */
    public static void oracleStockEncOrDec(Connection conn, DbhsmDbInstance instance, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd, Boolean encOrdec) throws Exception {
        // 1、创建存量数据加密触发器
        trFunStockOracle(conn, dbhsmEncryptColumnsAdd,encOrdec);
        //2 创建Oracle 游标更新列
        cursorProcOracle(conn,instance,dbhsmEncryptColumnsAdd);
        //3、Oracle 删除触发器
        delTrFunStockOracle(conn, dbhsmEncryptColumnsAdd,encOrdec);

    }

    /**
     *
     * 1、触发器
     * create or replace TRIGGER tr_stock_CHANGENAME_string_encrypt
     *   -- 触发器名称
     *     BEFORE UPDATE OF NAME
     *   -- name 是需要加密处理的列名称
     *     ON user60.TABLE1
     *   -- user1 是用户名，TABLE1是表格名称
     *     FOR EACH ROW
     * BEGIN
     *     c_oci_trans_string_encrypt_p(
     *  --c_oci_trans_string_encrypt 是储过程名称
     *     '442202483759124480',--策略唯一标识
     *     'http://192.168.6.88:10013/api/datahsm/v1/strategy/get',
     *  --策略下载地址
     *     SYS_CONTEXT('USERENV', 'IP_ADDRESS'),
     *  --客户端IP
     *     SYS_CONTEXT('USERENV', 'INSTANCE_NAME'),
     *  --库实例名
     *     SYS_CONTEXT('USERENV', 'DB_NAME'),
     *  --库名
     *     ' TABLE1',
     *  --表名
     *     'NAME',
     *  --列名，以列名 name 为例
     *     USER(),
     *  --用户名
     *     :OLD.NAME, 0, 0,:NEW.NAME);
     *  --变换的列，此处为变换 name 列
     * END;
     * encOrdec ：加密true，解密false
     */
    public static void trFunStockOracle(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd,Boolean encOrdec) throws Exception {
        Statement statement = null;
        String  alg = dbhsmEncryptColumnsAdd.getEncryptionAlgorithm();
        String dbUserName = dbhsmEncryptColumnsAdd.getDbUserName();
        String triggerName,procedureName;
        if (encOrdec) {
            triggerName= StringUtils.format("{}.tr_stock_CHANGENAME_{}_encrypt",dbUserName,DbConstants.algMappingStrOrFpe(alg));
            procedureName= StringUtils.format(" c_oci_trans_{}_encrypt_p(",DbConstants.algMappingStrOrFpe(alg));
        }else {
            triggerName= StringUtils.format("{}.tr_stock_CHANGENAME_{}_decrypt",dbUserName,DbConstants.algMappingStrOrFpe(alg));
            procedureName= StringUtils.format(" c_oci_trans_{}_decrypt_p(",DbConstants.algMappingStrOrFpe(alg));
        }

        String encryptColumns = dbhsmEncryptColumnsAdd.getEncryptColumns().toUpperCase();
        String dbName = dbhsmEncryptColumnsAdd.getDatabaseServerName();
        String dbTable = dbhsmEncryptColumnsAdd.getDbTable();
        Integer offset = dbhsmEncryptColumnsAdd.getEncryptionOffset();
        Long encryptionLength = dbhsmEncryptColumnsAdd.getEncryptionLength();
        Integer establishRules = dbhsmEncryptColumnsAdd.getEstablishRules();
        String userName = dbhsmEncryptColumnsAdd.getDbUserName();
        log.info("trFunStockPostgreSQL PID:{}",dbhsmEncryptColumnsAdd.getId());
        try{
            // 1、定义触发器
            log.info("1、创建Oracle存量数据触发器start");
            //函数名是动态的
            StringBuffer transFun = new StringBuffer(StringUtils.format("create or replace TRIGGER {}",triggerName));
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format(" BEFORE UPDATE OF {}", encryptColumns));
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("ON {}.{}",userName,dbTable));
            transFun.append(LINE_SEPARATOR);
            transFun.append("FOR EACH ROW");
            transFun.append(LINE_SEPARATOR);
            transFun.append("BEGIN");
            transFun.append(LINE_SEPARATOR);
            transFun.append("if (:OLD."+encryptColumns+" is not NULL) then");
            transFun.append(LINE_SEPARATOR);
            transFun.append(procedureName);
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("'{}',", dbhsmEncryptColumnsAdd.getId()));
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("'http://{}/prod-api/dbhsm/api/datahsm/v1/strategy/get',",dbhsmEncryptColumnsAdd.getIpAndPort()));
            transFun.append(LINE_SEPARATOR);
            transFun.append("SYS_CONTEXT('USERENV', 'IP_ADDRESS'),");
            transFun.append(LINE_SEPARATOR);
            transFun.append("SYS_CONTEXT('USERENV', 'INSTANCE_NAME'),");
            transFun.append(LINE_SEPARATOR);
            transFun.append("SYS_CONTEXT('USERENV', 'DB_NAME'),");
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("'{}',",dbTable));
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("'{}',",encryptColumns));
            transFun.append(LINE_SEPARATOR);
            //transFun.append("USER(),");
            transFun.append("'"+userName+"',");
            transFun.append(LINE_SEPARATOR);

            if (DbConstants.SGD_SM4.equals(alg)) {
                transFun.append(StringUtils.format(":OLD.{},0,0,:NEW.{});\n",encryptColumns,encryptColumns));

            } else {
                if (DbConstants.ESTABLISH_RULES_YES.equals(establishRules)) {
                    //加密列
                    transFun.append(":OLD." + encryptColumns + "," +
                            //偏移量
                            (offset -1 ) + "," +
                            //加密长度
                            (encryptionLength-(offset -1 )) + ",:NEW."+encryptColumns+","+alg+");\n");
                } else {
                    transFun.append(StringUtils.format(":OLD.{},0,0,:NEW.{},{});\n",encryptColumns,encryptColumns,alg));
                }
            }
            transFun.append("end if;\n");
            transFun.append("END;\n");
            transFun.append(LINE_SEPARATOR);
            log.info("创建Oracle存量数据触发器函数 sql:\n" + transFun);
            statement = conn.createStatement();
            statement.execute(transFun.toString());
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
     * 2、更新数据
     * declare
     *     cursor slc is select * from table1 for update;
     *     v_slc slc%rowtype;
     *   begin
     *     open slc;
     * 	loop
     * 	fetch slc into v_slc;
     * 	exit when slc%notfound or slc%notfound is null;
     * 	  update table1 set home = v_slc.home where CURRENT OF slc; --name 加密列
     * 	end loop;
     *   close slc;
     * end;2、更新数据
     * declare
     *     cursor slc is select * from table1 for update;
     *     v_slc slc%rowtype;
     *   begin
     *     open slc;
     * 	loop
     * 	fetch slc into v_slc;
     * 	exit when slc%notfound or slc%notfound is null;
     * 	  update table1 set home = v_slc.home where CURRENT OF slc; --name 加密列
     * 	end loop;
     *   close slc;
     * end;
     */
    public static void cursorProcOracle(Connection conn,DbhsmDbInstance instance, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd) throws Exception {

        Statement statement = null;
        String encryptColumns = dbhsmEncryptColumnsAdd.getEncryptColumns();
        String dbTable = dbhsmEncryptColumnsAdd.getDbTable();
        String dbUserName = dbhsmEncryptColumnsAdd.getDbUserName();
        try{
            // 2、更新数据
            log.info("2、Oracle更新数据start");
            //函数名是动态的
            StringBuffer transFun = new StringBuffer("declare");
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("  cursor slc is select * from {}.{} for update;", dbUserName,dbTable));
            transFun.append(LINE_SEPARATOR);
            transFun.append("  v_slc slc%rowtype;");
            transFun.append(LINE_SEPARATOR);
            transFun.append("begin");
            transFun.append(LINE_SEPARATOR);
            transFun.append("  open slc;");
            transFun.append(LINE_SEPARATOR);
            transFun.append("  loop");
            transFun.append(LINE_SEPARATOR);
            transFun.append("  fetch slc into v_slc;");
            transFun.append(LINE_SEPARATOR);
            transFun.append("  exit when slc%notfound or slc%notfound is null;");
            transFun.append(LINE_SEPARATOR);
            transFun.append(StringUtils.format("update {}.{} set {} = v_slc.{} where CURRENT OF slc;",dbUserName,dbTable,encryptColumns,encryptColumns));
            transFun.append(LINE_SEPARATOR);
            transFun.append("  end loop;");
            transFun.append(LINE_SEPARATOR);
            transFun.append(" close slc;");
            transFun.append(LINE_SEPARATOR);
            transFun.append("end;");
            transFun.append(LINE_SEPARATOR);
            log.info("Oracle更新数据sql:\n" + transFun);
            statement = conn.createStatement();
            statement.execute(transFun.toString());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("oracle存量加解密异常，加密列信息"+dbhsmEncryptColumnsAdd);
            throw new Exception(e.getMessage());
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * 3 删除触发器
     * @param conn
     * @param dbhsmEncryptColumnsAdd
     * @throws Exception
     */
    public static void delTrFunStockOracle(Connection conn, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd,Boolean encOrDec) throws Exception {
        String triName;
        if(encOrDec) {
            triName = StringUtils.format("{}.tr_stock_CHANGENAME_{}_encrypt",dbhsmEncryptColumnsAdd.getDbUserName(), DbConstants.algMappingStrOrFpe(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm()));
        }else {
            triName = StringUtils.format("{}.tr_stock_CHANGENAME_{}_decrypt",dbhsmEncryptColumnsAdd.getDbUserName(), DbConstants.algMappingStrOrFpe(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm()));
        }
        try {
            String sql ="drop TRIGGER "+triName;
            Statement statement = conn.createStatement();
            statement.execute(sql);
            log.info("3、Oracle 删除触发器：" + sql);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }

    }
}


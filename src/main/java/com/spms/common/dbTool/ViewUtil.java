package com.spms.common.dbTool;

import cn.hutool.json.JSONObject;
import com.ccsp.common.core.utils.bean.BeanUtils;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptColumnsMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 数据库视图操作类
 *
 * @author diq
 * @ClassName ViewUtil
 * @date 2022-12-14
 */
@Slf4j
public class ViewUtil {

    /***
     * 创建或修改视图
     * @param conn
     * @param zaDatabaseEncryptColumns
     * @param zaDatabaseEncryptColumnsMapper
     * @return
     */
    public static boolean operView(Connection conn, DbhsmEncryptColumnsAdd zaDatabaseEncryptColumns, DbhsmEncryptColumnsMapper zaDatabaseEncryptColumnsMapper) {

        try {
            if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(zaDatabaseEncryptColumns.getDatabaseType())) {
               return operViewToOracle(conn,zaDatabaseEncryptColumns,zaDatabaseEncryptColumnsMapper);

            }else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(zaDatabaseEncryptColumns.getDatabaseType())){
                return operViewToSqlServer(conn,zaDatabaseEncryptColumns);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    /***
     * 一个表只创建一个视图，
     *
     */
    private static boolean operViewToOracle(Connection conn, DbhsmEncryptColumnsAdd zaDatabaseEncryptColumns, DbhsmEncryptColumnsMapper dbhsmEncryptColumnsMapper) throws IOException, SQLException {

        DbhsmEncryptColumns columns = new DbhsmEncryptColumns();
        columns.setDbTable(zaDatabaseEncryptColumns.getDbTable());
        columns.setDbUserName(zaDatabaseEncryptColumns.getDbUserName());
        //查询已加密的加密列
        List<DbhsmEncryptColumns> encryptColumns = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(columns);
        //把当前加密列加入加密列list
        DbhsmEncryptColumns column = new DbhsmEncryptColumns();
        BeanUtils.copyProperties(zaDatabaseEncryptColumns, column);
        encryptColumns.add(column);
        JSONObject encJson = new JSONObject();
        if (encryptColumns != null && encryptColumns.size() > 0) {
            for (DbhsmEncryptColumns columns1 : encryptColumns) {
                encJson.set(columns1.getEncryptColumns(), columns1.getEncryptColumns());
                encJson.set(columns1.getEncryptColumns() + "pid",columns1.getId());
                String ipAddress = zaDatabaseEncryptColumns.getIpAndPort();
                encJson.set(columns1.getEncryptColumns() + "ip",ipAddress);
            }
        }
        encJson.set(zaDatabaseEncryptColumns.getEncryptColumns(), zaDatabaseEncryptColumns.getEncryptColumns());
        encJson.set(zaDatabaseEncryptColumns.getEncryptColumns() + "pid",zaDatabaseEncryptColumns.getId());
        String ipAddress = zaDatabaseEncryptColumns.getIpAndPort();
        encJson.set(zaDatabaseEncryptColumns.getEncryptColumns() + "ip",ipAddress);
        encJson.set("table",zaDatabaseEncryptColumns.getDbTable());
        encJson.set("userName", zaDatabaseEncryptColumns.getDbUserName());
        List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(conn, zaDatabaseEncryptColumns.getDbUserName() + "." + zaDatabaseEncryptColumns.getDbTable(),zaDatabaseEncryptColumns.getDatabaseType());
        String viewSql = ViewUtil.createOrReplaceViewSQL(allColumnsInfo,encJson,zaDatabaseEncryptColumns);
        Statement statement = null;
        try {
            statement = conn.createStatement();
            statement.execute(viewSql);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    public static String createOrReplaceViewSQL(List<Map<String, String>> columns, JSONObject encJson, DbhsmEncryptColumnsAdd encryptColumns) {
        StringBuffer view = new StringBuffer();
        Long length = 0L;
        int offset = 0,alg = 0;
        String algorithm=encryptColumns.getEncryptionAlgorithm();

        if(DbConstants.ESTABLISH_RULES_YES.equals(encryptColumns.getEstablishRules())) {
            offset = encryptColumns.getEncryptionOffset();
            length = encryptColumns.getEncryptionLength();
        }
        view.append("create or replace view " + encJson.getStr("userName") + ".v_" + encJson.getStr("table") + "\n");
        view.append("as\n");
        view.append("  select\n");
        int counter=1;
        //columns总列
        for (Map columnMap : columns) {
            String columnName = columnMap.get(DbConstants.DB_COLUMN_NAME).toString();
            //encjson:加密列
            if (encJson.containsKey(columnName)) {
                //对表中加密列进行视图语句拼装
                if(String.valueOf(DbConstants.SGD_SM4).equals(algorithm)) {
                    view.append("    c_oci_trans_string_decrypt_f(\n");
                }else {
                    view.append("    c_oci_trans_fpe_decrypt_f(\n");
                }
                view.append("    '" + encJson.getStr(columnName + "pid") + "',\n");
                view.append("    'http://" + encJson.getStr(columnName + "ip") + "/api/datahsm/v1/strategy/get',\n");
                view.append("    SYS_CONTEXT('USERENV', 'IP_ADDRESS'),\n");
                view.append("    SYS_CONTEXT('USERENV', 'INSTANCE_NAME'),\n");
                view.append("    SYS_CONTEXT('USERENV', 'DB_NAME'),\n");
                view.append("    '" + encJson.getStr("table") + "',\n");
                view.append("    '" + columnName + "',\n");
                view.append("    USER,\n");
                if(String.valueOf(DbConstants.SGD_SM4).equals(algorithm)) {
                    view.append(" " + columnName + "," + "0,0" + ") as " + columnName + (counter==columns.size()?"":","));
                }else {
                    if(DbConstants.ESTABLISH_RULES_YES.equals(encryptColumns.getEstablishRules())) {
                        view.append(" " + columnName + "," + (offset - 1) + "," + length + "," + alg + ") as " + columnName+ (counter==columns.size()?"":","));
                    }else{
                        view.append(" " + columnName + ",0,0," + alg  + ") as " + columnName+ (counter==columns.size()?"":","));
                    }
                }
            } else {
                //表中非加密列直接拼接列名
                view.append("    " + columnName + (counter==columns.size()?"":","));
            }
            counter++;
        }
        //view.deleteCharAt(view.length() - 1);
        view.append("  from " + encJson.getStr("table"));
        log.info("createOrReplaceViewSQL : " + view.toString());
        return view.toString();
    }


    /**
     *
     * @param conn
     * @param zaDatabaseEncryptColumns
     * @return
     */
    public static boolean operViewToSqlServer(Connection conn, DbhsmEncryptColumnsAdd zaDatabaseEncryptColumns) throws SQLException {

       /**
        *
        * USE [db_test1]
        * GO
        *
        *
        *SET ANSI_NULLS ON
                * GO
                *
        *SET QUOTED_IDENTIFIER ON
                * GO
                *
        *CREATE OR ALTER view[ dbo].[table1_view](NAME, ENCRYPTSTRING)
        *as SELECT NAME, ENCRYPTSTRING
                * from db_test1.dbo.table1
                * GO
                */

        List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(conn,  zaDatabaseEncryptColumns.getDbTable(),zaDatabaseEncryptColumns.getDatabaseType());

        if (allColumnsInfo == null || allColumnsInfo.size() == 0){
            return false;
        }

        StringBuffer viewSql = new StringBuffer();

        viewSql.append("CREATE OR ALTER view v_" + zaDatabaseEncryptColumns.getDbTable() + "(");
        viewSql.append(System.getProperty("line.separator"));

        String columns = "";
        for (Map<String, String> map :allColumnsInfo){
            columns += map.get(DbConstants.DB_COLUMN_NAME) + ",";
        }

        columns = columns.substring(0, columns.length()-1);
        viewSql.append(columns + ")");
        viewSql.append(System.getProperty("line.separator"));

        viewSql.append("as SELECT " + columns);
        viewSql.append(System.getProperty("line.separator"));
        viewSql.append("from " + zaDatabaseEncryptColumns.getDatabaseServerName() + ".dbo." + zaDatabaseEncryptColumns.getDbTable());
        PreparedStatement preparedStatement = null;
        log.info("sql server create view:" + viewSql.toString());
        try {
            preparedStatement = conn.prepareStatement(viewSql.toString());
            preparedStatement.execute();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    public static boolean deleteView(Connection conn, DbhsmEncryptColumnsAdd encryptColumns) throws SQLException {
        Statement statement = null;
        String delViewSql = null;
        //创建删除语句
        if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(encryptColumns.getDatabaseType())) {
            delViewSql="DROP VIEW IF EXISTS"+ encryptColumns.getDbUserName()+ ".v_" +encryptColumns.getDbTable();
        }else if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(encryptColumns.getDatabaseType())) {
             delViewSql = "DROP VIEW " + encryptColumns.getDbUserName() + ".v_" + encryptColumns.getDbTable();
        }
        log.info("deleteOracleView:"+delViewSql);
        //执行删除语句
        try {
            //删除之前的视图
            statement = conn.createStatement();
            statement.execute(delViewSql);
            return true;
        } catch (Exception e) {
            if (!e.getMessage().contains("不存在")){
                e.printStackTrace();
            }
            return false;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }
}

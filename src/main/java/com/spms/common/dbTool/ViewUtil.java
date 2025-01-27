package com.spms.common.dbTool;

import cn.hutool.json.JSONObject;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.utils.bean.BeanUtils;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptColumnsMapper;
import io.seata.common.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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
        return operView(conn, zaDatabaseEncryptColumns, zaDatabaseEncryptColumnsMapper, "");

    }

    public static boolean operView(Connection conn, DbhsmEncryptColumnsAdd zaDatabaseEncryptColumns, DbhsmEncryptColumnsMapper zaDatabaseEncryptColumnsMapper, String dbSchema) {

        try {
            if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(zaDatabaseEncryptColumns.getDatabaseType())) {
                return operViewToOracle(conn, zaDatabaseEncryptColumns, zaDatabaseEncryptColumnsMapper);

            } else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(zaDatabaseEncryptColumns.getDatabaseType())) {
                return operViewToSqlServer(conn, zaDatabaseEncryptColumns, zaDatabaseEncryptColumnsMapper);
            } else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(zaDatabaseEncryptColumns.getDatabaseType())) {
                return operViewToMySql(conn, zaDatabaseEncryptColumns, zaDatabaseEncryptColumnsMapper);
            } else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(zaDatabaseEncryptColumns.getDatabaseType())) {
                return operViewToPostGreSql(conn, zaDatabaseEncryptColumns, zaDatabaseEncryptColumnsMapper, dbSchema);
            } else if (DbConstants.DB_TYPE_KB.equalsIgnoreCase(zaDatabaseEncryptColumns.getDatabaseType())) {
                return operViewToKingBaseSql(conn, zaDatabaseEncryptColumns, zaDatabaseEncryptColumnsMapper, dbSchema);
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
                encJson.set(columns1.getEncryptColumns() + "pid", columns1.getId());
                String ipAddress = zaDatabaseEncryptColumns.getIpAndPort();
                encJson.set(columns1.getEncryptColumns() + "ip", ipAddress);
                encJson.set(columns1.getEncryptColumns() + "alg", columns1.getEncryptionAlgorithm());
                encJson.set(columns1.getEncryptColumns() + "establishRules", columns1.getEstablishRules());
                encJson.set(columns1.getEncryptColumns() + "offset", columns1.getEncryptionOffset());
                encJson.set(columns1.getEncryptColumns() + "length", columns1.getEncryptionLength());
            }
        }
        encJson.set(zaDatabaseEncryptColumns.getEncryptColumns(), zaDatabaseEncryptColumns.getEncryptColumns());
        encJson.set(zaDatabaseEncryptColumns.getEncryptColumns() + "pid", zaDatabaseEncryptColumns.getId());
        String ipAddress = zaDatabaseEncryptColumns.getIpAndPort();
        encJson.set(zaDatabaseEncryptColumns.getEncryptColumns() + "ip", ipAddress);
        encJson.set("table", zaDatabaseEncryptColumns.getDbTable());
        encJson.set("userName", zaDatabaseEncryptColumns.getDbUserName());
        List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(conn, zaDatabaseEncryptColumns.getDbUserName() + "." + zaDatabaseEncryptColumns.getDbTable(), zaDatabaseEncryptColumns.getDatabaseType());
        String viewSql = ViewUtil.createOrReplaceViewSQL(allColumnsInfo, encJson, zaDatabaseEncryptColumns);
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
        int offset = 0, establishRules;
        String algorithm;


        view.append("create or replace view " + encJson.getStr("userName") + ".v_" + encJson.getStr("table") + "\n");
        view.append("as\n");
        view.append("  select\n");
        int counter = 1;
        //columns总列
        for (Map columnMap : columns) {
            String columnName = columnMap.get(DbConstants.DB_COLUMN_NAME).toString();
            //encjson:加密列
            if (encJson.containsKey(columnName)) {
                //对表中加密列进行视图语句拼装
                algorithm = encJson.getStr(columnName + "alg");
                establishRules = encJson.getInt(columnName + "establishRules");
                if (DbConstants.ESTABLISH_RULES_YES.equals(establishRules)) {
                    offset = encJson.getInt(columnName + "offset");
                    length = encJson.getLong(columnName + "length");
                }
                if (DbConstants.SGD_SM4.equals(algorithm)) {
                    view.append("    c_oci_trans_string_decrypt_f(\n");
                } else {
                    view.append("    c_oci_trans_fpe_decrypt_f(\n");
                }
                view.append("    '" + encJson.getStr(columnName + "pid") + "',\n");
                view.append("    'http://" + encJson.getStr(columnName + "ip") + "/prod-api/dbhsm/api/datahsm/v1/strategy/get',\n");
                view.append("    SYS_CONTEXT('USERENV', 'IP_ADDRESS'),\n");
                view.append("    SYS_CONTEXT('USERENV', 'INSTANCE_NAME'),\n");
                view.append("    SYS_CONTEXT('USERENV', 'DB_NAME'),\n");
                view.append("    '" + encJson.getStr("table") + "',\n");
                view.append("    '" + columnName + "',\n");
                view.append("    USER,\n");
                if (DbConstants.SGD_SM4.equals(algorithm)) {
                    view.append(" " + columnName + "," + "0,0" + ") as " + columnName + (counter == columns.size() ? "" : ","));
                } else {
                    if (DbConstants.ESTABLISH_RULES_YES.equals(establishRules)) {
                        view.append(" " + columnName + "," + (offset - 1) + "," + (length - (offset - 1)) + "," + algorithm + ") as " + columnName + (counter == columns.size() ? "" : ","));
                    } else {
                        view.append(" " + columnName + ",0,0," + algorithm + ") as " + columnName + (counter == columns.size() ? "" : ","));
                    }
                }
            } else {
                //表中非加密列直接拼接列名
                view.append("    " + columnName + (counter == columns.size() ? "" : ","));
            }
            counter++;
        }
        view.append("  from " + encJson.getStr("table"));
        log.info("createOrReplaceViewSQL : " + view.toString());
        return view.toString();
    }


    /**
     * @param conn
     * @param
     * @return
     */
    public static boolean operViewToSqlServer(Connection conn, DbhsmEncryptColumnsAdd encryptColumns, DbhsmEncryptColumnsMapper encryptColumnsMapper) throws SQLException {

        log.info("创建sqlserver视图start: database:{},table:{}", encryptColumns.getDatabaseServerName(), encryptColumns.getDbTable());
        String sqlserverSchema = getSqlserverSchema(conn, encryptColumns);
        //sqlserver 需要schema查询
        String tableName = sqlserverSchema + ":" + encryptColumns.getDbTable() + "_ENCDATA";
        List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(conn, tableName, encryptColumns.getDatabaseType());

        if (CollectionUtils.isEmpty(allColumnsInfo)) {
            return false;
        }

        //查询出加密字段
        DbhsmEncryptColumns encryptColumnDto = new DbhsmEncryptColumns();
        encryptColumnDto.setDbInstanceId(encryptColumns.getDbInstanceId());
        encryptColumnDto.setDbTable(encryptColumns.getDbTable());
        List<DbhsmEncryptColumns> dbhsmEncryptColumns = encryptColumnsMapper.selectDbhsmEncryptColumnsList(encryptColumnDto);

        if (StringUtils.isEmpty(dbhsmEncryptColumns)) {
            dbhsmEncryptColumns = new ArrayList<>();
        }
        DbhsmEncryptColumns encryptColumn = new DbhsmEncryptColumns();
        BeanUtils.copyProperties(encryptColumns, encryptColumn);
        dbhsmEncryptColumns.add(encryptColumn);

        StringBuffer viewSql = new StringBuffer();

        viewSql.append("CREATE view dbo." + encryptColumns.getDbTable());
        viewSql.append(System.getProperty("line.separator"));
        viewSql.append("as SELECT ");
        viewSql.append(System.getProperty("line.separator"));

        //拼接字段
        String encColumns = "";
        boolean haveEncColumn = false;
        for (int i = 0; i < allColumnsInfo.size(); i++) {
            Map<String, String> map = allColumnsInfo.get(i);
            //防止列名为mysql关键字添加``
            String columnName = "[" + map.get(DbConstants.DB_COLUMN_NAME) + "]";
            String colName = map.get(DbConstants.DB_COLUMN_NAME);
            StringBuffer item = new StringBuffer();
            boolean isEncColumn = false;
            for (DbhsmEncryptColumns encryptColumn1 : dbhsmEncryptColumns) {
                String funNameDec = "dbo.func_string_decrypt_ex(";
                if (colName.equalsIgnoreCase(encryptColumn1.getEncryptColumns())) {
                    if (haveEncColumn) {
                        item.append("(select ");
                    }
                    item.append(funNameDec);
                    item.append(System.getProperty("line.separator"));
                    item.append("'" + encryptColumn1.getId() + "',");
                    item.append(System.getProperty("line.separator"));
                    item.append("'http://" + encryptColumns.getIpAndPort() + "/prod-api/dbhsm/api/datahsm/v1/strategy/get',");
                    item.append(System.getProperty("line.separator"));
                    item.append("'ip_addr',");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(@@ServerName as char),");
                    item.append(System.getProperty("line.separator"));
                    item.append("db_name(),");
                    item.append(System.getProperty("line.separator"));
                    item.append("'" + encryptColumn1.getDbTable() + "',");
                    item.append(System.getProperty("line.separator"));
                    item.append("'" + encryptColumn1.getEncryptColumns() + "',");
                    item.append(System.getProperty("line.separator"));
                    item.append("suser_name(),");
                    item.append(System.getProperty("line.separator"));
                    item.append(encryptColumn1.getEncryptColumns() + ",").append("DATALENGTH(" + encryptColumn1.getEncryptColumns() + "),");
                    item.append("0,").append("DATALENGTH(" + encryptColumn1.getEncryptColumns() + "),");
                    item.append(System.getProperty("line.separator"));
                    item.append("DATALENGTH(" + encryptColumn1.getEncryptColumns() + ") as ").append(encryptColumn1.getEncryptColumns());
                    item.append(System.getProperty("line.separator"));
                    isEncColumn = true;
                    haveEncColumn = true;
                    break;
                }
            }
            if (!isEncColumn) {
                item.append(columnName + " ,");
            }
            encColumns += item.toString();
        }

        if (encColumns.length() > 1) {
            encColumns = encColumns.substring(0, encColumns.length() - 1);
        }
        viewSql.append(encColumns);
        viewSql.append(System.getProperty("line.separator"));

        viewSql.append(" from dbo." + encryptColumns.getDbTable()+"_ENCDATA");
        PreparedStatement preparedStatement = null;
        log.info("sqlserver create view:" + viewSql);
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
        return deleteView(conn, encryptColumns, null);
    }

    public static boolean deleteView(Connection conn, DbhsmEncryptColumnsAdd encryptColumns, String dbSchema) throws SQLException {
        Statement statement = null;
        String delViewSql = null;
        //创建删除语句
        if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(encryptColumns.getDatabaseType())) {
            delViewSql = "DROP VIEW IF EXISTS " + getSqlserverSchema(conn, encryptColumns) + ".v_" + encryptColumns.getDbTable();
        } else if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(encryptColumns.getDatabaseType())) {
            delViewSql = "DROP VIEW " + encryptColumns.getDbUserName() + ".v_" + encryptColumns.getDbTable();
        } else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(encryptColumns.getDatabaseType())) {
            delViewSql = "DROP VIEW IF EXISTS " + encryptColumns.getDatabaseServerName() + ".v_" + encryptColumns.getDbTable();
        } else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(encryptColumns.getDatabaseType())) {
            delViewSql = "DROP VIEW  IF EXISTS  " + dbSchema + ".v_" + encryptColumns.getDbTable();
        }
        log.info("deleteView:" + delViewSql);
        //执行删除语句
        try {
            //删除之前的视图
            statement = conn.createStatement();
            statement.execute(delViewSql);
            return true;
        } catch (Exception e) {
            if (!e.getMessage().contains("不存在")) {
                e.printStackTrace();
            }
            return false;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    static String getSqlserverSchema(Connection conn, DbhsmEncryptColumnsAdd encryptColumns) {
        Map<String, String> schemaMap = DBUtil.findSchemaByTable(conn, encryptColumns.getDbTable());
        String schemaName = "dbo";
        if (schemaMap.containsKey("schemaName")) {
            schemaName = schemaMap.get("schemaName");
        }
        return schemaName;
    }
    /**
     * mysql创建视图
     *
     * @param conn
     * @param encryptColumns
     * @return
     */
    private static boolean operViewToMySql(Connection conn, DbhsmEncryptColumnsAdd encryptColumns, DbhsmEncryptColumnsMapper encryptColumnsMapper) throws Exception {
        /*
        create or replace view v_table1
        as select
        	StringDecrypt(name)
        as name from tests;
        */
        log.info("创建Mysql视图start: database:{},table:{}", encryptColumns.getDatabaseServerName(), encryptColumns.getDbTable());

        List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(conn, encryptColumns.getDbTable(), encryptColumns.getDatabaseType());

        if (allColumnsInfo == null || allColumnsInfo.size() == 0) {
            return false;
        }

        //查询出加密字段
        DbhsmEncryptColumns encryptColumnDto = new DbhsmEncryptColumns();
        encryptColumnDto.setDbInstanceId(encryptColumns.getDbInstanceId());
        encryptColumnDto.setDbTable(encryptColumns.getDbTable());
        List<DbhsmEncryptColumns> dbhsmEncryptColumns = encryptColumnsMapper.selectDbhsmEncryptColumnsList(encryptColumnDto);

        if (StringUtils.isEmpty(dbhsmEncryptColumns)) {
            dbhsmEncryptColumns = new ArrayList<>();
        }
        DbhsmEncryptColumns encryptColumn = new DbhsmEncryptColumns();
        BeanUtils.copyProperties(encryptColumns, encryptColumn);
        dbhsmEncryptColumns.add(encryptColumn);

        StringBuffer viewSql = new StringBuffer();

        viewSql.append("CREATE OR replace view " + encryptColumns.getDatabaseServerName() + ".v_" + encryptColumns.getDbTable());
        viewSql.append(System.getProperty("line.separator"));
        viewSql.append("as SELECT ");
        viewSql.append(System.getProperty("line.separator"));

        //拼接字段
        String encColumns = "";
        boolean haveEncColumn = false;
        for (int i = 0; i < allColumnsInfo.size(); i++) {
            Map<String, String> map = allColumnsInfo.get(i);
            //防止列名为mysql关键字添加``
            String columnName = "`" + map.get(DbConstants.DB_COLUMN_NAME) + "`";
            String colName = map.get(DbConstants.DB_COLUMN_NAME);
            StringBuffer item = new StringBuffer();
            boolean isEncColumn = false;
            for (DbhsmEncryptColumns encryptColumn1 : dbhsmEncryptColumns) {
                if (colName.equalsIgnoreCase(encryptColumn1.getEncryptColumns())) {
                    String algorithm = encryptColumn1.getEncryptionAlgorithm();
                    String funName = DbConstants.SGD_SM4.equals(algorithm) ? "StringDecrypt(" : "FpeStringDecrypt(";
                    if (haveEncColumn) {
                        item.append("(select ");
                    }
                    item.append(funName);
                    item.append(System.getProperty("line.separator"));
                    item.append("'" + encryptColumn1.getId() + "',");
                    item.append(System.getProperty("line.separator"));
                    item.append("'http://" + encryptColumns.getIpAndPort() + "/prod-api/dbhsm/api/datahsm/v1/strategy/get', #--'http://192.168.6.31:8080/prod-api/dbhsm/api/datahsm/v1/strategy/get',策略下载地址");
                    item.append(System.getProperty("line.separator"));
                    item.append("'ip_address',#--IP");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(Database() AS CHAR),#--实例名");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(Database() AS CHAR),#--库名");
                    item.append(System.getProperty("line.separator"));
                    item.append("'" + encryptColumn1.getDbTable() + "',#--表名");
                    item.append(System.getProperty("line.separator"));
                    item.append("'" + encryptColumn1.getEncryptColumns() + "',#--列名");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(User() AS CHAR),#--用户名");
                    item.append(System.getProperty("line.separator"));

                    if (DbConstants.SGD_SM4.equals(encryptColumn1.getEncryptionAlgorithm())) {
                        item.append(columnName + "," + "0,0)" + (haveEncColumn ? ")" : "") + "\n");
                    } else {
                        if (DbConstants.ESTABLISH_RULES_YES.equals(encryptColumn1.getEstablishRules())) {
                            //加密列
                            item.append(columnName + "," +
                                    //偏移量
                                    (encryptColumn1.getEncryptionOffset() - 1) + "," +
                                    (encryptColumn1.getEncryptionLength() - (encryptColumn1.getEncryptionOffset() - 1)) + "," + algorithm + ")" + (haveEncColumn ? ")" : "") + "\n");
                        } else {
                            item.append(columnName + "," + "0,0," + algorithm + ")" + (haveEncColumn ? ")" : "") + "\n");
                        }
                    }
                    item.append(System.getProperty("line.separator"));

                    item.append(" as `" + encryptColumn1.getEncryptColumns() + "` ,");
                    isEncColumn = true;
                    haveEncColumn = true;
                    break;
                }
            }
            if (!isEncColumn) {
                item.append(columnName + " ,");
            }
            encColumns += item.toString();
        }

        if (encColumns.length() > 1) {
            encColumns = encColumns.substring(0, encColumns.length() - 1);
        }
        viewSql.append(encColumns);
        viewSql.append(System.getProperty("line.separator"));

        viewSql.append(" from " + encryptColumns.getDatabaseServerName() + "." + encryptColumns.getDbTable());
        PreparedStatement preparedStatement = null;
        log.info("Mysql create view:" + viewSql);
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


    /**
     * 创建PostgreSQL解密视图(使用用户创建)
     *
     * @param conn
     * @param encryptColumns
     * @param encryptColumnsMapper
     * @return
     */
    public static boolean operViewToPostGreSql(Connection conn, DbhsmEncryptColumnsAdd encryptColumns, DbhsmEncryptColumnsMapper encryptColumnsMapper, String dbSchema) throws SQLException {

        /**
         * create or replace view v_table1 --视图名称
         * as select
         * 	testuser1.pgext_func_string_decrypt(name)
         * as name from table1;
         */
        log.info("创建PostgreSQL视图start: database:{},table:{}", encryptColumns.getDatabaseServerName(), encryptColumns.getDbTable());

        List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(conn, encryptColumns.getDbTable(), encryptColumns.getDatabaseType());

        if (allColumnsInfo == null || allColumnsInfo.size() == 0) {
            return false;
        }
        //查询出加密字段
        DbhsmEncryptColumns encryptColumnDto = new DbhsmEncryptColumns();
        encryptColumnDto.setDbInstanceId(encryptColumns.getDbInstanceId());
        encryptColumnDto.setDbTable(encryptColumns.getDbTable());
        encryptColumnDto.setDbUserName(encryptColumns.getDbUserName());
        List<DbhsmEncryptColumns> dbhsmEncryptColumns;
        dbhsmEncryptColumns = encryptColumnsMapper.selectDbhsmEncryptColumnsList(encryptColumnDto);
        //if (StringUtils.isEmpty(dbhsmEncryptColumns)) {
        //    dbhsmEncryptColumns = new ArrayList<>();
        //}
        //DbhsmEncryptColumns encryptColumn = new DbhsmEncryptColumns();
        //BeanUtils.copyProperties(encryptColumns, encryptColumn);
        //dbhsmEncryptColumns.add(encryptColumn);

        StringBuffer viewSql = new StringBuffer();

        viewSql.append("create or replace view " + dbSchema + "." + encryptColumns.getDbTable());
        viewSql.append(System.getProperty("line.separator"));
        viewSql.append("as SELECT ");
        viewSql.append(System.getProperty("line.separator"));

        //拼接字段
        String encColumns = "";
        boolean haveEncColumn = false;
        for (int i = 0; i < allColumnsInfo.size(); i++) {
            Map<String, String> map = allColumnsInfo.get(i);
            String columnName = map.get(DbConstants.DB_COLUMN_NAME);
            StringBuffer item = new StringBuffer();
            boolean isEncColumn = false;
            for (DbhsmEncryptColumns encryptColumn1 : dbhsmEncryptColumns) {
                if (columnName.equalsIgnoreCase(encryptColumn1.getEncryptColumns())) {
                    if (haveEncColumn) {
                        item.append("(select ");
                    }
                    item.append(dbSchema + ".pgext_func_" + DbConstants.algMappingStrOrFpe(encryptColumn1.getEncryptionAlgorithm()) + "_decrypt(");
                    item.append("'" + encryptColumn1.getId() + "',");
                    item.append(System.getProperty("line.separator"));
                    item.append("'http://" + encryptColumns.getIpAndPort() + "/prod-api/dbhsm/api/datahsm/v1/strategy/get', ");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(inet_client_addr() as text),");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(current_catalog as text),");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(current_catalog as text),");
                    item.append(System.getProperty("line.separator"));
                    item.append("'" + encryptColumn1.getDbTable() + "',");
                    item.append(System.getProperty("line.separator"));
                    item.append("'" + encryptColumn1.getEncryptColumns() + "',");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(user AS text),");
                    item.append(System.getProperty("line.separator"));
                    item.append(encryptColumn1.getEncryptColumns() + ",0,0,10)" + (haveEncColumn ? ")" : "") + "\n");
                    item.append(" as " + encryptColumn1.getEncryptColumns() + " ,");
                    isEncColumn = true;
                    haveEncColumn = true;
                    break;
                }
            }

            if (!isEncColumn) {
                item.append(columnName + " ,");
            }
            encColumns += item.toString();
        }

        if (encColumns.length() > 1) {
            encColumns = encColumns.substring(0, encColumns.length() - 1);
        }
        viewSql.append(encColumns);
        viewSql.append(System.getProperty("line.separator"));

        viewSql.append(" from " + dbSchema + ".\"" + encryptColumns.getDbTable() + "\"");
        PreparedStatement preparedStatement = null;
        log.info("PostgreSQL create view:" + viewSql);
        try {
            preparedStatement = conn.prepareStatement(viewSql.toString());
            preparedStatement.execute();
            String viemName = dbSchema + "." + encryptColumns.getDbTable();
            String permssionSql = "GRANT SELECT ON " + viemName + " TO  \"" + encryptColumns.getDbUserName() + "\"";
            log.info("给用户授权查询视图权限Sql:" + permssionSql);
            preparedStatement = conn.prepareStatement(permssionSql);
            preparedStatement.execute();
            return true;
        } catch (Exception e) {
            log.info("给用户授权查询视图权限失败！");
            e.printStackTrace();
            return false;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }


    private static boolean operViewToKingBaseSql(Connection conn, DbhsmEncryptColumnsAdd encryptColumns, DbhsmEncryptColumnsMapper encryptColumnsMapper, String dbSchema) throws SQLException {

        /**
         * create or replace view v_table1 --视图名称
         * as select
         * 	testuser1.pgext_func_string_decrypt(name)
         * as name from table1;
         */
        log.info("创建KingBase视图start: database:{},table:{}", encryptColumns.getDatabaseServerName(), encryptColumns.getDbTable());

        List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(conn, encryptColumns.getDbTable(), encryptColumns.getDatabaseType());

        if (allColumnsInfo == null || allColumnsInfo.size() == 0) {
            return false;
        }
        //查询出加密字段
        DbhsmEncryptColumns encryptColumnDto = new DbhsmEncryptColumns();
        encryptColumnDto.setDbInstanceId(encryptColumns.getDbInstanceId());
        encryptColumnDto.setDbTable(encryptColumns.getDbTable());
        encryptColumnDto.setDbUserName(encryptColumns.getDbUserName());
        List<DbhsmEncryptColumns> dbhsmEncryptColumns;
        dbhsmEncryptColumns = encryptColumnsMapper.selectDbhsmEncryptColumnsList(encryptColumnDto);

        StringBuffer viewSql = new StringBuffer();

        viewSql.append("create or replace view v_" + encryptColumns.getDbTable());
        viewSql.append(System.getProperty("line.separator"));
        viewSql.append("as SELECT ");
        viewSql.append(System.getProperty("line.separator"));

        //拼接字段
        String encColumns = "";
        boolean haveEncColumn = false;
        for (int i = 0; i < allColumnsInfo.size(); i++) {
            Map<String, String> map = allColumnsInfo.get(i);
            String columnName = map.get(DbConstants.DB_COLUMN_NAME);
            StringBuffer item = new StringBuffer();
            boolean isEncColumn = false;
            for (DbhsmEncryptColumns encryptColumn1 : dbhsmEncryptColumns) {
                if (columnName.equalsIgnoreCase(encryptColumn1.getEncryptColumns())) {
                    if (haveEncColumn) {
                        item.append("(select ");
                    }
                    item.append(dbSchema + ".kbext_func_" + DbConstants.algMappingStrOrFpe(encryptColumn1.getEncryptionAlgorithm()) + "_decrypt(");
                    item.append("'" + encryptColumn1.getId() + "',");
                    item.append(System.getProperty("line.separator"));
                    item.append("'http://" + encryptColumns.getIpAndPort() + "/api/datahsm/v1/strategy/get', ");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(inet_client_addr() as text),--ip");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(current_catalog as text), --实例名");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(current_catalog as text),--库名");
                    item.append(System.getProperty("line.separator"));
                    item.append("'" + encryptColumn1.getDbTable() + "',");
                    item.append(System.getProperty("line.separator"));
                    item.append("'" + encryptColumn1.getEncryptColumns() + "',");
                    item.append(System.getProperty("line.separator"));
                    item.append("CAST(user AS text),--用户名");
                    item.append(System.getProperty("line.separator"));

                    if (DbConstants.SGD_SM4.equals(encryptColumn1.getEncryptionAlgorithm())) {
                        item.append(encryptColumn1.getEncryptColumns() + ",0,0)" + (haveEncColumn ? ")" : "") + "\n");
                    } else {
                        if (DbConstants.ESTABLISH_RULES_YES.equals(encryptColumn1.getEstablishRules())) {
                            item.append(encryptColumn1.getEncryptColumns() + "," + //加密列
                                    (encryptColumn1.getEncryptionOffset() - 1) + "," + //偏移量
                                    (encryptColumn1.getEncryptionLength() - (encryptColumn1.getEncryptionOffset() - 1)) + "," +//加密长度
                                    encryptColumn1.getEncryptionAlgorithm() + ")" + (haveEncColumn ? ")" : "") + "\n");
                        } else {
                            item.append(encryptColumn1.getEncryptColumns() + ",0,0," + encryptColumn1.getEncryptionAlgorithm() + ")" + (haveEncColumn ? ")" : "") + "\n");
                        }
                    }
                    item.append(" as " + encryptColumn1.getEncryptColumns() + " ,");
                    isEncColumn = true;
                    haveEncColumn = true;
                    break;
                }
            }

            if (!isEncColumn) {
                item.append(columnName + " ,");
            }
            encColumns += item.toString();
        }

        if (encColumns.length() > 1) {
            encColumns = encColumns.substring(0, encColumns.length() - 1);
        }
        viewSql.append(encColumns);
        viewSql.append(System.getProperty("line.separator"));

        viewSql.append(" from " + dbSchema + ".\"" + encryptColumns.getDbTable() + "\"");
        PreparedStatement preparedStatement = null;
        log.info("KingBase create view:" + viewSql);
        try {
            preparedStatement = conn.prepareStatement(viewSql.toString());
            preparedStatement.execute();
            String viemName = "v_" + encryptColumns.getDbTable();
            String permssionSql = "select * from " + viemName;
            log.info("查询KingBase创建的视图:" + permssionSql);
            preparedStatement = conn.prepareStatement(permssionSql);
            preparedStatement.execute();
            return true;
        } catch (Exception e) {
            log.info("给用户授权查询视图权限失败！");
            e.printStackTrace();
            return false;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

}

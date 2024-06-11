package com.spms.common.dbTool;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.ParseCreateSQL;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class DBUtil {

    private DBUtil() {
    }

    /**
     * 查询获取数据库所有表名
     */
    public static List<String> findAllTables(Connection conn, String userName, String dbType) {
        return findAllTables(conn, userName, dbType, "", "");
    }

    public static List<String> findAllTables(Connection conn, String userName, String dbType, String dbName, String schema) {
        Statement stmt = null;
        List<String> tableNamesList = new ArrayList<String>();
        try {
            stmt = conn.createStatement();
            if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(dbType)) {
                ResultSet resultSet = stmt.executeQuery("select TABLE_NAME from all_tables WHERE owner='" + userName.toUpperCase() + "'");
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    tableNamesList.add(resultSet.getString("TABLE_NAME"));
                }
                resultSet.close();
            } else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(dbType)) {
                ResultSet resultSet = stmt.executeQuery(DbConstants.DB_SQL_SQLSERVER_TABLE_QUERY);
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    tableNamesList.add(resultSet.getString("name"));
                }
                resultSet.close();
            } else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(dbType)) {
                String sql = "SELECT table_name as table_name FROM information_schema.tables WHERE table_schema = '" + dbName + "' and table_type = 'BASE TABLE'";
                ResultSet resultSet = stmt.executeQuery(sql);
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    tableNamesList.add(resultSet.getString("table_name"));
                }
                resultSet.close();
            } else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(dbType)) {
                String sql = "select tablename from pg_tables where schemaname = '" + schema + "'";
                ResultSet resultSet = stmt.executeQuery(sql);
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    tableNamesList.add(resultSet.getString("tablename"));
                }
                resultSet.close();
            } else if (DbConstants.DB_TYPE_DM.equalsIgnoreCase(dbType)) {
                ResultSet resultSet = stmt.executeQuery("select TABLE_NAME FROM all_tables WHERE owner='" + userName.toUpperCase() + "'");
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    tableNamesList.add(resultSet.getString("TABLE_NAME"));
                }
                resultSet.close();
            } else if (DbConstants.DB_TYPE_CLICKHOUSE.equalsIgnoreCase(dbType)) {
                ResultSet resultSet = stmt.executeQuery("SELECT name as TABLE_NAME FROM system.tables WHERE database = '" + schema + "'");
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    tableNamesList.add(resultSet.getString("TABLE_NAME"));
                }
                resultSet.close();
            } else if (DbConstants.DB_TYPE_KB.equalsIgnoreCase(dbType)) {
                ResultSet resultSet = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = '" + schema + "' AND table_type = 'BASE TABLE'");
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    tableNamesList.add(resultSet.getString("table_name"));
                }
                resultSet.close();
            }

            stmt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tableNamesList;
    }

    public static String getCataLog(Connection conn) {
        if (null == conn) {
            return null;
        }
        try {
            return conn.getCatalog();
        } catch (SQLException e) {
            // ignore
        }

        return null;
    }

    public static String getSchema(Connection conn) {
        if (null == conn) {
            return null;
        }
        try {
            return conn.getSchema();
        } catch (SQLException e) {
            // ignore
        }

        return null;
    }

    public static List<Map<String, String>> findAllColumnsInfo(Connection conn, String tableName, String dbType) throws SQLException {
        List<Map<String, String>> colList = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        ResultSetMetaData rsmd = null;
        try {
            if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(dbType)) {
                ps = conn.prepareStatement("select * from " + tableName.toUpperCase() + " limit 1");
                rs = ps.executeQuery();
                rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    Map<String, String> colMap = new HashMap<>();
                    colMap.put(DbConstants.DB_COLUMN_NAME, rsmd.getColumnName(i));
                    colMap.put("columnType", rsmd.getColumnTypeName(i));
                    colList.add(colMap);
                }
            } else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(dbType)) {
                Statement stmt = null;
                try {
                    stmt = conn.createStatement();
                    ResultSet resultSet = stmt.executeQuery("select column_name,data_type,CHARACTER_MAXIMUM_LENGTH from information_schema.columns where table_name = '" + tableName.toUpperCase() + "'");
                    while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                        Map<String, String> colMap = new HashMap<>();
                        colMap.put(DbConstants.DB_COLUMN_NAME, resultSet.getString("column_name"));
                        colMap.put("columnType", resultSet.getString("data_type") + "(" + resultSet.getString("CHARACTER_MAXIMUM_LENGTH") + ")");
                        colList.add(colMap);
                    }
                    resultSet.close();
                    stmt.close();
                    resultSet.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(dbType)) {
                Statement stmt = null;
                try {
                    stmt = conn.createStatement();
                    ResultSet resultSet = stmt.executeQuery("DESCRIBE `" + tableName + "`");
                    while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                        Map<String, String> colMap = new HashMap<>();
                        colMap.put(DbConstants.DB_COLUMN_NAME, resultSet.getString("Field"));
                        colMap.put("columnType", resultSet.getString("Type"));
                        colMap.put("Key", resultSet.getString("Key"));
                        colList.add(colMap);
                    }
                    resultSet.close();
                    stmt.close();
                    resultSet.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(dbType)) {
                Statement stmt = null;
                try {
                    stmt = conn.createStatement();
                    //ResultSet resultSet = stmt.executeQuery(
                    //        "SELECT format_type(a.atttypid,a.atttypmod) as type,a.attname as name " +
                    //                "FROM pg_class as c,pg_attribute as a where c.relname = '" + tableName +
                    //                "' and a.attrelid = c.oid and a.attnum>0");
                    ResultSet resultSet = stmt.executeQuery(
                            "SELECT data_type as type ,column_name as name ,character_maximum_length as length   \n" +
                                    "FROM information_schema.columns \n" +
                                    "WHERE table_name ='" + tableName + "' ORDER BY ordinal_position");
                    while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                        Map<String, String> colMap = new HashMap<>();
                        colMap.put(DbConstants.DB_COLUMN_NAME, resultSet.getString("name"));
                        colMap.put("columnType", resultSet.getString("type"));
                        colList.add(colMap);
                    }

                    resultSet.close();
                    stmt.close();
                    resultSet.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (DbConstants.DB_TYPE_DM.equalsIgnoreCase(dbType)) {
                ps = conn.prepareStatement("select * from " + tableName + " limit 1");
                rs = ps.executeQuery();
                rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    Map<String, String> colMap = new HashMap<>();
                    colMap.put(DbConstants.DB_COLUMN_NAME, rsmd.getColumnName(i));
                    colMap.put("columnType", rsmd.getColumnTypeName(i));
                    colList.add(colMap);
                }
            } else if (DbConstants.DB_TYPE_KB.equalsIgnoreCase(dbType)) {
                ps = conn.prepareStatement("SELECT column_name, data_type, \n" +
                        "CASE WHEN (column_name = (SELECT a.attname AS pk_column_name\n" +
                        "FROM pg_class t,pg_attribute a,pg_constraint c WHERE c.contype = 'p'AND c.conrelid = t.oid AND a.attrelid = t.oid AND a.attnum = ANY(c.conkey) AND t.relkind = 'r' AND t.relname = '"+tableName+"'))THEN  'PRI' ELSE  '' END  as key\n" +
                        "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + tableName + "';");
                rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, String> colMap = new HashMap<>();
                    colMap.put(DbConstants.DB_COLUMN_NAME, rs.getString("column_name"));
                    colMap.put("columnType", rs.getString("data_type"));
                    colMap.put("Key", rs.getString("Key"));
                    colList.add(colMap);
                }
            } else if (DbConstants.DB_TYPE_CLICKHOUSE.equalsIgnoreCase(dbType)) {
                ps = conn.prepareStatement("select name, type,if(is_in_primary_key = 1,'PRI','') as Key from system.columns where table='" + tableName + "'");
                rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, String> colMap = new HashMap<>();
                    colMap.put(DbConstants.DB_COLUMN_NAME, rs.getString("name"));
                    colMap.put("columnType", rs.getString("type"));
                    colMap.put("Key", rs.getString("Key"));
                    colList.add(colMap);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }

        }
        return colList;
    }

    //获取用户表空间
    public static Map<String, String> findSchemaByTable(Connection conn, String table) {
        Statement stmt;
        Map<String, String> map = new HashMap<>();
        try {
            stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT s.name AS SchemaName,t.name AS TableName " +
                    "FROM sys.tables t INNER JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE t.name ='" + table + "'");
            while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                map.put("schemaName", resultSet.getString("SchemaName"));
            }
            resultSet.close();
            stmt.close();
            resultSet.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 获取列原始定义
     */
    public static String getColumnDefinition(Connection connection, DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd) throws ZAYKException {
        CallableStatement cstmt = null;
        String schemaName = dbhsmEncryptColumnsAdd.getDbUserName();
        String tableName = dbhsmEncryptColumnsAdd.getDbTable();
        String columnName = dbhsmEncryptColumnsAdd.getEncryptColumns();
        String columnDefinition = "";

        try {
            cstmt = connection.prepareCall("{call SP_TABLEDEF(?, ?)}");
            cstmt.setString(1, schemaName);
            cstmt.setString(2, tableName);
            ResultSet rs = cstmt.executeQuery();
            StringBuilder tableDefinition = new StringBuilder();
            while (rs.next()) {
                tableDefinition.append(rs.getString(1));
            }
            String createSQL = ParseCreateSQL.parseCreateSQL(tableDefinition.toString(), true);
            String[] lines = createSQL.split("\\n");
            for (String line : lines) {
                if (line.contains(columnName)) {
                    columnDefinition = line;
                    break;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new ZAYKException("获取列定义失败！");
        } finally {
            // Close resources
            try {
                if (cstmt != null) {
                    cstmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return columnDefinition;
    }

    public static void main(String[] args) {
        String columnDefinition = "CREATE TABLE \"W1229\".\"demo1229\"\n" +
                "(\n" +
                "\"COLUMN_2\" CHAR(10),\n" +
                "\"COLUMN_3\" CHAR(10),\n" +
                "\"COLUMN_4\" CHAR(10),\n" +
                "\"COLUMN_5\" CHAR(10),\n" +
                "\"COLUMN_6\" CHAR(10),\n" +
                "\"COLUMN_7\" CHAR(10),\n" +
                "\"COLUMN_8\" CHAR(10),\n" +
                "\"COLUMN_9\" CHAR(10),\n" +
                "\"COLUMN_10\" CHAR(10),\n" +
                "\"COLUMN_11\" CHAR(10) ENCRYPT WITH SM4_ECB MANUAL BY WRAPPED '0x1EC162702C34E1548A3ABB636D2085FFC45DF216915657D42DF93F1F0DCEA92E188E9044' USER(\"W1229\" ),\n" +
                "\"COLUMN_12\" CHAR(10)) STORAGE(ON \"MAIN\", CLUSTERBTR) ;\n";
        String col = "\"CREATE TABLE \\\"DMU1\\\".\\\"TABLE_4\\\"\\n\" +\n" +
                "                \"(\\n\" +\n" +
                "                \"\\\"COLUMN_1\\\" INT IDENTITY(1, 1) NOT NULL,\\n\" +\n" +
                "                \"\\\"COLUMN_2\\\" CHAR(10),\\n\" +\n" +
                "                \"UNIQUE(\\\"COLUMN_1\\\"),\\n\" +\n" +
                "                \"NOT CLUSTER PRIMARY KEY(\\\"COLUMN_1\\\")) STORAGE(ON \\\"MAIN\\\", CLUSTERBTR) ;";
        String c = "))";
        int i = c.indexOf(")");
        System.out.println(i);
    }
}

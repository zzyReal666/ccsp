package com.spms.common.dbTool;

import com.spms.common.constant.DbConstants;
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
    public static List<String> findAllTables(Connection conn, String userName,String dbType) {
        Statement stmt = null;
        List<String> tableNamesList = new ArrayList<String>();
        try {
            stmt = conn.createStatement();
            if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(dbType)){
                ResultSet resultSet = stmt.executeQuery("select TABLE_NAME from all_tables WHERE owner='" + userName.toUpperCase() + "'");
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    tableNamesList.add(resultSet.getString("TABLE_NAME"));
                }
                resultSet.close();
            }else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(dbType)){
                ResultSet resultSet = stmt.executeQuery(DbConstants.DB_SQL_SQLSERVER_TABLE_QUERY);
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    tableNamesList.add(resultSet.getString("name"));
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
            if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(dbType)){
                ps = conn.prepareStatement("select * from " + tableName.toUpperCase());
                rs = ps.executeQuery();
                rsmd = rs.getMetaData();
                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    Map<String,String> colMap =new HashMap<>();
                    colMap.put(DbConstants.DB_COLUMN_NAME, rsmd.getColumnName(i));
                    colMap.put("columnType", rsmd.getColumnTypeName(i));
                    colList.add(colMap);
                }
            }else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(dbType)){
                Statement stmt = null;
                try {
                    stmt = conn.createStatement();
                    ResultSet resultSet = stmt.executeQuery("select column_name,data_type,CHARACTER_MAXIMUM_LENGTH from information_schema.columns where table_name = '" + tableName.toUpperCase() + "'");
                    while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                        Map<String,String> colMap =new HashMap<>();
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
    public static Map<String, String> findSchemaByTable(Connection conn,String table) {
        Statement stmt ;
        Map<String,String> map=new HashMap<>();
        try {
            stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT s.name AS SchemaName,t.name AS TableName " +
                    "FROM sys.tables t INNER JOIN sys.schemas s ON t.schema_id = s.schema_id WHERE t.name ='" + table + "'");
            while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                map.put("schemaName",resultSet.getString("SchemaName"));
            }
            resultSet.close();
            stmt.close();
            resultSet.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}

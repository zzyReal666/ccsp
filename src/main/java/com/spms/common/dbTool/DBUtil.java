package com.spms.common.dbTool;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.StringUtils;
import com.spms.common.ParseCreateSQL;
import com.spms.common.constant.DbConstants;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;

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

    public static List<String> findHbaseTables(DbhsmDbInstance instance) {
        List<String> list = new ArrayList<>();
        try {
            org.apache.hadoop.hbase.client.Connection connection = DbConnectionPoolFactory.buildHbaseDataSource(instance);
            // 获取Admin对象来进行管理操作
            Admin admin = connection.getAdmin();
            // 获取所有表的名称
            TableName[] tableNames = admin.listTableNames();
            for (TableName tableName : tableNames) {
                list.add(tableName.getNameAsString());
            }

            admin.close();
            connection.close();
        } catch (Exception e) {
            log.error("查询Hbase表列表Error:{}", e.getMessage());
        }
        return list;
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
        try {
            if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(dbType)) {
                ps = conn.prepareStatement("SELECT data_type || CASE WHEN data_type IN ('VARCHAR2', 'CHAR', 'NUMBER') THEN '(' || data_length || ')' ELSE '' END AS \"data_type\",column_name FROM all_tab_columns WHERE table_name = '" + tableName.toUpperCase() + "'");
                rs = ps.executeQuery();
                Map<String, String> columnPAMKey = getColumnPAMKey(conn, tableName, dbType);
                while (rs.next()) {
                    Map<String, String> colMap = new HashMap<>();
                    String columName = rs.getString("column_name");
                    colMap.put(DbConstants.DB_COLUMN_NAME, columName);
                    colMap.put(DbConstants.DB_COLUMN_TYPE, rs.getString("data_type"));
                    //主键信息
                    if (!columnPAMKey.isEmpty() && columnPAMKey.containsKey(columName)) {
                        colMap.put(DbConstants.DB_COLUMN_KEY, columnPAMKey.get(columName));
                    }
                    colList.add(colMap);
                }
            } else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(dbType)) {
                Statement stmt;
                try {
                    String[] split = tableName.split(":");
                    stmt = conn.createStatement();
                    //需要加 schema条件 不然有重复字段
                    String sql = "select column_name,data_type,CHARACTER_MAXIMUM_LENGTH from information_schema.columns where TABLE_SCHEMA= '" + split[0].toUpperCase() + "' and table_name = '" + split[1].toUpperCase() + "'";
                    ResultSet resultSet = stmt.executeQuery(sql);
                    Map<String, String> columnPAMKey = getColumnPAMKey(conn, split[1], dbType);
                    while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                        Map<String, String> colMap = new HashMap<>();
                        String columName = resultSet.getString("column_name");
                        colMap.put(DbConstants.DB_COLUMN_NAME, columName);
                        //类型长度为空不进行拼装
                        colMap.put(DbConstants.DB_COLUMN_TYPE, resultSet.getString("data_type") + (StringUtils.isBlank(resultSet.getString("CHARACTER_MAXIMUM_LENGTH")) ? "" : "(" + resultSet.getString("CHARACTER_MAXIMUM_LENGTH") + ")"));
                        //主键信息
                        if (!columnPAMKey.isEmpty() && columnPAMKey.containsKey(columName)) {
                            colMap.put(DbConstants.DB_COLUMN_KEY, columnPAMKey.get(columName));
                        }
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
                        colMap.put(DbConstants.DB_COLUMN_TYPE, resultSet.getString("Type"));
                        colMap.put(DbConstants.DB_COLUMN_KEY, resultSet.getString(DbConstants.DB_COLUMN_KEY));
                        colList.add(colMap);
                    }
                    resultSet.close();
                    stmt.close();
                    resultSet.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(dbType)) {
                Statement stmt;
                try {
                    stmt = conn.createStatement();
                    //获取
                    ResultSet resultSet = stmt.executeQuery("SELECT data_type as type, column_name as name ,character_maximum_length as length , (SELECT constraint_type FROM information_schema.table_constraints WHERE constraint_type ='PRIMARY KEY' AND table_name = table_name and constraint_name = column_name) as Key\n" +
                            "FROM information_schema.columns WHERE table_name = '" + tableName + "' ORDER BY ordinal_position;");
                    Map<String, String> columnPAMKey = getColumnPAMKey(conn, tableName, dbType);
                    while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                        Map<String, String> colMap = new HashMap<>();
                        String columName = resultSet.getString("name");
                        colMap.put(DbConstants.DB_COLUMN_NAME, columName);
                        //类型长度为空不进行拼装
                        colMap.put(DbConstants.DB_COLUMN_TYPE, resultSet.getString("type") + (StringUtils.isBlank(resultSet.getString("length")) ? "" : "(" + resultSet.getString("length") + ")"));
                        colMap.put(DbConstants.DB_COLUMN_KEY, resultSet.getString(DbConstants.DB_COLUMN_KEY));
                        //主键信息
                        if (!columnPAMKey.isEmpty() && columnPAMKey.containsKey(columName)) {
                            colMap.put(DbConstants.DB_COLUMN_KEY, columnPAMKey.get(columName));
                        }
                        colList.add(colMap);
                    }

                    resultSet.close();
                    stmt.close();
                    resultSet.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (DbConstants.DB_TYPE_DM.equalsIgnoreCase(dbType)) {
                ps = conn.prepareStatement("SELECT column_name,data_type || CASE WHEN data_type !='TEXT' THEN '(' || data_length || ')' ELSE '' END AS data_type FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '" + tableName.toUpperCase() + "'");
                rs = ps.executeQuery();
                //获取主键信息
                Map<String, String> columnPAMKey = getColumnPAMKey(conn, tableName, dbType);
                while (rs.next()) {
                    Map<String, String> colMap = new HashMap<>();
                    String columnName = rs.getString("column_name");
                    colMap.put(DbConstants.DB_COLUMN_NAME, columnName);
                    colMap.put(DbConstants.DB_COLUMN_TYPE, rs.getString("data_type"));
                    //主键信息
                    if (!columnPAMKey.isEmpty() && columnPAMKey.containsKey(columnName)) {
                        colMap.put(DbConstants.DB_COLUMN_KEY, columnPAMKey.get(columnName));
                    }
                    colList.add(colMap);
                }
            } else if (DbConstants.DB_TYPE_KB.equalsIgnoreCase(dbType)) {
                String[] split = tableName.split(":");
                //查询V8R3版本列信息
                String sql = "SELECT column_name, CASE WHEN data_type = 'USER-DEFINED' THEN udt_name ELSE data_type END AS data_type FROM information_schema.columns WHERE table_name = '" + split[1] + "' AND table_schema='" + split[0] + "'";
                ps = conn.prepareStatement(sql);
                rs = ps.executeQuery();
                //获取主键信息
                Map<String, String> columnPAMKey = getColumnPAMKey(conn, tableName, dbType);
                while (rs.next()) {
                    Map<String, String> colMap = new HashMap<>();
                    String columnName = rs.getString("column_name");
                    colMap.put(DbConstants.DB_COLUMN_NAME, columnName);
                    colMap.put(DbConstants.DB_COLUMN_TYPE, rs.getString("data_type"));
                    if (!columnPAMKey.isEmpty() && columnPAMKey.containsKey(columnName)) {
                        colMap.put(DbConstants.DB_COLUMN_KEY, columnPAMKey.get(columnName));
                    }
                    colList.add(colMap);
                }
            } else if (DbConstants.DB_TYPE_CLICKHOUSE.equalsIgnoreCase(dbType)) {
                String[] split = tableName.split(":");
                ps = conn.prepareStatement("select name, type,if(is_in_primary_key = 1,'PRI','') as Key from system.columns where table='" + split[1] + "' and database='" + split[0] + "'");
                rs = ps.executeQuery();
                while (rs.next()) {
                    Map<String, String> colMap = new HashMap<>();
                    colMap.put(DbConstants.DB_COLUMN_NAME, rs.getString("name"));
                    colMap.put(DbConstants.DB_COLUMN_TYPE, rs.getString("type"));
                    colMap.put(DbConstants.DB_COLUMN_KEY, rs.getString(DbConstants.DB_COLUMN_KEY));
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

    /**
     * 获取列的主键信息以及外键信息
     *
     * @return Map<列, 主键或外键>
     */
    public static Map<String, String> getColumnPAMKey(Connection conn, String table, String databaseType) {
        Map<String, String> map = new HashMap<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            switch (databaseType) {
                case DbConstants.DB_TYPE_SQLSERVER:
                    ps = conn.prepareStatement("SELECT c.COLUMN_NAME,tc.CONSTRAINT_TYPE as 'Key' FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu ON tc.CONSTRAINT_NAME = ccu.CONSTRAINT_NAME JOIN INFORMATION_SCHEMA.COLUMNS c ON c.TABLE_NAME = ccu.TABLE_NAME AND c.COLUMN_NAME = ccu.COLUMN_NAME\n" +
                            "WHERE tc.TABLE_NAME = '" + table + "' AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY';\n");
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        if (StringUtils.isNotBlank(rs.getString("Key"))) {
                            map.put(rs.getString("column_name"), rs.getString("Key"));
                        }
                    }
                    break;
                case DbConstants.DB_TYPE_ORACLE:
                    ps = conn.prepareStatement("SELECT cols.column_name,CASE WHEN cons.CONSTRAINT_TYPE='P' then 'PRI' else 'MUL' END as Key FROM all_constraints cons JOIN all_cons_columns cols ON cons.constraint_name = cols.constraint_name WHERE cons.table_name = '" + table.toUpperCase() + "'");
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        if (StringUtils.isNotBlank(rs.getString("Key"))) {
                            map.put(rs.getString("column_name"), rs.getString("Key"));
                        }
                    }
                    break;
                case DbConstants.DB_TYPE_DM:
                    ps = conn.prepareStatement("SELECT b.column_name,CASE WHEN a.CONSTRAINT_TYPE='P' then 'PRI' else 'MUL' END as Key FROM USER_CONSTRAINTS a JOIN USER_CONS_COLUMNS b ON a.constraint_name = b.constraint_name WHERE b.table_name = '" + table.toUpperCase() + "'");
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        if (StringUtils.isNotBlank(rs.getString("Key"))) {
                            map.put(rs.getString("column_name"), rs.getString("Key"));
                        }
                    }
                    break;
                case DbConstants.DB_TYPE_KB:
                    String[] split = table.split(":");
                    ps = conn.prepareStatement("SELECT distinct kcu.column_name, kcu.table_name, CASE WHEN tc.constraint_type='PRIMARY KEY' THEN 'PRI' ELSE 'MUL' END as Key FROM information_schema.key_column_usage kcu JOIN information_schema.table_constraints tc ON kcu.constraint_name = tc.constraint_name " +
                            "WHERE tc.constraint_type in ('PRIMARY KEY','FOREIGN KEY')  and kcu.table_schema ='" + split[0] + "' and kcu.table_name ='" + split[1] + "'");
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        if (StringUtils.isNotBlank(rs.getString("Key"))) {
                            map.put(rs.getString("column_name"), rs.getString("Key"));
                        }
                    }
                    break;
                case DbConstants.DB_TYPE_POSTGRESQL:
                    ps = conn.prepareStatement("select  kcu.column_name,case when tc.constraint_type='PRIMARY KEY' then 'PRI' else 'MUL' END as Key from information_schema.table_constraints tc\n" +
                            "join information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema\n" +
                            "WHERE tc.constraint_type = 'PRIMARY KEY' AND tc.table_name = '" + table + "';");
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        if (StringUtils.isNotBlank(rs.getString("Key"))) {
                            map.put(rs.getString("column_name"), rs.getString("Key"));
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            log.info("获取主键信息失败：{}", e.getMessage());
        } finally {
            try {
                ps.close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            try {
                rs.close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
        return map;
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


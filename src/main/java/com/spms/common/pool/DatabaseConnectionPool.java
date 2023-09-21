package com.spms.common.pool;

import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseConnectionPool {
    private static DataSource dataSource;
    private static final int MAX_CONNECTIONS = 10;
    public static void initialize(DbInstanceGetConnDTO instance) {

        // 从数据库或其他地方获取以下信息
        String databaseIp = instance.getDatabaseIp();
        String databaseType = instance.getDatabaseType();
        String databasePort = instance.getDatabasePort();
        String databaseDba = instance.getDatabaseDba();
        String databaseDbaPassword = instance.getDatabaseDbaPassword();
        String databaseExampleType = instance.getDatabaseExampleType();
        String databaseServerName = instance.getDatabaseServerName();


        HikariConfig config = new HikariConfig();
        switch (databaseType) {
            case DbConstants.DB_TYPE_MYSQL:
                config.setJdbcUrl("jdbc:mysql://" + databaseIp + ":" + databasePort  + "/" + databaseServerName);
                break;
            case DbConstants.DB_TYPE_ORACLE:
                if (DbConstants.DB_EXAMPLE_TYPE_SID.equals(databaseExampleType)) {
                    config.setJdbcUrl("jdbc:oracle:thin:@//" + databaseIp + ":" + databasePort  + "/" + databaseServerName);
                } else {
                    config.setJdbcUrl("jdbc:oracle:thin:@" + databaseIp + ":" + databasePort + ":" + databaseServerName);
                }
                break;
            case DbConstants.DB_TYPE_SQLSERVER:
                config.setJdbcUrl("jdbc:sqlserver://" + databaseIp + ":" + databasePort + ";databaseName=" + databaseServerName);
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }
        config.setUsername(databaseDba);
        config.setPassword(databaseDbaPassword);
        // Enable prepared statement caching， 缓存PreparedStatement
        config.addDataSourceProperty("cachePrepStmts", "true");
        // Set the size of the prepared statement cache， PreparedStatement缓存大小
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        // Set the SQL limit for the prepared statement cache ，driver缓存的statement 最大长度
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(MAX_CONNECTIONS);
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void main(String[] args) throws  SQLException {

        DbInstanceGetConnDTO instance = new DbInstanceGetConnDTO();
        instance.setDatabaseType(DbConstants.DB_TYPE_ORACLE);
        instance.setDatabaseIp("192.168.6.158");
        instance.setDatabasePort("1521");
        instance.setDatabaseServerName("orcl");
        instance.setDatabaseExampleType(":");
        instance.setDatabaseDba("user55");
        instance.setDatabaseDbaPassword("12345678");
        initialize(instance);
        Connection connection = getConnection();
        System.out.println(connection);
        String sql = "select * from table1";
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            if (connection != null) {
                statement = connection.prepareStatement(sql);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    // 处理结果集
                    System.out.println(resultSet.getString("id"));
                    System.out.println(resultSet.getString("name"));
                }
            }
        } catch (SQLException e) {
            // 处理异常
            e.printStackTrace();
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) { e.printStackTrace();}
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}

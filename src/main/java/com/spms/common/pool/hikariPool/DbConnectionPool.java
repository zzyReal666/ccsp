package com.spms.common.pool.hikariPool;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;

@Slf4j
public class DbConnectionPool {
    private static DataSource dataSource;
    //连接池大小
    private static final int MAX_CONNECTIONS = 10;

    public static DataSource initialize(DbInstanceGetConnDTO instanceGetConnDTO) {
        String databaseIp = instanceGetConnDTO.getDatabaseIp();
        String databaseType = instanceGetConnDTO.getDatabaseType();
        String databasePort = instanceGetConnDTO.getDatabasePort();
        String databaseDba = instanceGetConnDTO.getDatabaseDba();
        String databaseDbaPassword = instanceGetConnDTO.getDatabaseDbaPassword();
        String databaseExampleType = instanceGetConnDTO.getDatabaseExampleType();
        String databaseServerName = instanceGetConnDTO.getDatabaseServerName();
        String className;
        HikariConfig config = new HikariConfig();
        switch (databaseType) {
            case DbConstants.DB_TYPE_ORACLE:
                if (DbConstants.DB_EXAMPLE_TYPE_SID.equals(databaseExampleType)) {
                    config.setJdbcUrl("jdbc:oracle:thin:@" + databaseIp + ":" + databasePort + ":" + databaseServerName);
                } else {
                    config.setJdbcUrl("jdbc:oracle:thin:@//" + databaseIp + ":" + databasePort + "/" + databaseServerName);
                }
                break;
            case DbConstants.DB_TYPE_SQLSERVER:
                config.setJdbcUrl("jdbc:sqlserver://" + databaseIp + ":" + databasePort + ";DatabaseName=" + databaseServerName + ";encrypt=false;integratedSecurity=false;");
                break;
            case DbConstants.DB_TYPE_MYSQL:
                config.setJdbcUrl("jdbc:mysql://" + databaseIp + ":" + databasePort + "/" + databaseServerName);
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
        try {
            return new HikariDataSource(config);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Could not create HikariCP data source");
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * 关闭数据源及其关联的池。
     * Shutdown the DataSource and its associated pool.
     */
    public static void destroy(javax.sql.DataSource dataSource) {
        if (null != dataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            hikariDataSource.close();
        }
        DbConnectionPoolFactory.queryPool();
    }

    public static void main(String[] args) throws SQLException, ZAYKException {

        DbInstanceGetConnDTO instance = new DbInstanceGetConnDTO();
        //instance.setDatabaseType(DbConstants.DB_TYPE_ORACLE);
        //instance.setDatabaseIp("192.168.6.158");
        //instance.setDatabasePort("1521");
        //instance.setDatabaseServerName("orcl");
        //instance.setDatabaseExampleType(":");
        //instance.setDatabaseDba("user55");
        //instance.setDatabaseDbaPassword("12345678");
        instance.setDatabaseType(DbConstants.DB_TYPE_SQLSERVER);
        instance.setDatabaseIp("192.168.7.177");
        instance.setDatabasePort("1433");
        instance.setDatabaseServerName("dbtest");
        instance.setDatabaseExampleType(":");
        instance.setDatabaseDba("sa");
        instance.setDatabaseDbaPassword("12345678");
        DbConnectionPoolFactory factory = new DbConnectionPoolFactory();
        Connection connection = factory.getConnection(instance);
        System.out.println(connection);
        //String sql = "select * from table1";
        String sql1 = "drop user testuser12";
        String sql = DbConstants.DB_SQL_SQLSERVER_USER_QUERY;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            statement = connection.prepareStatement(sql1);
            //statement = connSQLServer().prepareStatement(sql1);
            statement.execute();
            //connection.commit();
            if (connection != null) {
                statement = connection.prepareStatement(sql);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    // 处理结果集
                    //System.out.println(resultSet.getString("id"));
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
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

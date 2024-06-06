package com.spms.common.pool.hikariPool;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 增加新的数据库后需要增加的内容：
 * 1设置JdbcUrl
 * 2创建数据库连接池key类继承DbInstancePoolKeyDTO
 * 3 更改DbConnectionPoolFactory.getDbInstancePoolKeyDTO()方法，添加数据库类型
 * 4 DbConstants类增加数据库常量类型和
 * 5 web:web界面增加数据库字典类型 实例界面添加新增的字典，更改getDbEditionsDic()方法
 * 6 增加版本号字典值
 * 7 pom文件添加数据库驱动依赖
 */
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
        String databaseServerName = instanceGetConnDTO.getDatabaseServerName();
        HikariConfig config = new HikariConfig();
        switch (databaseType) {
            case DbConstants.DB_TYPE_ORACLE:
                String databaseExampleType = instanceGetConnDTO.getDatabaseExampleType();
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
            case DbConstants.DB_TYPE_POSTGRESQL:
                config.setJdbcUrl("jdbc:postgresql://" + databaseIp + ":" + databasePort + "/" + databaseServerName);
                break;
            case DbConstants.DB_TYPE_DM:
                config.setJdbcUrl("jdbc:dm://" + databaseIp + ":" + databasePort + "/" + databaseServerName);
                // 设置SSL连接
                //config.addDataSourceProperty("sslFilesPath", "E:\\dmdbms\\client_ssl\\SYSDBA");
                //config.addDataSourceProperty("sslKeystorePass", "abc123");
                break;
            case DbConstants.DB_TYPE_CLICKHOUSE:
                config.setJdbcUrl("jdbc:clickhouse://" + databaseIp + ":" + databasePort + "/" + databaseServerName);
                config.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
                break;
            case DbConstants.DB_TYPE_KB:
                config.setJdbcUrl("jdbc:kingbase8://" + databaseIp + ":" + databasePort + "/" + databaseServerName);
                config.setDriverClassName("com.kingbase8.Driver");
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
        //config.setConnectionTimeout(20 * 1000);
        try {
            return new HikariDataSource(config);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Could not create HikariCP data source");
            throw new RuntimeException("与数据库" + databaseServerName + "创建连接失败！请检查网络是否正常及DBA用户名密码是否正确。", e);
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
        instance.setDatabaseType(DbConstants.DB_TYPE_CLICKHOUSE);
        instance.setDatabaseIp("192.168.7.113");
        instance.setDatabasePort("18123");
        instance.setDatabaseServerName("default");
        instance.setDatabaseExampleType(":");
        instance.setDatabaseDba("default");
        instance.setDatabaseDbaPassword("123456");
        DbConnectionPoolFactory factory = new DbConnectionPoolFactory();
        Connection connection = factory.getConnection(instance);

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        String sql = "INSERT INTO default.student (id, name, age, address) VALUES (?,?,?,?);";
        statement = connection.prepareStatement(sql);

        try {
            connection.setAutoCommit(false);
            for (int i = 7; i < 2000000; i++) {
                statement.setInt(1, i);
                statement.setString(2, "新增数据第：" + i + "条");
                statement.setInt(3, i + 1);
                statement.setString(4, "济南：" + i);
                statement.addBatch();
            }
            //执行SQL
            statement.executeBatch();
            //清除SQL
            statement.clearBatch();
            connection.commit();
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

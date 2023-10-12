package com.spms.common.pool.hikariPool;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.*;
/**
 *  增加新的数据库后需要增加的内容：
 *  1设置JdbcUrl
 *  2创建数据库连接池key类继承DbInstancePoolKeyDTO
 *  3 更改DbConnectionPoolFactory.getDbInstancePoolKeyDTO()方法，添加数据库类型
 *  4 DbConstants类增加数据库常量类型和
 *  5 web:web界面增加数据库字典类型 实例界面添加新增的字典，更改getDbEditionsDic()方法
 *  6 增加版本号字典值
 *  7 pom文件添加数据库驱动依赖
 * */
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
        }catch (HikariPool.PoolInitializationException e){
            log.info("Could not create HikariCP data source");
            throw new RuntimeException("创建连接池失败！"+databaseIp+":"+databasePort+e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Could not create HikariCP data source");
            throw new RuntimeException("连接池创建失败！"+e.getMessage(), e);
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
        instance.setDatabaseIp("192.168.6.212");
        instance.setDatabasePort("5432");
        instance.setDatabaseServerName("pgDB");
        instance.setDatabaseExampleType(":");
        instance.setDatabaseDba("postgres");
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

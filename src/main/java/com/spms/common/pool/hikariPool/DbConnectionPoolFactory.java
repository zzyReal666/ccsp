package com.spms.common.pool.hikariPool;

import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;
import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DTO.*;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.security.UserGroupInformation;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * @author Funciton:该类是连接池的管理类
 */
public class DbConnectionPoolFactory {
    private static final Log log = LogFactory.getLog(DbConnectionPool.class);
    private static Hashtable<DbInstancePoolKeyDTO, javax.sql.DataSource> hashtable = null;
    private static DbConnectionPoolFactory dataSourcePoolFactory;

    public static DbConnectionPoolFactory getInstance() {
        if (null == dataSourcePoolFactory) {
            hashtable = new Hashtable<DbInstancePoolKeyDTO, javax.sql.DataSource>();
            dataSourcePoolFactory = new DbConnectionPoolFactory();
        }
        return dataSourcePoolFactory;
    }

    /**
     * 绑定连接池
     *
     * @param key            连接池的名称必须唯一
     * @param dataSourcePool 对应的连接池
     */
    public void bind(DbInstancePoolKeyDTO key, javax.sql.DataSource dataSourcePool) {
        if (IsBePool(key)) {
            DbConnectionPool.destroy(getDbConnectionPool(key));
        }
        hashtable.put(key, dataSourcePool);
    }

    /**
     * 重新绑定连接池
     *
     * @param key            连接池的名称必须唯一
     * @param dataSourcePool 对应的连接池
     */
    public void rebind(DbInstancePoolKeyDTO key, javax.sql.DataSource dataSourcePool) {
        if (IsBePool(key)) {
            DbConnectionPool.destroy(getDbConnectionPool(key));
        }
        hashtable.put(key, dataSourcePool);
    }

    /**
     * 关闭连接池，删除动态数据连接池中名称为key的连接池
     *
     * @param key
     */
    public void unbind(DbInstancePoolKeyDTO key) {
        if (IsBePool(key)) {
            DbConnectionPool.destroy(getDbConnectionPool(key));
        }
        hashtable.remove(key);
    }

    /**
     * 查找动态数据连接池中是否存在名称为key的连接池
     *
     * @param key
     * @return
     */
    public boolean IsBePool(DbInstancePoolKeyDTO key) {
        return hashtable.containsKey(key);
    }

    /**
     * 根据key返回key对应的连接池
     *
     * @param key
     * @return
     */
    public javax.sql.DataSource getDbConnectionPool(DbInstancePoolKeyDTO key) {
        if (!IsBePool(key)) {
            return null;
        }
        return hashtable.get(key);

    }

    /**
     * 获取数据库连接
     *
     * @param instance 从池中获取不同数据库连接使用
     * @return
     */
    public Connection getConnection(DbhsmDbInstance instance) throws ZAYKException, SQLException {
        DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(instance, connDTO);
        Connection connection = getConnection(connDTO);
        connection.setAutoCommit(false);
        return connection;
    }

    public Connection getConnection(DbInstanceGetConnDTO instanceGetConnDTO) throws ZAYKException, SQLException {
        DbInstancePoolKeyDTO dbInstancekey = new DbInstancePoolKeyDTO();
        if (ObjectUtils.isEmpty(instanceGetConnDTO)) {
            return null;
        }
        String databaseType = instanceGetConnDTO.getDatabaseType();
        dbInstancekey = getDbInstancePoolKeyDTO(dbInstancekey, databaseType);
        BeanUtils.copyProperties(instanceGetConnDTO, dbInstancekey);
        if (!DbConnectionPoolFactory.getInstance().IsBePool(dbInstancekey)) {
            buildDataSourcePool(instanceGetConnDTO, dbInstancekey);
        }
        Connection connection = DbConnectionPoolFactory.getInstance().getDbConnectionPool(dbInstancekey).getConnection();
//        if (!databaseType.equals(DbConstants.DB_TYPE_CLICKHOUSE)) {
//            connection.setAutoCommit(false);
//        }
        return connection;
    }

    private static DbInstancePoolKeyDTO getDbInstancePoolKeyDTO(DbInstancePoolKeyDTO dbInstancekey, String databaseType) throws ZAYKException {
        if (databaseType.equals(DbConstants.DB_TYPE_ORACLE)) {
            dbInstancekey = new DbOracleInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_SQLSERVER)) {
            dbInstancekey = new DbSQLServerInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_MYSQL)) {
            dbInstancekey = new DbMySQLInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_POSTGRESQL)) {
            dbInstancekey = new DbPostgreSQLInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_DM)) {
            dbInstancekey = new DbDMInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_CLICKHOUSE)) {
            dbInstancekey = new DbClickHouseInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_KB)) {
            dbInstancekey = new DbKingBaseInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_HB)) {
            dbInstancekey = new DbHBaseInstancePoolKeyDTO();
        } else {
            log.info("Error:未实现的数据库类型");
            throw new ZAYKException("未实现的数据库类型");
        }
        return dbInstancekey;
    }

    //构建Hbase连接
    public static org.apache.hadoop.hbase.client.Connection buildHbaseDataSource(DbhsmDbInstance instance) throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", instance.getDatabaseIp());
        conf.set("zookeeper.znode.parent", "/hbase");
        conf.set("hbase.zookeeper.property.clientPort", instance.getDatabasePort());
        UserGroupInformation.setConfiguration(conf);
        UserGroupInformation romoteUser = UserGroupInformation.createRemoteUser(instance.getDatabaseDba());
        UserGroupInformation.setLoginUser(romoteUser);
        return ConnectionFactory.createConnection(conf);
    }


    public static String buildDataSourcePool(DbInstanceGetConnDTO instanceGetConnDTO, DbInstancePoolKeyDTO instanceKey) throws ZAYKException, SQLException {
        //判断各个数据不为空
        checkGetConnIsEmpty(instanceGetConnDTO);
        DbConnectionPoolFactory factory = DbConnectionPoolFactory.getInstance();
        if (factory.IsBePool(instanceKey)) {
            log.info("Info：The data source already exists：" + instanceKey);
            throw new ZAYKException("Info：The data source already exists：" + instanceKey);
        }
        javax.sql.DataSource dataSourcePool = DbConnectionPool.initialize(instanceGetConnDTO);
        //数据源连接池绑定
        factory.bind(instanceKey, dataSourcePool);
        //判断数据库连接池是否注册成功
        if (factory.IsBePool(instanceKey)) {
            log.info("Success：Successfully registered a new database connection pool：" + instanceKey);
            return "Success：Successfully registered a new database connection pool";
        } else {
            log.info("Error：Failed to register a new database connection pool：" + instanceKey);
            throw new ZAYKException("创建数据库连接池失败！" + instanceKey);
        }

    }

    public static String buildDataSourcePool(DbInstanceGetConnDTO instanceGetConnDTO) throws ZAYKException {
        DbInstancePoolKeyDTO instanceKey = new DbInstancePoolKeyDTO();
        if (ObjectUtils.isEmpty(instanceGetConnDTO)) {
            return null;
        }
        String databaseType = instanceGetConnDTO.getDatabaseType();
        if (instanceGetConnDTO.getDatabaseType().equals(DbConstants.DB_TYPE_ORACLE)) {
            instanceKey = new DbOracleInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_SQLSERVER)) {
            instanceKey = new DbSQLServerInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_MYSQL)) {
            instanceKey = new DbMySQLInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_POSTGRESQL)) {
            instanceKey = new DbPostgreSQLInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_DM)) {
            instanceKey = new DbDMInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_CLICKHOUSE)) {
            instanceKey = new DbClickHouseInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_KB)) {
            instanceKey = new DbKingBaseInstancePoolKeyDTO();
        } else {
            throw new ZAYKException("不支持的数据库类型：" + databaseType);
        }
        BeanUtils.copyProperties(instanceGetConnDTO, instanceKey);
        checkGetConnIsEmpty(instanceGetConnDTO);
        DbConnectionPoolFactory factory = DbConnectionPoolFactory.getInstance();
        if (factory.IsBePool(instanceKey)) {
            log.info("Info：The data source already exists：" + instanceKey);
            throw new ZAYKException("Info：The data source already exists：" + instanceKey);
        }
        javax.sql.DataSource dataSourcePool = DbConnectionPool.initialize(instanceGetConnDTO);
        //数据源连接池绑定
        factory.bind(instanceKey, dataSourcePool);
        //判断数据库连接池是否注册成功
        if (factory.IsBePool(instanceKey)) {
            log.info("Success：Successfully registered a new database connection pool：" + instanceKey);
            return "Success：Successfully registered a new database connection pool";
        } else {
            log.info("Error：Failed to register a new database connection pool：" + instanceKey);
            throw new ZAYKException("创建数据库连接池失败！" + instanceKey);
        }

    }

    private static void checkGetConnIsEmpty(DbInstanceGetConnDTO instanceGetConnDTO) throws ZAYKException {
        //判断各个数据不为空
        if (StringUtils.isBlank(instanceGetConnDTO.getDatabaseIp()) || StringUtils.isBlank(instanceGetConnDTO.getDatabasePort()) || StringUtils.isBlank(instanceGetConnDTO.getDatabaseDba()) || StringUtils.isBlank(instanceGetConnDTO.getDatabaseDbaPassword()) || StringUtils.isBlank(instanceGetConnDTO.getDatabaseType()) || (StringUtils.isBlank(instanceGetConnDTO.getDatabaseExampleType()) && DbConstants.DB_TYPE_ORACLE.equals(instanceGetConnDTO.getDatabaseType())) || StringUtils.isBlank(instanceGetConnDTO.getDatabaseServerName())) {
            log.info("Error：Database configuration in the support library is incomplete");
            throw new ZAYKException("数据库配置不完整，请检查数据库配置");
        }
    }

    /**
     * DbhsmDbInstance 转DbInstancePoolKeyDTO
     *
     * @param instance 从池中获取不同数据库连接使用
     * @return
     */
    public static DbInstancePoolKeyDTO instanceConventKey(DbhsmDbInstance instance) throws ZAYKException {
        DbInstancePoolKeyDTO instanceKey = new DbInstancePoolKeyDTO();
        if (ObjectUtils.isEmpty(instance)) {
            return null;
        }
        String databaseType = instance.getDatabaseType();
        instanceKey = getDbInstancePoolKeyDTO(instanceKey, databaseType);
        BeanUtils.copyProperties(instance, instanceKey);
        return instanceKey;
    }

    /**
     * 遍历连接池
     */
    public static void queryPool() {
        if (CollectionUtils.isEmpty(hashtable)) {
            log.info("Warning：The database connection pool is empty");
            return;
        }
        Enumeration<DbInstancePoolKeyDTO> keys = hashtable.keys();
        log.info("遍历连接池:");
        while (keys.hasMoreElements()) {
            DbInstancePoolKeyDTO key = keys.nextElement();
            javax.sql.DataSource value = hashtable.get(key);
            log.info("Key: " + key + ", Value: " + value);
        }
    }

    public static void main(String[] args) throws ZAYKException, SQLException {

        DbInstanceGetConnDTO instance = new DbInstanceGetConnDTO();
        instance.setDatabaseType(DbConstants.DB_TYPE_ORACLE);
        //instance.setDatabaseIp("192.168.6.158");
        //instance.setDatabasePort("1521");
        //instance.setDatabaseServerName("orcl");
        //instance.setDatabaseExampleType(":");
        //instance.setDatabaseDba("usre55");
        //instance.setDatabaseDbaPassword("12345678");
        instance.setDatabaseType(DbConstants.DB_TYPE_ORACLE);
        instance.setDatabaseIp("192.168.7.113");
        instance.setDatabasePort("3181");
        instance.setDatabaseServerName("hbase");
        instance.setDatabaseExampleType("/");
        instance.setDatabaseDba("SYSDBA");
        instance.setDatabaseDbaPassword("SYSDBA");

        DbConnectionPoolFactory factory = new DbConnectionPoolFactory();
        Connection connection = factory.getConnection(instance);
        System.out.println(connection);
        //String sql = "select * from table1";
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


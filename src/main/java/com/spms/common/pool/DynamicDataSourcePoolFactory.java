package com.spms.common.pool;

import com.alibaba.druid.support.logging.Log;
import com.alibaba.druid.support.logging.LogFactory;
import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstancePoolKeyDTO;
import com.spms.dbhsm.dbInstance.domain.DTO.DbOracleInstancePoolKeyDTO;
import com.spms.dbhsm.dbInstance.domain.DTO.DbSQLServernstancePoolKeyDTO;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
/**
 * Created with CosmosRay
 *
 * @author
 * @date 2019/8/15
 * Funciton:该类是连接池的管理类
 */
public class DynamicDataSourcePoolFactory {
    private static final Log log = LogFactory.getLog(DynamicDataSourcePool.class);
    private static Hashtable<DbInstancePoolKeyDTO, DynamicDataSourcePool> hashtable = null;
    private static DynamicDataSourcePoolFactory dataSourcePoolFactory;

    public static DynamicDataSourcePoolFactory getInstance() {
        if (null == dataSourcePoolFactory) {
            hashtable = new Hashtable<DbInstancePoolKeyDTO, DynamicDataSourcePool>();
            dataSourcePoolFactory = new DynamicDataSourcePoolFactory();
        }
        return dataSourcePoolFactory;
    }

    /**
     * 绑定连接池
     *
     * @param key            连接池的名称必须唯一
     * @param dataSourcePool 对应的连接池
     */
    public void bind(DbInstancePoolKeyDTO key, DynamicDataSourcePool dataSourcePool) {
        if (IsBePool(key)) {
            getDynamicDataSourcePool(key).destroy();
        }
        hashtable.put(key, dataSourcePool);
    }

    /**
     * 重新绑定连接池
     *
     * @param key            连接池的名称必须唯一
     * @param dataSourcePool 对应的连接池
     */
    public void rebind(DbInstancePoolKeyDTO key, DynamicDataSourcePool dataSourcePool) {
        if (IsBePool(key)) {
            getDynamicDataSourcePool(key).destroy();
        }
        hashtable.put(key, dataSourcePool);
    }

    /**
     * 删除动态数据连接池中名称为key的连接池
     *
     * @param key
     */
    public void unbind(DbInstancePoolKeyDTO key) {
        if (IsBePool(key)) {
            getDynamicDataSourcePool(key).destroy();
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
    public DynamicDataSourcePool getDynamicDataSourcePool(DbInstancePoolKeyDTO key) {
        if (!IsBePool(key)) {
            return null;
        }
        return  hashtable.get(key);

    }

    /**
     * 获取数据库连接
     *
     * @param  instance 从池中获取不同数据库连接使用
     * @return
     */

    public  Connection getConnection(DbInstanceGetConnDTO instance) throws ZAYKException {
        DbInstancePoolKeyDTO dbInstancekey = new DbInstancePoolKeyDTO();
        if (ObjectUtils.isEmpty(instance)) {
            return null;
        }
        String databaseType = instance.getDatabaseType();
        if (instance.getDatabaseType().equals(DbConstants.DB_TYPE_ORACLE)) {
            dbInstancekey = new DbOracleInstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_SQLSERVER)) {
            dbInstancekey = new DbSQLServernstancePoolKeyDTO();
        } else if (databaseType.equals(DbConstants.DB_TYPE_MYSQL)) {

        }
        BeanUtils.copyProperties(instance,dbInstancekey);

        if (!DynamicDataSourcePoolFactory.getInstance().IsBePool(dbInstancekey)) {
            //生成连接池
            DataSource dataSource = DataSource.getDataSource(instance);
            buildDataSourcePool(dataSource,dbInstancekey);
        }
        return DynamicDataSourcePoolFactory.getInstance().getDynamicDataSourcePool(dbInstancekey).getConnection();
    }



    public static String buildDataSourcePool(DataSource dataSource, DbInstancePoolKeyDTO instanceKey) throws ZAYKException {
        if (dataSource == null) {
            throw new ZAYKException("数据源对象为 null，请检查数据库");
        }
        //判断 数据库驱动、数据库连接、数据库用户名不为空  密码可为空
        if (StringUtils.isBlank(dataSource.getDriverClass()) || StringUtils.isBlank(dataSource.getUrl()) || StringUtils.isBlank(dataSource.getDatabaseDba())) {
            log.info("================================================================");
            log.info("Error：Database configuration in the support library is incomplete");
            throw new ZAYKException("数据库配置不完整，请检查数据库配置");
        }
        DynamicDataSourcePoolFactory factory = DynamicDataSourcePoolFactory.getInstance();

        if (factory.IsBePool(instanceKey)) {
            throw new ZAYKException("Info：The data source already exists：" + instanceKey);
        }

        DynamicDataSourcePool dataSourcePool = new DynamicDataSourcePool(dataSource.getDatabaseDba().trim(), dataSource.getDatabaseDbaPassword().trim(), dataSource.getUrl().trim(), dataSource.getDriverClass().trim());
        //数据源连接池绑定
        factory.bind(instanceKey, dataSourcePool);
        //判断数据库连接池是否注册成功
        if (factory.IsBePool(instanceKey)) {
            log.info("Success：Successfully registered a new database connection pool：" + instanceKey);
            return "<p style='color:#7CD03B'>Success：Successfully registered a new database connection pool：" + instanceKey+"</p>";
        } else {
            log.info("Error：Failed to register a new database connection pool：" + instanceKey);
            throw new ZAYKException("创建数据库连接池失败！" + instanceKey);
        }
    }

    public static void main(String[] args) throws ZAYKException {

        DbInstanceGetConnDTO instance = new DbInstanceGetConnDTO();
        instance.setDatabaseType(DbConstants.DB_TYPE_ORACLE);
        instance.setDatabaseIp("192.168.6.158");
        instance.setDatabasePort("1521");
        instance.setDatabaseServerName("orcl");
        instance.setDatabaseExampleType(":");
        instance.setDatabaseDba("user55");
        instance.setDatabaseDbaPassword("12345678");

        DynamicDataSourcePoolFactory factory = new DynamicDataSourcePoolFactory();
        Connection connection = factory.getConnection(instance);
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


package com.spms.common.pool;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接池
 * @author diq
 * @ClassName DynamicDataSourcePool
 * @date 2023-09-18
 */
@Slf4j
public class DynamicDataSourcePool {
    /**
     *     申明C3p0数据连接池变量
     */
    private ComboPooledDataSource pool = null;

    /**
     * 默认的构造方法
     *
     * @param userName    数据库用户名
     * @param pass        数据库密码
     * @param url         连接的url
     * @param driverClass 数据驱动
     */
    public DynamicDataSourcePool(String userName, String pass, String url, String driverClass) {
        try {
            //创建对象
            this.pool = new ComboPooledDataSource();
            //设置驱动
            this.pool.setDriverClass(driverClass);
            //设置连接的url
            this.pool.setJdbcUrl(url);
            //设置数据库用户名
            this.pool.setUser(userName);
            //设置数据库密码
            this.pool.setPassword(pass);
            //当连接池中的连接耗尽的时候c3p0一次同时获取的连接数
            this.pool.setAcquireIncrement(3);
            //连接关闭时默认将所有未提交的操作回滚
            this.pool.setAutoCommitOnClose(false);
            //获取连接失败后该数据源将申明已断开并永久关闭
            this.pool.setBreakAfterAcquireFailure(false);
            //当连接池用完时客户端调用getConnection()后等待获取新连接的时间，超时后将抛出SQLException,如设为0则无限期等待。单位毫秒。
            this.pool.setCheckoutTimeout(3000);
            //每60秒检查所有连接池中的空闲连接
            this.pool.setIdleConnectionTestPeriod(60);
            //初始化时获取10个连接，取值应在minPoolSize与maxPoolSize之间
            this.pool.setInitialPoolSize(10);
            //连接池中保留的最大连接数
            this.pool.setMaxPoolSize(40);
            //连接池最小连接数
            this.pool.setMinPoolSize(5);
            //最大空闲时间,60秒内未使用则连接被丢弃
            this.pool.setMaxIdleTime(60);
            //c3p0是异步操作的，缓慢的JDBC操作通过帮助进程完成。扩展这些操作可以有效的提升性能通过多线程实现多个操作同时被执行
            this.pool.setNumHelperThreads(3);
            log.info("数据库连接池初始化成功");
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }

    /**
     * 得到连接
     *
     * @return
     */
    public Connection getConnection() {
        try {
            return this.pool.getConnection();
        } catch (SQLException e) {
            log.info("获取连接异常");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 关闭
     */
    public void destroy() {
        if (null != this.pool) {
            this.pool.close();
        }
    }

}

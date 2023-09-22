package com.spms.common.pool.hikariPool;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MultiThreadExample {
    public static void main(String[] args) {
        // 创建线程1
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                // 线程1的任务
                for (int i = 0; i < 5; i++) {
                    System.out.println("Thread 1: " + i);
                    try {
                        createConn();

                    } catch (ZAYKException e) {
                        e.printStackTrace();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
            }
        });

        // 创建线程2
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                // 线程2的任务
                for (int i = 0; i < 5; i++) {
                    System.out.println("Thread 2: " + i);
                    try {
                        createConn();
                    } catch (ZAYKException e) {
                        e.printStackTrace();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
            }
        });
        // 创建线程2
        Thread thread3 = new Thread(new Runnable() {
            @Override
            public void run() {
                // 线程2的任务
                for (int i = 0; i < 5; i++) {
                    System.out.println("Thread 3: " + i);
                    try {
                        createConn();
                    } catch (ZAYKException e) {
                        e.printStackTrace();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                }
            }
        });

        // 启动线程1和线程2
        thread1.start();
        //thread2.start();
        //thread3.start();
    }

    public static void createConn() throws ZAYKException, SQLException {

        DbInstanceGetConnDTO instance = new DbInstanceGetConnDTO();
        //instance.setDatabaseType(DbConstants.DB_TYPE_ORACLE);
        //instance.setDatabaseIp("192.168.6.158");
        //instance.setDatabasePort("1521");
        //instance.setDatabaseServerName("orcl");
        //instance.setDatabaseExampleType(":");
        //instance.setDatabaseDba("user55");
        //instance.setDatabaseDbaPassword("12345678");
        instance.setDatabaseType(DbConstants.DB_TYPE_ORACLE);
        instance.setDatabaseIp("192.168.7.177");
        instance.setDatabasePort("1521");
        instance.setDatabaseServerName("orc1");
        instance.setDatabaseExampleType("/");
        instance.setDatabaseDba("system");
        instance.setDatabaseDbaPassword("12345678");

        DbConnectionPoolFactory factory = new DbConnectionPoolFactory();
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

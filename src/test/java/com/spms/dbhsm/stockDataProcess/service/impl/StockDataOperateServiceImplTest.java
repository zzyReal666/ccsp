package com.spms.dbhsm.stockDataProcess.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.dbhsm.stockDataProcess.domain.dto.ColumnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.TableDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@SpringBootTest
public class StockDataOperateServiceImplTest {

    static final StockDataOperateServiceImpl service = new StockDataOperateServiceImpl();
    static Connection conn = null;

    @Test
    public void mysql() throws Exception {
        mysqlTest();
    }

    @Test
    public void mysql57() throws Exception {
        initMysql57();
        DatabaseDTO dto = getMysql57DTO();
        service.stockDataOperate(dto, true);
        service.stockDataOperate(dto, false);
    }

    @Test
    public void postgres() throws Exception {
        initpg();
        DatabaseDTO dto = getPostgresDTO();
        service.stockDataOperate(dto, true);
        service.stockDataOperate(dto, false);
    }

    @Test
    public void sqlServer() throws Exception {
        initSqlServer();
        DatabaseDTO dto = getSqlServerDTO();
        service.stockDataOperate(dto, true);
        service.stockDataOperate(dto, false);
    }


    @Test
    public void clickHouse() throws Exception {
        initCK();
        DatabaseDTO dto = getCLickHouseDTO();
        service.stockDataOperate(dto, true);
        service.stockDataOperate(dto, false);
    }

    @Test
    public void kingBase() throws Exception {
        initKingBase();
        DatabaseDTO dto = getKingBaseDto();
        service.stockDataOperate(dto, true);
    }

    @Test
    public void hBase() throws Exception {
        initHbase();
        DatabaseDTO dto = getHbaseDTO();
        service.stockDataOperate(dto, true);
        service.stockDataOperate(dto, false);

    }

    private void initSqlServer() throws Exception {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        conn = DriverManager.getConnection("jdbc:jtds:sqlserver://192.168.6.64:1433/wzhtest;instance=WIN-I287SD6IN93", "sa", "server@2020");
        //指定schema

        //准备表
        Statement statement = conn.createStatement();
        try {
            statement.execute("DROP TABLE dbo.student");
        } catch (SQLException e) {
            log.info("表不存在，忽略");
        }
        statement.execute("CREATE TABLE dbo.student (id INT PRIMARY KEY, name VARCHAR(50), age INT, phone VARCHAR(20), address VARCHAR(100))");
        statement.execute("TRUNCATE TABLE dbo.student");

        //准备数据
        conn.setAutoCommit(false);
        PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO dbo.student (id, name, age, phone, address) VALUES (?,?,?,?,?)");
        for (int i = 0; i < 10000; i++) {
            preparedStatement.setInt(1, i);
            preparedStatement.setString(2, "张三" + i);
            preparedStatement.setInt(3, i % 55);
            preparedStatement.setString(4, "13800138000");
            preparedStatement.setString(5, "北京市朝阳区" + i);
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
        conn.commit();
        conn.setAutoCommit(true);
        preparedStatement.close();
        statement.close();
        conn.close();
    }

    private DatabaseDTO getSqlServerDTO() {

        //准备加密函数的入参
        ColumnDTO name = new ColumnDTO();
        name.setId(1L);
        name.setColumnName("name");
        name.setColumnDefinition(Collections.singletonMap("type", "VARCHAR(50)"));  //todo 还原必须要求有这个数据类型
        name.setComment("名字");
        name.setNotNull(true);
        name.setEncryptAlgorithm("TestAlg");
        name.setEncryptKeyIndex("1");

        ColumnDTO address = new ColumnDTO();
        address.setId(3L);
        address.setColumnName("address");
        address.setColumnDefinition(Collections.singletonMap("type", "VARCHAR(100)")); //todo 还原必须要求有这个数据类型
        address.setComment("地址");
        address.setNotNull(true);
        address.setEncryptAlgorithm("TestAlg");
        address.setEncryptKeyIndex("1");

        List<ColumnDTO> columns = Arrays.asList(name, address);

        TableDTO tableDTO = new TableDTO();
        tableDTO.setId(1L);
        tableDTO.setSchema("dbo");   //todo 注意！！！此处很重要
        tableDTO.setBatchSize(200);
        tableDTO.setTableName("student");
        tableDTO.setThreadNum(10);
        tableDTO.setColumnDTOList(columns);

        List<TableDTO> tables = Collections.singletonList(tableDTO);

        DatabaseDTO dto = new DatabaseDTO();
        dto.setId(123456L);
        dto.setDatabaseDba("sa");
        dto.setDatabaseIp("192.168.6.64");
        dto.setDatabasePort("1433");
        dto.setDatabaseDbaPassword("server@2020");
        dto.setConnectUrl("jdbc:jtds:sqlserver://192.168.6.64:1433/wzhtest;instance=WIN-I287SD6IN93");   //todo 这个url 到现在还没使用过，代理去连数据库需要用
        dto.setDatabaseType("SQLServer");
        dto.setDatabaseVersion("11.2");
        dto.setDatabaseName("wzhtest");
        dto.setInstanceType("");
        dto.setServiceUser("sa");
        dto.setServicePassword("server@2020");
        dto.setTableDTOList(tables);
        return dto;
    }


    private void initpg() throws Exception {
        Class.forName("org.postgresql.Driver");
        conn = DriverManager.getConnection("jdbc:postgresql://192.168.6.158:5432/zzydb?currentSchema=zzyschema", "postgres", "server@2020");
        //准备表
        Statement statement = conn.createStatement();
        statement.execute("DROP TABLE IF EXISTS student");
        statement.execute("CREATE TABLE IF NOT EXISTS student (id SERIAL PRIMARY KEY, name VARCHAR(50), age INT, phone VARCHAR(20), address VARCHAR(100))");
        statement.execute("TRUNCATE TABLE student");

        //准备数据
        conn.setAutoCommit(false);
        PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO student (name, age, phone, address) VALUES (?,?,?,?)");
        for (int i = 0; i < 10000; i++) {
            preparedStatement.setString(1, "张三" + i);
            preparedStatement.setInt(2, i % 55);
            preparedStatement.setString(3, "13800138000");
            preparedStatement.setString(4, "北京市朝阳区" + i);
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
        conn.commit();
        preparedStatement.close();
        statement.close();
        conn.close();
    }

    private DatabaseDTO getPostgresDTO() {
        //准备加密函数的入参
        ColumnDTO name = new ColumnDTO();
        name.setId(1L);
        name.setColumnName("name");
        name.setColumnDefinition(Collections.singletonMap("type", "VARCHAR(50)"));  //todo 还原必须要求有这个数据类型
        name.setComment("名字");
        name.setNotNull(true);
        name.setEncryptAlgorithm("TestAlg");
        name.setEncryptKeyIndex("1");

        ColumnDTO address = new ColumnDTO();
        address.setId(3L);
        address.setColumnName("address");
        address.setColumnDefinition(Collections.singletonMap("type", "VARCHAR(100)")); //todo 还原必须要求有这个数据类型
        address.setComment("地址");
        address.setNotNull(true);
        address.setEncryptAlgorithm("TestAlg");
        address.setEncryptKeyIndex("1");

        List<ColumnDTO> columns = Arrays.asList(name, address);

        TableDTO tableDTO = new TableDTO();
        tableDTO.setId(1L);
        tableDTO.setSchema("zzyschema");   //todo 注意！！！此处很重要
        tableDTO.setBatchSize(200);
        tableDTO.setTableName("student");
        tableDTO.setThreadNum(10);
        tableDTO.setColumnDTOList(columns);

        List<TableDTO> tables = Collections.singletonList(tableDTO);

        DatabaseDTO dto = new DatabaseDTO();
        dto.setId(123456L);
        dto.setDatabaseDba("postgres");
        dto.setDatabaseIp("192.168.6.158");
        dto.setDatabasePort("5432");
        dto.setDatabaseDbaPassword("server@2020");
        dto.setConnectUrl("jdbc:postgresql://192.168.6.158:5432/zzydb?currentSchema=zzyschema");   //todo 这个url 到现在还没使用过，代理去连数据库需要用
        dto.setDatabaseType("PostgresSQL");
        dto.setDatabaseVersion("11.2");
        dto.setDatabaseName("zzydb");
        dto.setInstanceType("SID");
        dto.setServiceUser("postgres");
        dto.setServicePassword("server@2020");
        dto.setTableDTOList(tables);
        return dto;
    }

    private void initCK() throws Exception {
        //准备连接
        Class.forName("com.clickhouse.jdbc.ClickHouseDriver");
        conn = DriverManager.getConnection("jdbc:clickhouse://192.168.7.113:18123/demo_ds_0", "default", "123456");
        //准备表
        Statement statement = conn.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS student (id Int64, name String, age Int8, phone String, address String) ENGINE = MergeTree ORDER BY id");
        statement.execute("TRUNCATE TABLE student");

        //准备数据
        PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO student (id, name, age, phone, address) VALUES (?,?,?,?,?)");
        for (int i = 0; i < 1000; i++) {
            preparedStatement.setLong(1, i);
            preparedStatement.setString(2, "张三" + i);
            preparedStatement.setInt(3, i % 55);
            preparedStatement.setString(4, "13800138000");
            preparedStatement.setString(5, "北京市朝阳区" + i);
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
        preparedStatement.close();
        statement.close();
        conn.close();
    }

    private static DatabaseDTO getCLickHouseDTO() {
        //准备加密函数的入参
        ColumnDTO name = new ColumnDTO();
        name.setId(1L);
        name.setColumnName("name");
        name.setComment("名字");
        name.setNotNull(true);
        name.setEncryptAlgorithm("TestAlg");
        name.setEncryptKeyIndex("1");

        ColumnDTO address = new ColumnDTO();
        address.setId(3L);
        address.setColumnName("address");
        address.setComment("地址");
        address.setNotNull(true);
        address.setEncryptAlgorithm("TestAlg");
        address.setEncryptKeyIndex("1");

        List<ColumnDTO> columns = Arrays.asList(name, address);


        TableDTO tableDTO = new TableDTO();
        tableDTO.setId(1L);
        tableDTO.setBatchSize(10);
        tableDTO.setTableName("student");
        tableDTO.setThreadNum(10);
        tableDTO.setColumnDTOList(columns);

        List<TableDTO> tables = Collections.singletonList(tableDTO);

        DatabaseDTO dto = new DatabaseDTO();
        dto.setId(123456L);
        dto.setDatabaseDba("default");
        dto.setDatabaseIp("192.168.7.113");
        dto.setDatabasePort("18123");
        dto.setDatabaseDbaPassword("123456");
        dto.setConnectUrl("jdbc:clickhouse://192.168.7.113:18123/demo_ds_0");
        dto.setDatabaseType("ClickHouse");
        dto.setDatabaseVersion("22.02");
        dto.setDatabaseName("demo_ds_0");
        dto.setInstanceType("SID");
        dto.setServiceUser("default");
        dto.setServicePassword("123456");
        dto.setTableDTOList(tables);
        return dto;
    }

    private static void initHbase() throws IOException {
        //准备连接
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "192.168.7.113");
        conf.set("zookeeper.znode.parent", "/hbase");
        conf.set("hbase.zookeeper.property.clientPort", "3181");

        UserGroupInformation.setConfiguration(conf);
        UserGroupInformation romoteUser = UserGroupInformation.createRemoteUser("hbase");
        UserGroupInformation.setLoginUser(romoteUser);

        org.apache.hadoop.hbase.client.Connection connection = ConnectionFactory.createConnection(conf);

        //删除存在的表（没有则不执行，会表不存在）
        Admin admin = connection.getAdmin();
        admin.disableTable(TableName.valueOf("student"));
        admin.deleteTable(TableName.valueOf("student"));
        //准备HBase表
        TableDescriptorBuilder tdesBuilder = TableDescriptorBuilder.newBuilder(TableName.valueOf("student"));
        ArrayList<ColumnFamilyDescriptor> cflist = new ArrayList<>();
        cflist.add(ColumnFamilyDescriptorBuilder.of("info1"));
        cflist.add(ColumnFamilyDescriptorBuilder.of("info2"));
        tdesBuilder.setColumnFamilies(cflist);
        TableDescriptor tableDescriptor = tdesBuilder.build();
        admin.createTable(tableDescriptor);
        ArrayList<Put> puts = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            String rowKey = "rowKey" + i;
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes("info1"), Bytes.toBytes("name"), Bytes.toBytes("zhangsan" + i));
            put.addColumn(Bytes.toBytes("info2"), Bytes.toBytes("age"), Bytes.toBytes("20" + i));
            put.addColumn(Bytes.toBytes("info2"), Bytes.toBytes("address"), Bytes.toBytes("address" + i));
            puts.add(put);
        }
        Table table = connection.getTable(TableName.valueOf("student"));
        table.put(puts);
        table.close();
        connection.close();
    }

    private static DatabaseDTO getHbaseDTO() {
        //dto
        //准备加密函数的入参
        ColumnDTO name = new ColumnDTO();
        name.setId(1L);
        name.setColumnName("info1:name");
        name.setEncryptAlgorithm("TestAlg");
        name.setEncryptKeyIndex("1");

        ColumnDTO address = new ColumnDTO();
        address.setId(3L);
        address.setColumnName("info2:address");
        address.setEncryptAlgorithm("TestAlg");
        address.setEncryptKeyIndex("1");

        List<ColumnDTO> columns = Arrays.asList(name, address);

        TableDTO tableDTO = new TableDTO();
        tableDTO.setId(1L);
        tableDTO.setBatchSize(2000);
        tableDTO.setTableName("student");
        tableDTO.setThreadNum(10);
        tableDTO.setColumnDTOList(columns);

        List<TableDTO> tables = Collections.singletonList(tableDTO);

        DatabaseDTO dto = new DatabaseDTO();
        dto.setDbStorageMode(DatabaseDTO.DbStorageMode.COLUMN);
        dto.setId(123456L);
        dto.setDatabaseIp("192.168.7.113");
        dto.setDatabasePort("3181");
        dto.setDatabaseType("HBase");
        dto.setDatabaseName("hbase");
        dto.setDatabaseDba("hbase");
        dto.setDatabaseDbaPassword("");
        dto.setServiceUser("hbase");
        dto.setServicePassword("");
        dto.setTableDTOList(tables);

        return dto;
    }

    private DatabaseDTO getKingBaseDto() {
        //准备加密函数的入参
        ColumnDTO name = new ColumnDTO();
        name.setId(1L);
        name.setColumnName("name");
        name.setColumnDefinition(Collections.singletonMap("type", "VARCHAR(50)"));
        name.setComment("名字");
        name.setNotNull(true);
        name.setEncryptAlgorithm("TestAlg");
        name.setEncryptKeyIndex("1");

        ColumnDTO address = new ColumnDTO();
        address.setId(3L);
        address.setColumnName("address");
        address.setComment("地址");
        address.setNotNull(true);
        address.setEncryptAlgorithm("TestAlg");
        address.setEncryptKeyIndex("1");

        List<ColumnDTO> columns = Arrays.asList(name, address);

        TableDTO tableDTO = new TableDTO();
        tableDTO.setId(1L);
        tableDTO.setSchema("public");
        tableDTO.setBatchSize(200);
        tableDTO.setTableName("student");
        tableDTO.setThreadNum(10);
        tableDTO.setColumnDTOList(columns);

        List<TableDTO> tables = Collections.singletonList(tableDTO);

        DatabaseDTO dto = new DatabaseDTO();
        dto.setId(123456L);
        dto.setDatabaseDba("SYSTEM");
        dto.setDatabaseIp("192.168.7.113");
        dto.setDatabasePort("54321");
        dto.setDatabaseDbaPassword("123456");
        dto.setConnectUrl("jdbc:kingbase8://192.168.7.113:54321/ZZY");
        dto.setDatabaseType("KingBase");
        dto.setDatabaseVersion("8.0");
        dto.setDatabaseName("ZZY");
        dto.setInstanceType("SID");
        dto.setServiceUser("SYSTEM");
        dto.setServicePassword("123456");
        dto.setTableDTOList(tables);
        return dto;
    }

    private static void initKingBase() throws ClassNotFoundException, SQLException {
        Class.forName("com.kingbase8.Driver");
        conn = DriverManager.getConnection("jdbc:kingbase8://192.168.7.113:54321/ZZY", "SYSTEM", "123456");

        conn.createStatement().execute("DROP TABLE IF EXISTS student");
        //如果不存在则创建学生表
        String createSQL = "CREATE TABLE IF NOT EXISTS student\n" + "(\n" + "    id      INTEGER PRIMARY KEY,\n" + "    name    VARCHAR(100) NOT NULL,\n" + "    age     INTEGER      NOT NULL,\n" + "    address varchar(100)\n" + ");";
        conn.createStatement().execute(createSQL);

        //添加数据
        conn.setAutoCommit(false);
        String insertSQL = "INSERT INTO student (id, name, age, address) VALUES (?,?,?,?)";
        PreparedStatement preparedStatement = conn.prepareStatement(insertSQL);
        for (int i = 0; i < 20000; i++) {
            preparedStatement.setInt(1, i);
            preparedStatement.setString(2, "name" + i);
            preparedStatement.setInt(3, i % 55);
            preparedStatement.setString(4, "address" + i);
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
        conn.commit();
        conn.setAutoCommit(true);
        log.info("===================init environment success");
    }

    private static void mysqlTest() throws Exception {
        DatabaseDTO databaseDTO = getMysqlDTO();
        StockDataOperateServiceImpl service = new StockDataOperateServiceImpl();
        //开一个线程
        Thread operate = new Thread(() -> {
            try {
                service.stockDataOperate(databaseDTO, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        operate.start();
        Thread process = new Thread(() -> {
            try {
                while (true) {
                    int i = service.queryProgress(String.valueOf(1));
                    System.out.println("当前执行进度 百分之：" + i);
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        process.setDaemon(true);
        process.start();

        operate.join();
    }

    private static DatabaseDTO getMysqlDTO() throws ZAYKException, SQLException, InterruptedException {
        ColumnDTO name = new ColumnDTO();
        name.setId(1L);
        name.setColumnName("name");
        name.setComment("名字");
        name.setNotNull(true);
        name.setEncryptAlgorithm("TestAlg");
        name.setEncryptKeyIndex("1");

        //age
//        ColumnDTO age = new ColumnDTO();
//        age.setId(2L);
//        age.setColumnName("age");
//        age.setComment("年龄");
//        age.setNotNull(false);
//        age.setEncryptAlgorithm("TestAlg");
//        age.setEncryptKeyIndex("1");

//        //address
//        ColumnDTO address = new ColumnDTO();
//        address.setId(3L);
//        address.setColumnName("address");
//        address.setComment("地址");
//        address.setNotNull(false);
//        address.setEncryptAlgorithm("TestAlg");
//        address.setEncryptKeyIndex("1");

        //phone
        ColumnDTO phone = new ColumnDTO();
        phone.setId(3L);
        phone.setColumnName("phone");
        phone.setComment("电话");
        phone.setNotNull(false);
        phone.setEncryptAlgorithm("TestAlg");
        phone.setEncryptKeyIndex("1");

        List<ColumnDTO> columns = Arrays.asList(name, phone);

        TableDTO tableDTO = new TableDTO();
        tableDTO.setId(1L);
        tableDTO.setBatchSize(10);
        tableDTO.setTableName("student");
        tableDTO.setThreadNum(10);
        tableDTO.setColumnDTOList(columns);

        List<TableDTO> tables = Collections.singletonList(tableDTO);


        DatabaseDTO dto = new DatabaseDTO();
        dto.setId(10086L);
        dto.setDatabaseDba("root");
        dto.setDatabaseIp("192.168.7.113");
        dto.setDatabasePort("13306");
        dto.setDatabaseDbaPassword("123456");
        dto.setConnectUrl("jdbc:mysql://192.168.7.113:13306/encrypt");
        dto.setDatabaseType("MySQL");
        dto.setDatabaseVersion("8.3.0");
        dto.setDatabaseName("encrypt");
        dto.setInstanceType("SID");
        dto.setServiceUser("root");
        dto.setServicePassword("123456");
        dto.setTableDTOList(tables);
        return dto;
    }

    private void initMysql57() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        conn = DriverManager.getConnection("jdbc:mysql://192.168.6.64:3306/zzyTest", "root", "server@2020");
        //准备表
        Statement statement = conn.createStatement();
        statement.execute("DROP TABLE IF EXISTS student");
        statement.execute("CREATE TABLE IF NOT EXISTS student (id INT primary key, name VARCHAR(50), age INT, phone VARCHAR(20), address VARCHAR(100))");
        statement.execute("TRUNCATE TABLE student");

        //准备数据
        conn.setAutoCommit(false);
        PreparedStatement preparedStatement = conn.prepareStatement("INSERT INTO student (id, name, age, phone, address) VALUES (?,?,?,?,?)");
        for (int i = 0; i < 10000; i++) {
            preparedStatement.setInt(1, i);
            preparedStatement.setString(2, "张三" + i);
            preparedStatement.setInt(3, i % 55);
            preparedStatement.setString(4, "13800138000");
            preparedStatement.setString(5, "北京市朝阳区" + i);
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
        conn.commit();

        //关闭资源
        preparedStatement.close();
        statement.close();
        conn.close();
    }

    private static DatabaseDTO getMysql57DTO() throws ZAYKException, SQLException, InterruptedException {
        ColumnDTO name = new ColumnDTO();
        name.setId(1L);
        name.setColumnName("name");
        name.setColumnDefinition(Collections.singletonMap("type", "VARCHAR(50)"));
        name.setComment("名字");
        name.setNotNull(true);
        name.setEncryptAlgorithm("TestAlg");
        name.setEncryptKeyIndex("1");

        //age
        ColumnDTO age = new ColumnDTO();
        age.setId(2L);
        age.setColumnName("age");
        age.setColumnDefinition(Collections.singletonMap("type", "INT"));
        age.setComment("年龄");
        age.setNotNull(false);
        age.setEncryptAlgorithm("TestAlg");
        age.setEncryptKeyIndex("1");

        //address
        ColumnDTO address = new ColumnDTO();
        address.setColumnDefinition(Collections.singletonMap("type", "VARCHAR(100)"));
        address.setId(3L);
        address.setColumnName("address");
        address.setComment("地址");
        address.setNotNull(false);
        address.setEncryptAlgorithm("TestAlg");
        address.setEncryptKeyIndex("1");

        //phone
        ColumnDTO phone = new ColumnDTO();
        phone.setId(3L);
        phone.setColumnName("phone");
        phone.setColumnDefinition(Collections.singletonMap("type", "VARCHAR(20)"));
        phone.setComment("电话");
        phone.setNotNull(false);
        phone.setEncryptAlgorithm("TestAlg");
        phone.setEncryptKeyIndex("1");

        List<ColumnDTO> columns = Arrays.asList(name, phone);

        TableDTO tableDTO = new TableDTO();
        tableDTO.setId(1L);
        tableDTO.setBatchSize(100);
        tableDTO.setTableName("student");
        tableDTO.setThreadNum(10);
        tableDTO.setColumnDTOList(columns);

        List<TableDTO> tables = Collections.singletonList(tableDTO);


        DatabaseDTO dto = new DatabaseDTO();
        dto.setId(10086L);
        dto.setDatabaseDba("root");
        dto.setDatabaseIp("192.168.6.64");
        dto.setDatabasePort("3306");
        dto.setDatabaseDbaPassword("server@2020");
        dto.setConnectUrl("jdbc:mysql://192.168.6.64:3306/zzyTest");
        dto.setDatabaseType("Mysql57");
        dto.setDatabaseVersion("5.7");
        dto.setDatabaseName("zzyTest");
        dto.setInstanceType("SID");
        dto.setServiceUser("root");
        dto.setServicePassword("server@2020");
        dto.setTableDTOList(tables);
        return dto;
    }


}
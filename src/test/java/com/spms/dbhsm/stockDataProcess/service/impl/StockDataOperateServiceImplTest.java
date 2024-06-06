package com.spms.dbhsm.stockDataProcess.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.dbhsm.stockDataProcess.domain.dto.ColumnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.TableDTO;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StockDataOperateServiceImplTest {

    static final StockDataOperateServiceImpl service = new StockDataOperateServiceImpl();
    static Connection conn = null;

    @Test
    public void mysql() throws Exception {
        mysqlTest();
    }

    @Test
    public void clickHouse() throws Exception {
        StockDataOperateServiceImpl service = new StockDataOperateServiceImpl();
        DatabaseDTO dto = getCLickHouseDTO();
        service.stockDataOperate(dto, true);
    }

    @Test
    public void kingBase() throws Exception {
        initEnvironment();
        DatabaseDTO dto = getKingBaseDto();
        service.stockDataOperate(dto, true);
    }

    private DatabaseDTO getKingBaseDto() {
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

    private static void initEnvironment() throws ClassNotFoundException, SQLException {
        Class.forName("com.kingbase8.Driver");
        conn = DriverManager.getConnection("jdbc:kingbase8://192.168.7.113:54321/TEST", "SYSTEM", "123456");

        conn.createStatement().execute("DROP TABLE IF EXISTS student");
        //如果不存在则创建学生表
        String createSQL = "CREATE TABLE IF NOT EXISTS student\n" +
                "(\n" +
                "    id      INTEGER PRIMARY KEY,\n" +
                "    name    VARCHAR(100) NOT NULL,\n" +
                "    age     INTEGER      NOT NULL,\n" +
                "    address varchar(100)\n" +
                ");";
        conn.createStatement().execute(createSQL);

        //添加数据
        conn.setAutoCommit(false);
        String insertSQL = "INSERT INTO student (id, name, age, address) VALUES (?,?,?,?)";
        PreparedStatement preparedStatement = conn.prepareStatement(insertSQL);
        for (int i = 0; i < 200000; i++) {
            preparedStatement.setInt(1, i);
            preparedStatement.setString(2, "name" + i);
            preparedStatement.setInt(3, i % 55);
            preparedStatement.setString(4, "address" + i);
            preparedStatement.addBatch();
        }
        preparedStatement.executeBatch();
        conn.setAutoCommit(true);
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

    private void initEnvironment(DatabaseDTO databaseDTO) throws Exception {

        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection("jdbc:mysql://192.168.7.113:13306/encrypt", "root", "123456");
        Statement statement = conn.createStatement();


        //删除旧表
        String dropTable = "drop table testEnc";
        statement.execute(dropTable);

        //创建原始表
        String createTable = "create table " + databaseDTO.getTableDTOList().get(0).getTableName() + "\n" + "(\n" + "    id      int primary key,\n" + "    name    varchar(50),\n" + "    age     int,\n" + "    address varchar(50),\n" + "    phone   varchar(50)\n" + ")";

        statement.execute(createTable);

        //添加1000条测试数据
        String insertSql = "insert into " + databaseDTO.getTableDTOList().get(0).getTableName() + "(id,name,age,address,phone) values(?,?,?,?,?)";
        PreparedStatement ps = conn.prepareStatement(insertSql);
        conn.setAutoCommit(false);
        for (int i = 0; i < 1000; i++) {
            ps.setInt(1, i);
            ps.setString(2, "name" + i);
            ps.setInt(3, i % 55);
            ps.setString(4, "address" + i);
            ps.setString(5, "phone+i");
            ps.addBatch();
        }
        ps.executeBatch();
        conn.commit();
        conn.setAutoCommit(true);
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

}
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

    @Test
    public void stockDataOperate() throws Exception {
        DatabaseDTO databaseDTO = getParams();
//        initEnvironment(databaseDTO);
        StockDataOperateServiceImpl service = new StockDataOperateServiceImpl();
        service.stockDataOperate(databaseDTO, true);
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

    private static DatabaseDTO getParams() throws ZAYKException, SQLException, InterruptedException {
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

    @Test
    public void pause() {
    }

    @Test
    public void resume() {
    }

    @Test
    public void queryProgress() {
    }
}
package com.spms.dbhsm.stockDataProcess.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.dbhsm.stockDataProcess.domain.dto.ColumnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.TableDTO;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StockDataOperateServiceImplTest {

    @Test
    public void stockDataOperate() throws ZAYKException, SQLException, InterruptedException {
        DatabaseDTO databaseDTO = getParams();
        StockDataOperateServiceImpl service = new StockDataOperateServiceImpl();
        service.stockDataOperate(databaseDTO, true);
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
        ColumnDTO age = new ColumnDTO();
        age.setId(2L);
        age.setColumnName("age");
        age.setComment("年龄");
        age.setNotNull(false);
        age.setEncryptAlgorithm("TestAlg");
        age.setEncryptKeyIndex("1");

        //address
        ColumnDTO address = new ColumnDTO();
        address.setId(3L);
        address.setColumnName("address");
        address.setComment("地址");
        address.setNotNull(false);
        address.setEncryptAlgorithm("TestAlg");
        address.setEncryptKeyIndex("1");

        List<ColumnDTO> columns = Arrays.asList(name, age, address);

        TableDTO tableDTO = new TableDTO();
        tableDTO.setId(1L);
        tableDTO.setBatchSize(10);
        tableDTO.setTableName("testEnc");
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
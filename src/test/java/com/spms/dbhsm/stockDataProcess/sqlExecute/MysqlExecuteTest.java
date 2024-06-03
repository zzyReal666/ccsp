package com.spms.dbhsm.stockDataProcess.sqlExecute;


import com.spms.common.enums.DatabaseTypeEnum;
import com.spms.common.spi.typed.TypedSPIRegistry;
import com.spms.dbhsm.stockDataProcess.domain.dto.AddColumnsDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class MysqlExecuteTest {

    SqlExecuteSPI executeSPI;
    Connection connection;

    @Before
    public void getExecute() throws SQLException {
        Optional<SqlExecuteSPI> registeredService = TypedSPIRegistry.findRegisteredService(SqlExecuteSPI.class, DatabaseTypeEnum.MySQL.name());
        assert registeredService.isPresent();
        executeSPI = registeredService.get();

        //JDBC 获取连接
        connection = DriverManager.getConnection("jdbc:mysql://192.168.7.113:13306/encrypt", "root", "123456");

    }

    @Test
    public void getPK() throws SQLException {
        String primaryKey = executeSPI.getPrimaryKey(connection, "encrypt", "t1");
        assert primaryKey.equals("id");
    }

    @Test
    public void addTempColumns() {
        List<AddColumnsDTO> addColumnsDTOS = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AddColumnsDTO addColumnsDTO = new AddColumnsDTO(true,"column" + i, "colunmComment" + i, i % 2 == 0, new HashMap<>());
            addColumnsDTOS.add(addColumnsDTO);
        }
        executeSPI.addTempColumn(connection, "t1", addColumnsDTOS);
        //todo 数据库查看是否添加成功
    }

    @Test
    public void count() {
        int count = executeSPI.count(connection, "t1");
        log.info("count:{}", count);
    }

    @Test
    public void selectColumn() {
        List<String> list = new ArrayList<>();
        list.add("id");
        list.add("name");
        list.add("age");
        list.add("email");
        executeSPI.selectColumn(connection, "t1",list , 2, 0)
                .forEach(map -> log.info("map:{}", map));
    }

    @Test
    public void batchUpdate() {

        ArrayList<Map<String,String>> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            HashMap<String, String> map = new LinkedHashMap<>();
            map.put("id", String.valueOf(i+1));
            for (int j = 0; j < 5; j++) {
                map.put("column"+j, "update" + j);
            }
            list.add(map);
        }
        executeSPI.batchUpdate(connection, "t1", list, 0, 0);
    }

    @Test
    public void rename() {
        executeSPI.renameColumn(connection, "encrypt", "t1", "b", "a");
    }

    @Test
    public void dropColumns() {
        List<String> list = new ArrayList<>();
        list.add("column0_temp$zAyK_dbEnc_Mysql_");
        list.add("column1_temp$zAyK_dbEnc_Mysql_");
        list.add("column2_temp$zAyK_dbEnc_Mysql_");
        list.add("column3_temp$zAyK_dbEnc_Mysql_");
        list.add("column4_temp$zAyK_dbEnc_Mysql_");
        executeSPI.dropColumn(connection, "t1", list);
    }
}
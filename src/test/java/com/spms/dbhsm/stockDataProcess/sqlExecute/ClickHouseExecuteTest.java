package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.enums.DatabaseTypeEnum;
import com.spms.common.spi.typed.TypedSPIRegistry;
import com.spms.dbhsm.stockDataProcess.domain.dto.AddColumnsDTO;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClickHouseExecuteTest {

    SqlExecuteSPI executeSPI;
    Connection connection;

    @Before
    public void getExecute() throws SQLException {
        Optional<SqlExecuteSPI> registeredService = TypedSPIRegistry.findRegisteredService(SqlExecuteSPI.class, DatabaseTypeEnum.ClickHouse.name());
        assert registeredService.isPresent();
        executeSPI = registeredService.get();
        //JDBC 获取连接
        connection = DriverManager.getConnection("jdbc:clickhouse://192.168.7.113:18123/demo_ds_0", "default", "123456");

    }

    @Test
    public void getPrimaryKey() throws SQLException {
        String primaryKey = executeSPI.getPrimaryKey(connection, "demo_ds_0", "student");
        assert primaryKey.equals("id");
    }

    @Test
    public void getTempColumnSuffix() {
    }

    @Test
    public void addTempColumn() throws SQLException {

        // 对于clickhouse来说 新增临时表 并且修改需要加密的字段数据类型为String
        List<AddColumnsDTO> addColumnsDTOS = new ArrayList<>();
        AddColumnsDTO name = new AddColumnsDTO();
        name.setColumnName("name");
        AddColumnsDTO address = new AddColumnsDTO();
        address.setColumnName("address");
        addColumnsDTOS.add(name);
        addColumnsDTOS.add(address);
        executeSPI.addTempColumn(connection, "student", addColumnsDTOS);


    }

    @Test
    public void count() {
        int count = executeSPI.count(connection, "student");
        System.out.println("count:" + count);
    }

    @Test
    public void selectColumn() {
        List<String> list = new ArrayList<>();
        list.add("id");
        list.add("name");
        list.add("age");
        list.add("phone");
        list.add("address");

        List<Map<String, String>> student = executeSPI.selectColumn(connection, "student", list, 2, 999);

        student.forEach(map -> System.out.println("map:" + map));

    }

    //批量更新-实际执行的是插入新数据
    @Test
    public void columnUpdate() throws SQLException {

        addTempColumn();

        List<String> list = new ArrayList<>();
        list.add("id");
        list.add("name");
        list.add("phone");
        list.add("address");
        List<Map<String, String>> data = executeSPI.selectColumn(connection, "student", list, 10, 1000);

        Map<String, List<Map<String, String>>> dataIncludeCipher = new HashMap<>();
        data.forEach(map -> {
            map.forEach((fieldName, plain) -> {
                Map<String, String> valueMap = new HashMap<>();
                if (fieldName.equals("id")) {
                    valueMap.put("plain", plain);
                } else {
                    valueMap.put("plain", plain);
                    valueMap.put("cipher", plain+"--cipher");
                }
                // 将加密后的数据直接放入 transformedData
                dataIncludeCipher.putIfAbsent(fieldName, new ArrayList<>());
                dataIncludeCipher.get(fieldName).add(valueMap);
            });
        });

        executeSPI.columnBatchUpdate(connection, "student","id", dataIncludeCipher, 1000, 1000);

    }

    @Test
    public void renameColumn() {
        executeSPI.renameColumn(connection,"demo_ds_0", "student", "", "");
    }

    @Test
    public void dropColumn() {
        executeSPI.dropColumn(connection, "student", new ArrayList<>());
    }


}
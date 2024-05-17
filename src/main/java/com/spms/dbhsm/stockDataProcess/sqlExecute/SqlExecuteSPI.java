package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.spi.typed.TypedSPI;
import com.spms.dbhsm.stockDataProcess.domain.dto.AddColumnsDTO;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/4/28 17:5988
 */
public interface SqlExecuteSPI extends TypedSPI {


    /**
     * 查询主键 没有主键返回null 有主键返回主键字段名
     */
    String getPrimaryKey(Connection conn, String schema, String table);


    /**
     * 获取临时字段名字
     */
    String getTempColumnName(String column);


    /**
     * 修改表结构，新增临时字段，临时字段在原来字段后面
     */
    void addTempColumn(Connection conn, String table, List<AddColumnsDTO> addColumnsDtoList);


    /**
     * 数据总条数
     */
    int count(Connection conn, String table);


    /**
     * 分页查询指定的字段，可以指定多个字段，必须查询主键字段，主键字段放在第一个
     */
    List<Map<String, String>> selectColumn(Connection conn, String table, List<String> columns, int limit, int offset);


    /**
     * 批量更新
     */
    void batchUpdate(Connection conn, String table, List<Map<String, String>> data);

    /**
     * 字段重新命名
     */
    void renameColumn(Connection conn, String table, String oldColumn, String newColumn);


    /**
     * 删除字段
     */
    void dropColumn(Connection conn, String table, List<String> columns);


}
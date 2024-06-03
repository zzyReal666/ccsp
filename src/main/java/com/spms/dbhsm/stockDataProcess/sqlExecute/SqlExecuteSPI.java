package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.spi.typed.TypedSPI;
import com.spms.dbhsm.stockDataProcess.domain.dto.AddColumnsDTO;

import java.sql.Connection;
import java.sql.SQLException;
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
    String getPrimaryKey(Connection conn, String schema, String table) throws SQLException;


    /**
     * 获取临时表、临时字段的前｜后缀 注意，每个数据库对表名、字段的字符限制不同，需要根据实际测试情况确定
     */
    String getTempColumnSuffix();

    /**
     * 新增临时字段，注意 此处的table 为原始字段的名字
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
     * 批量更新 实际上是插入数据到新的字段
     */
    void batchUpdate(Connection conn, String table, List<Map<String, String>> data, int limit, int offset);

    /**
     * 列式的批量更新
     */
    default void columnBatchUpdate(Object... args) throws SQLException {

    }

    /**
     * 字段重新命名
     */
    void renameColumn(Connection conn, String schema, String table, String oldColumn, String newColumn);


    /**
     * 删除字段
     */
    void dropColumn(Connection conn, String table, List<String> columns);


}

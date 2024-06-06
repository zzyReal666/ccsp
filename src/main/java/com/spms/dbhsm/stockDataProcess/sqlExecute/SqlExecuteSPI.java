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
     *
     * @param conn   数据库连接
     * @param schema 数据库schema 名称
     * @param table  表名
     * @return 主键字段名 没有主键返回null
     * @throws SQLException 数据库异常
     */
    String getPrimaryKey(Connection conn, String schema, String table) throws SQLException;

    /**
     * 获取临时表、临时字段的前｜后缀
     * 注意，每个数据库对表名、字段的字符限制不同，需要根据实际测试情况确定
     * 例如 ： 需要新建的临时字段或者临时表的原名为Foo 后缀为suffix，则新建的临时名字为 Foosuffix
     * 交换新旧名字的时候 Foo更改为suffixFoo，Foosuffix更改为Foo
     *
     * @return 临时表、临时字段的前｜后缀
     */
    String getTempColumnSuffix();

    /**
     * 新增临时字段，注意 此处的table 为原始字段的名字
     *
     * @param conn              数据库连接
     * @param table             原始表名
     * @param addColumnsDtoList 新增字段列表 注意，此处为原名，添加后缀在实现类进行处理
     */
    void addTempColumn(Connection conn, String table, List<AddColumnsDTO> addColumnsDtoList);


    /**
     * 数据总条数
     *
     * @param conn  数据库连接
     * @param table 表名
     * @return 数据总条数
     */
    int count(Connection conn, String table);


    /**
     * 分页查询指定的字段，需要排序，可以指定多个字段，必须查询主键字段，主键字段放在第一个
     *
     * @param conn    数据库连接
     * @param table   表名
     * @param columns 字段列表 主键字段必须放在第一个
     * @param limit   查询条数
     * @param offset  偏移量
     * @return 查询结果 Map<字段名，字段值>
     */
    List<Map<String, String>> selectColumn(Connection conn, String table, List<String> columns, int limit, int offset);


    /**
     * 批量更新 实际上是插入数据到新的字段
     *
     * @param conn  数据库连接
     * @param table 表名
     * @param data  加密后的数据 list每一条是一行数据，map<字段名,密文>,map第一个是主键：主键明文
     */
    void batchUpdate(Connection conn, String table, List<Map<String, String>> data);

    /**
     * 列式的批量更新 不是必须实现
     *
     * @param args 参数 动态穿参
     */
    default void columnBatchUpdate(Object... args) {

    }

    /**
     * 字段重新命名
     *
     * @param conn      数据库连接
     * @param schema    数据库schema名
     * @param table     表名
     * @param oldColumn 旧字段名
     * @param newColumn 新字段名
     */
    void renameColumn(Connection conn, String schema, String table, String oldColumn, String newColumn);


    /**
     * 删除字段
     *
     * @param conn    数据库连接
     * @param table   表名
     * @param columns 字段列表
     */
    void dropColumn(Connection conn, String table, List<String> columns);


}

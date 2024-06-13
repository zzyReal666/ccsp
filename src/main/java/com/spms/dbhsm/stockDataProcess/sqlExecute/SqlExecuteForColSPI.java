package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.spi.typed.TypedSPI;

import java.util.Collection;

/**
 * @author zzypersonally@gmail.com
 * @description 列式存储数据库 SPI
 * @since 2024/6/11 15:38
 */
public interface SqlExecuteForColSPI extends TypedSPI {

    /**
     * 获取连接
     */
    Object getConnection(Object... args);

    /**
     * 获取前、后缀
     */
    String getPrefixOrSuffix();

    /**
     * 新建表
     */
    void createTable(Object... args);

    /**
     * 统计数据条数
     */
    int count(Object... args);

    /**
     * 查询数据 分页查询
     */
    Collection selectData(Object... args);

    /**
     * 插入数据
     */
    void insertData(Object... args);


    /**
     * 删除表
     */
    void dropTable(Object... args);


    /**
     * 修改表名字
     */
    void renameTable(Object... args);


}

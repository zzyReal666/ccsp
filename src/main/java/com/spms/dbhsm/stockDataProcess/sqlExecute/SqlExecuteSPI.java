package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.spi.typed.TypedSPI;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/4/28 17:5988
 */
public interface SqlExecuteSPI extends TypedSPI {


    /**
     * 查询主键 返回主键字段
     */
    String queryPrimaryKey(String schema, String table);





}

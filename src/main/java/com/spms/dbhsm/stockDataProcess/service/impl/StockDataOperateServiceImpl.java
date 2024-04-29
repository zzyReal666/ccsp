package com.spms.dbhsm.stockDataProcess.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.common.spi.typed.TypedSPIRegistry;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.stockDataProcess.adapter.DatabaseToDbInstanceGetConnDTOAdapter;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import com.spms.dbhsm.stockDataProcess.service.StockDataOperateService;
import com.spms.dbhsm.stockDataProcess.sqlExecute.SqlExecuteSPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/4/28 16:45
 */
@Slf4j
@Service
public class StockDataOperateServiceImpl implements StockDataOperateService {

    /**
     * 存量数据加密/解密
     *
     * @param databaseDTO 数据库信息
     * @param operateType 操作类型 true:加密 false:解密
     */
    @Async
    @Override
    public void stockDataOperate(DatabaseDTO databaseDTO, boolean operateType) throws ZAYKException, SQLException {
        //获取连接
        Connection dbaConn = getConnction(databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType.DBA);
        Connection serviceConn = getConnction(databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType.SERVICE);

        //获取SQL执行器
        Optional<SqlExecuteSPI> registeredService = TypedSPIRegistry.findRegisteredService(SqlExecuteSPI.class, databaseDTO.getDatabaseType());


        //是否有主键，没有主键不能加密

        //修改表结构

        //加密/解密

        //删除临时字段
    }

    private static Connection getConnction(DatabaseDTO databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType connType) throws ZAYKException, SQLException {
        DbInstanceGetConnDTO dbInstanceGetConnDTO = DatabaseToDbInstanceGetConnDTOAdapter.adapter(databaseDTO, connType);
        DbConnectionPoolFactory factory = new DbConnectionPoolFactory();
        return factory.getConnection(dbInstanceGetConnDTO);
    }

    @Override
    public void pause(String tableId) {

    }

    @Override
    public void resume(String tableId) {

    }

    @Override
    public void queryProgress(String tableId) {

    }
}

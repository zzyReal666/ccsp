package com.spms.dbhsm.stockDataProcess.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.common.spi.typed.TypedSPIRegistry;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.stockDataProcess.adapter.DatabaseToDbInstanceGetConnDTOAdapter;
import com.spms.dbhsm.stockDataProcess.domain.dto.AddColumnsDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.ColumnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.TableDTO;
import com.spms.dbhsm.stockDataProcess.service.StockDataOperateService;
import com.spms.dbhsm.stockDataProcess.sqlExecute.SqlExecuteSPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/4/28 16:45
 */
@Slf4j
@Service
public class StockDataOperateServiceImpl implements StockDataOperateService {


    /**
     * 线程池
     */
    private ExecutorService executor = null;

    /**
     * 暂停标志
     */
    private volatile boolean paused = false;

    /**
     * 执行进度 百分比
     */
    private volatile int progress = 0;


    /**
     * 存量数据加密/解密
     *
     * @param databaseDTO 数据库信息
     * @param operateType 操作类型 true:加密 false:解密
     */
    @Async
    @Override
    public void stockDataOperate(DatabaseDTO databaseDTO, boolean operateType) throws ZAYKException, SQLException {
        //表对象
        TableDTO tableDTO = databaseDTO.getTableDTOList().get(0);

        //获取连接 DBA用于修改结构 业务账号用于修改数据
        Connection dbaConn = getConnction(databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType.DBA);
        Connection serviceConn = getConnction(databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType.SERVICE);

        //获取SQL执行器
        Optional<SqlExecuteSPI> registeredService = TypedSPIRegistry.findRegisteredService(SqlExecuteSPI.class, databaseDTO.getDatabaseType());
        if (!registeredService.isPresent()) {
            throw new ZAYKException("未找到对应的数据库类型");
        }
        SqlExecuteSPI sqlExecute = registeredService.get();

        //是否有主键，没有主键不能加密
        String primaryKey = sqlExecute.hasPrimaryKey(dbaConn, databaseDTO.getDatabaseName(), tableDTO.getTableName());
        if (primaryKey == null) {
            throw new ZAYKException("表没有主键，不能加密");
        }

        //修改表结构 新增临时字段
        ArrayList<AddColumnsDTO> addColumnsDTOS = new ArrayList<>();
        tableDTO.getColumnDTOList().forEach(columnDTO -> addColumnsDTOS.add(convertToDTO(columnDTO)));
        sqlExecute.addTempColumn(dbaConn, tableDTO.getTableName(), addColumnsDTOS);

        //分块 加/解密
        blockOperate(operateType, sqlExecute, serviceConn, tableDTO, primaryKey);

        //删除临时字段
        sqlExecute.dropColumn(dbaConn, tableDTO.getTableName(), tableDTO.getColumnDTOList().stream().map(ColumnDTO::getColumnName).collect(Collectors.toList()));
    }

    /**
     * 分块 加/解密
     *
     * @param operateType true:加密 false:解密
     * @param sqlExecute  sql执行器
     * @param serviceConn 业务连接
     * @param tableDTO    表信息
     * @param primaryKey  主键
     */
    private void blockOperate(boolean operateType, SqlExecuteSPI sqlExecute, Connection serviceConn, TableDTO tableDTO, String primaryKey) {
        int count = sqlExecute.count(serviceConn, tableDTO.getTableName()); //总条数
        int limit = tableDTO.getBatchSize();  //块大小，每个线程每次执行的条数
        int threadNum = tableDTO.getThreadNum(); //线程数
        int offset = 0;  //偏移量 当前执行到的位置
        int totalPage = count % limit == 0 ? count / limit : count / limit + 1;  //总页数 用于计算进度
        //多线程执行
        executor = Executors.newFixedThreadPool(threadNum);
        for (int i = 0; i < totalPage; i++) {
            if (paused) {
                break;
            }
            int finalOffset = offset;
            executor.execute(() -> {
                process(operateType, primaryKey, tableDTO, sqlExecute, serviceConn, limit, finalOffset);
            });
            offset += limit;
            progress = (int) (((double) i / totalPage) * 100);
        }
    }

    /**
     * 线程任务
     *
     * @param operateType true:加密 false:解密
     * @param primaryKey  主键
     * @param tableDTO    表信息
     * @param sqlExecute  sql执行器
     * @param serviceConn 业务连接
     * @param limit       每次执行的条数
     * @param finalOffset 当前执行到的位置
     */
    private void process(boolean operateType, String primaryKey, TableDTO tableDTO, SqlExecuteSPI sqlExecute, Connection serviceConn, int limit, int finalOffset) {
        try {
            //查询数据
            ArrayList<String> columns = new ArrayList<>();
            columns.add(primaryKey);
            tableDTO.getColumnDTOList().forEach(columnDTO -> columns.add(columnDTO.getColumnName()));
            ArrayList<Map<String, String>> data = (ArrayList<Map<String, String>>) sqlExecute.selectColumn(serviceConn, tableDTO.getTableName(), columns, limit, finalOffset);
            //加密/解密
            data.forEach(map -> {
                map.forEach((key, value) -> {
                    if (key.equals(primaryKey)) {
                        return;
                    }
                    //加密/解密
                    if (operateType) {
                        //加密
                        //map.put(key, AESUtil.encrypt(value));
                    } else {
                        //解密
                        //map.put(key, AESUtil.decrypt(value));
                    }
                });
            });
            //更新数据
            sqlExecute.batchUpdate(serviceConn, tableDTO.getTableName(), data);
        } catch (Exception e) {
            log.error("加密/解密失败", e);
        }
    }

    private AddColumnsDTO convertToDTO(ColumnDTO columnDTO) {
        AddColumnsDTO addColumnsDTO = new AddColumnsDTO();
        addColumnsDTO.setColumnName(columnDTO.getColumnName());
        addColumnsDTO.setComment(columnDTO.getComment());
        addColumnsDTO.setNotNull(columnDTO.isNotNull());
        return addColumnsDTO;
    }

    private static Connection getConnction(DatabaseDTO databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType connType) throws ZAYKException, SQLException {
        DbInstanceGetConnDTO dbInstanceGetConnDTO = DatabaseToDbInstanceGetConnDTOAdapter.adapter(databaseDTO, connType);
        DbConnectionPoolFactory factory = new DbConnectionPoolFactory();
        return factory.getConnection(dbInstanceGetConnDTO);
    }

    @Override
    public void pause(String tableId) {
        paused = true;
    }

    @Override
    public void resume(String tableId) {
        paused = false;

    }

    @Override
    public int queryProgress(String tableId) {
        return progress;
    }
}

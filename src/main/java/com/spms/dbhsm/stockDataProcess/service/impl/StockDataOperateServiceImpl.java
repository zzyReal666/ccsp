package com.spms.dbhsm.stockDataProcess.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.common.spi.typed.TypedSPIRegistry;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.stockDataProcess.adapter.DatabaseToDbInstanceGetConnDTOAdapter;
import com.spms.dbhsm.stockDataProcess.algorithm.AlgorithmSPI;
import com.spms.dbhsm.stockDataProcess.domain.dto.AddColumnsDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.ColumnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.TableDTO;
import com.spms.dbhsm.stockDataProcess.service.StockDataOperateService;
import com.spms.dbhsm.stockDataProcess.sqlExecute.SqlExecuteSPI;
import com.spms.dbhsm.stockDataProcess.threadTask.InitZookeeperTask;
import com.spms.dbhsm.stockDataProcess.threadTask.UpdateZookeeperTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/4/28 16:45
 */
@Slf4j
@Service
public class StockDataOperateServiceImpl implements StockDataOperateService {

    // 暂停标志的映射，每个线程有自己的暂停标志
    private final Map<String, AtomicBoolean> pausedMap = new ConcurrentHashMap<>();

    // 进度的映射，每个线程有自己的进度
    private final Map<String, AtomicInteger> progressMap = new ConcurrentHashMap<>();

    //全局唯一标识，标志着是否是第一次调用该服务
    private static final Map<Long, Boolean> FIRST_CALL = new ConcurrentHashMap<>();

    //如果执行失败，记录当前表执行到的位置
    private static final Map<Long, Integer> FAIL_POSITION = new ConcurrentHashMap<>();


    /**
     * 存量数据加密/解密
     *
     * @param databaseDTO 数据库信息
     * @param operateType 操作类型 true:加密 false:解密
     */
    @Async
    @Override
    public void stockDataOperate(DatabaseDTO databaseDTO, boolean operateType) throws ZAYKException, SQLException, InterruptedException {


        //表对象
        TableDTO tableDTO = databaseDTO.getTableDTOList().get(0);

        //初始化暂停标志和进度
        pausedMap.put(String.valueOf(tableDTO.getId()), new AtomicBoolean(false));
        progressMap.put(String.valueOf(tableDTO.getId()), new AtomicInteger(0));

        //获取连接 DBA用于修改结构 业务账号用于修改数据
        Connection dbaConn = getConnection(databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType.DBA);
        Connection serviceConn = getConnection(databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType.SERVICE);

        //获取SQL执行器
        Optional<SqlExecuteSPI> registeredService = TypedSPIRegistry.findRegisteredService(SqlExecuteSPI.class, databaseDTO.getDatabaseType());
        if (!registeredService.isPresent()) {
            throw new ZAYKException("未找到对应的数据库类型");
        }
        SqlExecuteSPI sqlExecute = registeredService.get();

        //是否有主键，没有主键不能加密
        String primaryKey = sqlExecute.getPrimaryKey(dbaConn, databaseDTO.getDatabaseName(), tableDTO.getTableName());
        if (primaryKey == null) {
            throw new ZAYKException("表没有主键，不能加密");
        }

        //修改表结构 新增临时字段
        ArrayList<AddColumnsDTO> addColumns = new ArrayList<>();
        tableDTO.getColumnDTOList().forEach(columnDTO -> addColumns.add(convertToDTO(columnDTO)));
        sqlExecute.addTempColumn(dbaConn, tableDTO.getTableName(), addColumns);

        //分块 加/解密
        blockOperate(operateType, sqlExecute, serviceConn, tableDTO, primaryKey, 0);

        //交换字段名字
        tableDTO.getColumnDTOList().forEach(columnDTO -> {
            //原始字段更名为前缀+字段名字
            sqlExecute.renameColumn(dbaConn,databaseDTO.getDatabaseName(),tableDTO.getTableName(),columnDTO.getColumnName(),sqlExecute.getTempColumnSuffix()+columnDTO.getColumnName());

            //临时字段（cipher）由字段名+后缀更改为字段名
            sqlExecute.renameColumn(dbaConn,databaseDTO.getDatabaseName(),tableDTO.getTableName(),sqlExecute.getTempColumnSuffix()+columnDTO.getColumnName(),columnDTO.getColumnName());
        });

        //删除旧字段
        sqlExecute.dropColumn(dbaConn, tableDTO.getTableName(), tableDTO.getColumnDTOList().stream().map(ColumnDTO::getColumnName).collect(Collectors.toList()));

        writeConfigToZookeeper(databaseDTO);

    }

    private void writeConfigToZookeeper(DatabaseDTO databaseDTO) throws InterruptedException {
        //如果当前是第一次走该服务，初始化zk配置中心的数据 todo 这部分是否放在数据库管理部分更为合理？
        if (FIRST_CALL.get(databaseDTO.getId()) == null) {
            FIRST_CALL.put(databaseDTO.getId(), true);
            InitZookeeperTask initZookeeperTask = new InitZookeeperTask(databaseDTO);
            initZookeeperTask.start();
            initZookeeperTask.join();
        }
        //开一个线程 写加密策略到zk
        Thread writeZkthread = new UpdateZookeeperTask(databaseDTO);
        writeZkthread.start();
    }


    /**
     * 分块 加/解密
     *
     * @param operateType true:加密 false:解密
     * @param sqlExecute  sql执行器
     * @param serviceConn 业务连接
     * @param tableDTO    表信息
     * @param primaryKey  主键
     * @param offset      偏移量 即当前执行到的位置
     */
    private void blockOperate(boolean operateType, SqlExecuteSPI sqlExecute, Connection serviceConn, TableDTO tableDTO, String primaryKey, int offset) {
        //总条数
        int count = sqlExecute.count(serviceConn, tableDTO.getTableName());
        //块大小，每个线程每次执行的条数
        int limit = tableDTO.getBatchSize();
        //线程数
        int threadNum = tableDTO.getThreadNum();
        //从offset开始，总页数/总块数
        int totalPage = (count - offset) / limit + 1;

        //多线程执行
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        LinkedBlockingQueue<Integer> offsetQueue = new LinkedBlockingQueue<>();

        for (int i = 0; i < totalPage; i++) {
            offsetQueue.add(offset);
            offset += limit;
        }
        AtomicInteger progress = progressMap.get(String.valueOf(tableDTO.getId()));
        AtomicBoolean paused = pausedMap.get(String.valueOf(tableDTO.getId()));

        for (int i = 0; i < threadNum; i++) {
            executor.execute(() -> {
                try {
                    while (!offsetQueue.isEmpty() && !paused.get()) {
                        Integer currentOffset = offsetQueue.poll();
                        if (currentOffset == null) {
                            break;
                        }
                        process(operateType, primaryKey, tableDTO, sqlExecute, serviceConn, limit, currentOffset);
                        int processed = currentOffset + limit;
                        progress.set((int) (((double) processed / count) * 100));
                    }
                } catch (Exception e) {
                    //打印错误
                    log.error("加密/解密失败，当前线程:" + Thread.currentThread().getId(), e);
                }
            });

        }
        //关闭线程池
        executor.shutdown();
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
            List<Map<String, String>> data = sqlExecute.selectColumn(serviceConn, tableDTO.getTableName(), columns, limit, finalOffset);
            //加密/解密
            data.forEach(map -> {
                map.forEach((key, value) -> {
                    if (key.equals(primaryKey)) {
                        return;
                    }
                    map.put(key, encryptOrDecrypt(key, value, tableDTO, operateType));
                });
            });
            //更新数据
            sqlExecute.batchUpdate(serviceConn, tableDTO.getTableName(), data);
        } catch (Exception e) {
            log.error("加密/解密失败", e);
        }
    }

    /**
     * @param operateType true:加密 false:解密
     */
    private String encryptOrDecrypt(String key, String value, TableDTO tableDTO, boolean operateType) {
        AtomicReference<String> valueRef = new AtomicReference<>(value);
        tableDTO.getColumnDTOList().forEach(columnDTO -> {
            if (columnDTO.getColumnName().equals(key)) {
                String encryptAlgorithm = columnDTO.getEncryptAlgorithm();
                TypedSPIRegistry.findRegisteredService(AlgorithmSPI.class, encryptAlgorithm).ifPresent(encryptSPI -> {
                    if (operateType) {
                        valueRef.set(encryptSPI.encrypt(valueRef.get(), columnDTO.getEncryptKeyIndex(), columnDTO.getProperty()));
                    } else {
                        valueRef.set(encryptSPI.decrypt(valueRef.get(), columnDTO.getEncryptKeyIndex(), columnDTO.getProperty()));
                    }
                });
            }
        });
        return valueRef.get();
    }

    //DTO转换
    private AddColumnsDTO convertToDTO(ColumnDTO columnDTO) {
        AddColumnsDTO addColumnsDTO = new AddColumnsDTO();
        addColumnsDTO.setColumnName(columnDTO.getColumnName());
        addColumnsDTO.setComment(columnDTO.getComment());
        addColumnsDTO.setNotNull(columnDTO.isNotNull());
        return addColumnsDTO;
    }

    private static Connection getConnection(DatabaseDTO databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType connType) throws ZAYKException, SQLException {
        DbInstanceGetConnDTO dbInstanceGetConnDTO = DatabaseToDbInstanceGetConnDTOAdapter.adapter(databaseDTO, connType);
        DbConnectionPoolFactory factory = new DbConnectionPoolFactory();
        return factory.getConnection(dbInstanceGetConnDTO);
    }

    //暂停
    @Override
    public void pause(String tableId) {
        pausedMap.get(tableId).set(true);
    }

    //继续
    @Override
    public void resume(String tableId) {
        pausedMap.get(tableId).set(false);
    }

    //查询执行进度
    @Override
    public int queryProgress(String tableId) {
        return progressMap.get(tableId).get();
    }
}

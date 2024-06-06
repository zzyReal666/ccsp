package com.spms.dbhsm.stockDataProcess.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.common.enums.DatabaseTypeEnum;
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
import com.spms.dbhsm.stockDataProcess.sqlExecute.ClickHouseExecute;
import com.spms.dbhsm.stockDataProcess.sqlExecute.SqlExecuteSPI;
import com.spms.dbhsm.stockDataProcess.threadTask.UpdateZookeeperTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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

    // 暂停标志的映射，每个表有自己的暂停标志
    private static final Map<String, AtomicBoolean> PAUSED_MAP = new ConcurrentHashMap<>();

    // 进度的映射，每个表有自己的进度
    private static final Map<String, AtomicInteger> PROGRESS_MAP = new ConcurrentHashMap<>();

    //暂停｜失败 记录位置 tableId:offset
    private static final Map<Long, Integer> STOP_POSITION = new ConcurrentHashMap<>();


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
        initPauseAndProcess(tableDTO);

        //获取连接 DBA用于修改结构
        Connection dbaConn = getConnection(databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType.DBA);

        //获取SQL执行器
        SqlExecuteSPI sqlExecute = getSqlExecute(databaseDTO);

        //是否有主键，没有主键不能加密
        String primaryKey = sqlExecute.getPrimaryKey(dbaConn, databaseDTO.getDatabaseName(), tableDTO.getTableName());
        if (primaryKey == null) {
            throw new ZAYKException("表没有主键，不能加密");
        }

        // 新增临时字段
        addTempColumns(tableDTO, sqlExecute, dbaConn, operateType);

        //分块 加/解密
        blockOperate(operateType, sqlExecute, dbaConn, databaseDTO, primaryKey, 0);

        //设置进度 注意，执行到这里肯定已经完成，强制设置进度为100
        PROGRESS_MAP.put(String.valueOf(tableDTO.getId()), new AtomicInteger(100));

        //交换字段名字
        exchangeColumnName(databaseDTO, tableDTO, sqlExecute, dbaConn);

        //删除旧字段 注意，此时旧字段经过名字交换已经变为 前缀+字段名 todo 后续改成原字段存留一段时间，手动执行删除
        sqlExecute.dropColumn(dbaConn, tableDTO.getTableName(), tableDTO.getColumnDTOList().stream().map(columnDTO -> sqlExecute.getTempColumnSuffix() + columnDTO.getColumnName()).collect(Collectors.toList()));

        // 加密策略写入 zookeeper
        writeConfigToZookeeper(databaseDTO);

    }


    //暂停
    @Override
    public void pause(String tableId) {
        PAUSED_MAP.get(tableId).set(true);
    }

    //继续
    @Override
    public void resume(String tableId) {
        PAUSED_MAP.get(tableId).set(false);
    }

    //查询执行进度
    @Override
    public int queryProgress(String tableId) {
        return PROGRESS_MAP.get(tableId) == null ? 0 : PROGRESS_MAP.get(tableId).get();
    }


    //新增临时字段
    private void addTempColumns(TableDTO tableDTO, SqlExecuteSPI sqlExecute, Connection dbaConn, boolean operateType) {
        ArrayList<AddColumnsDTO> addColumns = new ArrayList<>();
        tableDTO.getColumnDTOList().forEach(columnDTO -> addColumns.add(convertToDTO(columnDTO, operateType)));
        sqlExecute.addTempColumn(dbaConn, tableDTO.getTableName(), addColumns);
    }

    //交换字段名字
    private static void exchangeColumnName(DatabaseDTO databaseDTO, TableDTO tableDTO, SqlExecuteSPI sqlExecute, Connection dbaConn) {
        if (sqlExecute instanceof ClickHouseExecute) {
            sqlExecute.renameColumn(dbaConn, databaseDTO.getDatabaseName(), tableDTO.getTableName(), null, null);
        } else {
            tableDTO.getColumnDTOList().forEach(columnDTO -> {
                //原始字段更名为前缀+字段名字
                sqlExecute.renameColumn(dbaConn, databaseDTO.getDatabaseName(), tableDTO.getTableName(), columnDTO.getColumnName(), sqlExecute.getTempColumnSuffix() + columnDTO.getColumnName());
                //临时字段（cipher）由字段名+后缀更改为字段名
                sqlExecute.renameColumn(dbaConn, databaseDTO.getDatabaseName(), tableDTO.getTableName(), columnDTO.getColumnName() + sqlExecute.getTempColumnSuffix(), columnDTO.getColumnName());
            });
        }
    }

    //获取sql执行器
    private static SqlExecuteSPI getSqlExecute(DatabaseDTO databaseDTO) throws ZAYKException {
        Optional<SqlExecuteSPI> registeredService = TypedSPIRegistry.findRegisteredService(SqlExecuteSPI.class, databaseDTO.getDatabaseType());
        if (!registeredService.isPresent()) {
            throw new ZAYKException("未找到对应的数据库类型");
        }
        return registeredService.get();
    }

    //初始化暂停标志和进度
    private void initPauseAndProcess(TableDTO tableDTO) {
        PAUSED_MAP.put(String.valueOf(tableDTO.getId()), new AtomicBoolean(false));
        PROGRESS_MAP.put(String.valueOf(tableDTO.getId()), new AtomicInteger(0));
    }

    //写入加密策略到zookeeper
    private void writeConfigToZookeeper(DatabaseDTO databaseDTO) throws InterruptedException {
        //开一个线程 写加密策略到zk
        Thread writeZkthread = new UpdateZookeeperTask(databaseDTO);
        writeZkthread.start();
        writeZkthread.join();

    }


    /**
     * 分块 加/解密
     *
     * @param operateType true:加密 false:解密
     * @param sqlExecute  sql执行器
     * @param dbaConn     dba账号
     * @param databaseDTO 数据库加密对象
     * @param primaryKey  主键
     * @param offset      偏移量 即当前执行到的位置
     */
    private void blockOperate(boolean operateType, SqlExecuteSPI sqlExecute, Connection dbaConn, DatabaseDTO databaseDTO, String primaryKey, int offset) {

        TableDTO tableDTO = databaseDTO.getTableDTOList().get(0);
        //总条数
        int count = sqlExecute.count(dbaConn, tableDTO.getTableName());
        //块大小，每个线程每次执行的条数
        int limit = tableDTO.getBatchSize();
        //线程数
        int threadNum = tableDTO.getThreadNum();
        //从offset开始，总块数
        int blockCount = (count - offset) / limit + 1;

        //多线程执行
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);

        //块的开始
        LinkedBlockingQueue<Integer> offsetQueue = new LinkedBlockingQueue<>();

        for (int i = 0; i < blockCount; i++) {
            offsetQueue.add(offset);
            offset += limit;
        }
        AtomicInteger progress = PROGRESS_MAP.get(String.valueOf(tableDTO.getId()));
        AtomicBoolean paused = PAUSED_MAP.get(String.valueOf(tableDTO.getId()));

        for (int i = 0; i < threadNum; i++) {
            executor.execute(() -> {
                //循环次数
                int loop = 1;
                log.info("当前线程:{},线程id:{}", Thread.currentThread().getName(), Thread.currentThread().getId());
                try (Connection connection = getConnection(databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType.SERVICE)) {
                    while (!offsetQueue.isEmpty() && !paused.get()) {
                        Integer currentOffset = offsetQueue.poll();
                        log.info("线程:{} 第{}轮执行，offset:{}", Thread.currentThread().getName(), loop, currentOffset);
                        if (currentOffset == null) {
                            break;
                        }
                        process(operateType, primaryKey, tableDTO, sqlExecute, connection, limit, currentOffset);
                        int processed = currentOffset + limit;
                        progress.set((int) (((double) processed / count) * 100));
                        PROGRESS_MAP.put(String.valueOf(tableDTO.getId()), progress);
                        // 检查暂停标志
                        if (paused.get()) {
                            STOP_POSITION.put(tableDTO.getId(), currentOffset);
                            break;
                        }
                        loop++;
                    }
                } catch (Exception e) {
                    //打印错误
                    log.error("加密/解密失败，当前线程:{}", Thread.currentThread().getName(), e);
                    throw new RuntimeException(e);
                }
            });
        }
        //关闭线程池
        executor.shutdown();
        try {
            // 等待所有任务完成，最多等待5小时
            if (!executor.awaitTermination(5, TimeUnit.HOURS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 线程任务
     *
     * @param operateType true:加密 false:解密
     * @param primaryKey  主键
     * @param tableDTO    表信息
     * @param sqlExecute  sql执行器
     * @param conn        业务连接
     * @param limit       每次执行的条数
     * @param finalOffset 当前执行到的位置
     */
    private void process(boolean operateType, String primaryKey, TableDTO tableDTO, SqlExecuteSPI sqlExecute, Connection conn, int limit, int finalOffset) {
        try {
            //查询数据
            ArrayList<String> columns = new ArrayList<>();
            columns.add(primaryKey);
            tableDTO.getColumnDTOList().forEach(columnDTO -> columns.add(columnDTO.getColumnName()));
            List<Map<String, String>> data = sqlExecute.selectColumn(conn, tableDTO.getTableName(), columns, limit, finalOffset);

            //clickHouse做特殊处理
            if (sqlExecute.getType().equals(DatabaseTypeEnum.ClickHouse.name())) {
                Map<String, List<Map<String, String>>> dataIncludeCipher = new HashMap<>();
                data.forEach(map -> {
                    map.forEach((fieldName, plain) -> {
                        Map<String, String> valueMap = new HashMap<>();
                        if (fieldName.equals(primaryKey)) {
                            valueMap.put("plain", plain);
                        } else {
                            valueMap.put("plain", plain);
                            valueMap.put("cipher", encryptOrDecrypt(fieldName, plain, tableDTO, operateType));
                        }
                        // 将加密后的数据直接放入 transformedData
                        dataIncludeCipher.putIfAbsent(fieldName, new ArrayList<>());
                        dataIncludeCipher.get(fieldName).add(valueMap);
                    });
                });
                // 更新数据
                sqlExecute.columnBatchUpdate(conn, tableDTO.getTableName(), primaryKey, dataIncludeCipher, limit, finalOffset);
            }
            //行式数据库
            else {
                //加密/解密
                data.forEach(map -> {
                    map.forEach((fieldName, plain) -> {
                        if (fieldName.equals(primaryKey)) {
                            return;
                        }
                        map.put(fieldName, encryptOrDecrypt(fieldName, plain, tableDTO, operateType));
                    });
                });
                //更新数据
                sqlExecute.batchUpdate(conn, tableDTO.getTableName(), data);
            }
        } catch (Exception e) {
            log.error("加密/解密失败", e);
            throw new RuntimeException();
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


    //region//======>工具方法
    //DTO转换
    private AddColumnsDTO convertToDTO(ColumnDTO columnDTO, boolean operateType) {
        AddColumnsDTO addColumnsDTO = new AddColumnsDTO();
        addColumnsDTO.setColumnName(columnDTO.getColumnName());
        addColumnsDTO.setComment(columnDTO.getComment());
        addColumnsDTO.setNotNull(columnDTO.isNotNull());
        addColumnsDTO.setColumnDefinition(columnDTO.getColumnDefinition());
        addColumnsDTO.setEncrypt(operateType);
        return addColumnsDTO;
    }

    //获取连接
    private static Connection getConnection(DatabaseDTO databaseDTO, DatabaseToDbInstanceGetConnDTOAdapter.AdapterType connType) throws ZAYKException, SQLException {
        DbInstanceGetConnDTO dbInstanceGetConnDTO = DatabaseToDbInstanceGetConnDTOAdapter.adapter(databaseDTO, connType);
        DbConnectionPoolFactory factory = new DbConnectionPoolFactory();
        Connection connection = factory.getConnection(dbInstanceGetConnDTO);
        connection.setSchema(databaseDTO.getDatabaseName());
        log.info("获取连接成功,schema:{}", connection.getSchema());
        return connection;
    }
    //endregion

}

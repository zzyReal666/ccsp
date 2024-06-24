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
import com.spms.dbhsm.stockDataProcess.service.OperateContext;
import com.spms.dbhsm.stockDataProcess.service.StockDataOperateService;
import com.spms.dbhsm.stockDataProcess.sqlExecute.ClickHouseExecute;
import com.spms.dbhsm.stockDataProcess.sqlExecute.SqlExecuteForColSPI;
import com.spms.dbhsm.stockDataProcess.sqlExecute.SqlExecuteSPI;
import com.spms.dbhsm.stockDataProcess.threadTask.UpdateZookeeperTask;
import lombok.extern.slf4j.Slf4j;
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

    //继续时的上下文
    private static final Map<Long, OperateContext> contexts = new ConcurrentHashMap<>();

    //暂停
    @Override
    public void pause(String tableId) {
        PAUSED_MAP.get(tableId).set(true);
    }

    //继续
    @Override
    public void resume(String tableId) {
        PAUSED_MAP.get(tableId).set(false);
        OperateContext context = contexts.get(Long.valueOf(tableId));
//        blockOperate(context.isEncrypt(), context.getSqlExecute(), context.getDbaConn(), context.getDatabaseDTO(), context.getPrimaryKey(), );
    }

    //查询执行进度
    @Override
    public int queryProgress(String tableId) {
        return PROGRESS_MAP.get(tableId) == null ? 0 : PROGRESS_MAP.get(tableId).get();
    }

    //清理map
    @Override
    public void clearMap(String tableId) {
        PAUSED_MAP.remove(tableId);
        PROGRESS_MAP.remove(tableId);
        STOP_POSITION.remove(Long.valueOf(tableId));
    }

    /**
     * 存量数据加密/解密
     * 注意！ 控制层需要异步调用，因为该流程很长，控制层不需要等待该方法完成
     *
     * @param databaseDTO 数据库信息
     * @param operateType 操作类型 true:加密 false:解密
     */
    @Override
    public void stockDataOperate(DatabaseDTO databaseDTO, boolean operateType) throws ZAYKException, SQLException, InterruptedException {
        //todo 参数检查
        if (databaseDTO.getDbStorageMode() == DatabaseDTO.DbStorageMode.COLUMN) {
            columnDatabase(databaseDTO, operateType);
        } else {
            // 行式数据库 todo ClickHouse从这个里面拿出来，放到列式数据库
            rowDatabase(databaseDTO, operateType);
        }
    }

    //列式数据库
    private void columnDatabase(DatabaseDTO databaseDTO, boolean operateType) throws ZAYKException {
        //表对象
        TableDTO tableDTO = databaseDTO.getTableDTOList().get(0);
        //初始化暂停标志和进度
        initPauseAndProcess(tableDTO);
        //获取SQL执行器
        Optional<SqlExecuteForColSPI> registeredService = TypedSPIRegistry.findRegisteredService(SqlExecuteForColSPI.class, databaseDTO.getDatabaseType());
        if (!registeredService.isPresent()) {
            throw new ZAYKException("未找到对应的数据库类型");
        }
        SqlExecuteForColSPI sqlExecute = registeredService.get();
        //获取连接
        org.apache.hadoop.hbase.client.Connection dbaConn = (org.apache.hadoop.hbase.client.Connection) sqlExecute.getConnection(databaseDTO.getDatabaseIp(), databaseDTO.getDatabasePort(), databaseDTO.getDatabaseName(), databaseDTO.getDatabaseDba(), databaseDTO.getDatabaseDbaPassword());
        //新建临时表
        sqlExecute.createTable(dbaConn, tableDTO.getTableName());
        //分块 加/解密
        blockOperateForCol(operateType, sqlExecute, dbaConn, databaseDTO);
        //进度强制置为100
        PROGRESS_MAP.put(String.valueOf(tableDTO.getId()), new AtomicInteger(100));
        //删除旧表
        sqlExecute.dropTable(dbaConn, tableDTO.getTableName());
        //临时表改名字
        sqlExecute.renameTable(dbaConn, tableDTO.getTableName() + sqlExecute.getPrefixOrSuffix(), tableDTO.getTableName());
    }

    //列式数据库的分块加密
    private void blockOperateForCol(boolean operateType, SqlExecuteForColSPI sqlExecute, org.apache.hadoop.hbase.client.Connection dbaConn, DatabaseDTO databaseDTO) {
        TableDTO tableDTO = databaseDTO.getTableDTOList().get(0);
        int offset = 0;
        //总条数
        int count = sqlExecute.count(dbaConn, tableDTO.getTableName());
        //块大小，每个线程每次执行的条数
        int limit = tableDTO.getBatchSize();
        //线程数
        int threadNum = tableDTO.getThreadNum();
        //从offset开始，总块数 不能整除要加1
        int blockCount = (count - offset) % limit == 0 ? ((count - offset) / limit) : (count - offset) / limit + 1;

        //多线程执行
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);

        //块的开始
        LinkedBlockingQueue<Integer> offsetQueue = new LinkedBlockingQueue<>();

        for (int i = 0; i < blockCount; i++) {
            offsetQueue.add(offset);
            offset += limit;
        }
        log.info("分块成功，共{}块", offsetQueue.size());
        AtomicInteger progress = PROGRESS_MAP.get(String.valueOf(tableDTO.getId()));
        AtomicBoolean paused = PAUSED_MAP.get(String.valueOf(tableDTO.getId()));
        org.apache.hadoop.hbase.client.Connection connection = getConnection(sqlExecute, databaseDTO);
        for (int i = 0; i < threadNum; i++) {
            executor.execute(() -> {
                processForCol(operateType, sqlExecute, offsetQueue, paused, databaseDTO, limit, progress, count, connection);
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

    //hbase
    private static org.apache.hadoop.hbase.client.Connection getConnection(SqlExecuteForColSPI sqlExecute, DatabaseDTO databaseDTO) {
        return (org.apache.hadoop.hbase.client.Connection) sqlExecute.getConnection(databaseDTO.getDatabaseIp(), databaseDTO.getDatabasePort(), databaseDTO.getDatabaseName(), databaseDTO.getServiceUser(), databaseDTO.getServicePassword());
    }

    //列式数据库的线程任务
    private void processForCol(boolean operateType, SqlExecuteForColSPI sqlExecute, LinkedBlockingQueue<Integer> offsetQueue, AtomicBoolean paused, DatabaseDTO databaseDTO, int limit, AtomicInteger progress, int count, org.apache.hadoop.hbase.client.Connection connection) {
        TableDTO tableDTO = databaseDTO.getTableDTOList().get(0);
        //循环次数
        int loop = 1;
        Integer currentOffset = 0;
        try {
            while (!offsetQueue.isEmpty()  && !paused.get()) {
                currentOffset = offsetQueue.poll();
                if (currentOffset == null) {
                    break;
                }
                log.info("线程:{} 第{}轮开始执行第{}块数据，offset:{}", Thread.currentThread().getName(), loop, currentOffset / limit, currentOffset);
                //查询数据
                List<Map<String, String>> data = (List<Map<String, String>>) sqlExecute.selectData(connection, tableDTO, currentOffset, limit, operateType);
                //加密数据
                data.forEach(map -> map.entrySet().forEach(entry -> entry.setValue(encryptOrDecrypt(entry.getKey(), entry.getValue(), tableDTO, operateType))));
                //存储数据
                sqlExecute.insertData(connection, tableDTO.getTableName() + sqlExecute.getPrefixOrSuffix(), data);

                int processed = currentOffset + limit;
                progress.set((int) (((double) processed / count) * 100));
                PROGRESS_MAP.put(String.valueOf(tableDTO.getId()), progress);
                // 检查暂停标志
                if (paused.get()) {
                    STOP_POSITION.put(tableDTO.getId(), currentOffset);
                    break;
                }
                log.info("线程:{} 第{}轮执行完成，offset:{}", Thread.currentThread().getName(), loop, currentOffset);
                loop++;
            }
        } catch (Exception e) {
            log.error("线程:{} 第{}轮执行第{}块数据错误，offset:{}", Thread.currentThread().getName(), loop, currentOffset / limit, currentOffset, e);
            throw new RuntimeException(e);
        }
    }

    //行数数据库
    private void rowDatabase(DatabaseDTO databaseDTO, boolean operateType) throws ZAYKException, SQLException, InterruptedException {
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

        // 新增临时字段
        addTempColumns(tableDTO, sqlExecute, dbaConn, operateType);

        //分块 加/解密
        contexts.put(tableDTO.getId(), new OperateContext(operateType, sqlExecute, dbaConn, databaseDTO, primaryKey, 0));
        blockOperate(operateType, sqlExecute, dbaConn, databaseDTO, primaryKey, 0);

        //洗数据完成后的操作
        afterOperate(databaseDTO, tableDTO, sqlExecute, dbaConn);
    }

    private void afterOperate(DatabaseDTO databaseDTO, TableDTO tableDTO, SqlExecuteSPI sqlExecute, Connection dbaConn) throws InterruptedException {
        //设置进度 注意，执行到这里肯定已经完成，强制设置进度为100
        PROGRESS_MAP.put(String.valueOf(tableDTO.getId()), new AtomicInteger(100));

        //交换字段名字
        exchangeColumnName(databaseDTO, tableDTO, sqlExecute, dbaConn);

        //删除旧字段 注意，此时旧字段经过名字交换已经变为 前缀+字段名 todo 后续改成原字段存留一段时间，手动执行删除
        sqlExecute.dropColumn(dbaConn, tableDTO.getTableName(), tableDTO.getColumnDTOList().stream().map(columnDTO -> sqlExecute.getTempColumnSuffix() + columnDTO.getColumnName()).collect(Collectors.toList()));

        // 加密策略写入 zookeeper 前置插件模式才需要 当前仅ck需要
        if (sqlExecute instanceof ClickHouseExecute) {
            writeConfigToZookeeper(databaseDTO);
        }
    }

    //行式新增临时字段
    private void addTempColumns(TableDTO tableDTO, SqlExecuteSPI sqlExecute, Connection dbaConn, boolean operateType) {
        ArrayList<AddColumnsDTO> addColumns = new ArrayList<>();
        tableDTO.getColumnDTOList().forEach(columnDTO -> addColumns.add(convertToDTO(columnDTO, operateType)));
        sqlExecute.addTempColumn(dbaConn, tableDTO.getTableName(), addColumns);
    }

    //行式交换字段名字
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

    //行式获取sql执行器
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
     * 行式线程任务
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
                //因为clickHouse的需要处理前的数据定位，所以这个地方需要携带着原始数据
                Map<String, List<Map<String, String>>> dataIncludeBefore = new HashMap<>();
                data.forEach(map -> {
                    map.forEach((fieldName, plain) -> {
                        Map<String, String> valueMap = new HashMap<>();
                        if (fieldName.equals(primaryKey)) {
                            valueMap.put("before", plain);
                        } else {
                            valueMap.put("before", plain);
                            valueMap.put("after", encryptOrDecrypt(fieldName, plain, tableDTO, operateType));
                        }
                        // 将加密后的数据直接放入 transformedData
                        dataIncludeBefore.putIfAbsent(fieldName, new ArrayList<>());
                        dataIncludeBefore.get(fieldName).add(valueMap);
                    });
                });
                // 更新数据
                sqlExecute.columnBatchUpdate(conn, tableDTO.getTableName(), primaryKey, dataIncludeBefore, limit, finalOffset);
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
     * 加密或者解密
     *
     * @param fieldName   字段名称
     * @param value       字段值
     * @param tableDTO    表信息 包含加密算法和密钥 需要加密的字段名
     * @param operateType true:加密 false:解密
     */
    private String encryptOrDecrypt(String fieldName, String value, TableDTO tableDTO, boolean operateType) {
        AtomicReference<String> valueRef = new AtomicReference<>(value);
        tableDTO.getColumnDTOList().forEach(columnDTO -> {
            if (columnDTO.getColumnName().equals(fieldName)) {
                String encryptAlgorithm = columnDTO.getEncryptAlgorithm();
                TypedSPIRegistry.findRegisteredService(AlgorithmSPI.class, encryptAlgorithm).ifPresent(algorithmSPI -> {
                    if (operateType) {
                        valueRef.set(algorithmSPI.encrypt(valueRef.get(), columnDTO.getEncryptKeyIndex(), columnDTO.getProperty()));
                    } else {
                        valueRef.set(algorithmSPI.decrypt(valueRef.get(), columnDTO.getEncryptKeyIndex(), columnDTO.getProperty()));
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
        getSqlExecute(databaseDTO).connectionOperate(connection, databaseDTO);
        return connection;
    }
    //endregion

}

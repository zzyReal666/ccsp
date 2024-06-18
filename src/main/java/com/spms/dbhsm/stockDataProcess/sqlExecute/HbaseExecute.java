package com.spms.dbhsm.stockDataProcess.sqlExecute;

import com.spms.common.enums.DatabaseTypeEnum;
import com.spms.common.spi.typed.TypedSPIRegistry;
import com.spms.dbhsm.stockDataProcess.algorithm.AlgorithmSPI;
import com.spms.dbhsm.stockDataProcess.domain.dto.ColumnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.TableDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/6/7 08:32
 */
@Slf4j
public class HbaseExecute implements SqlExecuteForColSPI {

    /**
     * 默认连接数 25
     */
    private static int connectionNum = 25;

    private static Connection connection = null;


    /**
     * 获取连接
     *
     * @param args
     * @return
     */
    @Override
    public Object getConnection(Object... args) {
        if (connection != null) {
            return connection;
        }
        String ip = (String) args[0];
        String port = (String) args[1];
        String databaseName = (String) args[2];
        String userName = (String) args[3];
        String password = (String) args[4];

        //如果有第5个参数，作为连接数
        if (args.length > 5) {
            connectionNum = (int) args[5];
        }

        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", ip);
        conf.set("zookeeper.znode.parent", "/" + databaseName);
        conf.set("hbase.zookeeper.property.clientPort", port);
        conf.set("hbase.client.ipc.pool.size", String.valueOf(connectionNum));

        UserGroupInformation.setConfiguration(conf);
        UserGroupInformation romoteUser = UserGroupInformation.createRemoteUser(userName);
        UserGroupInformation.setLoginUser(romoteUser);

        try {
            connection = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            log.error("getConnection error ip:{}, port:{}, databaseName:{}, userName:{}, password:{}, connectionNum:{} ", ip, port, databaseName, userName, password, connectionNum, e);
            throw new RuntimeException(e);
        }
        return connection;
    }

    @Override
    public String getPrefixOrSuffix() {
        return "_temp$zAyK_dbEnc_HBase_";
    }

    /**
     * 新建表
     *
     * @param args connection: 连接对象
     *             tableName : 原始表名
     */
    @Override
    public void createTable(Object... args) {
//        Connection connection = (Connection) args[0];
//        String tableName = (String) args[1];
//        try {
//            //旧表
//            Table table = connection.getTable(TableName.valueOf(tableName));
//            ColumnFamilyDescriptor[] oldColumnF = table.getDescriptor().getColumnFamilies();
//
//            //新表
//            TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(TableName.valueOf(tableName + getPrefixOrSuffix()));
//            tableBuilder.setColumnFamilies(Arrays.asList(oldColumnF));
//
//            //创建新表
//            Admin admin = connection.getAdmin();
//            admin.createTable(tableBuilder.build());
//        } catch (IOException e) {
//            log.error("createTable error tableName:{}, args:{}", tableName, args);
//            throw new RuntimeException(e);
//        }
    }

    /**
     * @param args connection: 连接对象
     *             tableName : 原始表名
     */
    @Override
    public int count(Object... args) {
        Connection connection = (Connection) args[0];
        String tableName = (String) args[1];
        try {
            Table table = connection.getTable(TableName.valueOf(tableName));
            Scan scan = new Scan();
            scan.setCaching(1000);
            scan.setCacheBlocks(false);
            ResultScanner scanner = table.getScanner(scan);
            int count = 0;
            for (Result ignored : scanner) {
                count++;
            }
            return count;
        } catch (IOException e) {
            log.error("count error tableName:{}, args:{}", tableName, args);
            throw new RuntimeException(e);
        }
    }

    /**
     * 查询数据
     *
     * @param args connection : 连接对象
     *             tableDTO   : 加密表信息
     *             offset     : 偏移量
     *             limit      : 限制条数
     *             operateType：操作类型 true 加密  ｜  false 解密
     * @return List 每条数据是一个Map key为列名（列族+列名字） value为值
     */
    @Override
    public List<Map<String, String>> selectData(Object... args) {
        Connection connection = (Connection) args[0];
        TableDTO tableDTO = (TableDTO) args[1];
        int offset = (int) args[2];
        int limit = (int) args[3];
        boolean operateType = (boolean) args[4];
        try {
            Table table = connection.getTable(TableName.valueOf(tableDTO.getTableName()));

            //获取开始行键
            byte[] startRow = getStartRow(table, offset);

            //设置scan
            Scan scan = new Scan();
            scan.withStartRow(startRow, false);
            scan.setFilter(new PageFilter(limit));

            //添加查询的列
            List<ColumnDTO> columnDTOList = tableDTO.getColumnDTOList();
            columnDTOList.forEach(columnDTO -> {
                String[] split = columnDTO.getColumnName().split(":");
                scan.addColumn(Bytes.toBytes(split[0]), Bytes.toBytes(split[1]));
            });
            //查询-修改-插入
            ArrayList<Put> puts = new ArrayList<>();
            ResultScanner scanner = table.getScanner(scan);
            for (Result result : scanner) {
                byte[] row = result.getRow();
                columnDTOList.forEach(columnDTO -> {
                    String[] split = columnDTO.getColumnName().split(":");
                    byte[] before = result.getValue(Bytes.toBytes(split[0]), Bytes.toBytes(split[1]));
                    //加、解密
                    String after = operate(before, columnDTO, operateType);
                    Put put = new Put(row);
                    put.addColumn(Bytes.toBytes(split[0]), Bytes.toBytes(split[1]), Bytes.toBytes(after));
                    puts.add(put);
                });
            }
            table.put(puts);
        } catch (IOException e) {
            log.error("selectData error tableName:{}, args:{}", tableDTO.getTableName(), args);
            throw new RuntimeException(e);
        }
        return new ArrayList<>();
    }

    private static byte[] getStartRow(Table table, int offset) {
        Scan scan1 = new Scan();
        scan1.setCaching(1000);
        List<byte[]> rowKeys = new ArrayList<>();
        try (ResultScanner scanner = table.getScanner(scan1)) {
            for (Result result : scanner) {
                rowKeys.add(result.getRow());
            }
        } catch (IOException e) {
            log.error("getStartRow error", e);
            throw new RuntimeException(e);
        }
        return rowKeys.get(offset);
    }

    /**
     * 加密或者解密操作
     *
     * @param before      操作前数据
     * @param columnDTO   dto
     * @param operateType 操作 true 加密
     */
    private String operate(byte[] before, ColumnDTO columnDTO, boolean operateType) {
        AtomicReference<String> valueRef = new AtomicReference<>(new String(before));
        TypedSPIRegistry.findRegisteredService(AlgorithmSPI.class, columnDTO.getEncryptAlgorithm())
                .ifPresent(algorithmSPI ->
                        valueRef.set(operateType ?
                                algorithmSPI.encrypt(new String(before), columnDTO.getEncryptKeyIndex(), columnDTO.getProperty())
                                : algorithmSPI.decrypt(new String(before), columnDTO.getEncryptKeyIndex(), columnDTO.getProperty())));
        return valueRef.get();
    }

    /**
     * 插入数据
     *
     * @param args connection : 连接对象
     *             tableName  : 原始表名
     *             offset     : 偏移量
     *             limit      : 限制条数
     */
    @Override
    public void insertData(Object... args) {

    }

    /**
     * 删除表
     *
     * @param args connection : 连接对象
     *             tableName  : 原始表名
     */
    @Override
    public void dropTable(Object... args) {
        Connection connection = (Connection) args[0];
        try {
            connection.close();
        } catch (IOException e) {
            log.error("dropTable error args:{}", args);
            throw new RuntimeException(e);
        }
//        Connection connection = (Connection) args[0];
//        String tableName = (String) args[1];
//        try {
//            Admin admin = connection.getAdmin();
//            admin.disableTable(TableName.valueOf(tableName));
//            admin.deleteTable(TableName.valueOf(tableName));
//        } catch (IOException e) {
//            log.error("dropTable error tableName:{}, args:{}", tableName, args);
//            throw new RuntimeException(e);
//        }
    }

    /**
     * 重命名表
     *
     * @param args connection : 连接对象
     *             oldName    : 原始表名
     *             newName    : 新表名
     */
    @Override
    public void renameTable(Object... args) {
//        Connection connection = (Connection) args[0];
//        String oldName = (String) args[1];
//        String newName = (String) args[2];
//        try {
//            Table table = connection.getTable(TableName.valueOf(oldName));
//            Admin admin = connection.getAdmin();
//            //disable表
//            admin.disableTable(TableName.valueOf(oldName));
//            //创建快照
//            admin.snapshot(oldName, TableName.valueOf("snapshot" + oldName));
//            //克隆快照
//            admin.cloneSnapshot("snapshot" + oldName, TableName.valueOf(newName));
//            //删除快照
//            admin.deleteSnapshot("snapshot" + oldName);
//            //删除旧表
//            admin.deleteTable(TableName.valueOf(oldName));
//        } catch (IOException e) {
//            log.error("renameTable error oldName:{}, newName:{}, args:{}", oldName, newName, args);
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public String getType() {
        return DatabaseTypeEnum.HBase.name();
    }
}

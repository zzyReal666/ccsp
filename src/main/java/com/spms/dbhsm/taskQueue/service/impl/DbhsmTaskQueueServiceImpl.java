package com.spms.dbhsm.taskQueue.service.impl;

import com.ccsp.cert.sm4.SM4Utils;
import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.SnowFlakeUtil;
import com.ccsp.common.core.utils.bean.BeanConvertUtils;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.security.utils.SecurityUtils;
import com.spms.common.CommandUtil;
import com.spms.common.DBIpUtil;
import com.spms.common.JSONDataUtil;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.DBUtil;
import com.spms.common.enums.DatabaseTypeEnum;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.dbUser.mapper.DbhsmDbUsersMapper;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptTable;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptColumnsMapper;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptTableMapper;
import com.spms.dbhsm.encryptcolumns.service.IDbhsmEncryptColumnsService;
import com.spms.dbhsm.encryptcolumns.vo.EncryptColumns;
import com.spms.dbhsm.encryptcolumns.vo.UpEncryptColumnsRequest;
import com.spms.dbhsm.secretKey.domain.DbhsmSecretKeyManage;
import com.spms.dbhsm.secretKey.mapper.DbhsmSecretKeyManageMapper;
import com.spms.dbhsm.stockDataProcess.domain.dto.ColumnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.TableDTO;
import com.spms.dbhsm.stockDataProcess.service.StockDataOperateService;
import com.spms.dbhsm.taskQueue.domain.DbhsmTaskQueue;
import com.spms.dbhsm.taskQueue.mapper.DbhsmTaskQueueMapper;
import com.spms.dbhsm.taskQueue.service.DbhsmTaskQueueService;
import com.spms.dbhsm.taskQueue.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-21 11:34 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Service
public class DbhsmTaskQueueServiceImpl implements DbhsmTaskQueueService {

    private static final Logger log = LoggerFactory.getLogger(DbhsmTaskQueueServiceImpl.class);

    @Autowired
    private DbhsmEncryptTableMapper dbhsmEncryptTableMapper;

    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

    @Autowired
    private DbhsmEncryptColumnsMapper dbhsmEncryptColumnsMapper;

    @Autowired
    private IDbhsmEncryptColumnsService dbhsmEncryptColumnsService;

    @Autowired
    private DbhsmTaskQueueMapper dbhsmTaskQueueMapper;

    @Autowired
    private StockDataOperateService stockDataOperateService;

    @Autowired
    private DbhsmSecretKeyManageMapper dbhsmSecretKeyManageMapper;

    @Autowired
    private DbhsmDbUsersMapper dbUsersMapper;

    @Override
    public List<TaskQueueListResponse> list(TaskQueueListRequest request) {
        return dbhsmTaskQueueMapper.querByEncryptColumnsList(request);
    }

    /*
     * @description 启停继续、加解密
     * @author wzh [zhwang2012@yeah.net]
     * @date 16:23 2024/5/23
     * @param id 队列任务ID
     * @param taskMode 加密/解密
     * @param taskType 启动/暂停/继续
     * @return null
     */
    @Override
    @Transactional
    public AjaxResult upEncryptColumns(TaskQueueRequest request) throws Exception {

        //查询队列信息
        DbhsmTaskQueue taskQueue = dbhsmTaskQueueMapper.findByPrimaryKey(request.getTaskId());
        //加密表信息
        DbhsmEncryptTable encryptTable = dbhsmEncryptTableMapper.findByPrimaryKey(taskQueue.getTableId());

        //查询需要加密的列
        DbhsmEncryptColumns dbhsmEncryptColumns = new DbhsmEncryptColumns();
        dbhsmEncryptColumns.setTableId(String.valueOf(encryptTable.getTableId()));

        if (null != request.getTaskType() && !DbConstants.UP.equals(request.getTaskType())) {
            int colStatus = 0;
            int taskStatus = 0;
            //暂停
            if (DbConstants.DOWN.equals(request.getTaskType())) {
                colStatus = null != taskQueue.getEncStatus() ? 0 : 3;
                taskStatus = 2;
                stockDataOperateService.pause(String.valueOf(encryptTable.getTableId()));
            } else if (DbConstants.CONTINUE.equals(request.getTaskType())) {
                colStatus = null != taskQueue.getEncStatus() ? 1 : 4;
                taskStatus = 1;
                //任务继续
                new Thread(() -> {
                    try {
                        stockDataOperateService.resume(taskQueue.getTableId());
                    } catch (Exception e) {
                        log.error("继续加解密操作失败：{}", e.getMessage());
                    }
                }).start();
            }

            //查询：继续操作：查询加密列为0  解密列为3    暂停操作：加密列为1  解密列为4
            int queryStatus = DbConstants.DOWN.equals(request.getTaskType()) ? colStatus + 1 : colStatus - 1;
            dbhsmEncryptColumns.setEncryptionStatus(queryStatus);
            List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(dbhsmEncryptColumns);

            //暂停：加密列为0 解密列为3   继续：加密列为1 解密列为4
            updateEncrypt(colStatus, columnsList);
            //暂停：2  继续：1
            taskQueue.setEncStatus(null != taskQueue.getEncStatus() ? taskStatus : null);
            taskQueue.setDecStatus(null != taskQueue.getDecStatus() ? taskStatus : null);

            dbhsmTaskQueueMapper.updateRecord(taskQueue);
            return AjaxResult.success();
        }

        dbhsmEncryptColumns.setEncryptionStatus(DbConstants.ENC_MODE.equals(request.getTaskMode()) ? 0 : 3);
        List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(dbhsmEncryptColumns);
        if (CollectionUtils.isEmpty(columnsList)) {
            return AjaxResult.error("需要加密的列为空，执行失败");
        }

        //获取数据库基本信息
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(encryptTable.getInstanceId());
        DatabaseDTO database = BeanConvertUtils.beanToBean(dbhsmDbInstance, DatabaseDTO.class);
        database.setDatabaseType(DatabaseTypeEnum.getNameByCode(database.getDatabaseType()));
        database.setDatabaseName(dbhsmDbInstance.getDatabaseServerName());
        database.setInstanceType(dbhsmDbInstance.getDatabaseExampleType());
        database.setDatabaseVersion(DbConstants.DB_TYPE_MYSQL.equals(dbhsmDbInstance.getDatabaseType()) ? "5.7" : dbhsmDbInstance.getDatabaseEdition());
        database.setDbStorageMode(DbConstants.DB_TYPE_HB.equals(dbhsmDbInstance.getDatabaseType()) ? DatabaseDTO.DbStorageMode.COLUMN : DatabaseDTO.DbStorageMode.ROW);
        String instance = CommandUtil.getInstance(dbhsmDbInstance);
        database.setConnectUrl(instance);

        //组装表基本信息
        TableDTO tableDTO = DbConstants.DB_TYPE_HB.equals(dbhsmDbInstance.getDatabaseType()) ? encryptHbaseColumnsInfo(dbhsmDbInstance, columnsList) : encryptColumnsAll(dbhsmDbInstance, columnsList);

        BeanUtils.copyProperties(encryptTable, tableDTO);
        tableDTO.setId(encryptTable.getTableId());
        tableDTO.setBatchSize(encryptTable.getBatchCount());
        tableDTO.setThreadNum(encryptTable.getThreadCount());

        //加密表信息
        List<TableDTO> list = new ArrayList<>();
        list.add(tableDTO);
        database.setTableDTOList(list);

        //加密
        if (DbConstants.ENC_MODE.equals(request.getTaskMode())) {
            new Thread(() -> {
                try {
                    stockDataOperateService.stockDataOperate(database, true);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(encryptTable.getTableName() + "加密失败！:{}", e.getMessage());
                    throw new RuntimeException(encryptTable.getTableName() + "加密失败！");
                }
            }).start();
            taskQueue.setEncStatus(1);
            //修改加密列状态
            updateEncrypt(1, columnsList);
            dbhsmTaskQueueMapper.updateRecord(taskQueue);
            //表状态修改为加密中
            encryptTable.setTableStatus(DbConstants.ENC_FLAG);
            dbhsmEncryptTableMapper.updateRecord(encryptTable);
        } else {
            //解密
            boolean isDel = delViewAndTrigger(columnsList.get(0).getDbTable(), dbhsmDbInstance);
            if (!isDel) {
                return AjaxResult.error("恢复表名失败，无法进行解密");
            }
            new Thread(() -> {
                try {
                    stockDataOperateService.stockDataOperate(database, false);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(encryptTable.getTableName(), "{}：解密失败！:{}", e.getMessage());
                    throw new RuntimeException(encryptTable.getTableName() + "解密失败！");
                }
            }).start();
            taskQueue.setDecStatus(1);
            updateEncrypt(4, columnsList);
            dbhsmTaskQueueMapper.updateRecord(taskQueue);
        }

        return AjaxResult.success();
    }

    public void updateEncrypt(Integer status, List<DbhsmEncryptColumns> dbhsmEncryptColumns) {
        for (DbhsmEncryptColumns dbhsmEncryptColumn : dbhsmEncryptColumns) {
            dbhsmEncryptColumn.setEncryptionStatus(status);
            //修改状态
            dbhsmEncryptColumnsMapper.updateDbhsmEncryptColumns(dbhsmEncryptColumn);
        }
    }

    public boolean operViewToSqlServer(List<DbhsmEncryptColumns> dbhsmEncryptColumns, DbhsmDbInstance instance) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd = BeanConvertUtils.beanToBean(dbhsmEncryptColumns.get(0), DbhsmEncryptColumnsAdd.class);
            dbhsmEncryptColumnsAdd.setDatabaseType(instance.getDatabaseType());
            dbhsmEncryptColumnsAdd.setDatabaseServerName(instance.getDatabaseServerName());
            connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
            instance.setSchema(getDataBaseSchema(connection, dbhsmEncryptColumns.get(0).getDbTable(), instance.getDatabaseType()));
            preparedStatement = connection.prepareStatement("USE  [" + instance.getDatabaseServerName() + "]");
            preparedStatement.execute();
            preparedStatement = connection.prepareStatement("EXEC sp_rename '" + dbhsmEncryptColumnsAdd.getDbTable() + "', '" + dbhsmEncryptColumnsAdd.getDbTable() + "_ENCDATA';");
            boolean execute = preparedStatement.execute();
            log.info("修改表名返回:{}", execute);
            createView(connection, dbhsmEncryptColumns, instance);
            connection.commit();

            //创建触发器
            insertTrigger(connection, dbhsmEncryptColumns, instance);
            connection.commit();
            //更新触发器
            updateTrigger(connection, dbhsmEncryptColumns, instance);
            connection.commit();
            return true;
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            log.error("创建解密函数失败：{}", e.getMessage());
        } finally {
            try {
                connection.close();
                preparedStatement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public void createView(Connection connection, List<DbhsmEncryptColumns> dbhsmEncryptColumnsList, DbhsmDbInstance instance) {
        PreparedStatement preparedStatement = null;
        try {
            DbhsmEncryptColumnsAdd dbhsmEncryptColumns = BeanConvertUtils.beanToBean(dbhsmEncryptColumnsList.get(0), DbhsmEncryptColumnsAdd.class);
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("create view ").append(instance.getSchema()).append(".").append(dbhsmEncryptColumns.getDbTable()).append(" AS ");
            stringBuffer.append(System.lineSeparator());

            //insert into 语句
            StringBuilder into = new StringBuilder();
            String tableName = instance.getSchema() + ":" + dbhsmEncryptColumns.getDbTable() + "_ENCDATA";
            List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(connection, tableName, DbConstants.DB_TYPE_SQLSERVER);
            for (Map<String, String> stringStringMap : allColumnsInfo) {
                String columnName = stringStringMap.get(DbConstants.DB_COLUMN_NAME);
                into.append(columnName).append(",");
            }
            String ip = DBIpUtil.getIp(dbhsmEncryptColumns.getEthernetPort());

            boolean isSel = true;
            for (DbhsmEncryptColumns encryptColumns : dbhsmEncryptColumnsList) {
                if (isSel) {
                    stringBuffer.append("select ").append(System.lineSeparator());
                }
                stringBuffer.append(instance.getSchema() + ".func_string_decrypt_ex(").append(System.lineSeparator());
                stringBuffer.append("'").append(encryptColumns.getId()).append("',").append(System.lineSeparator());
                stringBuffer.append("'http://").append(ip.trim()).append(":80/prod-api/dbhsm/api/datahsm/v1/strategy/get',").append(System.lineSeparator());
                stringBuffer.append("'ip_addr',").append(System.lineSeparator());
                stringBuffer.append("CAST(@@ServerName as char),").append(System.lineSeparator());
                stringBuffer.append("db_name(),").append(System.lineSeparator());
                stringBuffer.append("'").append(encryptColumns.getDbTable()).append("',").append(System.lineSeparator());
                stringBuffer.append("'").append(encryptColumns.getEncryptColumns()).append("',").append(System.lineSeparator());
                stringBuffer.append(" suser_name(),").append(System.lineSeparator());
                stringBuffer.append(encryptColumns.getEncryptColumns() + ",").append("DATALENGTH(" + encryptColumns.getEncryptColumns() + "),").append(System.lineSeparator());
                stringBuffer.append("0,").append("DATALENGTH(" + encryptColumns.getEncryptColumns() + "),").append(System.lineSeparator());
                stringBuffer.append("DATALENGTH(" + encryptColumns.getEncryptColumns() + ")) as " + encryptColumns.getEncryptColumns() + ",").append(System.lineSeparator());
                isSel = false;
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);
            //表中剩余列
            String[] split = into.toString().split(",");
            List<String> colNames = dbhsmEncryptColumnsList.stream().map(DbhsmEncryptColumns::getEncryptColumns).collect(Collectors.toList());
            List<String> residueCol = Arrays.stream(split).filter(s -> colNames.stream().noneMatch(s::equals)).collect(Collectors.toList());
            for (String col : residueCol) {
                stringBuffer.append(col).append(" as ").append(col).append(",");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);

            stringBuffer.append(" from " + instance.getSchema() + "." + dbhsmEncryptColumns.getDbTable() + "_ENCDATA;").append(System.lineSeparator());

            System.out.println(stringBuffer);
            log.info("创建视图：{}", stringBuffer);
            preparedStatement = connection.prepareStatement(stringBuffer.toString());
            boolean execute = preparedStatement.execute();
            log.info("创建视图返回：{}", execute);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("创建视图错误：{}", e.getMessage());
        } finally {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void insertTrigger(Connection connection, List<DbhsmEncryptColumns> dbhsmEncryptColumnsList, DbhsmDbInstance instance) throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            DbhsmEncryptColumnsAdd dbhsmEncryptColumns = BeanConvertUtils.beanToBean(dbhsmEncryptColumnsList.get(0), DbhsmEncryptColumnsAdd.class);
            StringBuffer stringBuffer = new StringBuffer();
            String encTableName = dbhsmEncryptColumns.getDbTable() + "_ENCDATA";
            stringBuffer.append("CREATE TRIGGER [tr_" + encTableName + "_sm4_insert] ").append(System.lineSeparator());
            stringBuffer.append(" ON dbo." + encTableName + " ").append(System.lineSeparator());
            stringBuffer.append(" INSTEAD OF INSERT ").append(System.lineSeparator());
            stringBuffer.append("AS").append(System.lineSeparator());
            stringBuffer.append(" declare").append(System.lineSeparator());
            stringBuffer.append(" @user_ipaddr NVARCHAR(max)").append(System.lineSeparator());
            stringBuffer.append(" BEGIN").append(System.lineSeparator());
            stringBuffer.append("  SET NOCOUNT ON;").append(System.lineSeparator());
            stringBuffer.append("  select @user_ipaddr = client_net_address FROM sys.dm_exec_connections WHERE session_id = @@SPID ").append(System.lineSeparator());
            stringBuffer.append(" insert into dbo." + encTableName);

            //insert into 语句
            StringBuilder into = new StringBuilder();
            String tableName = instance.getSchema() + ":" + encTableName;
            List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(connection, tableName, DbConstants.DB_TYPE_SQLSERVER);
            for (Map<String, String> stringStringMap : allColumnsInfo) {
                String columnName = stringStringMap.get(DbConstants.DB_COLUMN_NAME);
                into.append(columnName).append(",");
            }
            String[] split = into.toString().split(",");
            List<String> colNames = dbhsmEncryptColumnsList.stream().map(DbhsmEncryptColumns::getEncryptColumns).collect(Collectors.toList());
            List<String> residueCol = Arrays.stream(split).filter(s -> colNames.stream().noneMatch(s::equals)).collect(Collectors.toList());
            String encCol = StringUtils.join(colNames, ",");
            String noEncCol = StringUtils.join(residueCol, ",");
            stringBuffer.append("(").append(encCol).append(",").append(noEncCol).append(")").append(System.lineSeparator());
            boolean isSel = true;
            String ip = DBIpUtil.getIp(dbhsmEncryptColumns.getEthernetPort());

            for (DbhsmEncryptColumns encryptColumns : dbhsmEncryptColumnsList) {
                if (isSel) {
                    stringBuffer.append("select dbo.func_string_encrypt_ex(").append(System.lineSeparator());
                } else {
                    stringBuffer.append("dbo.func_string_encrypt_ex(").append(System.lineSeparator());
                }
                stringBuffer.append("'").append(encryptColumns.getId()).append("',").append(System.lineSeparator());
                stringBuffer.append("'http://").append(ip.trim()).append(":80/prod-api/dbhsm/api/datahsm/v1/strategy/get',").append(System.lineSeparator());
                stringBuffer.append(" @user_ipaddr, ").append(System.lineSeparator());
                stringBuffer.append(" CAST(@@ServerName as char),").append(System.lineSeparator());
                stringBuffer.append("db_name(),").append(System.lineSeparator());
                stringBuffer.append("'").append(encryptColumns.getDbTable()).append("',").append(System.lineSeparator());
                stringBuffer.append("'").append(encryptColumns.getEncryptColumns()).append("',").append(System.lineSeparator());
                stringBuffer.append(" suser_name(),").append(System.lineSeparator());
                stringBuffer.append("i." + encryptColumns.getEncryptColumns() + ",").append("DATALENGTH(i." + encryptColumns.getEncryptColumns() + "),").append(System.lineSeparator());
                stringBuffer.append("0,").append("DATALENGTH(i." + encryptColumns.getEncryptColumns() + "),").append(System.lineSeparator());
                stringBuffer.append("DATALENGTH(i." + encryptColumns.getEncryptColumns() + ")),").append(System.lineSeparator());
                isSel = false;
            }
//            stringBuffer.deleteCharAt(stringBuffer.length() - 1);
            //表中剩余列

            for (String col : residueCol) {
                stringBuffer.append("i.").append(col).append(",");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);

            stringBuffer.append(" FROM inserted i;").append(System.lineSeparator());
            stringBuffer.append("END").append(System.lineSeparator());

            log.info("插入触发器：{}", stringBuffer);
            preparedStatement = connection.prepareStatement(stringBuffer.toString());
            preparedStatement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("创建插入触发器错误：{}", e.getMessage());
        } finally {
            preparedStatement.close();
        }

    }

    public void updateTrigger(Connection connection, List<DbhsmEncryptColumns> dbhsmEncryptColumnsList, DbhsmDbInstance instance) throws SQLException {
        DbhsmEncryptColumnsAdd dbhsmEncryptColumns = BeanConvertUtils.beanToBean(dbhsmEncryptColumnsList.get(0), DbhsmEncryptColumnsAdd.class);
        StringBuilder update = new StringBuilder();
        String priKey = "";
        PreparedStatement preparedStatement = null;
        try {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("CREATE TRIGGER [tr_" + dbhsmEncryptColumns.getDbTable() + "_ENCDATA_sm4_update]").append(System.lineSeparator());
            stringBuffer.append(" ON dbo." + dbhsmEncryptColumns.getDbTable() + "_ENCDATA ").append(System.lineSeparator());
            stringBuffer.append("  AFTER UPDATE ").append(System.lineSeparator());
            stringBuffer.append("AS").append(System.lineSeparator());
            stringBuffer.append(" declare").append(System.lineSeparator());
            stringBuffer.append(" @user_ipaddr NVARCHAR(max)").append(System.lineSeparator());
            stringBuffer.append(" BEGIN").append(System.lineSeparator());
            stringBuffer.append("  SET NOCOUNT ON;").append(System.lineSeparator());
            stringBuffer.append("  select @user_ipaddr = client_net_address FROM sys.dm_exec_connections WHERE session_id = @@SPID ").append(System.lineSeparator());
            stringBuffer.append(" UPDATE dbo." + dbhsmEncryptColumns.getDbTable() + "_ENCDATA ");

            //insert into 语句
            String tableName = instance.getSchema() + ":" + dbhsmEncryptColumns.getDbTable() + "_ENCDATA";
            List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(connection, tableName, DbConstants.DB_TYPE_SQLSERVER);
            for (Map<String, String> stringStringMap : allColumnsInfo) {
                String columnName = stringStringMap.get(DbConstants.DB_COLUMN_NAME);
                update.append(columnName).append(",");
                if (stringStringMap.containsKey(DbConstants.DB_COLUMN_KEY)) {
                    priKey = stringStringMap.get(DbConstants.DB_COLUMN_NAME);
                }
            }
            update.deleteCharAt(update.length() - 1);

            String ip = DBIpUtil.getIp(dbhsmEncryptColumns.getEthernetPort());
            boolean isSet = true;
            for (DbhsmEncryptColumns encryptColumns : dbhsmEncryptColumnsList) {
                if (isSet) {
                    stringBuffer.append(" SET ");
                }
                stringBuffer.append(encryptColumns.getEncryptColumns()).append("= ").append(instance.getDatabaseServerName()).append(".dbo.func_string_encrypt_ex(").append(System.lineSeparator());
                stringBuffer.append("'").append(encryptColumns.getId()).append("',").append(System.lineSeparator());
                stringBuffer.append("'http://").append(ip.trim()).append(":80/prod-api/dbhsm/api/datahsm/v1/strategy/get',").append(System.lineSeparator());
                stringBuffer.append(" @user_ipaddr, ").append(System.lineSeparator());
                stringBuffer.append(" CAST(@@ServerName as char),").append(System.lineSeparator());
                stringBuffer.append("db_name(),").append(System.lineSeparator());
                stringBuffer.append("'").append(encryptColumns.getDbTable()).append("',").append(System.lineSeparator());
                stringBuffer.append("'").append(encryptColumns.getEncryptColumns()).append("',").append(System.lineSeparator());
                stringBuffer.append(" suser_name(),").append(System.lineSeparator());
                stringBuffer.append("i." + encryptColumns.getEncryptColumns() + ",").append("DATALENGTH(i." + encryptColumns.getEncryptColumns() + "),").append(System.lineSeparator());
                stringBuffer.append("0,").append("DATALENGTH(i." + encryptColumns.getEncryptColumns() + "),").append(System.lineSeparator());
                stringBuffer.append("DATALENGTH(i." + encryptColumns.getEncryptColumns() + ")),").append(System.lineSeparator());
                isSet = false;
            }

            stringBuffer.deleteCharAt(stringBuffer.length() - 1);

            //表中剩余列
            String[] split = update.toString().split(",");
            List<String> colNames = dbhsmEncryptColumnsList.stream().map(DbhsmEncryptColumns::getEncryptColumns).collect(Collectors.toList());
            List<String> residueCol = Arrays.stream(split).filter(s -> colNames.stream().noneMatch(s::equals)).collect(Collectors.toList());
            for (String col : residueCol) {
                stringBuffer.append(col).append("=i.").append(col).append(",");
            }
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);


            stringBuffer.append(" FROM dbo." + dbhsmEncryptColumns.getDbTable() + "_ENCDATA e").append(System.lineSeparator());
            stringBuffer.append(" JOIN inserted i ON e." + priKey + " = i." + priKey + ";").append(System.lineSeparator());
            stringBuffer.append("END").append(System.lineSeparator());

            log.info("更新触发器：{}", stringBuffer);
            preparedStatement = connection.prepareStatement(stringBuffer.toString());
            preparedStatement.executeUpdate();

            //instead of
            insteadBySqlServer(connection, "INSERT", dbhsmEncryptColumns.getDbTable(), update.toString(), priKey);
            insteadBySqlServer(connection, "UPDATE", dbhsmEncryptColumns.getDbTable(), update.toString(), priKey);
            insteadBySqlServer(connection, "DELETE", dbhsmEncryptColumns.getDbTable(), update.toString(), priKey);
        } catch (Exception e) {
            log.error("创建更新触发器失败：{}", e.getMessage());
        } finally {
            preparedStatement.close();
        }
    }

    public void insteadBySqlServer(Connection connection, String instType, String tableName, String cols, String keyCol) throws SQLException {
        StringBuffer sb = new StringBuffer();
        sb.append("create trigger ").append(tableName).append("_instead_" + instType.toLowerCase()).append(System.lineSeparator());
        sb.append("on dbo.").append(tableName).append(System.lineSeparator());
        sb.append("instead of ").append(instType).append(System.lineSeparator());
        sb.append("AS").append(System.lineSeparator());
        sb.append("begin").append(System.lineSeparator());
        if ("INSERT".equals(instType)) {
            sb.append("INSERT INTO dbo.").append(tableName).append("_ENCDATA ").append(System.lineSeparator());
            sb.append("(").append(cols).append(")").append(System.lineSeparator());
            sb.append("SELECT ").append(cols).append(System.lineSeparator());
            sb.append("FROM inserted;").append(System.lineSeparator());
        } else if ("UPDATE".equals(instType)) {
            sb.append("UPDATE dbo.").append(tableName).append("_ENCDATA").append(System.lineSeparator());
            sb.append("SET ").append(System.lineSeparator());
            String[] split = cols.split(",");
            for (String col : split) {
                sb.append(col).append(" = ").append("i.").append(col).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(" FROM dbo.").append(tableName).append("_ENCDATA e").append(System.lineSeparator());
            sb.append("JOIN inserted i ON e.").append(keyCol).append(" = i.").append(keyCol).append(";").append(System.lineSeparator());
        } else if ("DELETE".equals(instType)) {
            sb.append("DELETE FROM dbo.").append(tableName).append("_ENCDATA ").append(System.lineSeparator());
            sb.append("where ").append(keyCol).append(" in (select ").append(keyCol).append(" from deleted);").append(System.lineSeparator());
        }
        sb.append("end;").append(System.lineSeparator());

        log.info("instead of触发器执行语句：{}", sb);
        PreparedStatement preparedStatement = connection.prepareStatement(sb.toString());
        boolean execute = preparedStatement.execute();
        log.info(instType, "{}：类型instead of触发器：{}", execute);

        preparedStatement.close();
    }

    public boolean delViewAndTrigger(String tableName, DbhsmDbInstance instance) {
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        try {
            connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
            //1.切换database
            preparedStatement = connection.prepareStatement("USE  [" + instance.getDatabaseServerName() + "]");
            preparedStatement.execute();
            //2.删视图
            preparedStatement = connection.prepareStatement("DROP VIEW " + instance.getSchema() + "." + tableName + ";");
            boolean dropView = preparedStatement.execute();
            //3.删除触发器
            preparedStatement = connection.prepareStatement("DROP TRIGGER " + instance.getSchema() + ".tr_" + tableName + "_ENCDATA_sm4_insert;");
            boolean dropInsert = preparedStatement.execute();
            preparedStatement = connection.prepareStatement("DROP TRIGGER " + instance.getSchema() + ".tr_" + tableName + "_ENCDATA_sm4_update;");
            boolean dropUpdate = preparedStatement.execute();
            //4.恢复表名
            preparedStatement = connection.prepareStatement("EXEC " + instance.getDatabaseServerName() + ".sys.sp_rename N'" + instance.getDatabaseServerName() + "." + instance.getSchema() + "." + tableName + "_ENCDATA', N'" + tableName + "', 'OBJECT';");
            boolean execTable = preparedStatement.execute();

            connection.commit();
            log.info("删除视图：{}，删除新增触发器：{}，删除更新触发器：{}，修改表名：{}", dropView, dropInsert, dropUpdate, execTable);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("删除视图、触发器、修改表名错误：{}", e.getMessage());
            return false;
        } finally {
            try {
                preparedStatement.close();
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 组装字段基本信息
     *
     * @param dbhsmDbInstance     数据库实例
     * @param dbhsmEncryptColumns 加密字段列
     * @return List<ColumnDTO>
     * @throws ZAYKException
     * @throws SQLException
     */
    public TableDTO encryptColumnsAll(DbhsmDbInstance dbhsmDbInstance, List<DbhsmEncryptColumns> dbhsmEncryptColumns) {
        TableDTO tableDTO = new TableDTO();
        Connection conn = null;
        try {
            tableDTO.setSchema(dbhsmDbInstance.getDatabaseServerName());
            DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
            BeanUtils.copyProperties(dbhsmDbInstance, connDTO);
            conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
            if (Optional.ofNullable(conn).isPresent()) {
                //SQL Server 单独获取schema
                String schema = "";
                if (DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType())) {
                    schema = getDataBaseSchema(conn, dbhsmEncryptColumns.get(0).getDbTable(), dbhsmDbInstance.getDatabaseType());
                    tableDTO.setSchema(schema);
                    dbhsmDbInstance.setSchema(schema);
                }

                List<ColumnDTO> list = new ArrayList<>();
                for (DbhsmEncryptColumns dbhsmEncryptColumn : dbhsmEncryptColumns) {
                    ColumnDTO columnDTO = new ColumnDTO();
                    Statement stmt = conn.createStatement();
                    ResultSet resultSet = null;
                    String tableName = dbhsmEncryptColumn.getDbTable();
                    if (dbhsmEncryptColumn.getEncryptionStatus() == 3 && DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType()) && DbConstants.BE_PLUG.equals(dbhsmDbInstance.getPlugMode())) {
                        tableName = dbhsmEncryptColumn.getDbTable() + "_encdata";
                    }
                    String sql = databaseTypeSqlColumns(dbhsmDbInstance.getDatabaseType(), tableName, schema);
                    log.info("获取表字段信息SQL：{}", sql);
                    resultSet = stmt.executeQuery(sql);
                    while (resultSet.next()) {
                        Map<String, String> map = new HashMap<>();
                        String columnName = resultSet.getString("Field");
                        //只获取需要加密的列字段
                        if (!dbhsmEncryptColumn.getEncryptColumns().equals(columnName)) {
                            continue;
                        }
                        //解密时 使用加密前的列类型  从加密列字段获取
                        String type = dbhsmEncryptColumn.getColumnsType();
                        String isNullable = resultSet.getString("Null");
                        String key = resultSet.getString("Key");
                        String isDefault = resultSet.getString("Default");
                        String remarks = resultSet.getString("Comment");

                        map.put("Field", columnName);
                        map.put("type", type);
                        map.put("Null", isNullable);
                        map.put("Key", key);
                        map.put("Default", isDefault);
                        map.put("Comment", remarks);

                        columnDTO.setColumnDefinition(map);
                        columnDTO.setColumnName(columnName);
                        columnDTO.setNotNull(!"YES".equalsIgnoreCase(isNullable));
                        columnDTO.setComment(remarks);
                        columnDTO.setEncryptAlgorithm("TestAlg");
                        DbhsmSecretKeyManage dbhsmSecretKeyManage = dbhsmSecretKeyManageMapper.selectDbhsmSecretKeyId(dbhsmEncryptColumn.getSecretKeyId());
                        if (null != dbhsmSecretKeyManage) {
                            columnDTO.setEncryptKeyIndex(Long.toString(dbhsmSecretKeyManage.getSecretKeyIndex()));
                        }
                        list.add(columnDTO);
                    }
                }
                tableDTO.setColumnDTOList(list);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return new TableDTO();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return tableDTO;
    }

    public static String databaseTypeSqlColumns(String type, String table, String schema) {
        String sql = "";
        if (DbConstants.DB_TYPE_ORACLE.equals(type)) {
            sql = "SELECT cols.column_name AS \"Field\", cols.data_type || CASE WHEN cols.data_type IN ('VARCHAR2', 'CHAR', 'NUMBER') THEN '(' || cols.data_length || ')' ELSE '' END AS \"type\",\n" +
                    "   CASE WHEN cols.nullable = 'Y' THEN 'YES' ELSE 'NO' END AS  \"Null\", cols.data_default AS \"Default\", comments.comments AS \"Comment\",CASE WHEN pk.constraint_type = 'P' THEN 'PRI' ELSE '' END AS \"Key\"\n" +
                    "FROM  user_tab_columns cols LEFT JOIN user_col_comments comments\n" +
                    "ON cols.table_name = comments.table_name AND cols.column_name = comments.column_name\n" +
                    "LEFT JOIN (SELECT uc.table_name, ucc.column_name, uc.constraint_type FROM user_cons_columns ucc JOIN user_constraints uc \n" +
                    "     ON uc.constraint_name = ucc.constraint_name WHERE uc.constraint_type = 'P') pk ON cols.table_name = pk.table_name AND cols.column_name = pk.column_name\n" +
                    "WHERE cols.table_name = '" + table + "'";
        } else if (DbConstants.DB_TYPE_SQLSERVER.equals(type)) {
            //sqlserver
            sql = "SELECT \n" +
                    "  'Field' = a.name, 'Key' = case when exists( SELECT 1 FROM sysobjects \n" +
                    "    where xtype = 'PK' and parent_obj = a.id and name in (\n" +
                    "        SELECT name FROM sysindexes WHERE indid in( SELECT indid FROM sysindexkeys WHERE id = a.id AND colid = a.colid))\n" +
                    "  ) then 'PRI' else '' end, 'Type' = b.name, \n" +
                    "  'Null' = case when a.isnullable = 1 then 'YES' else 'NO' end, 'Default' = isnull(e.text, ''), 'Comment' = isnull(g.[value], '') \n" +
                    "FROM syscolumns a \n" +
                    "  left join systypes b on a.xusertype = b.xusertype inner join sysobjects d on a.id = d.id and d.xtype = 'U' and d.name<>'dtproperties' \n" +
                    "  left join syscomments e on a.cdefault = e.id \n" +
                    "  left join sys.extended_properties g on a.id = G.major_id and a.colid = g.minor_id \n" +
                    "  left join sys.extended_properties f on d.id = f.major_id and f.minor_id = 0   INNER JOIN sys.schemas s ON d.uid = s.schema_id \n" +
                    "where d.name = '" + table + "' and  s.name = '" + schema + "' order by a.id, a.colorder";
        } else if (DbConstants.DB_TYPE_DM.equals(type)) {
            //dm
            sql = "SELECT\n" +
                    "\ta.column_name AS \"Field\",\n" +
                    "\ta.data_type || CASE WHEN a.data_type != 'TEXT' THEN '(' || a.data_length || ')'ELSE '' END AS \"type\" ,\n" +
                    "\tc.comments AS \"Comment\",\n" +
                    "\ta.nullable AS \"Null\",\n" +
                    "\ta.data_default AS \"Default\",\n" +
                    "\t(SELECT CASE WHEN t.CONSTRAINT_TYPE='P' then 'PRI' else 'MUL' END as KEY FROM USER_CONSTRAINTS t JOIN USER_CONS_COLUMNS b ON t.constraint_name = b.constraint_name WHERE b.table_name = '" + table + "' AND b.column_name = a.column_name) AS \"key\"\n" +
                    "FROM all_tab_columns a LEFT JOIN all_col_comments c ON a.table_name = c.table_name AND a.column_name = c.column_name\n" +
                    "WHERE a.TABLE_NAME = '" + table + "'";
        } else if (DbConstants.DB_TYPE_POSTGRESQL.equals(type)) {
            //pgSql
            sql = "SELECT table_schema,column_name as Field, data_type as Type, is_nullable as Null, column_default as Default,'' as Comment,\n" +
                    "CASE WHEN (column_name = (SELECT a.attname AS pk_column_name FROM pg_class t,pg_attribute a,pg_constraint c WHERE c.contype = 'p'AND c.conrelid = t.oid AND a.attrelid = t.oid AND a.attnum = ANY(c.conkey) AND t.relkind = 'r' AND t.relname = '" + table + "' limit 1))THEN  'PRI' ELSE  '' END  as key\n" +
                    "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + table + "' and table_schema=(SELECT table_schema FROM information_schema.columns WHERE table_name = '" + table + "' LIMIT 1);";
        } else if (DbConstants.DB_TYPE_KB.equals(type)) {
            sql = "SELECT c.table_schema,c.column_name AS Field, c.data_type AS Type, c.is_nullable AS Null, c.column_default AS Default,'' as Comment,\n" +
                    "(SELECT tc.constraint_type\n" +
                    "FROM information_schema.table_constraints AS tc\n" +
                    "JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema\n" +
                    "WHERE tc.constraint_type = 'PRIMARY KEY' AND tc.table_name = c.table_name and kcu.column_name = c.column_name) as Key\n" +
                    "FROM information_schema.columns c WHERE c.table_name = '" + table + "' AND c.table_schema not in('S-C','public')";
        } else if (DbConstants.DB_TYPE_MYSQL.equals(type)) {
            //MySql
            sql = "SHOW FULL COLUMNS from " + table;
        } else if (DbConstants.DB_TYPE_CLICKHOUSE.equals(type)) {
            //ClickHouse
            sql = "select name as Field,type as Type,comment as Comment,default_expression as Default,if(is_in_primary_key = 1,'PRI','') as Key,\n" +
                    "if(type like '%Nullable%','YES','NO') as Null from system.columns where database = '" + schema + "' and  table='" + table + "';";
        }
        return sql;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public AjaxResult insertDecColumnsOnEnc(TaskDecColumnsOnEncRequest request) {
        /**
         * 解密队列添加到加密队列
         */
        DbhsmTaskQueue dbhsmTaskQueue = dbhsmTaskQueueMapper.findByPrimaryKey(request.getTaskId());
        DbhsmEncryptTable encryptTable = dbhsmEncryptTableMapper.findByPrimaryKey(dbhsmTaskQueue.getTableId());

        //更新表数据
        encryptTable.setThreadCount(request.getThreadCount());
        encryptTable.setBatchCount(request.getBatchCount());
        dbhsmEncryptTableMapper.updateRecord(encryptTable);

        DbhsmEncryptColumns dbhsmEncryptColumn = new DbhsmEncryptColumns();
        dbhsmEncryptColumn.setTableId(String.valueOf(encryptTable.getTableId()));
        dbhsmEncryptColumn.setEncryptionStatus(3);
        //如果当前队列是加密队列  查询已加密的列  ->  否则查询未开始解密的列
        if (null == dbhsmTaskQueue.getDecStatus()) {
            dbhsmEncryptColumn.setEncryptionStatus(2);
        }

        //查询表中的解密列
        List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(dbhsmEncryptColumn);

        //队列数据为空不做任何操作
        if (CollectionUtils.isEmpty(columnsList)) {
            return AjaxResult.error("表中的字段无法添加至加密队列");
        }

        //更新当期队列中列的状态  如果当前队列为加密完成更改为未解密密  如果是未解密数据  更改完已加密
        for (DbhsmEncryptColumns dbhsmEncryptColumns : columnsList) {
            for (EncryptColumns encryptColumns : request.getEncryptedLists()) {
                if (encryptColumns.getEncryptColumns().equals(dbhsmEncryptColumns.getEncryptColumns())) {
                    BeanUtils.copyProperties(encryptColumns, dbhsmEncryptColumns);
                    dbhsmEncryptColumns.setEncryptionStatus(2);
                    if (null == dbhsmTaskQueue.getDecStatus()) {
                        dbhsmEncryptColumns.setEncryptionStatus(3);
                    }
                    dbhsmEncryptColumnsMapper.updateDbhsmEncryptColumns(dbhsmEncryptColumns);
                }
            }
        }

        //如果当前队列中的数据全部被选中 那么当前任务队列直接删除 不记录队列状态
        if (columnsList.size() == request.getEncryptedLists().size()) {
            dbhsmTaskQueueMapper.deleteRecords(request.getTaskId());
        }

        //改步骤为查询  该表是否存在任务队列，如果当前任务是加密 ->解密  就查询当前表的解密队列反之加密队列
        DbhsmTaskQueue taskQueue = dbhsmTaskQueueMapper.queryTableTask(dbhsmTaskQueue.getTableId(), null == dbhsmTaskQueue.getEncStatus() ? "enc" : "dec");

        //如果当前表没有需要操作的任务队列，进行新增反之什么也不做
        //队列存储结构为  一个表->多个任务
        if (null == taskQueue) {
            DbhsmTaskQueue newTaskQueue = new DbhsmTaskQueue();
            newTaskQueue.setTableId(dbhsmTaskQueue.getTableId());
            newTaskQueue.setCreateTime(new Date());
            newTaskQueue.setCreateBy(SecurityUtils.getUsername());
            if (null == dbhsmTaskQueue.getDecStatus()) {
                newTaskQueue.setDecStatus(0);
            } else {
                //解密至加密队列队列状态为已完成
                newTaskQueue.setEncStatus(3);
            }
            //如果为空则新数据
            dbhsmTaskQueueMapper.insertRecord(newTaskQueue);
        }

        return AjaxResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public AjaxResult insertTask(TaskQueueInsertRequest request) {

        if (request.getBatchCount() > 3500 || request.getBatchCount() < 100) {
            return AjaxResult.error("每批条数限制为：100-3500");
        }

        if (request.getThreadCount() > 16 || request.getThreadCount() < 1) {
            return AjaxResult.error("线程数限制为：1-16");
        }


        //数据库实例信息
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(request.getInstanceId());

        DbhsmEncryptTable dbhsmEncryptTable = BeanConvertUtils.beanToBean(request, DbhsmEncryptTable.class);
        dbhsmEncryptTable.setTableStatus(DbConstants.CREATED_ON_WEB_SEDE);

        try {
            //Hbase不添加DDL语句
            if (!DbConstants.DB_TYPE_HB.equals(dbhsmDbInstance.getDatabaseType()) && !DbConstants.DB_TYPE_POSTGRESQL.equals(dbhsmDbInstance.getDatabaseType())) {
                //获取表的DDL语句
                String ddl = showTableDdl(dbhsmDbInstance, request.getTableName());
                dbhsmEncryptTable.setTableDdl(ddl);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String snowflakeId;
        //加密逻辑
        DbhsmEncryptTable encryptTable = dbhsmEncryptTableMapper.queryTableRecord(request.getInstanceId(), request.getTableName());
        if (null == encryptTable) {
            snowflakeId = SnowFlakeUtil.getSnowflakeId();
            dbhsmEncryptTable.setTableId(Long.valueOf(snowflakeId));
            dbhsmEncryptTable.setCreateTime(DateUtils.getNowDate());
            dbhsmEncryptTable.setCreateBy(SecurityUtils.getUsername());
            dbhsmEncryptTableMapper.insertRecord(dbhsmEncryptTable);
        } else {
            snowflakeId = String.valueOf(encryptTable.getTableId());
            List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptByTableId(snowflakeId);
            //删除未开始加密的数据  删除  ->   新增
            String[] array = columnsList.stream().filter(col -> DbConstants.DEC_FLAG == col.getEncryptionStatus()).map(DbhsmEncryptColumns::getId).toArray(String[]::new);
            if (array.length > 0) {
                dbhsmEncryptColumnsMapper.deleteDbhsmEncryptColumnsByIds(array);
            }
        }

        String instance = CommandUtil.getInstance(dbhsmDbInstance);

        //加密队列
        List<EncryptColumns> encryptedLists = request.getEncryptedLists();
        for (EncryptColumns encryptColumns : encryptedLists) {
            DbhsmEncryptColumns dbhsmEncryptColumns = BeanConvertUtils.beanToBean(encryptColumns, DbhsmEncryptColumns.class);
            dbhsmEncryptColumns.setId(SnowFlakeUtil.getSnowflakeId());
            dbhsmEncryptColumns.setTableId(snowflakeId);
            dbhsmEncryptColumns.setDbTable(dbhsmEncryptTable.getTableName());
            dbhsmEncryptColumns.setDbInstance(instance);
            dbhsmEncryptColumns.setDbInstanceId(dbhsmDbInstance.getId());
            //加密队列
            dbhsmEncryptColumns.setCreateTime(DateUtils.getNowDate());
            dbhsmEncryptColumns.setCreateBy(SecurityUtils.getUsername());
            dbhsmEncryptColumns.setEncryptionStatus(DbConstants.DEC_FLAG);
            dbhsmEncryptColumns.setDbUserName(StringUtils.isBlank(request.getUserName()) ? null : request.getUserName());
            dbhsmEncryptColumnsMapper.insertDbhsmEncryptColumns(dbhsmEncryptColumns);
        }

        //先查询是否已经有队列存在，防止追加
        DbhsmTaskQueue taskQueue = dbhsmTaskQueueMapper.queryTableTask(snowflakeId, DbConstants.ENC_MODE);
        if (null == taskQueue) {
            //添加任务表
            DbhsmTaskQueue dbhsmTaskQueue = new DbhsmTaskQueue();
            dbhsmTaskQueue.setTableId(snowflakeId);
            dbhsmTaskQueue.setCreateTime(DateUtils.getNowDate());
            dbhsmTaskQueue.setCreateBy(SecurityUtils.getUsername());
            dbhsmTaskQueue.setEncStatus(DbConstants.DEC_FLAG);
            dbhsmTaskQueueMapper.insertRecord(dbhsmTaskQueue);
        }

        return AjaxResult.success();
    }

    public String showTableDdl(DbhsmDbInstance dbhsmDbInstance, String table) throws Exception {
        String ddl = "";
        DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance, connDTO);

        Connection conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
        Statement stmt = conn.createStatement();

        String sql = "";
        int index = 1;
        if (DbConstants.DB_TYPE_ORACLE.equals(dbhsmDbInstance.getDatabaseType())) {
            sql = "SELECT DBMS_METADATA.GET_DDL('TABLE', '" + table + "') FROM " + table;
        } else if (DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType())) {
            String sqlServerSchema = getDataBaseSchema(conn, table, dbhsmDbInstance.getDatabaseType());
            return getSqlServerDDL(sqlServerSchema, table, conn);
        } else if (DbConstants.DB_TYPE_MYSQL.equals(dbhsmDbInstance.getDatabaseType()) || DbConstants.DB_TYPE_CLICKHOUSE.equals(dbhsmDbInstance.getDatabaseType())) {
            sql = "SHOW CREATE TABLE " + table;
            index = DbConstants.DB_TYPE_MYSQL.equals(dbhsmDbInstance.getDatabaseType()) ? 2 : 1;
        } else if (DbConstants.DB_TYPE_KB.equals(dbhsmDbInstance.getDatabaseType())) {
            //组装创建表的DDL以及主键信息
            sql = "SELECT 'CREATE TABLE ' || table_name || ' (' || array_to_string( array_agg(\n" +
                    "            column_name || ' ' || data_type ||\n" +
                    "            CASE WHEN data_type IN ('integer', 'int', 'int4', 'int8') THEN '' ELSE COALESCE('(' || character_maximum_length || ')', '') END ||\n" +
                    "            CASE WHEN is_nullable = 'NO' THEN ' NOT NULL' ELSE '' END ||\n" +
                    "            COALESCE(' ' || column_default, '') ), ', ' ) || '); ' ||\n" +
                    "    COALESCE((\n" +
                    "        SELECT 'ALTER TABLE ' || kcu.table_name || ' ADD CONSTRAINT ' || tc.constraint_name || ' PRIMARY KEY (' || string_agg(kcu.column_name, ', ') || ');'\n" +
                    "        FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name\n" +
                    "        WHERE tc.table_schema = ( SELECT table_schema FROM information_schema.columns WHERE table_name = '" + table + "' LIMIT 1 )\n" +
                    "        AND tc.table_name = '" + table + "' AND tc.constraint_type = 'PRIMARY KEY' GROUP BY kcu.table_name, tc.constraint_name ), '') AS ddl_statement\n" +
                    "FROM information_schema.columns\n" +
                    "WHERE table_schema = ( SELECT table_schema FROM information_schema.columns WHERE table_name = '" + table + "'  LIMIT 1 )\n" +
                    "    AND table_name = '" + table + "' GROUP BY table_name;";
        }

        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            ddl = rs.getString(index);
        }
        conn.close();
        return ddl;
    }

    public static String getSqlServerDDL(String schemaName, String tableName, Connection connection) throws SQLException {
        // Step 1: Query the INFORMATION_SCHEMA.COLUMNS view
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = '" + schemaName + "' AND TABLE_NAME = '" + tableName + "' " +
                "ORDER BY ORDINAL_POSITION";

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);

        StringBuilder createTableSQL = new StringBuilder("CREATE TABLE [" + tableName + "] (\n");

        while (resultSet.next()) {
            String columnName = resultSet.getString("COLUMN_NAME");
            String dataType = resultSet.getString("DATA_TYPE");
            int maxLength = resultSet.getInt("CHARACTER_MAXIMUM_LENGTH");
            String isNullable = resultSet.getString("IS_NULLABLE");
            String columnDefault = resultSet.getString("COLUMN_DEFAULT");

            createTableSQL.append("    [").append(columnName).append("] ").append(dataType);

            if (maxLength > 0 && maxLength != Integer.MAX_VALUE) {
                createTableSQL.append("(").append(maxLength).append(")");
            } else if (maxLength == Integer.MAX_VALUE) {
                createTableSQL.append("(MAX)");
            }

            if ("NO".equals(isNullable)) {
                createTableSQL.append(" NOT NULL");
            } else {
                createTableSQL.append(" NULL");
            }

            if (columnDefault != null) {
                createTableSQL.append(" DEFAULT ").append(columnDefault);
            }

            createTableSQL.append(",\n");
        }

        // Remove the last comma and add the closing parenthesis
        int lastCommaIndex = createTableSQL.lastIndexOf(",");
        if (lastCommaIndex != -1) {
            createTableSQL.delete(lastCommaIndex, lastCommaIndex + 1);
        }
        createTableSQL.append("\n);");

        // Step 3: Print the generated CREATE TABLE statement
        System.out.println(createTableSQL.toString());
        return createTableSQL.toString();
    }

    /*
     * @description 根据表名查询所属schema 但不包含默认的schema,当找不到schema时使用默认schema
     * 不包含默认schema：SQLserver dbo \ kingbase public
     * @author wzh [zhwang2012@yeah.net]
     * @date 16:25 2024/6/27
     */
    public static String getDataBaseSchema(Connection conn, String tableName, String dataBaseType) {
        String schemaName = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        try {
            if (DbConstants.DB_TYPE_SQLSERVER.equals(dataBaseType)) {
                Map<String, String> map = new HashMap<>();
                stmt = conn.createStatement();
                resultSet = stmt.executeQuery("SELECT s.name AS SchemaName,t.name AS TableName FROM sys.tables t INNER JOIN sys.schemas s ON t.schema_id = s.schema_id  where s.name !='dbo' and t.name ='" + tableName + "'");
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    map.put("schemaName", resultSet.getString("SchemaName"));
                }
                schemaName = "dbo";
                if (map.containsKey("schemaName")) {
                    schemaName = map.get("schemaName");
                }
            }
            if (DbConstants.DB_TYPE_POSTGRESQL.equals(dataBaseType)) {
                List<String> list = new ArrayList<>();
                stmt = conn.createStatement();
                resultSet = stmt.executeQuery("SELECT table_schema FROM information_schema.tables WHERE table_name = '" + tableName + "';");
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    list.add(resultSet.getString(1));
                }
                schemaName = !CollectionUtils.isEmpty(list) ? list.get(0) : "";
            }
            if (DbConstants.DB_TYPE_KB.equals(dataBaseType)) {
                List<String> list = new ArrayList<>();
                stmt = conn.createStatement();
                resultSet = stmt.executeQuery("SELECT table_schema FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '" + tableName + "' and table_schema not in('PUBLIC','S-C') limit 1");
                while (resultSet.next()) {//如果对象中有数据，就会循环打印出来
                    list.add(resultSet.getString(1));
                }
                schemaName = !CollectionUtils.isEmpty(list) ? list.get(0) : "public";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != resultSet) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                if (null != stmt) {
                    try {
                        stmt.close();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return schemaName;
    }

    /**
     * 修改数据库加密列
     *
     * @param request 数据库加密列
     * @return 结果
     */
    @Override
    public AjaxResult updateDbhsmEncryptColumns(UpEncryptColumnsRequest request) {

        //查询当前任务队列信息
        DbhsmTaskQueue taskQueue = dbhsmTaskQueueMapper.findByPrimaryKey(request.getTaskId());

        if (null == taskQueue) {
            return AjaxResult.error("未找到配置的加密队列信息");
        }

        //如果当前队列状态不是未开始和已完成
        if (DbConstants.DEC_FLAG != taskQueue.getEncStatus() && !DbConstants.ENCRYPTING.equals(taskQueue.getEncStatus())) {
            return AjaxResult.error("当前表中的字段无法编辑");
        }

        //获取实例信息
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(Long.valueOf(request.getDbInstanceId()));
        String instance = CommandUtil.getInstance(dbhsmDbInstance);

        DbhsmEncryptColumns columns = new DbhsmEncryptColumns();
        //根据数据库实例查询配置的加密列有哪些
        columns.setDbInstanceId(Long.valueOf(request.getDbInstanceId()));
        columns.setDbInstance(instance);
        columns.setTableId(taskQueue.getTableId());

        //获取实例在队列中的数据
        List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(columns);

        //获取需要放到队列里面的列信息
        List<EncryptColumns> list = request.getList();

        //先删除所有当前队列中未加密的列
        String[] noEncList = columnsList.stream().filter(ec -> DbConstants.DEC_FLAG == ec.getEncryptionStatus()).map(DbhsmEncryptColumns::getId).toArray(String[]::new);
        if (0 != noEncList.length) {
            dbhsmEncryptColumnsMapper.deleteDbhsmEncryptColumnsByIds(noEncList);
        }

        DbhsmEncryptTable encryptTable = dbhsmEncryptTableMapper.findByPrimaryKey(taskQueue.getTableId());
        encryptTable.setBatchCount(request.getBatchCount());
        encryptTable.setThreadCount(request.getThreadCount());
        dbhsmEncryptTableMapper.updateRecord(encryptTable);

        columns.setTableId(String.valueOf(encryptTable.getTableId()));
        columns.setDbTable(encryptTable.getTableName());

        //新增数据
        for (EncryptColumns encryptColumns : list) {
            //赋值字段信息
            BeanUtils.copyProperties(encryptColumns, columns);
            columns.setEncryptionStatus(DbConstants.DEC_FLAG);
            columns.setId(SnowFlakeUtil.getSnowflakeId());
            columns.setCreateTime(DateUtils.getNowDate());
            columns.setCreateBy(SecurityUtils.getUsername());
            dbhsmEncryptColumnsMapper.insertDbhsmEncryptColumns(columns);
        }

        if (!CollectionUtils.isEmpty(list)) {
            //如果有追加数据修改队列状态
            if (null != taskQueue.getDecStatus()) {
                taskQueue.setDecStatus(0);
            }
            if (null != taskQueue.getEncStatus()) {
                taskQueue.setEncStatus(0);
            }
            dbhsmTaskQueueMapper.updateRecord(taskQueue);
        }

        return AjaxResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public AjaxResult queryEncryptionProgress(Long taskId) {
        DbhsmTaskQueue taskQueue = dbhsmTaskQueueMapper.findByPrimaryKey(taskId);
        if (null == taskQueue) {
            return AjaxResult.error("查询任务队列进度失败，未找到队列任务");
        }
        DbhsmEncryptTable encryptTable = dbhsmEncryptTableMapper.findByPrimaryKey(String.valueOf(taskQueue.getTableId()));
        int i = 0;
        try {
            i = stockDataOperateService.queryProgress(taskQueue.getTableId());
        } catch (Exception e) {
            log.error("查询进度失败：{}", e.getMessage());
            return AjaxResult.error("查询进度失败", i);
        }
        //加密状态是否到达阈值
        if (100 == i) {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //更新列状态
            List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptByTableId(String.valueOf(encryptTable.getTableId()));

            DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(encryptTable.getInstanceId());
            //解密状态为空，当前队列为加密
            if (null == taskQueue.getDecStatus()) {
                //加密完成更新加密列状态    解密完成不需要更新
                updateEncrypt(2, columnsList);
                //解密状态为空说明是加密队列  加密表状态修改为   加密后
                encryptTable.setTableStatus(DbConstants.ENCRYPTED);
                dbhsmEncryptTableMapper.updateRecord(encryptTable);
                //设置队列状态
                taskQueue.setEncStatus(DbConstants.ENCRYPTING);
                dbhsmTaskQueueMapper.updateRecord(taskQueue);
                if (DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType()) && DbConstants.BE_PLUG.equals(dbhsmDbInstance.getPlugMode())) {
                    boolean b = operViewToSqlServer(columnsList, dbhsmDbInstance);
                    if (!b) {
                        log.error("创建解密视图失败");
                    }
                }
            } else {
                List<DbhsmEncryptColumns> decComplete = columnsList.stream().filter(col -> DbConstants.DECRYPTING.equals(col.getEncryptionStatus())).collect(Collectors.toList());
                //过滤需要删除的加密列ID
                String[] array = decComplete.stream().map(DbhsmEncryptColumns::getId).toArray(String[]::new);
                //1.删除解密中的的数据
                dbhsmEncryptColumnsMapper.deleteDbhsmEncryptColumnsByIds(array);

                //2.如果当前表中的列全部属于解密状态   删除加密表的信息以及解密列
                if (decComplete.size() == columnsList.size()) {
                    dbhsmEncryptTableMapper.deleteRecords(String.valueOf(encryptTable.getTableId()));
                    dbhsmTaskQueueMapper.deleteRecords(taskId);
                } else {
                    //一个字段解密完后直接删掉该字段的队列信息
                    dbhsmTaskQueueMapper.deleteRecords(taskId);
                }
            }
            //删除任务队列
            try {
                stockDataOperateService.clearMap(taskQueue.getTableId());
            } catch (Exception e) {
                log.error("删除任务队列失败：{}", e.getMessage());
            }
        }

        return AjaxResult.success(i);
    }

    @Override
    public List<TaskPolicyDetailsResponse> taskQueueDetails(TaskQueueDetailsRequest request) {
        List<TaskPolicyDetailsResponse> list = new ArrayList<>();

        //获取数据库的实例信息
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(request.getDbInstanceId());

        //查询以及配置的加密列
        DbhsmEncryptColumns columns = BeanConvertUtils.beanToBean(request, DbhsmEncryptColumns.class);
        columns.setDbTable(request.getTableName());
        List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(columns);

        if (!CollectionUtils.isEmpty(columnsList)) {
            list = new ArrayList<>(BeanConvertUtils.beanToBeanInList(columnsList, TaskPolicyDetailsResponse.class));
        }

        List<String> columnNames = list.stream().map(TaskPolicyDetailsResponse::getEncryptColumns).collect(Collectors.toList());

        List<TaskPolicyDetailsResponse> unEncColumns;
        if (!DbConstants.DB_TYPE_HB.equals(instance.getDatabaseType())) {
            //如果不是加密列详情，不获取当前表的其他列
            unEncColumns = encryptColumnsAll(instance, request.getTableName(), columnNames);
        } else {
            //Hbase使用API获取信息
            unEncColumns = encryptHbaseColumnsAll(instance, columnNames, request.getTableName());
        }
        if (!CollectionUtils.isEmpty(unEncColumns)) {
            list.addAll(unEncColumns);
        }

        //条件筛选
        if (StringUtils.isNotBlank(request.getStatus())) {
            return list.stream().filter(pro -> StringUtils.equals("0", request.getStatus()) == (null != pro.getEncryptionStatus())).collect(Collectors.toList());
        }

        return list;
    }

    @Override
    public AjaxResult2<TaskQueueListResponse> queryDbInstanceInfo(Long dbInstanceId, Long dbUserId, String dbTableName) {
        //实例基本信息
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbInstanceId);

        TaskQueueListResponse response = BeanConvertUtils.beanToBean(dbhsmDbInstance, TaskQueueListResponse.class);

        //设置用户信息 前端插件模式用户信息为DBA信息
        DbhsmDbUser dbhsmDbUser = dbUsersMapper.selectDbhsmDbUsersById(dbUserId);
        if (!DbConstants.BE_PLUG.equals(dbhsmDbInstance.getPlugMode())) {
            dbhsmDbUser = new DbhsmDbUser();
            dbhsmDbUser.setUserName(dbhsmDbInstance.getDatabaseDba());
        }
        response.setUserName(dbhsmDbUser.getUserName());
        response.setTableName(dbTableName);
        DbhsmEncryptTable encryptTable = dbhsmEncryptTableMapper.queryTableRecord(dbInstanceId, dbTableName);
        if (null != encryptTable) {
            //copy相同字段数据
            BeanUtils.copyProperties(encryptTable, response);
        }

        return AjaxResult2.success(response);
    }

    @Override
    public List<EncryptColumns> details(Long taskId, String detailsMode) {

        /**
         *1.加密队列编辑      获取队列中的和原表中的列数据
         *2.加密详情         查看存在加密队列中的数据
         *3.解密详情         查看存在解密队列中的数据
         *4.添加到解密队列    所有列的状态为已加密
         *5.添加到加密队列    所有列的状态为未开始解密
         *注：解密完成自动删除数据
         */
        DbhsmTaskQueue taskQueue = dbhsmTaskQueueMapper.findByPrimaryKey(taskId);

        //查询任务队列中的表
        DbhsmEncryptTable dbTable = dbhsmEncryptTableMapper.findByPrimaryKey(taskQueue.getTableId());
        //查询表关联的字段信息
        List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptByTableId(String.valueOf(dbTable.getTableId()));

        //数据转换
        List<EncryptColumns> list = new ArrayList<>(BeanConvertUtils.beanToBeanInList(columnsList, EncryptColumns.class));

        //加密队列详情
        if ("1".equals(detailsMode)) {
            DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbTable.getInstanceId());

            //过滤加密列
            List<String> columnNames = list.stream().map(EncryptColumns::getEncryptColumns).collect(Collectors.toList());
            List<TaskPolicyDetailsResponse> unEncColumns = encryptColumnsAll(dbhsmDbInstance, dbTable.getTableName(), columnNames);
            if (!CollectionUtils.isEmpty(unEncColumns)) {
                unEncColumns = unEncColumns.stream().filter(po -> "-".equals(po.getDisablingEncryption())).collect(Collectors.toList());
                list.addAll(new ArrayList<>(BeanConvertUtils.beanToBeanInList(unEncColumns, EncryptColumns.class)));
            }
        } else if ("2".equals(detailsMode)) {
            //加密详情 ：   未开始加密、加密中、已加密
            return list.stream().filter(enc -> DbConstants.CREATED_ON_WEB_SEDE.equals(enc.getEncryptionStatus())
                    || DbConstants.NOT_ENCRYPTED.equals(enc.getEncryptionStatus()) || DbConstants.ENCRYPTED.equals(enc.getEncryptionStatus())).collect(Collectors.toList());
        } else if ("3".equals(detailsMode)) {
            //解密详情  获取解密中或者未开始解密的数据
            return list.stream().filter(enc -> DbConstants.ENCRYPTING.equals(enc.getEncryptionStatus()) || DbConstants.DECRYPTING.equals(enc.getEncryptionStatus())).collect(Collectors.toList());
        } else if ("4".equals(detailsMode)) {
            //过滤状态为已加密的数据
            return list.stream().filter(enc -> DbConstants.ENCRYPTED.equals(enc.getEncryptionStatus())).collect(Collectors.toList());
        } else if ("5".equals(detailsMode)) {
            return list.stream().filter(enc -> DbConstants.ENCRYPTING.equals(enc.getEncryptionStatus())).collect(Collectors.toList());
        }
        return list;
    }

    @Override
    public AjaxResult2<List<EncryptColumns>> taskQueueNoEncList(Long id, String taskMode) {
        List<DbhsmEncryptColumns> encryptColumns = dbhsmTaskQueueMapper.selectDbhsmEncryptColumnsDetails(id, taskMode);
        if (CollectionUtils.isEmpty(encryptColumns)) {
            return AjaxResult2.success(new ArrayList<>());
        }

        List<DbhsmEncryptColumns> collect = encryptColumns.stream().filter(enc -> DbConstants.DEC_FLAG == enc.getEncryptionStatus()).collect(Collectors.toList());
        List<EncryptColumns> list = new ArrayList<>(BeanConvertUtils.beanToBeanInList(collect, EncryptColumns.class));
        return AjaxResult2.success(list);
    }

    /**
     * 获取指定表的其他字段
     *
     * @param dbhsmDbInstance
     * @param tableName
     * @return
     */
    public static List<TaskPolicyDetailsResponse> encryptColumnsAll(DbhsmDbInstance dbhsmDbInstance, String tableName, List<String> columnNames) {
        List<TaskPolicyDetailsResponse> list = new ArrayList<>();
        Connection conn = null;
        try {
            DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
            BeanUtils.copyProperties(dbhsmDbInstance, connDTO);
            conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
            if (Optional.ofNullable(conn).isPresent()) {

                //单独处理 带schema
                if (DbConstants.DB_TYPE_CLICKHOUSE.equals(dbhsmDbInstance.getDatabaseType())) {
                    tableName = dbhsmDbInstance.getDatabaseServerName() + ":" + tableName;
                } else if (DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType())) {
                    tableName = getDataBaseSchema(conn, tableName, dbhsmDbInstance.getDatabaseType()) + ":" + tableName;
                } else if (DbConstants.DB_TYPE_KB.equals(dbhsmDbInstance.getDatabaseType())) {
                    tableName = getDataBaseSchema(conn, tableName, dbhsmDbInstance.getDatabaseType()) + ":" + tableName;
                }

                //查询列字段信息
                List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(conn, tableName, dbhsmDbInstance.getDatabaseType());
                for (Map<String, String> stringStringMap : allColumnsInfo) {
                    TaskPolicyDetailsResponse encryptColumns = new TaskPolicyDetailsResponse();
                    String columnName = stringStringMap.get(DbConstants.DB_COLUMN_NAME);
                    if (columnNames.contains(columnName)) {
                        continue;
                    }
                    //是否非空
                    String columnType = stringStringMap.get("columnType");
                    encryptColumns.setColumnsType(columnType);
                    encryptColumns.setEncryptColumns(columnName);

                    String key = stringStringMap.get("Key");
                    if (StringUtils.isNotBlank(key)) {
                        encryptColumns.setDisablingEncryption(key.contains("PRI") ? "主键禁止加密" : (key.contains("MUL") ? "外键禁止加密" : "-"));
                    }

                    list.add(encryptColumns);
                }
            }
        } catch (Exception e) {
            log.error("获取表字段失败：{}", e.getMessage());
            return null;
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return list;
    }


    //组装Hbase加密数据
    public TableDTO encryptHbaseColumnsInfo(DbhsmDbInstance dbhsmDbInstance, List<DbhsmEncryptColumns> dbhsmEncryptColumns) {
        TableDTO tableDTO = new TableDTO();
        try {
            tableDTO.setSchema(dbhsmDbInstance.getDatabaseServerName());
            org.apache.hadoop.hbase.client.Connection connection = DbConnectionPoolFactory.buildHbaseDataSource(dbhsmDbInstance);
            Admin admin = connection.getAdmin();

            //获取表名
            String dbTable = dbhsmEncryptColumns.get(0).getDbTable();
            // 获取表描述信息
            TableDescriptor tableDescriptor = admin.getDescriptor(TableName.valueOf(dbTable));

            // Map用于存储列族及其对应的列
            Map<String, Set<String>> columnFamilyColumns = new HashMap<>();

            // 获取表对象
            Table table = connection.getTable(TableName.valueOf(dbTable));

            // 创建扫描器
            Scan scan = new Scan();

            // 扫描表并收集列名
            ResultScanner scanner = table.getScanner(scan);
            for (Result result : scanner) {
                for (ColumnFamilyDescriptor columnFamilyDescriptor : tableDescriptor.getColumnFamilies()) {
                    String columnFamilyName = columnFamilyDescriptor.getNameAsString();
                    columnFamilyColumns.putIfAbsent(columnFamilyName, new HashSet<>());
                    for (byte[] column : result.getFamilyMap(Bytes.toBytes(columnFamilyName)).keySet()) {
                        columnFamilyColumns.get(columnFamilyName).add(Bytes.toString(column));
                    }
                }
            }

            List<ColumnDTO> list = getColumnDTOS(dbhsmEncryptColumns, columnFamilyColumns);
            tableDTO.setColumnDTOList(list);
            // 关闭扫描器和表对象
            scanner.close();
            table.close();
            // 关闭Admin和连接
            admin.close();
            connection.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            return new TableDTO();
        }
        return tableDTO;
    }

    private List<ColumnDTO> getColumnDTOS(List<DbhsmEncryptColumns> dbhsmEncryptColumns, Map<String, Set<String>> columnFamilyColumns) {
        List<ColumnDTO> list = new ArrayList<>();
        for (DbhsmEncryptColumns dbhsmEncryptColumn : dbhsmEncryptColumns) {
            ColumnDTO columnDTO = new ColumnDTO();
            // 打印列族及其对应的列名
            for (Map.Entry<String, Set<String>> entry : columnFamilyColumns.entrySet()) {
                for (String column : entry.getValue()) {
                    if (!dbhsmEncryptColumn.getEncryptColumns().contains(column)) {
                        continue;
                    }
                    columnDTO.setColumnName(entry.getKey() + ":" + column);
                    columnDTO.setEncryptAlgorithm("TestAlg");
                    DbhsmSecretKeyManage dbhsmSecretKeyManage = dbhsmSecretKeyManageMapper.selectDbhsmSecretKeyId(dbhsmEncryptColumn.getSecretKeyId());
                    if (null != dbhsmSecretKeyManage) {
                        String systemMasterKey = JSONDataUtil.getSysDataToDB(DbConstants.DBENC_SYSTEM_MASTER_KEY);
                        byte[] decode = Base64.decode(dbhsmSecretKeyManage.getSecretKey());
                        byte[] plaintext = SM4Utils.decryptData_ECB(decode, Hex.decode(systemMasterKey));
                        columnDTO.setEncryptKeyIndex(Hex.toHexString(plaintext));
                    }

                    list.add(columnDTO);
                }
            }
        }
        return list;
    }

    public static List<TaskPolicyDetailsResponse> encryptHbaseColumnsAll(DbhsmDbInstance instance, List<String> columnNames, String tableName) {
        List<TaskPolicyDetailsResponse> list = new ArrayList<>();
        try {
            org.apache.hadoop.hbase.client.Connection connection = DbConnectionPoolFactory.buildHbaseDataSource(instance);
            Admin admin = connection.getAdmin();

            // 获取表描述信息
            TableDescriptor tableDescriptor = admin.getDescriptor(TableName.valueOf(tableName));

            // Map用于存储列族及其对应的列
            Map<String, Set<String>> columnFamilyColumns = new HashMap<>();

            // 获取表对象
            Table table = connection.getTable(TableName.valueOf(tableName));

            // 创建扫描器
            Scan scan = new Scan();

            // 扫描表并收集列名
            ResultScanner scanner = table.getScanner(scan);
            for (Result result : scanner) {
                for (ColumnFamilyDescriptor columnFamilyDescriptor : tableDescriptor.getColumnFamilies()) {
                    String columnFamilyName = columnFamilyDescriptor.getNameAsString();
                    columnFamilyColumns.putIfAbsent(columnFamilyName, new HashSet<>());
                    for (byte[] column : result.getFamilyMap(Bytes.toBytes(columnFamilyName)).keySet()) {
                        columnFamilyColumns.get(columnFamilyName).add(Bytes.toString(column));
                    }
                }
            }

            // 打印列族及其对应的列名
            for (Map.Entry<String, Set<String>> entry : columnFamilyColumns.entrySet()) {
                for (String column : entry.getValue()) {
                    String familyColum = entry.getKey() + ":" + column;
                    if (columnNames.contains(familyColum)) {
                        continue;
                    }
                    TaskPolicyDetailsResponse response = new TaskPolicyDetailsResponse();
                    response.setEncryptColumns(familyColum);
                    list.add(response);
                }
            }

            // 关闭扫描器和表对象
            scanner.close();
            table.close();

            // 关闭Admin和连接
            admin.close();
            connection.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return list;
    }


    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public AjaxResult deleteEncryptColumns(Long taskId) {
        log.info("需要删除的任务ID：{}", taskId);
        /**
         * 删除 :任务队列、加密列、加密表
         */
        DbhsmTaskQueue taskQueue = dbhsmTaskQueueMapper.findByPrimaryKey(taskId);

        //任务
        dbhsmTaskQueueMapper.deleteRecords(taskId);
        //字段
        dbhsmEncryptColumnsMapper.deleteByEncryptColumnsOnTable(taskQueue.getTableId());
        //删表
        dbhsmEncryptTableMapper.deleteRecords(taskQueue.getTableId());

        return AjaxResult.success();
    }
}


package com.spms.dbhsm.taskQueue.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.SnowFlakeUtil;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.utils.bean.BeanConvertUtils;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.security.utils.SecurityUtils;
import com.spms.common.CommandUtil;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.DBUtil;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.dbUser.mapper.DbhsmDbUsersMapper;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptTable;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptColumnsMapper;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptTableMapper;
import com.spms.dbhsm.encryptcolumns.vo.EncryptColumns;
import com.spms.dbhsm.encryptcolumns.vo.UpEncryptColumnsRequest;
import com.spms.dbhsm.stockDataProcess.domain.dto.ColumnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.TableDTO;
import com.spms.dbhsm.stockDataProcess.service.StockDataOperateService;
import com.spms.dbhsm.taskQueue.domain.DbhsmTaskQueue;
import com.spms.dbhsm.taskQueue.mapper.DbhsmTaskQueueMapper;
import com.spms.dbhsm.taskQueue.service.DbhsmTaskQueueService;
import com.spms.dbhsm.taskQueue.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    private DbhsmTaskQueueMapper dbhsmTaskQueueMapper;

    @Autowired
    private StockDataOperateService stockDataOperateService;

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
    public AjaxResult upEncryptColumns(TaskQueueRequest request) {

        DbhsmTaskQueue taskQueue = dbhsmTaskQueueMapper.findByPrimaryKey(request.getTaskId());
        //加密表信息
        DbhsmEncryptTable encryptTable = dbhsmEncryptTableMapper.findByPrimaryKey(taskQueue.getTableId());

        DbhsmEncryptColumns dbhsmEncryptColumns = new DbhsmEncryptColumns();
        dbhsmEncryptColumns.setTableId(encryptTable.getTableId());
        dbhsmEncryptColumns.setEncryptionStatus(DbConstants.ENC_MODE.equals(request.getTaskMode()) ? 0 : 3);
        List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(dbhsmEncryptColumns);

        try {
            if (null != request.getTaskType() && !DbConstants.UP.equals(request.getTaskType())) {
                //暂停
                if (DbConstants.DOWN.equals(request.getTaskType())) {
//                    stockDataOperateService.pause(encryptTable.getTableId());
                } else if (DbConstants.CONTINUE.equals(request.getTaskType())) {
                    //任务继续
//                    stockDataOperateService.resume(taskQueue.getTableId());
                }

                if (null != taskQueue.getEncStatus()) {
                    taskQueue.setEncStatus(DbConstants.DOWN.equals(request.getTaskType()) ? 2 : 1);
                } else {
                    taskQueue.setDecStatus(DbConstants.DOWN.equals(request.getTaskType()) ? 2 : 1);
                }
                dbhsmTaskQueueMapper.updateRecord(taskQueue);
                return AjaxResult.success();
            }

            //获取数据库基本信息
            DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(encryptTable.getInstanceId());
            DatabaseDTO database = BeanConvertUtils.beanToBean(dbhsmDbInstance, DatabaseDTO.class);
            database.setInstanceType(dbhsmDbInstance.getDatabaseExampleType());
            String instance = CommandUtil.getInstance(dbhsmDbInstance);
            database.setConnectUrl(instance);

            //组装表基本信息
            List<ColumnDTO> columnDTOS = encryptColumnsAll(dbhsmDbInstance, columnsList);
            TableDTO tableDTO = BeanConvertUtils.beanToBean(encryptTable, TableDTO.class);
            tableDTO.setBatchSize(encryptTable.getBatchCount());
            tableDTO.setThreadNum(encryptTable.getThreadCount());
            tableDTO.setColumnDTOList(columnDTOS);

            //加密表信息
            List<TableDTO> list = new ArrayList<>();
            list.add(tableDTO);
            database.setTableDTOList(list);

            //加密
            if (DbConstants.ENC_MODE.equals(request.getTaskMode())) {
                stockDataOperateService.stockDataOperate(database, true);
                taskQueue.setEncStatus(1);
                //修改加密列状态
                updateEncrypt(1, columnsList);
                dbhsmTaskQueueMapper.updateRecord(taskQueue);
            } else {
                //解密
                stockDataOperateService.stockDataOperate(database, false);
                taskQueue.setDecStatus(1);
                updateEncrypt(4, columnsList);
                dbhsmTaskQueueMapper.updateRecord(taskQueue);
            }
        } catch (Exception e) {
            return AjaxResult.error("操作任务队列表：" + encryptTable.getTableName() + "失败！");
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


    /**
     * 组装字段基本信息
     *
     * @param dbhsmDbInstance     数据库实例
     * @param dbhsmEncryptColumns 加密字段列
     * @return List<ColumnDTO>
     * @throws ZAYKException
     * @throws SQLException
     */
    public static List<ColumnDTO> encryptColumnsAll(DbhsmDbInstance dbhsmDbInstance, List<DbhsmEncryptColumns> dbhsmEncryptColumns) {
        List<ColumnDTO> list = new ArrayList<>();
        Connection conn = null;
        try {
            DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
            BeanUtils.copyProperties(dbhsmDbInstance, connDTO);
            conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
            if (Optional.ofNullable(conn).isPresent()) {
                for (DbhsmEncryptColumns dbhsmEncryptColumn : dbhsmEncryptColumns) {
                    ColumnDTO columnDTO = new ColumnDTO();
                    Statement stmt = conn.createStatement();
                    ResultSet resultSet = null;
                    String sql = databaseTypeSqlColumns(dbhsmDbInstance.getDatabaseType(), dbhsmEncryptColumn.getDbTable());
                    stmt.executeQuery(sql);
                    while (resultSet.next()) {
                        Map<String, String> map = new HashMap<>();
                        String columnName = resultSet.getString("Field");
                        if (!dbhsmEncryptColumn.getEncryptColumns().equals(columnName)) {
                            continue;
                        }
                        String type = resultSet.getString("Type");
                        String isNullable = resultSet.getString("Null");
                        String key = resultSet.getString("Key");
                        String isDefault = resultSet.getString("Default");
                        String remarks = resultSet.getString("Comment");

                        map.put("Field", columnName);
                        map.put("Type", type);
                        map.put("Null", isNullable);
                        map.put("Key", key);
                        map.put("Default", isDefault);
                        map.put("Comment", remarks);

                        columnDTO.setColumnDefinition(map);
                        columnDTO.setColumnName(columnName);
                        columnDTO.setNotNull(!"YES".equalsIgnoreCase(isNullable));
                        columnDTO.setComment(remarks);
                        list.add(columnDTO);
                    }
                    columnDTO.setEncryptAlgorithm(dbhsmEncryptColumn.getEncryptionAlgorithm());
                    columnDTO.setEncryptKeyIndex(dbhsmEncryptColumn.getSecretKeyId());
                }

            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ArrayList<>();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return list;
    }

    public static String databaseTypeSqlColumns(String type, String table) {
        String sql = "";
        //sqlserver
        if (DbConstants.DB_TYPE_SQLSERVER.equals(type)) {
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
                    "  left join sys.extended_properties f on d.id = f.major_id and f.minor_id = 0 \n" +
                    "where d.name = '" + table + "' order by a.id, a.colorder";
        } else if (DbConstants.DB_TYPE_ORACLE.equals(type)) {
            //Oracle
        } else if (DbConstants.DB_TYPE_POSTGRESQL.equals(type)) {
            sql = "SELECT column_name as Field, data_type as Type, is_nullable as 'Null', column_default as 'Default' FROM information_schema.columns WHERE  table_name = '" + table + "'";
            //PostgreSQL
        } else if (DbConstants.DB_TYPE_MYSQL.equals(type)) {
            //MySql
            sql = "SHOW FULL COLUMNS from " + table;
        }
        return sql;
    }


    @Override
    @Transactional
    public AjaxResult insertDecColumnsOnEnc(TaskDecColumnsOnEncRequest request) {
        /**
         * 解密队列添加到加密队列
         */
        DbhsmTaskQueue dbhsmTaskQueue = dbhsmTaskQueueMapper.findByPrimaryKey(request.getTaskId());
        DbhsmEncryptTable encryptTable = dbhsmEncryptTableMapper.findByPrimaryKey(dbhsmTaskQueue.getTableId());

        //更新数据
        encryptTable.setThreadCount(request.getThreadCount());
        encryptTable.setBatchCount(request.getBatchCount());
        dbhsmEncryptTableMapper.updateRecord(encryptTable);

        DbhsmEncryptColumns dbhsmEncryptColumn = new DbhsmEncryptColumns();
        dbhsmEncryptColumn.setTableId(encryptTable.getTableId());
        dbhsmEncryptColumn.setEncryptionStatus(3);
        if (null == dbhsmTaskQueue.getDecStatus()) {
            //加密队列加到解密队列
            dbhsmEncryptColumn.setEncryptionStatus(2);
        }

        //查询表中的解密列
        List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(dbhsmEncryptColumn);

        //return
        if (CollectionUtils.isEmpty(columnsList)) {
            return AjaxResult.error("表中的字段无法添加至加密队列");
        }

        //组装update数据
        for (DbhsmEncryptColumns dbhsmEncryptColumns : columnsList) {
            for (EncryptColumns encryptColumns : request.getEncryptedLists()) {
                if (encryptColumns.getEncryptColumns().equals(dbhsmEncryptColumns.getEncryptColumns())) {
                    BeanUtils.copyProperties(encryptColumns, dbhsmEncryptColumns);
                    //修改未未开始加密
                    dbhsmEncryptColumns.setEncryptionStatus(2);
                    if (null == dbhsmTaskQueue.getDecStatus()) {
                        dbhsmEncryptColumn.setEncryptionStatus(3);
                    }
                    dbhsmEncryptColumnsMapper.updateDbhsmEncryptColumns(dbhsmEncryptColumns);
                }
            }
        }

        //如果数据一样
        if (columnsList.size() == request.getEncryptedLists().size()) {
            //删除数据
            dbhsmTaskQueueMapper.deleteRecords(request.getTaskId());
        }

        return AjaxResult.success();
    }

    @Override
    @Transactional
    public AjaxResult insertTask(TaskQueueInsertRequest request) {

        //数据库实例信息
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(request.getInstanceId());

        DbhsmEncryptTable dbhsmEncryptTable = BeanConvertUtils.beanToBean(request, DbhsmEncryptTable.class);
        dbhsmEncryptTable.setTableStatus(DbConstants.CREATED_ON_WEB_SEDE);

        try {
            String ddl = showTableDdl(dbhsmDbInstance, request.getTableName());
            dbhsmEncryptTable.setTableDdl(ddl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        boolean isTask = false;
        String snowflakeId = request.getTableId();
        //加密逻辑
        if (DbConstants.ENC_MODE.equals(request.getQueueMode())) {
            DbhsmEncryptTable encryptTable = dbhsmEncryptTableMapper.queryTableRecord(request.getInstanceId(), request.getTableName());
            if (null == encryptTable) {
                snowflakeId = SnowFlakeUtil.getSnowflakeId();
                dbhsmEncryptTable.setTableId(snowflakeId);
                dbhsmEncryptTable.setCreateTime(DateUtils.getNowDate());
                dbhsmEncryptTable.setCreateBy(SecurityUtils.getUsername());
                dbhsmEncryptTableMapper.insertRecord(dbhsmEncryptTable);
            } else {
                snowflakeId = encryptTable.getTableId();
                isTask = true;
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
            //如果当前新增的是解密队列
            if (DbConstants.DEC_MODE.equals(request.getQueueMode())) {
                dbhsmEncryptColumns.setUpdateTime(DateUtils.getNowDate());
                dbhsmEncryptColumns.setUpdateBy(SecurityUtils.getUsername());
                dbhsmEncryptColumns.setEncryptionStatus(DbConstants.ENCRYPTING);
                dbhsmEncryptColumnsMapper.updateDbhsmEncryptColumnsByTableIdAndFeild(dbhsmEncryptColumns);
            } else {
                //加密队列
                dbhsmEncryptColumns.setCreateTime(DateUtils.getNowDate());
                dbhsmEncryptColumns.setCreateBy(SecurityUtils.getUsername());
                dbhsmEncryptColumns.setEncryptionStatus(DbConstants.DEC_FLAG);
                dbhsmEncryptColumnsMapper.insertDbhsmEncryptColumns(dbhsmEncryptColumns);
            }
        }


        if (isTask){
            List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptByTableId(snowflakeId);
            columnsList = columnsList.stream().filter(db -> 3 > db.getEncryptionStatus()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(columnsList)){
                //如果为空删除该列
            }
            return AjaxResult.success();
        }

        //添加任务表
        DbhsmTaskQueue dbhsmTaskQueue = new DbhsmTaskQueue();
        dbhsmTaskQueue.setTableId(snowflakeId);
        dbhsmTaskQueue.setCreateTime(DateUtils.getNowDate());
        dbhsmTaskQueue.setCreateBy(SecurityUtils.getUsername());

        if (DbConstants.DEC_MODE.equals(request.getQueueMode())) {
            //解密
            dbhsmTaskQueue.setDecStatus(DbConstants.DEC_FLAG);
            dbhsmTaskQueueMapper.insertRecord(dbhsmTaskQueue);
        } else {
            //加密
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
        ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE " + table);
        if (rs.next()) {
            ddl = rs.getString(2);
        }
        conn.close();
        return ddl;
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

        columns.setTableId(encryptTable.getTableId());
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
            log.error(e.getMessage());
            AjaxResult.error("查询" + encryptTable.getTableName() + "表任务队列进度失败！");
        }
        return AjaxResult.success(i);
    }

    @Override
    public List<TaskPolicyDetailsResponse> taskQueueDetails(TaskQueueDetailsRequest request) {
        List<TaskPolicyDetailsResponse> list = new ArrayList<>();

        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(request.getDbInstanceId());

        DbhsmEncryptColumns columns = BeanConvertUtils.beanToBean(request, DbhsmEncryptColumns.class);
        columns.setDbTable(request.getTableName());
        List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(columns);

        if (!CollectionUtils.isEmpty(columnsList)) {
            list = new ArrayList<>(BeanConvertUtils.beanToBeanInList(columnsList, TaskPolicyDetailsResponse.class));
        }

        List<String> columnNames = list.stream().map(TaskPolicyDetailsResponse::getEncryptColumns).collect(Collectors.toList());

        //如果不是加密列详情，不获取当前表的其他列
        List<TaskPolicyDetailsResponse> unEncColumns = encryptColumnsAll(instance, request.getTableName(), columnNames);
        if (!CollectionUtils.isEmpty(unEncColumns)) {
            list.addAll(unEncColumns);
        }

        //条件筛选
        if (StringUtils.isNotBlank(request.getStatus())) {
            return list.stream().filter(pro -> StringUtils.inStringIgnoreCase("0", request.getStatus()) == (null != pro.getEncryptionStatus())).collect(Collectors.toList());
        }

        return list;
    }

    @Override
    public AjaxResult2<TaskQueueListResponse> queryDbInstanceInfo(Long dbInstanceId, Long dbUserId, String dbTableName) {
        //实例基本信息
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbInstanceId);

        TaskQueueListResponse response = BeanConvertUtils.beanToBean(dbhsmDbInstance, TaskQueueListResponse.class);

        //设置用户信息
        DbhsmDbUser dbhsmDbUser = dbUsersMapper.selectDbhsmDbUsersById(dbUserId);
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
        List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptByTableId(dbTable.getTableId());

        //数据转换
        List<EncryptColumns> list = new ArrayList<>(BeanConvertUtils.beanToBeanInList(columnsList, EncryptColumns.class));

        //加密队列详情
        if ("1".equals(detailsMode)) {
            DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbTable.getInstanceId());

            //过滤加密列
            List<String> columnNames = list.stream().map(EncryptColumns::getEncryptColumns).collect(Collectors.toList());
            List<TaskPolicyDetailsResponse> unEncColumns = encryptColumnsAll(dbhsmDbInstance, dbTable.getTableName(), columnNames);
            if (!CollectionUtils.isEmpty(unEncColumns)) {
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
    public AjaxResult2<List<EncryptColumns>> taskQueueNoEncList(String id, String taskMode) {
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

                List<Map<String, String>> allColumnsInfo = DBUtil.findAllColumnsInfo(conn, tableName, dbhsmDbInstance.getDatabaseType());
                for (int j = 0; j < allColumnsInfo.size(); j++) {
                    TaskPolicyDetailsResponse encryptColumns = new TaskPolicyDetailsResponse();
                    String columnName = allColumnsInfo.get(j).get(DbConstants.DB_COLUMN_NAME);
                    if (columnNames.contains(columnName)) {
                        continue;
                    }
                    //是否非空
                    String columnType = allColumnsInfo.get(j).get("columnType");
                    encryptColumns.setColumnsType(columnType);
                    encryptColumns.setEncryptColumns(columnName);

                    String key = allColumnsInfo.get(j).get("Key");
                    encryptColumns.setDisablingEncryption("-");
                    if (StringUtils.isNotBlank(key)) {
                        encryptColumns.setDisablingEncryption("PRI".equals(key) ? "主键禁止加密" : ("MUL".equals(key) ? "外键禁止加密" : "-"));
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


    @Override
    @Transactional
    public AjaxResult deleteEncryptColumns(Long taskId) {
        /**
         * 删除 :任务队列、加密列、加密表
         */
        DbhsmTaskQueue taskQueue = dbhsmTaskQueueMapper.findByPrimaryKey(taskId);
        System.out.println(JSON.toJSONString(taskQueue, SerializerFeature.PrettyFormat));

        //任务
        dbhsmTaskQueueMapper.deleteRecords(taskId);
        //字段
        dbhsmEncryptColumnsMapper.deleteByEncryptColumnsOnTable(taskQueue.getTableId());
        //删表
        dbhsmEncryptTableMapper.deleteRecords(taskQueue.getTableId());

        return AjaxResult.success();
    }
}


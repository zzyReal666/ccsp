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
import com.spms.common.enums.DatabaseTypeEnum;
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
import org.springframework.transaction.annotation.Propagation;
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
    @Transactional
    public AjaxResult upEncryptColumns(TaskQueueRequest request) {

        DbhsmTaskQueue taskQueue = dbhsmTaskQueueMapper.findByPrimaryKey(request.getTaskId());
        //加密表信息
        DbhsmEncryptTable encryptTable = dbhsmEncryptTableMapper.findByPrimaryKey(taskQueue.getTableId());

        DbhsmEncryptColumns dbhsmEncryptColumns = new DbhsmEncryptColumns();
        dbhsmEncryptColumns.setTableId(String.valueOf(encryptTable.getTableId()));
        dbhsmEncryptColumns.setEncryptionStatus(DbConstants.ENC_MODE.equals(request.getTaskMode()) ? 0 : 3);
        List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(dbhsmEncryptColumns);
        if (CollectionUtils.isEmpty(columnsList)) {
            return AjaxResult.error("需要加密的列为空，执行失败");
        }

        try {
            if (null != request.getTaskType() && !DbConstants.UP.equals(request.getTaskType())) {
                //暂停
                if (DbConstants.DOWN.equals(request.getTaskType())) {
                    stockDataOperateService.pause(String.valueOf(encryptTable.getTableId()));
                } else if (DbConstants.CONTINUE.equals(request.getTaskType())) {
                    //任务继续
                    stockDataOperateService.resume(taskQueue.getTableId());
                }

                //设置状态
                Integer status = DbConstants.DOWN.equals(request.getTaskType()) ? 2 : 1;
                if (null != taskQueue.getEncStatus()) {
                    taskQueue.setEncStatus(status);
                } else {
                    taskQueue.setDecStatus(status);
                }
                dbhsmTaskQueueMapper.updateRecord(taskQueue);
                return AjaxResult.success();
            }

            //获取数据库基本信息
            DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(encryptTable.getInstanceId());
            DatabaseDTO database = BeanConvertUtils.beanToBean(dbhsmDbInstance, DatabaseDTO.class);
            database.setDatabaseType(DatabaseTypeEnum.getNameByCode(database.getDatabaseType()));
            database.setDatabaseName(dbhsmDbInstance.getDatabaseServerName());
            database.setInstanceType(dbhsmDbInstance.getDatabaseExampleType());
            String instance = CommandUtil.getInstance(dbhsmDbInstance);
            database.setConnectUrl(instance);

            //组装表基本信息
            TableDTO tableDTO = encryptColumnsAll(dbhsmDbInstance, columnsList);
            BeanUtils.copyProperties(encryptTable,tableDTO);
            tableDTO.setId(encryptTable.getTableId());
            tableDTO.setBatchSize(encryptTable.getBatchCount());
            tableDTO.setThreadNum(encryptTable.getThreadCount());

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
                //表状态修改为加密中
                encryptTable.setTableStatus(DbConstants.ENC_FLAG);
                dbhsmEncryptTableMapper.updateRecord(encryptTable);
            } else {
                //解密
                stockDataOperateService.stockDataOperate(database, false);
                taskQueue.setDecStatus(1);
                updateEncrypt(4, columnsList);
                dbhsmTaskQueueMapper.updateRecord(taskQueue);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
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
    public static TableDTO encryptColumnsAll(DbhsmDbInstance dbhsmDbInstance, List<DbhsmEncryptColumns> dbhsmEncryptColumns) {
        TableDTO tableDTO = new TableDTO();
        Connection conn = null;
        try {
            tableDTO.setSchema(dbhsmDbInstance.getDatabaseServerName());
            DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
            BeanUtils.copyProperties(dbhsmDbInstance, connDTO);
            conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
            if (Optional.ofNullable(conn).isPresent()) {
                List<ColumnDTO> list = new ArrayList<>();
                for (DbhsmEncryptColumns dbhsmEncryptColumn : dbhsmEncryptColumns) {
                    ColumnDTO columnDTO = new ColumnDTO();
                    Statement stmt = conn.createStatement();
                    ResultSet resultSet = null;
                    String sql = databaseTypeSqlColumns(dbhsmDbInstance.getDatabaseType(), dbhsmEncryptColumn.getDbTable(), dbhsmDbInstance.getDatabaseServerName());
                    log.info("获取表字段信息SQL：{}",sql);
                    resultSet = stmt.executeQuery(sql);
                    while (resultSet.next()) {
                        Map<String, String> map = new HashMap<>();
                        String columnName = resultSet.getString("Field");
                        //只获取需要加密的列字段
                        if (!dbhsmEncryptColumn.getEncryptColumns().equals(columnName)) {
                            continue;
                        }
                        if (DbConstants.DB_TYPE_KB.equals(dbhsmDbInstance.getDatabaseType())){
                            String schema = resultSet.getString("table_schema");
                            tableDTO.setSchema(schema);
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
                        columnDTO.setEncryptAlgorithm("TestAlg");
                        columnDTO.setEncryptKeyIndex(dbhsmEncryptColumn.getSecretKeyId());
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
            sql = "select * from " + table.toUpperCase() + " limit 1";
        } else if (DbConstants.DB_TYPE_DM.equals(type)) {
            //dm
            sql = "select * from " + table + " limit 1";
        } else if (DbConstants.DB_TYPE_POSTGRESQL.equals(type) || DbConstants.DB_TYPE_KB.equals(type)) {
            //pgSql || Kingbase TODO 多个架构SQL报错
            sql = "SELECT table_schema,column_name as Field, data_type as Type, is_nullable as Null, column_default as Default,\n" +
                    "CASE WHEN (column_name = (SELECT a.attname AS pk_column_name FROM pg_class t,pg_attribute a,pg_constraint c WHERE c.contype = 'p'AND c.conrelid = t.oid AND a.attrelid = t.oid AND a.attnum = ANY(c.conkey) AND t.relkind = 'r' AND t.relname = '"+table+"'))THEN  'PRI' ELSE  '' END  as key,\n" +
                    "(SELECT col_description(c.oid, a.attnum) AS column_comment FROM pg_class AS c JOIN pg_attribute AS a ON c.oid = a.attrelid WHERE c.relname = 'student' and a.attname = column_name AND a.attnum > 0 ORDER BY a.attnum)  as Comment\n" +
                    "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '"+table+"';";
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

        //数据库实例信息
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(request.getInstanceId());

        DbhsmEncryptTable dbhsmEncryptTable = BeanConvertUtils.beanToBean(request, DbhsmEncryptTable.class);
        dbhsmEncryptTable.setTableStatus(DbConstants.CREATED_ON_WEB_SEDE);

        try {
            //获取表的DDL语句
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
                dbhsmEncryptTable.setTableId(Long.valueOf(snowflakeId));
                dbhsmEncryptTable.setCreateTime(DateUtils.getNowDate());
                dbhsmEncryptTable.setCreateBy(SecurityUtils.getUsername());
                dbhsmEncryptTableMapper.insertRecord(dbhsmEncryptTable);
            } else {
                snowflakeId = String.valueOf(encryptTable.getTableId());
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


        if (isTask) {
            List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptByTableId(snowflakeId);
            columnsList = columnsList.stream().filter(db -> 3 > db.getEncryptionStatus()).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(columnsList)) {
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

        String sql = "";
        int index = 1;
        if (DbConstants.DB_TYPE_ORACLE.equals(dbhsmDbInstance.getDatabaseType())) {
            sql = "SELECT DBMS_METADATA.GET_DDL('TABLE', '" + table + "') FROM " + table + ";";
        } else if (DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType())) {
            sql = "select 'create table [' + so.name + '] (' + o.list + ')' + CASE\n" +
                    " WHEN tc.Constraint_Name IS NULL THEN '' ELSE 'ALTER TABLE ' + so.Name +\n" +
                    " ' ADD CONSTRAINT ' + tc.Constraint_Name + ' PRIMARY KEY ' + ' (' + LEFT(j.List, Len(j.List)-1) + ')' END\n" +
                    " from sysobjects so cross apply (SELECT ' ['+ column_name +'] ' data_type + case data_type when 'sql_variant' then '' when 'text' then '' when 'ntext' then '' when 'xml' then '' when 'image' then ''\n" +
                    " when 'decimal' then '(' + cast (numeric_precision as varchar) + ', ' + cast (numeric_scale as varchar) + ')'\n" +
                    " else coalesce ('('+ case when character_maximum_length = -1 then 'MAX' else cast (character_maximum_length as varchar) end +')', '') end + ' ' +\n" +
                    " case when exists ( select id from syscolumns where object_name(id)=so.name and name = column_name and columnproperty(id, name, 'IsIdentity') = 1 ) then\n" +
                    " 'IDENTITY(' cast (ident_seed(so.name) as varchar) + ',' cast (ident_incr(so.name) as varchar) + ')'else '' end + ' ' +\n" +
                    " (case when IS_NULLABLE = 'No' then 'NOT ' else '' end ) + 'NULL ' + case when information_schema.columns.COLUMN_DEFAULT IS NOT NULL THEN 'DEFAULT '+ information_schema.columns.COLUMN_DEFAULT ELSE '' END + ', '\n" +
                    " from information_schema.columns where table_name = so.name order by ordinal_position FOR XML PATH ('')) o (list) left join information_schema.table_constraints tc\n" +
                    " on tc.Table_name = so.Name AND tc.Constraint_Type = 'PRIMARY KEY' cross apply (select '[' + Column_Name + '], '\n" +
                    " FROM information_schema.key_column_usage kcu WHERE kcu.Constraint_Name = tc.Constraint_Name ORDER BY ORDINAL_POSITION FOR XML PATH ('')) j (list) where xtype = 'U' AND name = '" + table + "' ";
        } else if (DbConstants.DB_TYPE_POSTGRESQL.equals(dbhsmDbInstance.getDatabaseType())) {
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
                    "        WHERE tc.table_schema = ( SELECT table_schema FROM information_schema.columns WHERE table_name = '"+table+"' LIMIT 1 )\n" +
                    "        AND tc.table_name = '"+table+"' AND tc.constraint_type = 'PRIMARY KEY' GROUP BY kcu.table_name, tc.constraint_name ), '') AS ddl_statement\n" +
                    "FROM information_schema.columns\n" +
                    "WHERE table_schema = ( SELECT table_schema FROM information_schema.columns WHERE table_name = '"+table+"'  LIMIT 1 )\n" +
                    "    AND table_name = '"+table+"' GROUP BY table_name;";
        }

        ResultSet rs = stmt.executeQuery(sql);
        if (rs.next()) {
            ddl = rs.getString(index);
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
            log.error(e.getMessage());
            AjaxResult.error("查询" + encryptTable.getTableName() + "表任务队列进度失败！");
        }

        //加密状态是否到达阈值
        if (100 == i) {
            //更新列状态
            List<DbhsmEncryptColumns> columnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptByTableId(String.valueOf(encryptTable.getTableId()));
            if (null == taskQueue.getDecStatus()) {
                //加密完成更新加密列状态    解密完成不需要更新
                updateEncrypt(2, columnsList);
                //解密状态为空说明是加密队列  加密表状态修改为   加密后
                encryptTable.setTableStatus(DbConstants.ENCRYPTED);
                dbhsmEncryptTableMapper.updateRecord(encryptTable);
            } else {
                String[] array = columnsList.stream().filter(col -> DbConstants.DECRYPTING.equals(col.getEncryptionStatus())).map(DbhsmEncryptColumns::getId).toArray(String[]::new);
                //1.删除解密中的的数据
                dbhsmEncryptColumnsMapper.deleteDbhsmEncryptColumnsByIds(array);

                //2.如果当前表中的列全部属于解密状态   删除加密表的信息以及解密列
                if (array.length == columnsList.size()) {
                    dbhsmEncryptTableMapper.deleteRecords(String.valueOf(encryptTable.getTableId()));
                    dbhsmTaskQueueMapper.deleteRecords(taskId);
                }
//                //解密完成  删除队列数据
//                dbhsmEncryptTableMapper.updateRecord(encryptTable);
//                //否则是解密队列  表状态应为全部还原
//                encryptTable.setTableStatus(DbConstants.ENCRYPTING);
            }
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

        //设置用户信息 前端插件模式用户信息为DBA信息
        DbhsmDbUser dbhsmDbUser = dbUsersMapper.selectDbhsmDbUsersById(dbUserId);
        if (DbConstants.FG_PLUG.equals(dbhsmDbInstance.getPlugMode())) {
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
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
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


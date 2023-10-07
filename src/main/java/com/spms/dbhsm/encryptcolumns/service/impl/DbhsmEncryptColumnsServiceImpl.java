package com.spms.dbhsm.encryptcolumns.service.impl;

import cn.hutool.db.DbRuntimeException;
import cn.hutool.db.DbUtil;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.SnowFlakeUtil;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.utils.tree.EleTreeWrapper;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.security.utils.SecurityUtils;
import com.spms.common.CommandUtil;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.DBUtil;
import com.spms.common.dbTool.TransUtil;
import com.spms.common.dbTool.ViewUtil;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.dbUser.service.IDbhsmDbUsersService;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsDto;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptColumnsMapper;
import com.spms.dbhsm.encryptcolumns.service.IDbhsmEncryptColumnsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * 数据库加密列Service业务层处理
 *
 * @author diq
 * @date 2023-09-27
 */
@Slf4j
@Service
public class DbhsmEncryptColumnsServiceImpl implements IDbhsmEncryptColumnsService {
    @Autowired
    private DbhsmEncryptColumnsMapper dbhsmEncryptColumnsMapper;

    @Autowired
    private DbhsmDbInstanceMapper instanceMapper;

    @Autowired
    private IDbhsmDbUsersService dbUsersService;

    @Value("${server.port:10013}")
    private int dbhsmPort;

    /**
     * 查询数据库加密列
     *
     * @param id 数据库加密列主键
     * @return 数据库加密列
     */
    @Override
    public DbhsmEncryptColumns selectDbhsmEncryptColumnsById(String id) {
        return dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsById(id);
    }

    /**
     * 查询数据库加密列列表
     *
     * @param columnsDto 数据库加密列
     * @return 数据库加密列
     */
    @Override
    public List<DbhsmEncryptColumns> selectDbhsmEncryptColumnsList(DbhsmEncryptColumnsDto columnsDto) throws Exception {
        String tableName = "";
        List<DbhsmEncryptColumns> columnsList = new ArrayList<>();
        Connection conn = null;
        //根据用户Id获取用户信息
        DbhsmDbUser dbUserInfo = dbUsersService.selectDbhsmDbUsersById(columnsDto.getDbUserId());

        if (dbUserInfo == null) {
            log.error("未获取到数据库用户 dbUserId:{}", columnsDto.getDbUserId());
            throw new Exception("未获取到数据库用户");
        }

        if (StringUtils.isEmpty(dbUserInfo.getUserName()) || StringUtils.isEmpty(dbUserInfo.getPassword())) {
            log.error("数据库用户名或密码为空: UserName:{},Password:{}", dbUserInfo.getUserName(), dbUserInfo.getPassword());
            throw new Exception("数据库用户名或密码为空");
        }

        //创建数据库服务连接
        DbhsmDbInstance instance = instanceMapper.selectDbhsmDbInstanceById(columnsDto.getDbInstanceId());
        if (dbUserInfo == null) {
            log.error("根据ID未获取到数据库实例 dbInstanceId:{}", columnsDto.getDbInstanceId());
            throw new Exception("未获取到数据库实例：dbInstanceId：" + columnsDto.getDbInstanceId());
        }
        try {
            DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
            BeanUtils.copyProperties(instance, connDTO);
            conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
            if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(instance.getDatabaseType())) {
                tableName = dbUserInfo.getUserName() + "." + columnsDto.getDbTableName();
            } else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(instance.getDatabaseType())) {
                tableName = columnsDto.getDbTableName();
            }

            List<Map<String, String>> columnsInfoList = DBUtil.findAllColumnsInfo(conn, tableName, instance.getDatabaseType());
            for (int j = 0; j < columnsInfoList.size(); j++) {
                DbhsmEncryptColumns encryptColumns = new DbhsmEncryptColumns();
                encryptColumns.setDbInstanceId(instance.getId());
                encryptColumns.setDbInstance(getInstance(instance));
                encryptColumns.setDbUserName(dbUserInfo.getUserName());
                encryptColumns.setDbTable(columnsDto.getDbTableName());
                encryptColumns.setEncryptColumns(columnsInfoList.get(j).get(DbConstants.DB_COLUMN_NAME));
                encryptColumns.setColumnsType(columnsInfoList.get(j).get("columnType"));
                List<DbhsmEncryptColumns> dbEncryptColumnsList = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(encryptColumns);
                if (dbEncryptColumnsList.size() == 1) {
                    //加密列已配置过，将配置信息回显界面
                    encryptColumns.setId(dbEncryptColumnsList.get(0).getId());
                    encryptColumns.setEncryptionAlgorithm(dbEncryptColumnsList.get(0).getEncryptionAlgorithm());
                    encryptColumns.setEncryptionStatus(dbEncryptColumnsList.get(0).getEncryptionStatus());
                    encryptColumns.setSecretKeyId(dbEncryptColumnsList.get(0).getSecretKeyId());
                } else if (dbEncryptColumnsList.size() == 0) {
                    encryptColumns.setId("-");
                    encryptColumns.setEncryptionAlgorithm("-");
                    encryptColumns.setEncryptionStatus(DbConstants.NOT_ENCRYPTED);
                }
                columnsList.add(encryptColumns);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
        if (conn != null) {
            conn.close();
        }
        return columnsList;
    }

    /**
     * 新增数据库加密列
     *
     * @param dbhsmEncryptColumnsAdd 数据库加密列
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int insertDbhsmEncryptColumns(DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd) throws Exception {

        // 创建连接
        DbhsmDbInstance instance = instanceMapper.selectDbhsmDbInstanceById(dbhsmEncryptColumnsAdd.getDbInstanceId());
        dbhsmEncryptColumnsAdd.setDatabaseType(instance.getDatabaseType());
        dbhsmEncryptColumnsAdd.setDatabaseServerName(instance.getDatabaseServerName());
        Connection conn = null;

        DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(instance, connDTO);

        conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);

        //获取端口
        String ip = CommandUtil.exeCmd("ip a| grep " + dbhsmEncryptColumnsAdd.getEthernetPort() + " |grep inet |awk '{print $2}'|awk -F '/' '{print $1}'");
        if (StringUtils.isEmpty(ip)) {
            throw new Exception(dbhsmEncryptColumnsAdd.getEthernetPort() + "口IP不存在,请先进行配置IP");
        }
        ip = ip.trim().replaceAll("\n", ",");
        if (StringUtils.isEmpty(ip)) {
            throw new Exception(dbhsmEncryptColumnsAdd.getEthernetPort() + "口IP不存在,请先进行配置IP");
        }
        if (ip.lastIndexOf(",") == 0) {
            ip = ip.substring(0, ip.length() - 1);
        }
        dbhsmEncryptColumnsAdd.setIpAndPort(ip + ":" + dbhsmPort);
        dbhsmEncryptColumnsAdd.setEncryptionStatus(DbConstants.ENCRYPTED);
        dbhsmEncryptColumnsAdd.setId(SnowFlakeUtil.getSnowflakeId());

        dbhsmEncryptColumnsAdd.setCreateTime(DateUtils.getNowDate());
        dbhsmEncryptColumnsAdd.setCreateBy(SecurityUtils.getUsername());
        DbhsmEncryptColumns dbhsmEncryptColumns = new DbhsmEncryptColumns();
        BeanUtils.copyProperties(dbhsmEncryptColumnsAdd, dbhsmEncryptColumns);
        int ret = dbhsmEncryptColumnsMapper.insertDbhsmEncryptColumns(dbhsmEncryptColumns);
        if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(instance.getDatabaseType())) {
            //oracle创建触发器
            TransUtil.transEncryptColumns(conn, dbhsmEncryptColumnsAdd);
            TransUtil.transFPEEncryptColumns(conn, dbhsmEncryptColumnsAdd);
        } else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(instance.getDatabaseType())) {
            //创建触发器
            TransUtil.transEncryptColumnsToSqlServer(conn, dbhsmEncryptColumnsAdd);
        }

        //先删除之前的视图
        ViewUtil.deleteView(conn, dbhsmEncryptColumnsAdd);
        //创建视图
        boolean viewRet = ViewUtil.operView(conn, dbhsmEncryptColumnsAdd, dbhsmEncryptColumnsMapper);
        if (viewRet) {
            conn.commit();
        } else {
            log.error("创建视图异常");
            throw new Exception("创建视图异常");
        }

        if (conn != null) {
            conn.close();
        }
        return ret;
    }

    /**
     * 修改数据库加密列
     *
     * @param dbhsmEncryptColumns 数据库加密列
     * @return 结果
     */
    @Override
    public int updateDbhsmEncryptColumns(DbhsmEncryptColumns dbhsmEncryptColumns) {
        dbhsmEncryptColumns.setUpdateTime(DateUtils.getNowDate());
        return dbhsmEncryptColumnsMapper.updateDbhsmEncryptColumns(dbhsmEncryptColumns);
    }

    /**
     * 批量删除数据库加密列
     *
     * @param ids 需要删除的数据库加密列主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmEncryptColumnsByIds(String[] ids) {
        return dbhsmEncryptColumnsMapper.deleteDbhsmEncryptColumnsByIds(ids);
    }

    /**
     * 删除数据库加密列信息
     *
     * @param id 数据库加密列主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmEncryptColumnsById(String id) {
        return dbhsmEncryptColumnsMapper.deleteDbhsmEncryptColumnsById(id);
    }

    /**
     * 树结构
     *
     * @return
     */
    @Override
    public AjaxResult2 treeData() {
        List<DbhsmDbUser> usersList = new ArrayList<>();
        DbhsmDbInstance instance;
        DbhsmDbUser user;
        String tableName;
        Connection conn = null;
        List<Map<String, Object>> instancetTrees = new ArrayList<Map<String, Object>>();
        List<DbhsmDbInstance> instanceList = instanceMapper.selectDbhsmDbInstanceList(new DbhsmDbInstance());
        for (int i = 0; i < instanceList.size(); i++) {
            instance = instanceList.get(i);
            try {
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);

                Map<String, Object> instanceMap = new HashMap<String, Object>();
                instanceMap.put("id", instance.getId());
                instanceMap.put("pId", "0");
                instanceMap.put("title", instance.getDatabaseServerName());
                instanceMap.put("level", 1);
                instancetTrees.add(instanceMap);

                DbhsmDbUser dbUser = new DbhsmDbUser();
                dbUser.setDatabaseInstanceId(instance.getId());
                dbUser.setDbInstanceGetConnDTO(connDTO);
                usersList = dbUsersService.selectDbhsmDbUsersList(dbUser);

                for (int j = 0; j < usersList.size(); j++) {
                    try {
                        user = usersList.get(j);
                        if (user.getIsSelfBuilt().intValue() != 0) {
                            continue;
                        }

                        //只有通过web创建的用户才允许创建加密列
                        Map<String, Object> userMap = new HashMap<String, Object>();
                        userMap.put("id", user.getId());
                        userMap.put("pId", instance.getId());
                        userMap.put("title", user.getUserName());
                        userMap.put("level", 2);
                        instancetTrees.add(userMap);
                        conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                        if (Optional.ofNullable(conn).isPresent()) {
                            List<String> tableNameList = DBUtil.findAllTables(conn, user.getUserName(), instance.getDatabaseType());
                            for (int k = 0; k < tableNameList.size(); k++) {
                                tableName = tableNameList.get(k);
                                Map<String, Object> dbTableMetaMap = new HashMap<String, Object>();
                                dbTableMetaMap.put("id", tableName);
                                dbTableMetaMap.put("pId", user.getId());
                                dbTableMetaMap.put("title", tableName);
                                dbTableMetaMap.put("level", 3);
                                instancetTrees.add(dbTableMetaMap);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (conn != null) {
                                conn.close();
                                conn = null;
                            }
                        } catch (Exception e) {
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (StringUtils.isEmpty(instancetTrees)) {
            return AjaxResult2.success();
        }
        return AjaxResult2.success(EleTreeWrapper.getInstance().getTree(instancetTrees, "pId", "id"));
    }

    /**
     * 获得表的所有列名
     *
     * @param
     * @param
     * @return 列数组
     * @throws DbRuntimeException SQL执行异常
     */
    public String[] getColumnNames(Long dbInstanceId, Long dbUserId, String dbTableName) throws Exception {
        final List<String> columnNames = new ArrayList<>();
        String dbServerName, userPwd;
        //根据用户Id获取用户信息
        DbhsmDbUser dbUserInfo = dbUsersService.selectDbhsmDbUsersById(dbUserId);

        if (dbUserInfo == null) {
            log.error("未获取到数据库用户 dbUserId:{}", dbUserId);
            throw new Exception("未获取到数据库用户");
        }

        if (StringUtils.isEmpty(dbUserInfo.getUserName()) || StringUtils.isEmpty(dbUserInfo.getPassword())) {
            log.error("数据库用户名或密码为空: UserName:{},Password:{}", dbUserInfo.getUserName(), dbUserInfo.getPassword());
            throw new Exception("数据库用户名或密码为空");
        }

        //创建数据库服务连接
        DbhsmDbInstance instance = instanceMapper.selectDbhsmDbInstanceById(dbInstanceId);
        if (dbUserInfo == null) {
            log.error("根据ID未获取到数据库实例 dbInstanceId:{}", dbInstanceId);
            throw new Exception("未获取到数据库实例：dbInstanceId：" + dbInstanceId);
        }
        DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(instance, connDTO);
        connDTO.setDatabaseDba(dbUserInfo.getUserName());
        connDTO.setDatabaseDbaPassword(dbUserInfo.getPassword());
        Connection conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
        ;
        if (Optional.ofNullable(conn).isPresent()) {
            try {
                // catalog和schema获取失败默认使用null代替
                final String catalog = DBUtil.getCataLog(conn);
                final String schema = DBUtil.getSchema(conn);

                final DatabaseMetaData metaData = conn.getMetaData();
                try (final ResultSet rs = metaData.getColumns(catalog, schema, dbTableName, null)) {
                    if (null != rs) {
                        while (rs.next()) {
                            columnNames.add(rs.getString("COLUMN_NAME"));
                        }
                    }
                }
                return columnNames.toArray(new String[0]);
            } catch (Exception e) {
                throw new DbRuntimeException("Get columns error!", e);
            } finally {
                DbUtil.close(conn);
            }
        }
        return new String[0];
    }

    private String getInstance(DbhsmDbInstance instance) {
        String databaseType = "";
        if (DbConstants.DB_TYPE_ORACLE.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_ORACLE_DESC;
        } else if (DbConstants.DB_TYPE_SQLSERVER.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_SQLSERVER_DESC;
        } else if (DbConstants.DB_TYPE_MYSQL.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_MYSQL_DESC;
        }
        return databaseType + ":" + instance.getDatabaseIp() + ":" + instance.getDatabasePort() + instance.getDatabaseExampleType() + instance.getDatabaseServerName();
    }
}

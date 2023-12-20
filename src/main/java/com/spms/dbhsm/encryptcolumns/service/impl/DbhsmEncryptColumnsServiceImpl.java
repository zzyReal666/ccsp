package com.spms.dbhsm.encryptcolumns.service.impl;

import cn.hutool.db.DbRuntimeException;
import cn.hutool.db.DbUtil;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.SnowFlakeUtil;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.utils.tree.EleTreeWrapper;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.security.utils.DictUtils;
import com.ccsp.common.security.utils.SecurityUtils;
import com.ccsp.system.api.systemApi.domain.SysDictData;
import com.spms.common.CommandUtil;
import com.spms.common.JSONDataUtil;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.DBUtil;
import com.spms.common.dbTool.TransUtil;
import com.spms.common.dbTool.ViewUtil;
import com.spms.common.dbTool.stockDataProcess.mysql.MysqlStock;
import com.spms.common.dbTool.stockDataProcess.oracle.OraclelStock;
import com.spms.common.dbTool.stockDataProcess.postgresql.PostgreSQLStock;
import com.spms.common.dbTool.stockDataProcess.sqlserver.SqlServerStock;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.dbUser.mapper.DbhsmDbUsersMapper;
import com.spms.dbhsm.dbUser.service.IDbhsmDbUsersService;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsDto;
import com.spms.dbhsm.encryptcolumns.mapper.DbhsmEncryptColumnsMapper;
import com.spms.dbhsm.encryptcolumns.service.IDbhsmEncryptColumnsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.*;
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

    @Autowired
    private DbhsmDbUsersMapper dbUsersMapper;


    //@Value("${server.port:10013}")
    private static int dbhsmPort =80;

    static {
            String sysDataToDB = JSONDataUtil.getSysDataToDB(DbConstants.DBENC_WEB_PORT);
            if (StringUtils.isNotEmpty(sysDataToDB)){
                assert sysDataToDB != null;
                dbhsmPort = Integer.parseInt(sysDataToDB);
            }
    }
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
        DbhsmDbUser dbUserInfo = dbUsersMapper.selectDbhsmDbUsersById(columnsDto.getDbUserId());

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
            }else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(instance.getDatabaseType())) {
                tableName = columnsDto.getDbTableName();
            }else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(instance.getDatabaseType())) {
                tableName = columnsDto.getDbTableName();
            }

            List<Map<String, String>> columnsInfoList = DBUtil.findAllColumnsInfo(conn, tableName, instance.getDatabaseType());
            for (int j = 0; j < columnsInfoList.size(); j++) {
                DbhsmEncryptColumns encryptColumns = new DbhsmEncryptColumns();
                encryptColumns.setDbInstanceId(instance.getId());
                encryptColumns.setDbInstance(getInstance(instance));
                //Oracle数据库需要区分用户 查询使用
                if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(instance.getDatabaseType())) {
                    encryptColumns.setDbUserName(dbUserInfo.getUserName());
                }
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
                encryptColumns.setDbUserName(dbUserInfo.getUserName());
                columnsList.add(encryptColumns);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }finally {
            if (conn != null) {
                conn.close();
            }
        }
        return columnsList;
    }

    /**
     * 新增数据库加密列
     *
     * @param dbhsmEncryptColumnsAdd 数据库加密列
     * @return 结果
     */
    @Override
    public int insertDbhsmEncryptColumns(DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd) throws Exception {
        insertDbEncryptColumns(dbhsmEncryptColumnsAdd);
        Thread thread = new Thread(() -> {
            try {
                stockDataEnc(dbhsmEncryptColumnsAdd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
        return 1;
    }

    @Transactional(rollbackFor = Exception.class)
     public void stockDataEnc(DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd) throws Exception {
        // 使用DBA创建连接
        DbhsmDbInstance instance = instanceMapper.selectDbhsmDbInstanceById(dbhsmEncryptColumnsAdd.getDbInstanceId());
        dbhsmEncryptColumnsAdd.setDatabaseType(instance.getDatabaseType());
        dbhsmEncryptColumnsAdd.setDatabaseServerName(instance.getDatabaseServerName());
        Connection conn = null;
        try {
            DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
            BeanUtils.copyProperties(instance, connDTO);
            conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
            //获取端口对应的IP
            String ip = getIp(dbhsmEncryptColumnsAdd.getEthernetPort());
            dbhsmEncryptColumnsAdd.setIpAndPort(ip + ":" + dbhsmPort);
            dbhsmEncryptColumnsAdd.setEncryptionStatus(DbConstants.ENCRYPTED);
            dbhsmEncryptColumnsAdd.setCreateTime(DateUtils.getNowDate());
            dbhsmEncryptColumnsAdd.setCreateBy(SecurityUtils.getUsername());
            DbhsmEncryptColumns dbhsmEncryptColumns = new DbhsmEncryptColumns();
            BeanUtils.copyProperties(dbhsmEncryptColumnsAdd, dbhsmEncryptColumns);
            DbhsmDbUser user = new DbhsmDbUser();
            if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(instance.getDatabaseType())) {
                OraclelStock.oracleStockEncOrDec(conn,instance, dbhsmEncryptColumnsAdd, DbConstants.STOCK_DATA_ENCRYPTION);
            } else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(instance.getDatabaseType())) {
                SqlServerStock.sqlserverStockEncOrDec(conn, dbhsmEncryptColumnsAdd,DbConstants.ENC_FLAG);
            } else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(instance.getDatabaseType())) {
                MysqlStock.mysqlStockEncOrDec(conn, dbhsmEncryptColumnsAdd,DbConstants.STOCK_DATA_ENCRYPTION);
            }else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(instance.getDatabaseType())) {
                //改成使用用户的连接
                DbhsmDbUser dbUser = new DbhsmDbUser();
                dbUser.setUserName(dbhsmEncryptColumnsAdd.getDbUserName());
                dbUser.setDatabaseInstanceId(dbhsmEncryptColumnsAdd.getDbInstanceId());
                List<DbhsmDbUser> dbhsmDbUsers = dbUsersMapper.selectDbhsmDbUsersList(dbUser);
                if (StringUtils.isEmpty(dbhsmDbUsers)){
                    log.error("根据实例ID和用户名未获取到用户信息, InstanceId：{}，DbUserName：{}", dbUser.getDatabaseInstanceId(),dbUser.getUserName());
                    throw new Exception("根据实例ID和用户名未获取到用户信息,用户名：" + dbUser.getUserName());
                }
                user = dbhsmDbUsers.get(0);
                PostgreSQLStock.postgreSQLStockEncOrDec(conn, dbhsmEncryptColumnsAdd, user, DbConstants.STOCK_DATA_ENCRYPTION);
            }
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public int insertDbEncryptColumns(DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd) throws Exception {
        // 使用DBA创建连接
        DbhsmDbInstance instance = instanceMapper.selectDbhsmDbInstanceById(dbhsmEncryptColumnsAdd.getDbInstanceId());
        dbhsmEncryptColumnsAdd.setDatabaseType(instance.getDatabaseType());
        dbhsmEncryptColumnsAdd.setDatabaseServerName(instance.getDatabaseServerName());
        Connection conn = null;
       try {
        DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(instance, connDTO);
        conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);

        //获取端口对应的IP
        String ip = getIp(dbhsmEncryptColumnsAdd.getEthernetPort());
        dbhsmEncryptColumnsAdd.setIpAndPort(ip + ":" + dbhsmPort);
        dbhsmEncryptColumnsAdd.setEncryptionStatus(DbConstants.ENCRYPTED);
        dbhsmEncryptColumnsAdd.setId(SnowFlakeUtil.getSnowflakeId());

        dbhsmEncryptColumnsAdd.setCreateTime(DateUtils.getNowDate());
        dbhsmEncryptColumnsAdd.setCreateBy(SecurityUtils.getUsername());
        DbhsmEncryptColumns dbhsmEncryptColumns = new DbhsmEncryptColumns();
        BeanUtils.copyProperties(dbhsmEncryptColumnsAdd, dbhsmEncryptColumns);
        int ret = dbhsmEncryptColumnsMapper.insertDbhsmEncryptColumns(dbhsmEncryptColumns);
        DbhsmDbUser user = new DbhsmDbUser();
        if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(instance.getDatabaseType())) {
            //oracle创建触发器
            if (DbConstants.SGD_SM4.equals(dbhsmEncryptColumnsAdd.getEncryptionAlgorithm())) {
                TransUtil.transEncryptColumns(conn, dbhsmEncryptColumnsAdd);
            } else {
                TransUtil.transFPEEncryptColumns(conn, dbhsmEncryptColumnsAdd);
            }
        } else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(instance.getDatabaseType())) {
            //创建 SqlServer 触发器
            TransUtil.transEncryptColumnsToSqlServer(conn, dbhsmEncryptColumnsAdd);
        } else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(instance.getDatabaseType())) {
            //创建Mysql触发器
            TransUtil.transEncryptColumnsToMySql(conn, dbhsmEncryptColumnsAdd);
        }else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(instance.getDatabaseType())) {
            //改成使用用户的连接
            DbhsmDbUser dbUser = new DbhsmDbUser();
            dbUser.setUserName(dbhsmEncryptColumnsAdd.getDbUserName());
            dbUser.setDatabaseInstanceId(dbhsmEncryptColumnsAdd.getDbInstanceId());
            List<DbhsmDbUser> dbhsmDbUsers = dbUsersMapper.selectDbhsmDbUsersList(dbUser);
            if (StringUtils.isEmpty(dbhsmDbUsers)){
                log.error("根据实例ID和用户名未获取到用户信息, InstanceId：{}，DbUserName：{}", dbUser.getDatabaseInstanceId(),dbUser.getUserName());
                throw new Exception("根据实例ID和用户名未获取到用户信息,用户名：" + dbUser.getUserName());
            }
            user = dbhsmDbUsers.get(0);
            //sm4/fpe触发器函数
            TransUtil.transEncryptFunToPostgreSql(conn,dbhsmEncryptColumnsAdd,user);
            //触发器
            TransUtil.transEncryptColumnsToPostgreSql(conn, dbhsmEncryptColumnsAdd,user);

        }

        //先删除之前的视图
        ViewUtil.deleteView(conn, dbhsmEncryptColumnsAdd,"\""+user.getDbSchema()+"\"");
        //创建视图
        boolean viewRet = ViewUtil.operView(conn, dbhsmEncryptColumnsAdd, dbhsmEncryptColumnsMapper,"\""+user.getDbSchema()+"\"");
        if (viewRet) {
            conn.commit();

            //存量数据加密
            //ip = getIp(dbhsmEncryptColumnsAdd.getEthernetPort());
            //dbhsmEncryptColumnsAdd.setIpAndPort(ip + ":" + dbhsmPort);;
            //if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(instance.getDatabaseType())) {
            //
            //} else if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(instance.getDatabaseType())) {
            //    SqlServerStock.sqlserverStockEncOrDec(conn, dbhsmEncryptColumnsAdd,DbConstants.ENC_FLAG);
            //} else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(instance.getDatabaseType())) {
            //
            //}else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(instance.getDatabaseType())) {
            //    PostgreSQLStock.postgreSQLStockEncOrDec(conn, dbhsmEncryptColumnsAdd, user, DbConstants.STOCK_DATA_ENCRYPTION);
            //}
            //conn.commit();
        } else {
            log.error("创建视图异常");
            throw new Exception("创建视图异常");
        }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
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
    public int deleteDbhsmEncryptColumnsByIds(String[] ids) throws Exception {
        DbhsmEncryptColumns certView = null;
        String userSchema = "";
        DbhsmEncryptColumns encryptColumns =new DbhsmEncryptColumns();
        DbhsmDbInstance instance = new DbhsmDbInstance();
        for (int i = 0; i < ids.length; i++) {
            encryptColumns = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsById(ids[i]);
            if (encryptColumns == null) {
                log.error("未获取到ID为{}的加密列信息" , ids[i]);
                continue;
            }

            if (i == 0){
                certView = new DbhsmEncryptColumns();
                certView.setDbInstanceId(encryptColumns.getDbInstanceId());
                certView.setDbUserName(encryptColumns.getDbUserName());
                certView.setDbTable(encryptColumns.getDbTable());
            }

            instance = instanceMapper.selectDbhsmDbInstanceById(encryptColumns.getDbInstanceId());
            Connection connection = null;
            //如果是SQLserver需要删除对应的触发器
            PreparedStatement preparedStatement = null;
            String algorithm = encryptColumns.getEncryptionAlgorithm();
            String flag = DbConstants.SGD_SM4.equals(algorithm) ? "_" : "_fpe_";
                String sql = "";
                try {
                    int resultSet = 0;
                    DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                    BeanUtils.copyProperties(instance, connDTO);
                    connection = DbConnectionPoolFactory.getInstance().getConnection(connDTO);

                    DbhsmEncryptColumnsAdd encryptColumnsAdd = new DbhsmEncryptColumnsAdd();
                    BeanUtils.copyProperties(encryptColumns, encryptColumnsAdd);
                    String ip = getIp(encryptColumnsAdd.getEthernetPort());
                    encryptColumnsAdd.setIpAndPort(ip + ":" + dbhsmPort);
                    encryptColumnsAdd.setDatabaseServerName(instance.getDatabaseServerName());
                    if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(instance.getDatabaseType())) {
                        sql = "DROP TRIGGER IF EXISTS tr_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns()+"_" + DbConstants.algMapping(encryptColumns.getEncryptionAlgorithm()) + "_encrypt";
                        preparedStatement = connection.prepareStatement(sql);
                        resultSet = preparedStatement.executeUpdate();
                        //删除加密列时存量数据解密
                        //sqlserverStockEncOrDec(connection, encryptColumnsAdd);
                    } else if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(instance.getDatabaseType())) {
                        sql = "DROP TRIGGER " + encryptColumns.getDbUserName() + ".tr" + flag + encryptColumns.getDbUserName() + "_" + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns();
                        preparedStatement = connection.prepareStatement(sql);
                        resultSet = preparedStatement.executeUpdate();
                    }else  if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(instance.getDatabaseType())) {
                        sql = "DROP TRIGGER tri_"  + encryptColumns.getDbTable() + "_" + encryptColumns.getEncryptColumns();
                        preparedStatement = connection.prepareStatement(sql);
                        resultSet = preparedStatement.executeUpdate();

                    }else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(instance.getDatabaseType())) {
                        DbhsmDbUser dbUser = new DbhsmDbUser();
                        dbUser.setUserName(encryptColumns.getDbUserName());
                        dbUser.setDatabaseInstanceId(encryptColumns.getDbInstanceId());
                        List<DbhsmDbUser> dbhsmDbUsers = dbUsersMapper.selectDbhsmDbUsersList(dbUser);
                        if (StringUtils.isEmpty(dbhsmDbUsers)){
                            log.error("根据实例ID和用户名未获取到用户信息, InstanceId：{}，DbUserName：{}", dbUser.getDatabaseInstanceId(),dbUser.getUserName());
                            throw new Exception("根据实例ID和用户名未获取到用户信息,用户名：" + dbUser.getUserName());
                        }
                        DbhsmDbUser user = dbhsmDbUsers.get(0);

                        userSchema="\"" + user.getDbSchema() +"\"";
                        String transName = "tr_" + DbConstants.algMapping(encryptColumns.getEncryptionAlgorithm()) + "_" + encryptColumns.getDbTable()+ "_" + encryptColumns.getEncryptColumns() ;
                        sql = "DROP TRIGGER IF EXISTS " + transName + " on " + encryptColumns.getDbTable() + " CASCADE";
                        log.info(sql);
                        preparedStatement = connection.prepareStatement(sql);
                        resultSet = preparedStatement.executeUpdate();

                        String funName = userSchema + ".tr_" + DbConstants.algMapping(encryptColumns.getEncryptionAlgorithm()) + "_" + user.getUserName() + "_" + encryptColumns.getDbTable()+ "_" + encryptColumns.getEncryptColumns();
                        sql = "DROP FUNCTION " + funName +" CASCADE;";
                        preparedStatement = connection.prepareStatement(sql);
                         resultSet = preparedStatement.executeUpdate();
                        connection.commit();

                        //存量数据解密
                        //postgreSQLStockEncOrDec(connection,encryptColumnsAdd, user);
                    }
                    //执行删除视图
                    try{
                        encryptColumnsAdd.setDatabaseServerName(instance.getDatabaseServerName());
                        encryptColumnsAdd.setDatabaseType(instance.getDatabaseType());
                        ViewUtil.deleteView(connection,encryptColumnsAdd,userSchema);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    connection.commit();
                } catch (SQLException e) {
                    //触发器不存在异常不抛出，其他异常抛出
                    if (!e.getMessage().contains("不存在")) {
                        e.printStackTrace();
                        throw new SQLException(e.getMessage());
                    }
                } finally {
                    //释放资源
                    if (preparedStatement != null) {
                        preparedStatement.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }
                }
        }
        //存量数据解密
        stockDecBeforeDel(encryptColumns,instance);
        Thread.sleep(3000);
        int ret = dbhsmEncryptColumnsMapper.deleteDbhsmEncryptColumnsByIds(ids);
        if (ret > 0) {
            //重新创建视图
            List<DbhsmEncryptColumns> dbhsmEncryptColumns = dbhsmEncryptColumnsMapper.selectDbhsmEncryptColumnsList(certView);
            if (StringUtils.isNotEmpty(dbhsmEncryptColumns)) {
                instance = instanceMapper.selectDbhsmDbInstanceById(certView.getDbInstanceId());
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);
                Connection connection = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                try {
                    //for (int i = 0; i < dbhsmEncryptColumns.size(); i++) {
                    DbhsmEncryptColumnsAdd dbhsmEncryptColumnsAdd = new DbhsmEncryptColumnsAdd();
                    BeanUtils.copyProperties(dbhsmEncryptColumns.get(0), dbhsmEncryptColumnsAdd);
                    String ip = getIp(dbhsmEncryptColumns.get(0).getEthernetPort());
                    dbhsmEncryptColumnsAdd.setIpAndPort(ip + ":" + dbhsmPort);
                    dbhsmEncryptColumnsAdd.setDatabaseType(instance.getDatabaseType());
                    dbhsmEncryptColumnsAdd.setDatabaseServerName(instance.getDatabaseServerName());
                    boolean viewRet = ViewUtil.operView(connection, dbhsmEncryptColumnsAdd, dbhsmEncryptColumnsMapper,userSchema);
                    if (viewRet) {
                        connection.commit();
                    } else {
                        log.error("创建视图异常");
                        throw new Exception("创建视图异常");
                    }
                    //}
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                }
            }
        }

        return ret;
    }

    private void stockDecBeforeDel(DbhsmEncryptColumns encryptColumns,DbhsmDbInstance instance) throws Exception {
            Connection connection = null;
            //如果是SQLserver需要删除对应的触发器
            PreparedStatement preparedStatement = null;
            try {
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);
                connection = DbConnectionPoolFactory.getInstance().getConnection(connDTO);

                DbhsmEncryptColumnsAdd encryptColumnsAdd = new DbhsmEncryptColumnsAdd();
                BeanUtils.copyProperties(encryptColumns, encryptColumnsAdd);
                String ip = getIp(encryptColumnsAdd.getEthernetPort());
                encryptColumnsAdd.setIpAndPort(ip + ":" + dbhsmPort);
                encryptColumnsAdd.setDatabaseServerName(instance.getDatabaseServerName());
                if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(instance.getDatabaseType())) {
                    //删除加密列时存量数据解密
                    stockDec(connection, encryptColumnsAdd,instance);
                } else if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(instance.getDatabaseType())) {
                    stockDec(connection, encryptColumnsAdd,instance);
                } else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(instance.getDatabaseType())) {
                    stockDec(connection, encryptColumnsAdd,instance);
                } else if (DbConstants.DB_TYPE_POSTGRESQL.equalsIgnoreCase(instance.getDatabaseType())) {
                    DbhsmDbUser dbUser = new DbhsmDbUser();
                    dbUser.setUserName(encryptColumns.getDbUserName());
                    dbUser.setDatabaseInstanceId(encryptColumns.getDbInstanceId());
                    List<DbhsmDbUser> dbhsmDbUsers = dbUsersMapper.selectDbhsmDbUsersList(dbUser);
                    if (StringUtils.isEmpty(dbhsmDbUsers)) {
                        log.error("根据实例ID和用户名未获取到用户信息, InstanceId：{}，DbUserName：{}", dbUser.getDatabaseInstanceId(), dbUser.getUserName());
                        throw new Exception("根据实例ID和用户名未获取到用户信息,用户名：" + dbUser.getUserName());
                    }
                    DbhsmDbUser user = dbhsmDbUsers.get(0);
                    //存量数据解密
                    postgreSQLStockEncOrDec(connection, encryptColumnsAdd, user);
                }

            } catch (SQLException e) {
                //触发器不存在异常不抛出，其他异常抛出
                if (!e.getMessage().contains("不存在")) {
                    e.printStackTrace();
                    throw new SQLException(e.getMessage());
                }
            } finally {
                //释放资源
                if (preparedStatement != null) {
                    preparedStatement.close();
                }

            }
        }

    private void stockDec(Connection connection, DbhsmEncryptColumnsAdd encryptColumnsAdd,DbhsmDbInstance instance) throws Exception {
        Thread thread = new Thread(() -> {
            try {
                if (DbConstants.DB_TYPE_SQLSERVER.equalsIgnoreCase(instance.getDatabaseType())) {
                    //存量数据解密
                    SqlServerStock.sqlserverStockEncOrDec(connection, encryptColumnsAdd,DbConstants.DEC_FLAG);
                } else if (DbConstants.DB_TYPE_ORACLE.equalsIgnoreCase(instance.getDatabaseType())) {
                    OraclelStock.oracleStockEncOrDec(connection,instance, encryptColumnsAdd,DbConstants.STOCK_DATA_DECRYPTION);
                } else if (DbConstants.DB_TYPE_MYSQL.equalsIgnoreCase(instance.getDatabaseType())) {
                    MysqlStock.mysqlStockEncOrDec(connection, encryptColumnsAdd,DbConstants.STOCK_DATA_DECRYPTION);
                }
                connection.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

    }

    private void postgreSQLStockEncOrDec(Connection finalConnection,DbhsmEncryptColumnsAdd encryptColumnsAdd,DbhsmDbUser user ) {
        Thread thread = new Thread(() -> {
            try {
                //存量数据解密
                PostgreSQLStock.postgreSQLStockEncOrDec(finalConnection, encryptColumnsAdd, user, DbConstants.STOCK_DATA_DECRYPTION);
                finalConnection.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if (finalConnection != null) {
                    try {
                        finalConnection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 根据网口名称获取IP
     * @param ethernetPort
     * @return
     */
    private String getIp(String ethernetPort) throws Exception {
        //获取端口
        String osName = System.getProperty("os.name");
        String ip = "";
         if (osName.toLowerCase().startsWith("linux")) {
             ip = CommandUtil.exeCmd("ip a| grep " + ethernetPort.split("@")[0] + " |grep inet |awk '{print $2}'|awk -F '/' '{print $1}'");
             if (StringUtils.isEmpty(ip)) {
                 throw new Exception(ethernetPort + "口IP不存在,请先进行配置IP");
             }
             ip = ip.trim().replaceAll("\n", ",");
             if (StringUtils.isEmpty(ip)) {
                 throw new Exception(ethernetPort + "口IP不存在,请先进行配置IP");
             }
             if (ip.lastIndexOf(",") == 0) {
                 ip = ip.substring(0, ip.length() - 1);
             }

        }

        return ip.split(",")[0];
    }

    public static void main(String[] args) {
        String ethernetPort="eth0";
            ethernetPort=ethernetPort.split("@")[0];
        System.out.println(ethernetPort);
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
        if (StringUtils.isEmpty(instanceList)) {
            return AjaxResult2.success();
        }


        List<SysDictData> dictDatas = DictUtils.getDictCache(DbConstants.DB_TYPE);
        if (StringUtils.isEmpty(dictDatas)) {
            dictDatas = addDefaultDb();
        }


        for (int i = 0; i < instanceList.size(); i++) {
            instance = instanceList.get(i);
            try {
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);
                String idPrefix = "$"+getInstance(instance);
                Map<String, Object> instanceMap = new HashMap<String, Object>();
                instanceMap.put("id", instance.getId()+idPrefix);
                instanceMap.put("pId", "0");
                instanceMap.put("title", instance.getDatabaseServerName() + getDataBaseName(instance.getDatabaseType(), dictDatas));
                instanceMap.put("level", 1);
                instancetTrees.add(instanceMap);
                if(isDatabaseServerReachable(connDTO.getDatabaseIp(),Integer.parseInt(connDTO.getDatabasePort()),5)) {
                    continue;
                }
                DbhsmDbUser dbUser = new DbhsmDbUser();
                dbUser.setDatabaseInstanceId(instance.getId());
                dbUser.setDbInstanceGetConnDTO(connDTO);
                usersList = dbUsersMapper.selectDbhsmDbUsersList(dbUser);

                for (int j = 0; j < usersList.size(); j++) {
                    try {
                        user = usersList.get(j);
                        if (user.getIsSelfBuilt() != 0) {
                            continue;
                        }

                        //只有通过web创建的用户才允许创建加密列
                        Map<String, Object> userMap = new HashMap<String, Object>();
                        userMap.put("id", user.getId()+idPrefix);
                        userMap.put("pId", instance.getId()+idPrefix);
                        userMap.put("title", user.getUserName());
                        userMap.put("level", 2);
                        instancetTrees.add(userMap);
                        conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                        if (Optional.ofNullable(conn).isPresent()) {
                            List<String> tableNameList = DBUtil.findAllTables(conn, user.getUserName(), instance.getDatabaseType(),instance.getDatabaseServerName(),user.getDbSchema());
                            for (int k = 0; k < tableNameList.size(); k++) {
                                tableName = tableNameList.get(k);
                                Map<String, Object> dbTableMetaMap = new HashMap<String, Object>();
                                dbTableMetaMap.put("id", tableName+idPrefix);
                                dbTableMetaMap.put("pId", user.getId()+idPrefix);
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
                            e.printStackTrace();
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
        } else if(DbConstants.DB_TYPE_POSTGRESQL.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_POSTGRESQL_DESC;
        }
        return databaseType + ":" + instance.getDatabaseIp() + ":" + instance.getDatabasePort() + instance.getDatabaseExampleType() + instance.getDatabaseServerName();
    }


    /**
     * 添加默认数据库类型
     *
     * @return
     */
    private List<SysDictData> addDefaultDb() {
        List<SysDictData> dictData = new ArrayList<>();
        SysDictData oracle = new SysDictData();
        oracle.setDictLabel(DbConstants.DB_TYPE_ORACLE_DESC);
        oracle.setDictValue(DbConstants.DB_TYPE_ORACLE);
        dictData.add(oracle);

        SysDictData sqlserver = new SysDictData();
        sqlserver.setDictLabel(DbConstants.DB_TYPE_SQLSERVER_DESC);
        sqlserver.setDictValue(DbConstants.DB_TYPE_SQLSERVER);
        dictData.add(sqlserver);

        SysDictData mysql = new SysDictData();
        mysql.setDictLabel(DbConstants.DB_TYPE_MYSQL_DESC);
        mysql.setDictValue(DbConstants.DB_TYPE_MYSQL);
        dictData.add(mysql);

        SysDictData postgreSQL = new SysDictData();
        postgreSQL.setDictLabel(DbConstants.DB_TYPE_POSTGRESQL_DESC);
        postgreSQL.setDictValue(DbConstants.DB_TYPE_POSTGRESQL);
        dictData.add(postgreSQL);
        return dictData;
    }

    /**
     * 获取数据库名
     *
     * @param databaseType
     * @param dictDatas
     * @return
     */
    private String getDataBaseName(String databaseType, List<SysDictData> dictDatas) {
        if (StringUtils.isEmpty(databaseType)) {
            return "";
        }
        for (SysDictData dictData : dictDatas) {
            if (databaseType.equals(dictData.getDictValue()) && StringUtils.isNotEmpty(dictData.getDictLabel())) {
                return "(" + dictData.getDictLabel() + ")";
            }
        }

        return "";
    }

    /**
     * 探测网络是否可以访问数据库
     * @param host
     * @param port
     * @param timeout 超时时间秒
     * @return
     */
    public static boolean isDatabaseServerReachable(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return false;
        } catch (IOException e) {
            return true;
        }
    }
}

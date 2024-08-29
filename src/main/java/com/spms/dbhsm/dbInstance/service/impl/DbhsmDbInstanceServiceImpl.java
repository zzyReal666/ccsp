package com.spms.dbhsm.dbInstance.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.spms.common.SelectOption;
import com.spms.common.constant.DbConstants;
import com.spms.common.dbTool.FunctionUtil;
import com.spms.common.pool.hikariPool.DbConnectionPoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstancePoolKeyDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.domain.VO.InstanceServerNameVO;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbInstance.service.IDbhsmDbInstanceService;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.dbUser.mapper.DbhsmDbUsersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据库实例Service业务层处理
 *
 * @author spms
 * @date 2023-09-19
 */
@Service
@Slf4j
public class DbhsmDbInstanceServiceImpl implements IDbhsmDbInstanceService {
    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

    @Autowired
    private DbhsmDbUsersMapper dbhsmDbUsersMapper;

    @PostConstruct
    public void init() {
        // 创建线程1
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                initDbConnectionPool();
            }
        });
        executor.shutdown();
    }

    public void initDbConnectionPool() {
        try {
            List<DbhsmDbInstance> dbhsmDbInstanceList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(null);
            if (CollectionUtils.isEmpty(dbhsmDbInstanceList)) {
                log.info("数据库实例列表为空，无需初始化数据库连接池");
                return;
            }
            for (DbhsmDbInstance dbhsmDbInstance : dbhsmDbInstanceList) {
                if (DbConstants.DB_TYPE_HB.equals(dbhsmDbInstance.getDatabaseType())) {
                    continue;
                }
                DbInstancePoolKeyDTO instanceKey = new DbInstancePoolKeyDTO();
                BeanUtils.copyProperties(dbhsmDbInstance, instanceKey);
                DbInstanceGetConnDTO instanceGetConnDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(dbhsmDbInstance, instanceGetConnDTO);
                try {
                    DbConnectionPoolFactory.buildDataSourcePool(instanceGetConnDTO);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info("初始化数据库连接池失败:{}", e.getMessage());
                }
            }
            DbConnectionPoolFactory.queryPool();
            log.info("初始化数据库连接池成功");
        } catch (Exception e) {

        }
    }

    /**
     * 查询数据库实例
     *
     * @param id 数据库实例主键
     * @return 数据库实例
     */
    @Override
    public DbhsmDbInstance selectDbhsmDbInstanceById(Long id) {
        return dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
    }

    @Override
    public List<InstanceServerNameVO> listDbInstanceSelect(InstanceServerNameVO instanceServerNameVO) {
        List<InstanceServerNameVO> voList = dbhsmDbInstanceMapper.listDbInstanceSelect(instanceServerNameVO);
        for (InstanceServerNameVO vo : voList) {
            switch (vo.getDatabaseType()) {
                case DbConstants.DB_TYPE_ORACLE:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_ORACLE_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_SQLSERVER:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_SQLSERVER_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_MYSQL:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_MYSQL_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_POSTGRESQL:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_POSTGRESQL_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_DM:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_DM_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_CLICKHOUSE:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_CLICKHOUSE_DESC + ")");
                    break;
                case DbConstants.DB_TYPE_KB:
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_KING_BASE_DESC + ")");
                    break;
                default:
                    // 处理未知的数据库类型
                    break;
            }
        }
        return voList;
    }

    /**
     * 查询数据库实例列表
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 数据库实例
     */
    @Override
    public List<DbhsmDbInstance> selectDbhsmDbInstanceList(DbhsmDbInstance dbhsmDbInstance) {
        return dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(dbhsmDbInstance);
    }

    /**
     * 新增数据库实例
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException, SQLException {
        int i = 0;
        CallableStatement cstmt = null;
        //数据类型为oracle
        if (DbConstants.DB_TYPE_ORACLE.equals(dbhsmDbInstance.getDatabaseType())) {
            //数据库实例唯一性判断
            if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(checkDBOracleInstanceUnique(dbhsmDbInstance))) {
                throw new ZAYKException("数据库实例已存在");
            }
        } else {
            //数据库实例唯一性判断
            if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(checkOtherDBUnique(dbhsmDbInstance))) {
                throw new ZAYKException("数据库实例已存在");
            }
            dbhsmDbInstance.setDatabaseExampleType("-");
        }
        dbhsmDbInstance.setCreateTime(com.zayk.util.DateUtils.getNowDate());
        i = dbhsmDbInstanceMapper.insertDbhsmDbInstance(dbhsmDbInstance);
        //创建连接池
        DbInstanceGetConnDTO dbInstanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance, dbInstanceGetConnDTO);
        DbConnectionPoolFactory.buildDataSourcePool(dbInstanceGetConnDTO);
        DbConnectionPoolFactory.queryPool();
        if (DbConstants.DB_TYPE_DM.equals(dbhsmDbInstance.getDatabaseType())) {
            Connection connection = DbConnectionPoolFactory.getInstance().getConnection(dbhsmDbInstance);
            try {
                cstmt = connection.prepareCall("{call SP_SET_PARA_STRING_VALUE(2,'COMM_ENCRYPT_NAME','DES_OFB');}");
                cstmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cstmt.close();
                connection.close();
            }
        }
        return i;
    }

    public String checkDBOracleInstanceUnique(DbhsmDbInstance dbhsmDbInstance) {
        DbhsmDbInstance instance = new DbhsmDbInstance();
        instance.setDatabaseIp(dbhsmDbInstance.getDatabaseIp());
        instance.setDatabasePort(dbhsmDbInstance.getDatabasePort());
        instance.setDatabaseServerName(dbhsmDbInstance.getDatabaseServerName());
        instance.setDatabaseExampleType(dbhsmDbInstance.getDatabaseExampleType());
        instance.setDatabaseType(dbhsmDbInstance.getDatabaseType());
        List<DbhsmDbInstance> infoList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(instance);
        if (CollectionUtils.isEmpty(infoList)) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }
        return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
    }

    public String checkOtherDBUnique(DbhsmDbInstance dbhsmDbInstance) {
        DbhsmDbInstance instance = new DbhsmDbInstance();
        instance.setDatabaseIp(dbhsmDbInstance.getDatabaseIp());
        instance.setDatabasePort(dbhsmDbInstance.getDatabasePort());
        instance.setDatabaseServerName(dbhsmDbInstance.getDatabaseServerName());
        instance.setDatabaseType(dbhsmDbInstance.getDatabaseType());
        List<DbhsmDbInstance> sqlServerList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(instance);
        if (sqlServerList.size() > 0) {
            return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
        }
        return DbConstants.DBHSM_GLOBLE_UNIQUE;
    }

    /**
     * 修改数据库实例
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException, SQLException {
        if (DbConstants.DB_TYPE_ORACLE.equals(dbhsmDbInstance.getDatabaseType())) {
            if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(editCheckDBOracleInstanceUnique(dbhsmDbInstance))) {
                throw new ZAYKException("修改失败，数据库实例" + dbhsmDbInstance.getDatabaseIp() + ":" + dbhsmDbInstance.getDatabasePort() + dbhsmDbInstance.getDatabaseServerName() + "已存在");
            }
        } else if (DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType())) {
            //数据库实例唯一性判断
            if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(editCheckDBSqlServerUnique(dbhsmDbInstance))) {
                throw new ZAYKException("修改失败，数据库实例" + dbhsmDbInstance.getDatabaseIp() + ":" + dbhsmDbInstance.getDatabasePort() + dbhsmDbInstance.getDatabaseServerName() + "已存在");
            }
        }
        DbhsmDbInstance instanceById = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbhsmDbInstance.getId());
        dbhsmDbInstance.setUpdateTime(DateUtils.getNowDate());
        int i = dbhsmDbInstanceMapper.updateDbhsmDbInstance(dbhsmDbInstance);
        //如果创建连接池需要的数据数据未做修改，不需要重新建链接，否则需要在修改时先销毁之前的池，再生成新连接池
        if (DbInstanceGetConnDTO.instanceConvertGetConnDTO(instanceById).equals(DbInstanceGetConnDTO.instanceConvertGetConnDTO(dbhsmDbInstance))) {
            return i;
        }
        //删除动态数据连接池中名称为dbhsmDbInstance的连接池
        DbConnectionPoolFactory.getInstance().unbind(DbConnectionPoolFactory.instanceConventKey(dbhsmDbInstance));
        //重建连接池
        DbInstanceGetConnDTO dbInstanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance, dbInstanceGetConnDTO);
        DbConnectionPoolFactory.buildDataSourcePool(dbInstanceGetConnDTO);
        DbConnectionPoolFactory.queryPool();
        return i;
    }


    public String editCheckDBOracleInstanceUnique(DbhsmDbInstance dbhsmDbInstance) {
        DbhsmDbInstance instance = new DbhsmDbInstance();
        instance.setDatabaseIp(dbhsmDbInstance.getDatabaseIp());
        instance.setDatabasePort(dbhsmDbInstance.getDatabasePort());
        instance.setDatabaseServerName(dbhsmDbInstance.getDatabaseServerName());
        instance.setDatabaseExampleType(dbhsmDbInstance.getDatabaseExampleType());
        List<DbhsmDbInstance> infoList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(instance);
        if (!CollectionUtils.isEmpty(infoList)) {
            if (!dbhsmDbInstance.getId().equals(infoList.get(0).getId())) {
                return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
            }
        }
        return DbConstants.DBHSM_GLOBLE_UNIQUE;
    }

    public String editCheckDBSqlServerUnique(DbhsmDbInstance dbhsmDbInstance) {
        DbhsmDbInstance instance = new DbhsmDbInstance();
        instance.setDatabaseIp(dbhsmDbInstance.getDatabaseIp());
        instance.setDatabasePort(dbhsmDbInstance.getDatabasePort());
        List<DbhsmDbInstance> sqlServerList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(instance);
        if (sqlServerList.size() > 0) {
            if (!dbhsmDbInstance.getId().equals(sqlServerList.get(0).getId())) {
                return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
            }
        }
        return DbConstants.DBHSM_GLOBLE_UNIQUE;
    }

    /**
     * 批量删除数据库实例
     *
     * @param ids 需要删除的数据库实例主键
     * @return 结果
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult deleteDbhsmDbInstanceByIds(Long[] ids) {
        int i = 0;
        List<String> isUsedInstances = new ArrayList<String>();
        for (Long id : ids) {
            //删除之前先销毁之前的池
            DbhsmDbInstance instanceById = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
            //查看实例是否创建过用户
            if (checkInstanceCreatedUser(id)) {
                isUsedInstances.add(getInstance(instanceById));
                continue;
            }
            i = dbhsmDbInstanceMapper.deleteDbhsmDbInstanceById(id);
            try {
                //删除加解密函数
                delEncDecFunction(instanceById);
                //删除连接池
                DbConnectionPoolFactory.getInstance().unbind(DbConnectionPoolFactory.instanceConventKey(instanceById));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isUsedInstances.size() > 0 ? AjaxResult.error("实例：" + StringUtils.join(isUsedInstances, ",") + "已从管理端创建过用户，无法删除！") : AjaxResult.success();
    }

    private void delEncDecFunction(DbhsmDbInstance instanceById) {
        String delEncFunSql = "", delDecFunSql = "";
        try {
            Connection connection = DbConnectionPoolFactory.getInstance().getConnection(instanceById);
            String databaseType = instanceById.getDatabaseType();
            //删除mysql加解密函数
            switch (databaseType) {
                case DbConstants.DB_TYPE_ORACLE:
                    break;
                case DbConstants.DB_TYPE_SQLSERVER:
                    break;
                case DbConstants.DB_TYPE_MYSQL:
                    delEncFunSql = "DROP FUNCTION StringEncrypt;";
                    delDecFunSql = "DROP FUNCTION StringDecrypt;";
                    break;
                case DbConstants.DB_TYPE_POSTGRESQL:
                    break;
                case DbConstants.DB_TYPE_DM:
                    break;
                default:
                    log.info("Unknown database type: " + databaseType);
            }
            //执行SQL
            if (!StringUtils.isEmpty(delEncFunSql)) {
                connection.prepareStatement(delEncFunSql).executeUpdate();
                connection.prepareStatement(delDecFunSql).executeUpdate();
                connection.commit();
            }
        } catch (ZAYKException | SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除数据库实例信息
     *
     * @param id 数据库实例主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmDbInstanceById(Long id) {
        return dbhsmDbInstanceMapper.deleteDbhsmDbInstanceById(id);
    }

    /**
     * 查询Oracle表空间
     */
    @Override
    public List<SelectOption> getDbTablespace(Long id) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        int i = 0;
        List<SelectOption> tablespaceList = new ArrayList<>();
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (DbConstants.DB_TYPE_ORACLE.equals(instance.getDatabaseType()) || DbConstants.DB_TYPE_DM.equals(instance.getDatabaseType())) {
            if (!ObjectUtils.isEmpty(instance)) {
                //创建数据库连接
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);
                try {
                    conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                    if (Optional.ofNullable(conn).isPresent()) {
                        stmt = conn.createStatement();
                        String selectTableSpaceSql = null;
                        if (DbConstants.DB_TYPE_ORACLE.equals(instance.getDatabaseType())) {
                            selectTableSpaceSql = DbConstants.DB_SQL_ORACLE_TABLESPACE_QUERY;
                        } else if (DbConstants.DB_TYPE_DM.equals(instance.getDatabaseType())) {
                            selectTableSpaceSql = DbConstants.DB_SQL_DM_TABLESPACE_QUERY;
                        }
                        resultSet = stmt.executeQuery(selectTableSpaceSql);
                        while (resultSet.next()) {
                            SelectOption option = new SelectOption();
                            option.setId(i++);
                            option.setLabel(resultSet.getString("tablespace_name"));
                            tablespaceList.add(option);
                        }
                    }
                } catch (SQLException | ZAYKException e) {
                    e.printStackTrace();
                } finally {
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            return Collections.emptyList();
        }
        return tablespaceList;
    }

    //获取PostgreSQL 架构（schema)
    @Override
    public List<SelectOption> getDbSchema(Long id) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        int i = 0;
        String sql = null;
        List<SelectOption> list = new ArrayList<>();
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (DbConstants.DB_TYPE_POSTGRESQL.equals(instance.getDatabaseType())) {
            if (!ObjectUtils.isEmpty(instance)) {
                //创建数据库连接
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);
                try {
                    conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                    if (Optional.ofNullable(conn).isPresent()) {
                        stmt = conn.createStatement();
                        sql = "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('pg_catalog', 'pg_toast', 'information_schema');";
                        resultSet = stmt.executeQuery(sql);
                        while (resultSet.next()) {
                            SelectOption option = new SelectOption();
                            option.setId(i++);
                            option.setLabel(resultSet.getString("schema_name"));
                            list.add(option);
                        }
                    }
                } catch (SQLException | ZAYKException e) {
                    e.printStackTrace();
                } finally {
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else {
            return Collections.emptyList();
        }
        return list;
    }

    public boolean checkInstanceCreatedUser(Long instanceId) {
        DbhsmDbUser user = new DbhsmDbUser();
        user.setDatabaseInstanceId(instanceId);
        List<DbhsmDbUser> dbhsmDbUsers = dbhsmDbUsersMapper.selectDbhsmDbUsersList(user);
        return dbhsmDbUsers.size() > 0;
    }

    private String getInstance(DbhsmDbInstance instance) {
        String databaseType = "";
        if (DbConstants.DB_TYPE_ORACLE.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_ORACLE_DESC;
        } else if (DbConstants.DB_TYPE_SQLSERVER.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_SQLSERVER_DESC;
        } else if (DbConstants.DB_TYPE_MYSQL.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_MYSQL_DESC;
        } else if (DbConstants.DB_TYPE_POSTGRESQL.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_POSTGRESQL_DESC;
        } else if (DbConstants.DB_TYPE_DM.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_DM_DESC;
        }
        return databaseType + ":" + instance.getDatabaseIp() + ":" + instance.getDatabasePort() + instance.getDatabaseExampleType() + instance.getDatabaseServerName();
    }

    @Override
    public int getPwdPolicyToDM(Long id) {
        int pwdPolicyToDM = 2;
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        try {
            Connection connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
            pwdPolicyToDM = FunctionUtil.getPwdPolicyToDM(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pwdPolicyToDM;
    }

    @Override
    public int getPwdMinLenToDM(Long id) {
        int pwdMinLenToDM = 9;
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        try {
            Connection connection = DbConnectionPoolFactory.getInstance().getConnection(instance);
            pwdMinLenToDM = FunctionUtil.getPwdMinLenToDM(connection);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pwdMinLenToDM;
    }


    @Override
    public AjaxResult2<Boolean> connectionTest(Long id) {
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if (null == dbhsmDbInstance) {
            return AjaxResult2.error("实例信息错误！");
        }
        DbInstancePoolKeyDTO instanceKey = new DbInstancePoolKeyDTO();
        BeanUtils.copyProperties(dbhsmDbInstance, instanceKey);
        DbInstanceGetConnDTO instanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance, instanceGetConnDTO);
        try {
            DbConnectionPoolFactory.queryPool();
        } catch (Exception e) {
            e.printStackTrace();
            log.info("初始化数据库连接池失败:{}", e.getMessage());
            return AjaxResult2.error("测试连接失败，请稍后重试！");
        }
        return AjaxResult2.success(true);
    }

    @Override
    public AjaxResult openProxy(Long id) {
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        return null;
    }

    @Override
    public AjaxResult proxyTest(Long id) {
        DbhsmDbInstance dbhsmDbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        return null;
    }
}

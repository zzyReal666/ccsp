package com.spms.dbhsm.dbInstance.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.StringUtils;
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
public class DbhsmDbInstanceServiceImpl implements IDbhsmDbInstanceService
{
    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

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
    public  void initDbConnectionPool()  {
        try {
            List<DbhsmDbInstance> dbhsmDbInstanceList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(null);
            if (CollectionUtils.isEmpty(dbhsmDbInstanceList)) {
                log.info("数据库实例列表为空，无需初始化数据库连接池");
                return;
            }
            for (DbhsmDbInstance dbhsmDbInstance : dbhsmDbInstanceList) {
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
        }catch (Exception e){

        }
    }
    /**
     * 查询数据库实例
     *
     * @param id 数据库实例主键
     * @return 数据库实例
     */
    @Override
    public DbhsmDbInstance selectDbhsmDbInstanceById(Long id)
    {
        return dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
    }

    @Override
    public List<InstanceServerNameVO> listDbInstanceSelect(InstanceServerNameVO instanceServerNameVO) {
        List<InstanceServerNameVO> voList = dbhsmDbInstanceMapper.listDbInstanceSelect(instanceServerNameVO);
        for (InstanceServerNameVO vo : voList) {
            switch (vo.getDatabaseType()) {
                case "0":
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_ORACLE_DESC + ")");
                    break;
                case "1":
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_SQLSERVER_DESC + ")");
                    break;
                case "2":
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_MYSQL_DESC + ")");
                    break;
                case "3":
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_POSTGRESQL_DESC + ")");
                    break;
                case "4":
                    vo.setLabel(vo.getLabel() + "(" + DbConstants.DB_TYPE_DB2_DESC + ")");
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
    public List<DbhsmDbInstance> selectDbhsmDbInstanceList(DbhsmDbInstance dbhsmDbInstance)
    {
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
        //数据类型为oracle
        if(DbConstants.DB_TYPE_ORACLE.equals(dbhsmDbInstance.getDatabaseType())) {
            //数据库实例唯一性判断
            if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(checkDBOracleInstanceUnique(dbhsmDbInstance))) {
                throw new ZAYKException("数据库实例已存在");
            }
        }else{
            //数据库实例唯一性判断
            if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(checkOtherDBUnique(dbhsmDbInstance))) {
                throw new ZAYKException("数据库实例已存在");
            }
            dbhsmDbInstance.setDatabaseExampleType("-");
        }
        dbhsmDbInstance.setCreateTime(com.zayk.util.DateUtils.getNowDate());
        i = dbhsmDbInstanceMapper.insertDbhsmDbInstance(dbhsmDbInstance);
        //创建连接池
        DbInstanceGetConnDTO  dbInstanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance,dbInstanceGetConnDTO);
        DbConnectionPoolFactory.buildDataSourcePool(dbInstanceGetConnDTO);
        Connection connection = DbConnectionPoolFactory.getInstance().getConnection(dbInstanceGetConnDTO);
        DbConnectionPoolFactory.queryPool();
        //创建加解密函数
        try {
            FunctionUtil.createEncryptDecryptFunction(connection,dbhsmDbInstance);
        }catch (Exception e) {
            e.printStackTrace();
            //关闭连接池
            DbConnectionPoolFactory.getInstance().unbind(DbConnectionPoolFactory.instanceConventKey(dbhsmDbInstance));
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
        if(DbConstants.DB_TYPE_ORACLE.equals(dbhsmDbInstance.getDatabaseType())) {
            if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(editCheckDBOracleInstanceUnique(dbhsmDbInstance))) {
                throw new ZAYKException("修改失败，数据库实例" + dbhsmDbInstance.getDatabaseIp() + ":" + dbhsmDbInstance.getDatabasePort() +  dbhsmDbInstance.getDatabaseServerName() + "已存在");
            }
        }else if(DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType())){
            //数据库实例唯一性判断
            if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(editCheckDBSqlServerUnique(dbhsmDbInstance))) {
                throw new ZAYKException("修改失败，数据库实例" + dbhsmDbInstance.getDatabaseIp() + ":" + dbhsmDbInstance.getDatabasePort() +  dbhsmDbInstance.getDatabaseServerName() + "已存在");
            }
        }
        DbhsmDbInstance instanceById = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(dbhsmDbInstance.getId());
        dbhsmDbInstance.setUpdateTime(DateUtils.getNowDate());
        int i = dbhsmDbInstanceMapper.updateDbhsmDbInstance(dbhsmDbInstance);
        //如果创建连接池需要的数据数据未做修改，不需要重新建链接，否则需要在修改时先销毁之前的池，再生成新连接池
        if(DbInstanceGetConnDTO.instanceConvertGetConnDTO(instanceById).equals(DbInstanceGetConnDTO.instanceConvertGetConnDTO(dbhsmDbInstance))){
            return i;
        }
        //删除动态数据连接池中名称为dbhsmDbInstance的连接池
        DbConnectionPoolFactory.getInstance().unbind(DbConnectionPoolFactory.instanceConventKey(dbhsmDbInstance));
        //重建连接池
        DbInstanceGetConnDTO  dbInstanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance,dbInstanceGetConnDTO);
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
    public int deleteDbhsmDbInstanceByIds(Long[] ids){
        int i = 0;
        for (Long id : ids) {
            //删除之前先销毁之前的池
            DbhsmDbInstance instanceById = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
            i = dbhsmDbInstanceMapper.deleteDbhsmDbInstanceById(id);
            //删除加解密函数
            delEncDecFunction(instanceById);
            //删除连接池
            DbConnectionPoolFactory.getInstance().unbind(DbConnectionPoolFactory.instanceConventKey(instanceById));
        }
        return i;
    }

    private void delEncDecFunction(DbhsmDbInstance instanceById) {
        String delEncFunSql = "",delDecFunSql = "";
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
                    delEncFunSql="DROP FUNCTION StringEncrypt;";
                    delDecFunSql="DROP FUNCTION StringDecrypt;";
                    break;
                case DbConstants.DB_TYPE_POSTGRESQL:
                    break;
                default:
                    log.info("Unknown database type: " + databaseType);
            }
            //执行SQL
            if(!StringUtils.isEmpty(delEncFunSql)){
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
    public int deleteDbhsmDbInstanceById(Long id)
    {
        return dbhsmDbInstanceMapper.deleteDbhsmDbInstanceById(id);
    }

    @Override
    public List<SelectOption>  getDbTablespace(Long id) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        int i =0;
        List<SelectOption>  tablespaceList = new ArrayList<>();
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if(DbConstants.DB_TYPE_ORACLE.equals(instance.getDatabaseType())) {
            if (!ObjectUtils.isEmpty(instance)) {
                //创建数据库连接
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);
                try {
                    conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                    if (Optional.ofNullable(conn).isPresent()) {
                        stmt = conn.createStatement();
                        resultSet = stmt.executeQuery("select tablespace_name from dba_data_files");
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
        }else {
            return Collections.emptyList();
        }
        return tablespaceList;
    }

    @Override
    public List<SelectOption> getDbSchema(Long id) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        int i =0;
        List<SelectOption>  list = new ArrayList<>();
        DbhsmDbInstance instance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(id);
        if(DbConstants.DB_TYPE_POSTGRESQL.equals(instance.getDatabaseType())) {
            if (!ObjectUtils.isEmpty(instance)) {
                //创建数据库连接
                DbInstanceGetConnDTO connDTO = new DbInstanceGetConnDTO();
                BeanUtils.copyProperties(instance, connDTO);
                try {
                    conn = DbConnectionPoolFactory.getInstance().getConnection(connDTO);
                    if (Optional.ofNullable(conn).isPresent()) {
                        stmt = conn.createStatement();
                        resultSet = stmt.executeQuery("SELECT schema_name FROM information_schema.schemata;");
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
        }else {
            return Collections.emptyList();
        }
        return list;
    }
}

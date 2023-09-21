package com.spms.dbhsm.dbInstance.service.impl;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.DateUtils;
import com.spms.common.constant.DbConstants;
import com.spms.common.pool.DataSource;
import com.spms.common.pool.DynamicDataSourcePoolFactory;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstancePoolKeyDTO;
import com.spms.dbhsm.dbInstance.domain.DTO.DbOracleInstancePoolKeyDTO;
import com.spms.dbhsm.dbInstance.domain.DTO.DbSQLServernstancePoolKeyDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.dbInstance.service.IDbhsmDbInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.List;

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
    public  void initDbConnectionPool()  {
        List<DbhsmDbInstance> dbhsmDbInstanceList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(null);
        if(CollectionUtils.isEmpty(dbhsmDbInstanceList)){
            log.info("数据库实例列表为空，无需初始化数据库连接池");
            return;
        }
        for(DbhsmDbInstance dbhsmDbInstance:dbhsmDbInstanceList){
            DbInstancePoolKeyDTO instanceKey = new DbInstancePoolKeyDTO();
            BeanUtils.copyProperties(dbhsmDbInstance,instanceKey);

            DbInstanceGetConnDTO instanceGetConnDTO = new DbInstanceGetConnDTO();
            BeanUtils.copyProperties(dbhsmDbInstance,instanceGetConnDTO);

            DataSource dataSource = DataSource.getDataSource(instanceGetConnDTO);
            try {
                DynamicDataSourcePoolFactory.buildDataSourcePool(dataSource,instanceKey);
            } catch (ZAYKException e) {
                e.printStackTrace();
                log.info("初始化数据库连接池失败:{}",e.getMessage());
            }
            log.info("初始化数据库连接池成功");
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
    public int insertDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException {
        int i = 0;

        //数据类型为oracle
        if(DbConstants.DB_TYPE_ORACLE.equals(dbhsmDbInstance.getDatabaseType())) {
            //数据库实例唯一性判断
            if (DbConstants.DATABASE_INSTANCE_NOT_UNIQUE.equals(checkDBOracleInstanceUnique(dbhsmDbInstance))) {
                throw new ZAYKException("数据库实例已存在"+dbhsmDbInstance);
            }
            i = insertDBOracleInstance(dbhsmDbInstance);
        }else if(DbConstants.DB_TYPE_SQLSERVER.equals(dbhsmDbInstance.getDatabaseType())){
            //数据库实例唯一性判断
            if (DbConstants.DATABASE_INSTANCE_NOT_UNIQUE.equals(checkDBSqlServerUnique(dbhsmDbInstance))) {
                throw new ZAYKException("数据库实例已存在"+dbhsmDbInstance);
            }
            i = insertDBSQLServerInstance(dbhsmDbInstance);
        }

        return i;
    }
    public int insertDBOracleInstance(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException {
        //创建连接池
        DbInstanceGetConnDTO  dbInstanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance,dbInstanceGetConnDTO);
        String result = DynamicDataSourcePoolFactory.buildDataSourcePool(DataSource.getDataSource(dbInstanceGetConnDTO), DbOracleInstancePoolKeyDTO.getInstancePoolKeyDTO(dbhsmDbInstance));
        dbhsmDbInstance.setCreateTime(com.zayk.util.DateUtils.getNowDate());
        return dbhsmDbInstanceMapper.insertDbhsmDbInstance(dbhsmDbInstance);
    }

    public int insertDBSQLServerInstance(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException {
        //创建连接池
        DbInstanceGetConnDTO  dbInstanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(dbhsmDbInstance,dbInstanceGetConnDTO);
        String result = DynamicDataSourcePoolFactory.buildDataSourcePool(DataSource.getDataSource(dbInstanceGetConnDTO), DbSQLServernstancePoolKeyDTO.getInstancePoolKeyDTO(dbhsmDbInstance));
        dbhsmDbInstance.setCreateTime(com.zayk.util.DateUtils.getNowDate());
        dbhsmDbInstance.setDatabaseExampleType("-");
        return dbhsmDbInstanceMapper.insertDbhsmDbInstance(dbhsmDbInstance);
    }

    public String checkDBOracleInstanceUnique(DbhsmDbInstance dbhsmDbInstance) {
        DbhsmDbInstance instance = new DbhsmDbInstance();
        instance.setDatabaseIp(dbhsmDbInstance.getDatabaseIp());
        instance.setDatabasePort(dbhsmDbInstance.getDatabasePort());
        instance.setDatabaseServerName(dbhsmDbInstance.getDatabaseServerName());
        instance.setDatabaseExampleType(dbhsmDbInstance.getDatabaseExampleType());
        List<DbhsmDbInstance> infoList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(instance);
        if (CollectionUtils.isEmpty(infoList)) {
            return DbConstants.DATABASE_INSTANCE_UNIQUE;
        }
        return DbConstants.DATABASE_INSTANCE_NOT_UNIQUE;
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
                return DbConstants.DATABASE_INSTANCE_NOT_UNIQUE;
            }
        }
        return DbConstants.DATABASE_INSTANCE_UNIQUE;
    }


    public String checkDBSqlServerUnique(DbhsmDbInstance dbhsmDbInstance) {
        DbhsmDbInstance instance = new DbhsmDbInstance();
        instance.setDatabaseIp(dbhsmDbInstance.getDatabaseIp());
        instance.setDatabasePort(dbhsmDbInstance.getDatabasePort());
        instance.setDatabaseServerName(dbhsmDbInstance.getDatabaseServerName());
        List<DbhsmDbInstance> sqlServerList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(instance);
        if (sqlServerList.size() > 0) {
            return DbConstants.DATABASE_INSTANCE_NOT_UNIQUE;
        }
        return DbConstants.DATABASE_INSTANCE_UNIQUE;
    }

    public String editCheckDBSqlServerUnique(DbhsmDbInstance dbhsmDbInstance) {
        DbhsmDbInstance instance = new DbhsmDbInstance();
        instance.setDatabaseIp(dbhsmDbInstance.getDatabaseIp());
        instance.setDatabasePort(dbhsmDbInstance.getDatabasePort());
        List<DbhsmDbInstance> sqlServerList = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(instance);
        if (sqlServerList.size() > 0) {
            if (!dbhsmDbInstance.getId().equals(sqlServerList.get(0).getId())) {
                return DbConstants.DATABASE_INSTANCE_NOT_UNIQUE;
            }
        }
        return DbConstants.DATABASE_INSTANCE_UNIQUE;
    }
    /**
     * 修改数据库实例
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 结果
     */
    @Override
    public int updateDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance)
    {
        dbhsmDbInstance.setUpdateTime(DateUtils.getNowDate());
        return dbhsmDbInstanceMapper.updateDbhsmDbInstance(dbhsmDbInstance);
    }

    /**
     * 批量删除数据库实例
     *
     * @param ids 需要删除的数据库实例主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmDbInstanceByIds(Long[] ids)
    {
        return dbhsmDbInstanceMapper.deleteDbhsmDbInstanceByIds(ids);
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
}

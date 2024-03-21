package com.spms.dbhsm.warningConfig.service.impl;

import java.util.ArrayList;
import java.util.List;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.warningInfo.mapper.DbhsmWarningInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.spms.dbhsm.warningConfig.mapper.DbhsmWarningConfigMapper;
import com.spms.dbhsm.warningConfig.domain.DbhsmWarningConfig;
import com.spms.dbhsm.warningConfig.service.IDbhsmWarningConfigService;

/**
 * warningConfigService业务层处理
 * 
 * @author diq
 * @date 2024-03-21
 */
@Service
public class DbhsmWarningConfigServiceImpl implements IDbhsmWarningConfigService 
{
    @Autowired
    private DbhsmWarningConfigMapper dbhsmWarningConfigMapper;

    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

    /**
     * 查询warningConfig
     * 
     * @param id warningConfig主键
     * @return warningConfig
     */
    @Override
    public DbhsmWarningConfig selectDbhsmWarningConfigById(Long id)
    {
        return dbhsmWarningConfigMapper.selectDbhsmWarningConfigById(id);
    }

    /**
     * 查询warningConfig列表
     * 
     * @param dbhsmWarningConfig warningConfig
     * @return warningConfig
     */
    @Override
    public List<DbhsmWarningConfig> selectDbhsmWarningConfigList(DbhsmWarningConfig dbhsmWarningConfig)
    {
        return dbhsmWarningConfigMapper.selectDbhsmWarningConfigList(dbhsmWarningConfig);
    }

    /**
     * 新增warningConfig
     * 
     * @param dbhsmWarningConfig warningConfig
     * @return 结果
     */
    @Override
    public int insertDbhsmWarningConfig(DbhsmWarningConfig dbhsmWarningConfig)
    {
        dbhsmWarningConfig.setCreateTime(DateUtils.getNowDate());
        return dbhsmWarningConfigMapper.insertDbhsmWarningConfig(dbhsmWarningConfig);
    }

    /**
     * 修改warningConfig
     * 
     * @param dbhsmWarningConfig warningConfig
     * @return 结果
     */
    @Override
    public int updateDbhsmWarningConfig(DbhsmWarningConfig dbhsmWarningConfig)
    {
        dbhsmWarningConfig.setUpdateTime(DateUtils.getNowDate());
        return dbhsmWarningConfigMapper.updateDbhsmWarningConfig(dbhsmWarningConfig);
    }

    /**
     * 批量删除warningConfig
     * 
     * @param ids 需要删除的warningConfig主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmWarningConfigByIds(Long[] ids)
    {
        return dbhsmWarningConfigMapper.deleteDbhsmWarningConfigByIds(ids);
    }

    /**
     * 删除warningConfig信息
     * 
     * @param id warningConfig主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmWarningConfigById(Long id)
    {
        return dbhsmWarningConfigMapper.deleteDbhsmWarningConfigById(id);
    }

    @Override
    public AjaxResult2<List<String>> queryDataBaseConnection() {
        List<String> result = new ArrayList<>();
        List<DbhsmDbInstance> dbhsmDbInstances = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(new DbhsmDbInstance());
        for (DbhsmDbInstance dbhsmDbInstance : dbhsmDbInstances) {
            String instance = getInstance(dbhsmDbInstance);
            result.add(instance);
        }
        return AjaxResult2.success(result);
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
        }else if(DbConstants.DB_TYPE_DM.equals(instance.getDatabaseType())) {
            databaseType = DbConstants.DB_TYPE_DM_DESC;
        }
        return databaseType + ":" + instance.getDatabaseIp() + ":" + instance.getDatabasePort() + instance.getDatabaseExampleType() + instance.getDatabaseServerName();
    }
}

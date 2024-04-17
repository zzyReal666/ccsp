package com.spms.dbhsm.warningFile.service.impl;

import java.util.List;
import com.ccsp.common.core.utils.DateUtils;
import com.spms.dbhsm.warningFile.domain.DbhsmIntegrityFileConfig;
import com.spms.dbhsm.warningFile.mapper.DbhsmIntegrityFileConfigMapper;
import com.spms.dbhsm.warningFile.service.IDbhsmIntegrityFileConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 文件完整性校验Service业务层处理
 * 
 * @author diq
 * @date 2024-04-17
 */
@Service
public class DbhsmIntegrityFileConfigServiceImpl implements IDbhsmIntegrityFileConfigService
{
    @Autowired
    private DbhsmIntegrityFileConfigMapper dbhsmIntegrityFileConfigMapper;

    /**
     * 查询文件完整性校验
     * 
     * @param id 文件完整性校验主键
     * @return 文件完整性校验
     */
    @Override
    public DbhsmIntegrityFileConfig selectDbhsmIntegrityFileConfigById(Long id)
    {
        return dbhsmIntegrityFileConfigMapper.selectDbhsmIntegrityFileConfigById(id);
    }

    /**
     * 查询文件完整性校验列表
     * 
     * @param dbhsmIntegrityFileConfig 文件完整性校验
     * @return 文件完整性校验
     */
    @Override
    public List<DbhsmIntegrityFileConfig> selectDbhsmIntegrityFileConfigList(DbhsmIntegrityFileConfig dbhsmIntegrityFileConfig)
    {
        return dbhsmIntegrityFileConfigMapper.selectDbhsmIntegrityFileConfigList(dbhsmIntegrityFileConfig);
    }

    /**
     * 新增文件完整性校验
     * 
     * @param dbhsmIntegrityFileConfig 文件完整性校验
     * @return 结果
     */
    @Override
    public int insertDbhsmIntegrityFileConfig(DbhsmIntegrityFileConfig dbhsmIntegrityFileConfig)
    {
        dbhsmIntegrityFileConfig.setCreateTime(DateUtils.getNowDate());
        return dbhsmIntegrityFileConfigMapper.insertDbhsmIntegrityFileConfig(dbhsmIntegrityFileConfig);
    }

    /**
     * 修改文件完整性校验
     * 
     * @param dbhsmIntegrityFileConfig 文件完整性校验
     * @return 结果
     */
    @Override
    public int updateDbhsmIntegrityFileConfig(DbhsmIntegrityFileConfig dbhsmIntegrityFileConfig)
    {
        dbhsmIntegrityFileConfig.setUpdateTime(DateUtils.getNowDate());
        return dbhsmIntegrityFileConfigMapper.updateDbhsmIntegrityFileConfig(dbhsmIntegrityFileConfig);
    }

    /**
     * 批量删除文件完整性校验
     * 
     * @param ids 需要删除的文件完整性校验主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmIntegrityFileConfigByIds(Long[] ids)
    {
        return dbhsmIntegrityFileConfigMapper.deleteDbhsmIntegrityFileConfigByIds(ids);
    }

    /**
     * 删除文件完整性校验信息
     * 
     * @param id 文件完整性校验主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmIntegrityFileConfigById(Long id)
    {
        return dbhsmIntegrityFileConfigMapper.deleteDbhsmIntegrityFileConfigById(id);
    }
}

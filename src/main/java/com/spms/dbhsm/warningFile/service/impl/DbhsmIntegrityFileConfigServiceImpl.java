package com.spms.dbhsm.warningFile.service.impl;

import com.ccsp.common.core.constant.Constants;
import com.ccsp.common.core.domain.R;
import com.ccsp.common.core.exception.ServiceException;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.system.api.hsmSvsTsaApi.SpmsDevBaseDataService;
import com.ccsp.system.api.hsmSvsTsaApi.domain.DevBaseData;
import com.spms.common.task.DbhsmWarningJobLoad;
import com.spms.dbhsm.warningFile.domain.DbhsmIntegrityFileConfig;
import com.spms.dbhsm.warningFile.mapper.DbhsmIntegrityFileConfigMapper;
import com.spms.dbhsm.warningFile.service.IDbhsmIntegrityFileConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

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

    @Autowired
    private SpmsDevBaseDataService devBaseDataService;

    @Autowired
    private DbhsmWarningJobLoad dbhsmWarningJobLoad;

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
        //判断文件是否存在
        R<DevBaseData> devBaseDataR = devBaseDataService.selectBaseDataByKey(Constants.integrityFilePath);
        String integrityFilePath = "/opt/integrityFile/";
        if (devBaseDataR !=null && devBaseDataR.getData() != null && StringUtils.isNotEmpty(devBaseDataR.getData().getDataValue())){
            integrityFilePath = devBaseDataR.getData().getDataValue();
        }

        File file = new File(integrityFilePath + dbhsmIntegrityFileConfig.getFilePath());

        if (!file.exists()){
            throw new ServiceException("文件不存在,不允许进行配置");
        }

        dbhsmIntegrityFileConfig.setCreateTime(DateUtils.getNowDate());
        int i = dbhsmIntegrityFileConfigMapper.insertDbhsmIntegrityFileConfig(dbhsmIntegrityFileConfig);
        dbhsmWarningJobLoad.fileIntegrityJob();
        return i;
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
        int i = dbhsmIntegrityFileConfigMapper.updateDbhsmIntegrityFileConfig(dbhsmIntegrityFileConfig);
        dbhsmWarningJobLoad.fileIntegrityJob();
        return i;
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
        int i = dbhsmIntegrityFileConfigMapper.deleteDbhsmIntegrityFileConfigByIds(ids);
        dbhsmWarningJobLoad.fileIntegrityJob();
        return i;
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
        int i = dbhsmIntegrityFileConfigMapper.deleteDbhsmIntegrityFileConfigById(id);
        dbhsmWarningJobLoad.fileIntegrityJob();
        return i;
    }
}

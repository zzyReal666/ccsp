package com.spms.dbhsm.warningConfig.service;

import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.spms.dbhsm.warningConfig.domain.DbhsmWarningConfig;
import com.spms.dbhsm.warningConfig.vo.DataBaseConnectionResponse;
import com.spms.dbhsm.warningConfig.vo.DbhsmWarningConfigListResponse;

import java.util.List;

/**
 * warningConfigService接口
 * 
 * @author diq
 * @date 2024-03-21
 */
public interface IDbhsmWarningConfigService 
{
    /**
     * 查询warningConfig
     * 
     * @param id warningConfig主键
     * @return warningConfig
     */
    public DbhsmWarningConfig selectDbhsmWarningConfigById(Long id);

    /**
     * 查询warningConfig列表
     * 
     * @param dbhsmWarningConfig warningConfig
     * @return warningConfig集合
     */
    public List<DbhsmWarningConfigListResponse> selectDbhsmWarningConfigList(DbhsmWarningConfig dbhsmWarningConfig);

    /**
     * 新增warningConfig
     * 
     * @param dbhsmWarningConfig warningConfig
     * @return 结果
     */
    public AjaxResult insertDbhsmWarningConfig(DbhsmWarningConfig dbhsmWarningConfig);

    /**
     * 修改warningConfig
     * 
     * @param dbhsmWarningConfig warningConfig
     * @return 结果
     */
    public int updateDbhsmWarningConfig(DbhsmWarningConfig dbhsmWarningConfig);

    /**
     * 批量删除warningConfig
     * 
     * @param ids 需要删除的warningConfig主键集合
     * @return 结果
     */
    public int deleteDbhsmWarningConfigByIds(Long[] ids);

    /**
     * 删除warningConfig信息
     * 
     * @param id warningConfig主键
     * @return 结果
     */
    public int deleteDbhsmWarningConfigById(Long id);

    AjaxResult2<List<DataBaseConnectionResponse>> queryDataBaseConnection();
}

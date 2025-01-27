package com.spms.dbhsm.warningConfig.mapper;

import com.spms.dbhsm.warningConfig.domain.DbhsmWarningConfig;
import com.spms.dbhsm.warningConfig.vo.DbhsmWarningConfigListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * warningConfigMapper接口
 * 
 * @author diq
 * @date 2024-03-21
 */
@Mapper
public interface DbhsmWarningConfigMapper 
{
    /**
     * 查询warningConfig
     * 
     * @param id warningConfig主键
     * @return warningConfig
     */
    public DbhsmWarningConfig selectDbhsmWarningConfigById(Long id);

    /**
     * 查询warningConfig
     *1
     * @param verificationValue 校验值
     * @return warningConfig
     */
    DbhsmWarningConfig selectDbhsmWarningConfigByVerificationValue(@Param("verificationValue") String verificationValue);

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
    public int insertDbhsmWarningConfig(DbhsmWarningConfig dbhsmWarningConfig);

    /**
     * 修改warningConfig
     * 
     * @param dbhsmWarningConfig warningConfig
     * @return 结果
     */
    public int updateDbhsmWarningConfig(DbhsmWarningConfig dbhsmWarningConfig);

    /**
     * 删除warningConfig
     * 
     * @param id warningConfig主键
     * @return 结果
     */
    public int deleteDbhsmWarningConfigById(Long id);

    /**
     * 批量删除warningConfig
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmWarningConfigByIds(Long[] ids);
}

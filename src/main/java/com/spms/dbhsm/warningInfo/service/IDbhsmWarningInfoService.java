package com.spms.dbhsm.warningInfo.service;

import java.util.List;
import com.spms.dbhsm.warningInfo.domain.DbhsmWarningInfo;

/**
 * warningInfoService接口
 * 
 * @author diq
 * @date 2024-03-21
 */
public interface IDbhsmWarningInfoService 
{
    /**
     * 查询warningInfo
     * 
     * @param id warningInfo主键
     * @return warningInfo
     */
    public DbhsmWarningInfo selectDbhsmWarningInfoById(Long id);

    /**
     * 查询warningInfo列表
     * 
     * @param dbhsmWarningInfo warningInfo
     * @return warningInfo集合
     */
    public List<DbhsmWarningInfo> selectDbhsmWarningInfoList(DbhsmWarningInfo dbhsmWarningInfo);

    /**
     * 新增warningInfo
     * 
     * @param dbhsmWarningInfo warningInfo
     * @return 结果
     */
    public int insertDbhsmWarningInfo(DbhsmWarningInfo dbhsmWarningInfo);

    /**
     * 修改warningInfo
     * 
     * @param dbhsmWarningInfo warningInfo
     * @return 结果
     */
    public int updateDbhsmWarningInfo(DbhsmWarningInfo dbhsmWarningInfo);

    /**
     * 批量删除warningInfo
     * 
     * @param ids 需要删除的warningInfo主键集合
     * @return 结果
     */
    public int deleteDbhsmWarningInfoByIds(Long[] ids);

    /**
     * 删除warningInfo信息
     * 
     * @param id warningInfo主键
     * @return 结果
     */
    public int deleteDbhsmWarningInfoById(Long id);
}

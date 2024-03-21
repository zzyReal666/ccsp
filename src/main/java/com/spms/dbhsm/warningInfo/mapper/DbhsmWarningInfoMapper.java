package com.spms.dbhsm.warningInfo.mapper;

import java.util.List;
import com.spms.dbhsm.warningInfo.domain.DbhsmWarningInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * warningInfoMapper接口
 * 
 * @author diq
 * @date 2024-03-21
 */
@Mapper
public interface DbhsmWarningInfoMapper 
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
     * 删除warningInfo
     * 
     * @param id warningInfo主键
     * @return 结果
     */
    public int deleteDbhsmWarningInfoById(Long id);

    /**
     * 批量删除warningInfo
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmWarningInfoByIds(Long[] ids);
}

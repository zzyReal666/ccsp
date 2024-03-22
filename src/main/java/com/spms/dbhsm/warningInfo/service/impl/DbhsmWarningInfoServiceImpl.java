package com.spms.dbhsm.warningInfo.service.impl;

import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.bean.BeanUtils;
import com.spms.common.CommandUtil;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.warningInfo.domain.DbhsmWarningInfo;
import com.spms.dbhsm.warningInfo.mapper.DbhsmWarningInfoMapper;
import com.spms.dbhsm.warningInfo.service.IDbhsmWarningInfoService;
import com.spms.dbhsm.warningInfo.vo.DbhsmWarningInfoListRequest;
import com.spms.dbhsm.warningInfo.vo.DbhsmWarningInfoListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * warningInfoService业务层处理
 * 
 * @author diq
 * @date 2024-03-21
 */
@Service
public class DbhsmWarningInfoServiceImpl implements IDbhsmWarningInfoService 
{
    @Autowired
    private DbhsmWarningInfoMapper dbhsmWarningInfoMapper;

    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

    /**
     * 查询warningInfo
     * 
     * @param id warningInfo主键
     * @return warningInfo
     */
    @Override
    public DbhsmWarningInfo selectDbhsmWarningInfoById(Long id)
    {
        return dbhsmWarningInfoMapper.selectDbhsmWarningInfoById(id);
    }

    /**
     * 查询warningInfo列表
     * 
     * @param dbhsmWarningInfo warningInfo
     * @return warningInfo
     */
    @Override
    public List<DbhsmWarningInfoListResponse> selectDbhsmWarningInfoList(DbhsmWarningInfoListRequest dbhsmWarningInfo)
    {
        List<DbhsmWarningInfoListResponse> dbhsmWarningConfigs = dbhsmWarningInfoMapper.queryDbhsmWarningInfoAllList(dbhsmWarningInfo);

        //数据转换
        dbhsmWarningConfigs = dbhsmWarningConfigs.stream().peek(warningInfo -> {
            //组装实例信息
            DbhsmDbInstance dbInstance = new DbhsmDbInstance();
            BeanUtils.copyProperties(warningInfo,dbInstance);
            String instance = CommandUtil.getInstance(dbInstance);
            warningInfo.setConnectionInfo(instance);
        }).collect(Collectors.toList());
        return dbhsmWarningConfigs;
    }

    /**
     * 新增warningInfo
     * 
     * @param dbhsmWarningInfo warningInfo
     * @return 结果
     */
    @Override
    public int insertDbhsmWarningInfo(DbhsmWarningInfo dbhsmWarningInfo)
    {
        dbhsmWarningInfo.setCreateTime(DateUtils.getNowDate());
        return dbhsmWarningInfoMapper.insertDbhsmWarningInfo(dbhsmWarningInfo);
    }

    /**
     * 修改warningInfo
     * 
     * @param dbhsmWarningInfo warningInfo
     * @return 结果
     */
    @Override
    public int updateDbhsmWarningInfo(DbhsmWarningInfo dbhsmWarningInfo)
    {
        dbhsmWarningInfo.setUpdateTime(DateUtils.getNowDate());
        return dbhsmWarningInfoMapper.updateDbhsmWarningInfo(dbhsmWarningInfo);
    }

    /**
     * 批量删除warningInfo
     * 
     * @param ids 需要删除的warningInfo主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmWarningInfoByIds(Long[] ids)
    {
        return dbhsmWarningInfoMapper.deleteDbhsmWarningInfoByIds(ids);
    }

    /**
     * 删除warningInfo信息
     * 
     * @param id warningInfo主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmWarningInfoById(Long id)
    {
        return dbhsmWarningInfoMapper.deleteDbhsmWarningInfoById(id);
    }
}

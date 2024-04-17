package com.spms.dbhsm.warningConfig.service.impl;

import cn.hutool.crypto.digest.SM3;
import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.SM3Util;
import com.ccsp.common.core.utils.bean.BeanUtils;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.spms.common.CommandUtil;
import com.spms.common.task.DbhsmWarningJobLoad;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.mapper.DbhsmDbInstanceMapper;
import com.spms.dbhsm.warningConfig.domain.DbhsmWarningConfig;
import com.spms.dbhsm.warningConfig.mapper.DbhsmWarningConfigMapper;
import com.spms.dbhsm.warningConfig.service.IDbhsmWarningConfigService;
import com.spms.dbhsm.warningConfig.vo.DataBaseConnectionResponse;
import com.spms.dbhsm.warningConfig.vo.DbhsmWarningConfigListResponse;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * warningConfigService业务层处理
 *
 * @author diq
 * @date 2024-03-21
 */
@Service
public class DbhsmWarningConfigServiceImpl implements IDbhsmWarningConfigService {
    @Autowired
    private DbhsmWarningConfigMapper dbhsmWarningConfigMapper;

    @Autowired
    private DbhsmDbInstanceMapper dbhsmDbInstanceMapper;

    @Autowired
    private DbhsmWarningJobLoad dbhsmWarningJobLoad;

    /**
     * 查询warningConfig
     *
     * @param id warningConfig主键
     * @return warningConfig
     */
    @Override
    public DbhsmWarningConfig selectDbhsmWarningConfigById(Long id) {
        return dbhsmWarningConfigMapper.selectDbhsmWarningConfigById(id);
    }

    /**
     * 查询warningConfig列表
     *
     * @param dbhsmWarningConfig warningConfig
     * @return warningConfig
     */
    @Override
    public List<DbhsmWarningConfigListResponse> selectDbhsmWarningConfigList(DbhsmWarningConfig dbhsmWarningConfig) {
        List<DbhsmWarningConfigListResponse> dbhsmWarningConfigs = dbhsmWarningConfigMapper.selectDbhsmWarningConfigList(dbhsmWarningConfig);

        for (DbhsmWarningConfigListResponse dbhsmWarningConfigListResponse : dbhsmWarningConfigs) {
            if (StringUtils.isNotBlank(dbhsmWarningConfigListResponse.getDatabaseConnectionInfo())) {
                DbhsmDbInstance dbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(Long.valueOf(dbhsmWarningConfigListResponse.getDatabaseConnectionInfo()));
                if (null != dbInstance) {
                    BeanUtils.copyProperties(dbhsmWarningConfig, dbInstance);
                    String instance = CommandUtil.getInstance(dbInstance);
                    dbhsmWarningConfigListResponse.setConnectionInfo(instance);
                }
            }
        }

        return dbhsmWarningConfigs;
    }
    /**
     * 新增warningConfig
     *
     * @param dbhsmWarningConfig warningConfig
     * @return 结果
     */
    @Override
    public AjaxResult insertDbhsmWarningConfig(DbhsmWarningConfig dbhsmWarningConfig) {
        dbhsmWarningConfig.setCreateTime(DateUtils.getNowDate());

        //获取数据库连接
        String instance = "";
        DbhsmDbInstance dbInstance = dbhsmDbInstanceMapper.selectDbhsmDbInstanceById(Long.valueOf(dbhsmWarningConfig.getDatabaseConnectionInfo()));
        if (null != dbInstance) {
            BeanUtils.copyProperties(dbhsmWarningConfig, dbInstance);
            instance = CommandUtil.getInstance(dbInstance);
        }
        //数据库连接 + 表 + 校验列 组合进行唯一性校验
        String check = instance+dbhsmWarningConfig.getDatabaseTableInfo()+dbhsmWarningConfig.getTableFields();
        String hexString = Hex.toHexString(SM3Util.hash(check.getBytes()));

        DbhsmWarningConfig verDb = dbhsmWarningConfigMapper.selectDbhsmWarningConfigByVerificationValue(hexString);
        if (null != verDb) {
            return AjaxResult.error("定时任务配置重复，创建失败");
        }

        dbhsmWarningConfig.setVerificationValue(hexString);
        int count = dbhsmWarningConfigMapper.insertDbhsmWarningConfig(dbhsmWarningConfig);

        if (0 == dbhsmWarningConfig.getEnableTiming()) {
            dbhsmWarningJobLoad.initLoading();
        }

        return AjaxResult.success();
    }

    /**
     * 修改warningConfig
     *
     * @param dbhsmWarningConfig warningConfig
     * @return 结果
     */
    @Override
    public int updateDbhsmWarningConfig(DbhsmWarningConfig dbhsmWarningConfig) {
        dbhsmWarningConfig.setUpdateTime(DateUtils.getNowDate());
        int i = dbhsmWarningConfigMapper.updateDbhsmWarningConfig(dbhsmWarningConfig);
        dbhsmWarningJobLoad.initLoading();
        return i;
    }

    /**
     * 批量删除warningConfig
     *
     * @param ids 需要删除的warningConfig主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmWarningConfigByIds(Long[] ids) {
        int i = dbhsmWarningConfigMapper.deleteDbhsmWarningConfigByIds(ids);
        dbhsmWarningJobLoad.initLoading();
        return i;
    }

    /**
     * 删除warningConfig信息
     *
     * @param id warningConfig主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmWarningConfigById(Long id) {
        return dbhsmWarningConfigMapper.deleteDbhsmWarningConfigById(id);
    }

    @Override
    public AjaxResult2<List<DataBaseConnectionResponse>> queryDataBaseConnection() {
        List<DataBaseConnectionResponse> result = new ArrayList<>();
        List<DbhsmDbInstance> dbhsmDbInstances = dbhsmDbInstanceMapper.selectDbhsmDbInstanceList(new DbhsmDbInstance());
        for (DbhsmDbInstance dbhsmDbInstance : dbhsmDbInstances) {
            DataBaseConnectionResponse response = new DataBaseConnectionResponse();
            String instance = CommandUtil.getInstance(dbhsmDbInstance);
            response.setConnectionInfo(instance);
            response.setId(String.valueOf(dbhsmDbInstance.getId()));
            result.add(response);
        }
        return AjaxResult2.success(result);
    }

}

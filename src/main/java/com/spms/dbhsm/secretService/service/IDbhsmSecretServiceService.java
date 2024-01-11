package com.spms.dbhsm.secretService.service;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.spms.dbhsm.secretService.domain.DbhsmSecretService;

import java.io.IOException;
import java.util.List;

/**
 * 密码服务Service接口
 *
 * @author diq
 * @date 2023-09-25
 */
public interface IDbhsmSecretServiceService
{
    /**
     * 查询密码服务
     *
     * @param id 密码服务主键
     * @return 密码服务
     */
    public DbhsmSecretService selectDbhsmSecretServiceById(Long id);

    /**
     * 查询密码服务列表
     *
     * @param dbhsmSecretService 密码服务
     * @return 密码服务集合
     */
    public List<DbhsmSecretService> selectDbhsmSecretServiceList(DbhsmSecretService dbhsmSecretService);

    /**
     * 新增密码服务
     *
     * @param dbhsmSecretService 密码服务
     * @return 结果
     */
    public int insertDbhsmSecretService(DbhsmSecretService dbhsmSecretService) throws Exception;

    /**
     * 修改密码服务
     *
     * @param dbhsmSecretService 密码服务
     * @return 结果
     */
    public int updateDbhsmSecretService(DbhsmSecretService dbhsmSecretService) throws IOException, ZAYKException;

    /**
     * 批量删除密码服务
     *
     * @param ids 需要删除的密码服务主键集合
     * @return 结果
     */
    public AjaxResult deleteDbhsmSecretServiceByIds(Long[] ids);

    /**
     * 删除密码服务信息
     *
     * @param id 密码服务主键
     * @return 结果
     */
    public int deleteDbhsmSecretServiceById(Long id);

    List<DbhsmSecretService> listSecretServiceForDropDown(DbhsmSecretService dbhsmSecretService);
}

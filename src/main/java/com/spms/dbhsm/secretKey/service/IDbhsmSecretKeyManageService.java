package com.spms.dbhsm.secretKey.service;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.dbhsm.secretKey.domain.DbhsmSecretKeyManage;

import java.util.List;

/**
 * 数据库密钥Service接口
 *
 * @author ccsp
 * @date 2023-09-22
 */
public interface IDbhsmSecretKeyManageService
{
    /**
     * 查询数据库密钥
     *
     * @param id 数据库密钥主键
     * @return 数据库密钥
     */
    public DbhsmSecretKeyManage selectDbhsmSecretKeyManageById(Long id);

    /**
     * 查询数据库密钥列表
     *
     * @param dbhsmSecretKeyManage 数据库密钥
     * @return 数据库密钥集合
     */
    public List<DbhsmSecretKeyManage> selectDbhsmSecretKeyManageList(DbhsmSecretKeyManage dbhsmSecretKeyManage);

    /**
     * 新增数据库密钥
     *
     * @param dbhsmSecretKeyManage 数据库密钥
     * @return 结果
     */
    public int insertDbhsmSecretKeyManage(DbhsmSecretKeyManage dbhsmSecretKeyManage) throws Exception;

    /**
     * 修改数据库密钥
     *
     * @param dbhsmSecretKeyManage 数据库密钥
     * @return 结果
     */
    public int updateDbhsmSecretKeyManage(DbhsmSecretKeyManage dbhsmSecretKeyManage) throws Exception;

    /**
     * 批量删除数据库密钥
     *
     * @param ids 需要删除的数据库密钥主键集合
     * @return 结果
     */
    public int deleteDbhsmSecretKeyManageByIds(Long[] ids) throws ZAYKException;

    /**
     * 删除数据库密钥信息
     *
     * @param id 数据库密钥主键
     * @return 结果
     */
    public int deleteDbhsmSecretKeyManageById(Long id);

    /**
     * 校验密钥名称是否唯一
     * @param dbhsmSecretKeyManage
     * @return
     */
    String checkSecretKeyUnique(DbhsmSecretKeyManage dbhsmSecretKeyManage);
    /**
     * 校验密钥索引是否唯一
     * @param dbhsmSecretKeyManage
     * @return
     */
    String checkSecretKeyIndexUnique(DbhsmSecretKeyManage dbhsmSecretKeyManage);
}

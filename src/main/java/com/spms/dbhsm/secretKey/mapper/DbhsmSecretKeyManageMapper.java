package com.spms.dbhsm.secretKey.mapper;

import com.spms.dbhsm.secretKey.domain.DbhsmSecretKeyManage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据库密钥Mapper接口
 * 
 * @author ccsp
 * @date 2023-09-22
 */
@Mapper
public interface DbhsmSecretKeyManageMapper 
{
    /**
     * 查询数据库密钥
     * 
     * @param id 数据库密钥主键
     * @return 数据库密钥
     */
    public DbhsmSecretKeyManage selectDbhsmSecretKeyManageById(Long id);

    /*
     * @description 根据密钥ID查询密钥
     * @author wzh [zhwang2012@yeah.net]
     * @date 10:47 2024/7/2
     * @param secretKeyId  密钥ID
     * @return DbhsmSecretKeyManage
     */
    public DbhsmSecretKeyManage selectDbhsmSecretKeyId(@Param("secretKeyId") String secretKeyId);

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
    public int insertDbhsmSecretKeyManage(DbhsmSecretKeyManage dbhsmSecretKeyManage);

    /**
     * 修改数据库密钥
     * 
     * @param dbhsmSecretKeyManage 数据库密钥
     * @return 结果
     */
    public int updateDbhsmSecretKeyManage(DbhsmSecretKeyManage dbhsmSecretKeyManage);

    /**
     * 删除数据库密钥
     * 
     * @param id 数据库密钥主键
     * @return 结果
     */
    public int deleteDbhsmSecretKeyManageById(Long id);

    /**
     * 批量删除数据库密钥
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmSecretKeyManageByIds(Long[] ids);
    /**
     * 校验密钥名称是否唯一
     * @param secretKeyName
     * @return
     */
    DbhsmSecretKeyManage checkSecretKeyUnique(String secretKeyName);
}

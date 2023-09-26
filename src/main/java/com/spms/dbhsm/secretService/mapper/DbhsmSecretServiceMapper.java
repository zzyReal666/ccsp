package com.spms.dbhsm.secretService.mapper;

import com.spms.dbhsm.secretService.domain.DbhsmSecretService;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 密码服务Mapper接口
 * 
 * @author diq
 * @date 2023-09-25
 */
@Mapper
public interface DbhsmSecretServiceMapper 
{
    /**
     * 查询密码服务
     * 
     * @param id 密码服务主键
     * @return 密码服务
     */
    public DbhsmSecretService selectDbhsmSecretServiceById(Long id);
    /**
     * 查询密码服务
     *
     * @param secretService 密码服务名称
     * @return 密码服务
     */
    public DbhsmSecretService selectDbhsmSecretServiceBySecretService(String secretService);

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
    public int insertDbhsmSecretService(DbhsmSecretService dbhsmSecretService);

    /**
     * 修改密码服务
     * 
     * @param dbhsmSecretService 密码服务
     * @return 结果
     */
    public int updateDbhsmSecretService(DbhsmSecretService dbhsmSecretService);

    /**
     * 删除密码服务
     * 
     * @param id 密码服务主键
     * @return 结果
     */
    public int deleteDbhsmSecretServiceById(Long id);

    /**
     * 批量删除密码服务
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmSecretServiceByIds(Long[] ids);
}

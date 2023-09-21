package com.spms.dbInstance.service;

import com.ccsp.common.core.exception.ZAYKException;
import com.spms.dbInstance.domain.DbhsmDbInstance;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 数据库实例Service接口
 *
 * @author spms
 * @date 2023-09-19
 */
@Mapper
public interface IDbhsmDbInstanceService
{
    /**
     * 查询数据库实例
     *
     * @param id 数据库实例主键
     * @return 数据库实例
     */
    public DbhsmDbInstance selectDbhsmDbInstanceById(Long id);

    /**
     * 查询数据库实例列表
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 数据库实例集合
     */
    public List<DbhsmDbInstance> selectDbhsmDbInstanceList(DbhsmDbInstance dbhsmDbInstance);

    /**
     * 新增数据库实例
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 结果
     */
    public int insertDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance) throws ZAYKException;

    /**
     * 修改数据库实例
     *
     * @param dbhsmDbInstance 数据库实例
     * @return 结果
     */
    public int updateDbhsmDbInstance(DbhsmDbInstance dbhsmDbInstance);

    /**
     * 批量删除数据库实例
     *
     * @param ids 需要删除的数据库实例主键集合
     * @return 结果
     */
    public int deleteDbhsmDbInstanceByIds(Long[] ids);

    /**
     * 删除数据库实例信息
     *
     * @param id 数据库实例主键
     * @return 结果
     */
    public int deleteDbhsmDbInstanceById(Long id);
}

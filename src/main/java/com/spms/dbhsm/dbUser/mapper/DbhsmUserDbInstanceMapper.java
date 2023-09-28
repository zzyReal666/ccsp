package com.spms.dbhsm.dbUser.mapper;

import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbUser.domain.DbhsmUserDbInstance;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户与数据库实例关联Mapper接口
 *
 * @author Kong
 * @date 2023-09-26
 */
@Mapper
public interface DbhsmUserDbInstanceMapper
{
    /**
     * 查询用户与数据库实例关联
     *
     * @param userId 用户与数据库实例关联主键
     * @return 用户与数据库实例关联
     */
    public DbhsmUserDbInstance selectDbhsmUserDbInstanceByUserId(Long userId);

    /**
     * 查询用户与数据库实例关联列表
     *
     * @param dbhsmUserDbInstance 用户与数据库实例关联
     * @return 用户与数据库实例关联集合
     */
    public List<DbhsmUserDbInstance> selectDbhsmUserDbInstanceList(DbhsmUserDbInstance dbhsmUserDbInstance);

    /**
     * 新增用户与数据库实例关联
     *
     * @param dbhsmUserDbInstance 用户与数据库实例关联
     * @return 结果
     */
    public int insertDbhsmUserDbInstance(DbhsmUserDbInstance dbhsmUserDbInstance);

    /**
     * 修改用户与数据库实例关联
     *
     * @param dbhsmUserDbInstance 用户与数据库实例关联
     * @return 结果
     */
    public int updateDbhsmUserDbInstance(DbhsmUserDbInstance dbhsmUserDbInstance);

    /**
     * 删除用户与数据库实例关联
     *
     * @param userId 用户与数据库实例关联主键
     * @return 结果
     */
    public int deleteDbhsmUserDbInstanceByUserId(Long userId);

    /**
     * 批量删除用户与数据库实例关联
     *
     * @param userIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmUserDbInstanceByUserIds(Long[] userIds);

    DbhsmDbInstance selectInstanceByUserId(Long id);
}

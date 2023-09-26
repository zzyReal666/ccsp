package com.spms.dbhsm.dbUser.mapper;

import com.spms.dbhsm.dbUser.domain.DbhsmUserPermissionGroup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户与权限组关联关系Mapper接口
 *
 * @author Kong
 * @date 2023-09-26
 */
@Mapper
public interface DbhsmUserPermissionGroupMapper
{
    /**
     * 查询用户与权限组关联关系
     *
     * @param userId 用户与权限组关联关系主键
     * @return 用户与权限组关联关系
     */
    public DbhsmUserPermissionGroup selectDbhsmUserPermissionGroupByUserId(Long userId);

    /**
     * 查询用户与权限组关联关系列表
     *
     * @param dbhsmUserPermissionGroup 用户与权限组关联关系
     * @return 用户与权限组关联关系集合
     */
    public List<DbhsmUserPermissionGroup> selectDbhsmUserPermissionGroupList(DbhsmUserPermissionGroup dbhsmUserPermissionGroup);

    /**
     * 新增用户与权限组关联关系
     *
     * @param dbhsmUserPermissionGroup 用户与权限组关联关系
     * @return 结果
     */
    public int insertDbhsmUserPermissionGroup(DbhsmUserPermissionGroup dbhsmUserPermissionGroup);

    /**
     * 修改用户与权限组关联关系
     *
     * @param dbhsmUserPermissionGroup 用户与权限组关联关系
     * @return 结果
     */
    public int updateDbhsmUserPermissionGroup(DbhsmUserPermissionGroup dbhsmUserPermissionGroup);

    /**
     * 删除用户与权限组关联关系
     *
     * @param userId 用户与权限组关联关系主键
     * @return 结果
     */
    public int deleteDbhsmUserPermissionGroupByUserId(Long userId);

    /**
     * 批量删除用户与权限组关联关系
     *
     * @param userIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmUserPermissionGroupByUserIds(Long[] userIds);
}

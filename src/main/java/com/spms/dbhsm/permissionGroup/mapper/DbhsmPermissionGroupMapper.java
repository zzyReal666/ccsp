package com.spms.dbhsm.permissionGroup.mapper;

import com.spms.common.SelectOption;
import com.spms.dbhsm.permissionGroup.domain.DbhsmPermissionGroup;
import com.spms.dbhsm.permissionGroup.domain.DbhsmPermissionUnionPermissionGroup;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 数据库权限组信息Mapper接口
 *
 * @author diq
 * @date 2023-09-20
 */
@Mapper
public interface DbhsmPermissionGroupMapper
{
    /**
     * 查询数据库权限组信息
     *
     * @param permissionGroupId 数据库权限组信息主键
     * @return 数据库权限组信息
     */
    public DbhsmPermissionGroup selectDbhsmPermissionGroupByPermissionGroupId(Long permissionGroupId);

    /**
     * 查询数据库权限组信息列表
     *
     * @param dbhsmPermissionGroup 数据库权限组信息
     * @return 数据库权限组信息集合
     */
    public List<DbhsmPermissionGroup> selectDbhsmPermissionGroupList(DbhsmPermissionGroup dbhsmPermissionGroup);

    /**
     * 新增数据库权限组信息
     *
     * @param dbhsmPermissionGroup 数据库权限组信息
     * @return 结果
     */
    public int insertDbhsmPermissionGroup(DbhsmPermissionGroup dbhsmPermissionGroup);

    /**
     * 修改数据库权限组信息
     *
     * @param dbhsmPermissionGroup 数据库权限组信息
     * @return 结果
     */
    public int updateDbhsmPermissionGroup(DbhsmPermissionGroup dbhsmPermissionGroup);

    /**
     * 删除数据库权限组信息
     *
     * @param permissionGroupId 数据库权限组信息主键
     * @return 结果
     */
    public int deleteDbhsmPermissionGroupByPermissionGroupId(Long permissionGroupId);

    /**
     * 批量删除数据库权限组信息
     *
     * @param permissionGroupIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmPermissionGroupByPermissionGroupIds(Long[] permissionGroupIds);

    /**
     * 批量删除权限和权限组关联
     *
     * @param permissionGroupIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmPermissionUnionPermissionGroupByPermissionGroupIds(Long[] permissionGroupIds);

    /**
     * 批量新增权限和权限组关联
     *
     * @param dbhsmPermissionUnionPermissionGroupList 权限和权限组关联列表
     * @return 结果
     */
    public int batchDbhsmPermissionUnionPermissionGroup(List<DbhsmPermissionUnionPermissionGroup> dbhsmPermissionUnionPermissionGroupList);


    /**
     * 通过数据库权限组信息主键删除权限和权限组关联信息
     *
     * @param permissionGroupId 数据库权限组信息ID
     * @return 结果
     */
    public int deleteDbhsmPermissionUnionPermissionGroupByPermissionGroupId(Long permissionGroupId);


    DbhsmPermissionGroup checkPermissionGroupNameUnique(String permissionGroupName);

    List<SelectOption> selectDbhsmPermissionGroupOption();

    List<String> getPermissionsSqlByPermissionsGroupid(Long permissionGroupId);

    int deleteDbhsmPermissionUnionPermissionGroupByPermissionIds(Long[] permissionIds);
}

package com.spms.dbhsm.permissionGroup.service;

import com.spms.dbhsm.permissionGroup.domain.DbhsmPermissionGroup;
import com.spms.dbhsm.permissionGroup.domain.dto.PermissionGroupEditDto;

import java.util.List;

/**
 * 数据库权限组信息Service接口
 * 
 * @author diq
 * @date 2023-09-20
 */
public interface IDbhsmPermissionGroupService 
{
    /**
     * 查询数据库权限组信息
     * 
     * @param permissionGroupId 数据库权限组信息主键
     * @return 数据库权限组信息
     */
    public DbhsmPermissionGroup selectDbhsmPermissionGroupByPermissionGroupId(Long permissionGroupId);
    public PermissionGroupEditDto selectDbhsmPermissionGroupByPermissionGroupId2(Long permissionGroupId);
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
     * 批量删除数据库权限组信息
     * 
     * @param permissionGroupIds 需要删除的数据库权限组信息主键集合
     * @return 结果
     */
    public int deleteDbhsmPermissionGroupByPermissionGroupIds(Long[] permissionGroupIds);

    /**
     * 删除数据库权限组信息信息
     * 
     * @param permissionGroupId 数据库权限组信息主键
     * @return 结果
     */
    public int deleteDbhsmPermissionGroupByPermissionGroupId(Long permissionGroupId);

    /**
     * 校验权限组名称是否唯一
     * @param permissionGroupName
     * @return
     */
    String checkPermissionGroupNameUnique(String permissionGroupName);
}

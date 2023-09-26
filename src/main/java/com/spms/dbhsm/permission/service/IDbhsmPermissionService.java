package com.spms.dbhsm.permission.service;

import com.spms.dbhsm.permission.domain.DbhsmPermission;

import java.util.List;

/**
 * 数据库权限Service接口
 * 
 * @author diq
 * @date 2023-09-20
 */
public interface IDbhsmPermissionService 
{
    /**
     * 查询数据库权限
     * 
     * @param permissionId 数据库权限主键
     * @return 数据库权限
     */
    public DbhsmPermission selectDbhsmPermissionByPermissionId(Long permissionId);

    /**
     * 查询数据库权限列表
     *
     * @param permissionIds 数据库权限id
     * @return 数据库权限集合
     */
    public List<DbhsmPermission> selectDbhsmPermissionByPermissionIds(Long[] permissionIds);
    /**
     * 查询数据库权限列表
     * 
     * @param dbhsmPermission 数据库权限
     * @return 数据库权限集合
     */
    public List<DbhsmPermission> selectDbhsmPermissionList(DbhsmPermission dbhsmPermission);

    /**
     * 新增数据库权限
     * 
     * @param dbhsmPermission 数据库权限
     * @return 结果
     */
    public int insertDbhsmPermission(DbhsmPermission dbhsmPermission);

    /**
     * 修改数据库权限
     * 
     * @param dbhsmPermission 数据库权限
     * @return 结果
     */
    public int updateDbhsmPermission(DbhsmPermission dbhsmPermission);

    /**
     * 批量删除数据库权限
     * 
     * @param permissionIds 需要删除的数据库权限主键集合
     * @return 结果
     */
    public int deleteDbhsmPermissionByPermissionIds(Long[] permissionIds);

    /**
     * 删除数据库权限信息
     * 
     * @param permissionId 数据库权限主键
     * @return 结果
     */
    public int deleteDbhsmPermissionByPermissionId(Long permissionId);

    /**
     * 校验权限名是否唯一
     * @param permissionName
     * @return
     */
    String checkPermissionNameUnique(Long permissionId,String permissionName);

    /**
     * 校验权限SQL是否唯一
     * @param permissionName
     * @return
     */
    String checkPermissionSqlUnique(Long permissionId,String permissionName);
}

package com.spms.dbhsm.permission.mapper;

import com.spms.dbhsm.permission.domain.DbhsmPermission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 数据库权限Mapper接口
 *
 * @author diq
 * @date 2023-09-20
 */
@Mapper
public interface DbhsmPermissionMapper
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
     * 删除数据库权限
     *
     * @param permissionId 数据库权限主键
     * @return 结果
     */
    public int deleteDbhsmPermissionByPermissionId(Long permissionId);

    /**
     * 批量删除数据库权限
     *
     * @param permissionIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDbhsmPermissionByPermissionIds(Long[] permissionIds);
    /**
     * 批量查询数据库权限
     *
     * @param permissionIds 需要删除的数据主键集合
     * @return 结果
     */
    public List<DbhsmPermission> selectDbhsmPermissionByPermissionIds(Long[] permissionIds);

    /**
     * 校验权限名是否唯一
     * @param permissionName
     * @return
     */
    DbhsmPermission checkPermissionNameUnique(String permissionName);

    /**
     * 校验权限SQL是否唯一
     * @param permissionSql
     * @return
     */
    DbhsmPermission checkPermissionSqlUnique(String permissionSql);

    List<DbhsmPermission> selectDbhsmPermissionListByGroupId(Long permissionGroupId);
}

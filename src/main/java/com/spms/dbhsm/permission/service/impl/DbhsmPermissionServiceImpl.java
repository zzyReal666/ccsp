package com.spms.dbhsm.permission.service.impl;

import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.security.utils.SecurityUtils;
import com.spms.dbhsm.permission.domain.DbhsmPermission;
import com.spms.dbhsm.permission.mapper.DbhsmPermissionMapper;
import com.spms.dbhsm.permission.service.IDbhsmPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据库权限Service业务层处理
 *
 * @author diq
 * @date 2023-09-20
 */
@Service
public class DbhsmPermissionServiceImpl implements IDbhsmPermissionService {
    @Autowired
    private DbhsmPermissionMapper dbhsmPermissionMapper;

    /**
     * 查询数据库权限
     *
     * @param permissionId 数据库权限主键
     * @return 数据库权限
     */
    @Override
    public DbhsmPermission selectDbhsmPermissionByPermissionId(Long permissionId) {
        return dbhsmPermissionMapper.selectDbhsmPermissionByPermissionId(permissionId);
    }

    /**
     * 查询数据库权限列表
     *
     * @param dbhsmPermission 数据库权限
     * @return 数据库权限
     */
    @Override
    public List<DbhsmPermission> selectDbhsmPermissionList(DbhsmPermission dbhsmPermission) {
        return dbhsmPermissionMapper.selectDbhsmPermissionList(dbhsmPermission);
    }

    /**
     * 新增数据库权限
     *
     * @param dbhsmPermission 数据库权限
     * @return 结果
     */
    @Override
    public int insertDbhsmPermission(DbhsmPermission dbhsmPermission) {
        dbhsmPermission.setCreateTime(DateUtils.getNowDate());
        dbhsmPermission.setCreateBy(SecurityUtils.getUsername());
        return dbhsmPermissionMapper.insertDbhsmPermission(dbhsmPermission);
    }

    /**
     * 修改数据库权限
     *
     * @param dbhsmPermission 数据库权限
     * @return 结果
     */
    @Override
    public int updateDbhsmPermission(DbhsmPermission dbhsmPermission) {
        dbhsmPermission.setUpdateTime(DateUtils.getNowDate());
        dbhsmPermission.setUpdateBy(SecurityUtils.getUsername());
        return dbhsmPermissionMapper.updateDbhsmPermission(dbhsmPermission);
    }

    /**
     * 批量删除数据库权限
     *
     * @param permissionIds 需要删除的数据库权限主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmPermissionByPermissionIds(Long[] permissionIds) {
        return dbhsmPermissionMapper.deleteDbhsmPermissionByPermissionIds(permissionIds);
    }

    /**
     * 批量查询数据库权限
     *
     * @param permissionIds 需要删除的数据库权限主键
     * @return 结果
     */
    @Override
    public List<DbhsmPermission> selectDbhsmPermissionByPermissionIds(Long[] permissionIds) {
        return dbhsmPermissionMapper.selectDbhsmPermissionByPermissionIds(permissionIds);
    }

    /**
     * 删除数据库权限信息
     *
     * @param permissionId 数据库权限主键
     * @return 结果
     */
    @Override
    public int deleteDbhsmPermissionByPermissionId(Long permissionId) {
        return dbhsmPermissionMapper.deleteDbhsmPermissionByPermissionId(permissionId);
    }
}
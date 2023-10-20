package com.spms.dbhsm.permission.service.impl;

import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.security.utils.SecurityUtils;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.permission.domain.DbhsmPermission;
import com.spms.dbhsm.permission.mapper.DbhsmPermissionMapper;
import com.spms.dbhsm.permission.service.IDbhsmPermissionService;
import com.spms.dbhsm.permissionGroup.mapper.DbhsmPermissionGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
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

    @Autowired
    private DbhsmPermissionGroupMapper dbhsmPermissionGroupMapper;
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
    public AjaxResult deleteDbhsmPermissionByPermissionIds(Long[] permissionIds) {
        //校验权限是否被权限组使用,
        List<String> pNameList = new ArrayList<String>();
        for (Long permissionId : permissionIds) {
            String pName = checkPermissionIsUsed(permissionId);
            if(StringUtils.isEmpty(pName)){
                deleteDbhsmPermissionByPermissionId(permissionId);
            }else{
                pNameList.add(pName);
            }
        }
        if (CollectionUtils.isEmpty(pNameList)){
            return AjaxResult.success();
        }else{
            StringUtils.join(pNameList, ",");
            return AjaxResult.error("权限"+pNameList+"被使用，无法删除");
        }

    }

    private String checkPermissionIsUsed(Long permissionId) {
        //校验是否被权限组使用
         DbhsmPermission permission = dbhsmPermissionGroupMapper.selectDbhsmPermissionGroupPermissionIdList(permissionId);
         if(!ObjectUtils.isEmpty(permission)){
             return permission.getPermissionName();
         }
         return null;
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

    /**
     * 校验权限名是否唯一
     * @param permissionName
     * @return
     */
    @Override
    public String checkPermissionNameUnique(Long permissionId,String permissionName) {
        DbhsmPermission dbhsmPermission = dbhsmPermissionMapper.checkPermissionNameUnique(permissionName);
        if (dbhsmPermission == null) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }
        if (StringUtils.isEmpty(dbhsmPermission.getPermissionName())){
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }

        //有值的情况，如果是修改，允许有一条
        if (permissionId != null && permissionId.intValue() == dbhsmPermission.getPermissionId().intValue()) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }
        return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
    }

    /**
     * 校验权限SQL是否唯一
     * @param permissionSql
     * @return
     */
    @Override
    public String checkPermissionSqlUnique(Long permissionId,String permissionSql) {
        DbhsmPermission dbhsmPermission = new DbhsmPermission();
        dbhsmPermission.setPermissionSql(permissionSql);
        List<DbhsmPermission> dbhsmPermissionList =  dbhsmPermissionMapper.selectDbhsmPermissionList(dbhsmPermission);
        if (StringUtils.isEmpty(dbhsmPermissionList)) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }

        if (dbhsmPermissionList.size() > 1) {
            //至少两条
            return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
        }

        //只有一条
        if (permissionId != null && permissionId.intValue() == dbhsmPermissionList.get(0).getPermissionId().intValue()) {
            return DbConstants.DBHSM_GLOBLE_UNIQUE;
        }
        return DbConstants.DBHSM_GLOBLE_NOT_UNIQUE;
    }
}

package com.spms.dbhsm.permissionGroup.service.impl;

import com.ccsp.common.core.utils.DateUtils;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.security.utils.SecurityUtils;
import com.spms.dbhsm.permission.domain.DbhsmPermission;
import com.spms.dbhsm.permission.mapper.DbhsmPermissionMapper;
import com.spms.dbhsm.permissionGroup.domain.DbhsmPermissionGroup;
import com.spms.dbhsm.permissionGroup.domain.DbhsmPermissionUnionPermissionGroup;
import com.spms.dbhsm.permissionGroup.domain.dto.PermissionGroupEditDto;
import com.spms.dbhsm.permissionGroup.mapper.DbhsmPermissionGroupMapper;
import com.spms.dbhsm.permissionGroup.service.IDbhsmPermissionGroupService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库权限组信息Service业务层处理
 *
 * @author diq
 * @date 2023-09-20
 */
@Service
public class DbhsmPermissionGroupServiceImpl implements IDbhsmPermissionGroupService {
    @Autowired
    private DbhsmPermissionGroupMapper dbhsmPermissionGroupMapper;
    @Autowired
    private DbhsmPermissionMapper dbhsmPermissionMapper;
    /**
     * 查询数据库权限组信息
     *
     * @param permissionGroupId 数据库权限组信息主键
     * @return 数据库权限组信息
     */
    @Override
    public DbhsmPermissionGroup selectDbhsmPermissionGroupByPermissionGroupId(Long permissionGroupId) {
        return dbhsmPermissionGroupMapper.selectDbhsmPermissionGroupByPermissionGroupId(permissionGroupId);
    }

    @Override
    public PermissionGroupEditDto selectDbhsmPermissionGroupByPermissionGroupId2(Long permissionGroupId) {
        DbhsmPermissionGroup permissionGroup = dbhsmPermissionGroupMapper.selectDbhsmPermissionGroupByPermissionGroupId(permissionGroupId);

        if (permissionGroup == null) {
            return new PermissionGroupEditDto();
        }
        if (StringUtils.isEmpty(permissionGroup.getDbhsmPermissionUnionPermissionGroupList())) {
            return new PermissionGroupEditDto();
        }
        PermissionGroupEditDto permissionGroupEditDto = new PermissionGroupEditDto();
        BeanUtils.copyProperties(permissionGroup,permissionGroupEditDto);


        Long[] permissionIds = new Long[permissionGroup.getDbhsmPermissionUnionPermissionGroupList().size()];
        for (int i = 0; i < permissionGroup.getDbhsmPermissionUnionPermissionGroupList().size(); i++) {
            permissionIds[i] = permissionGroup.getDbhsmPermissionUnionPermissionGroupList().get(i).getPermissionId();
        }
        List<DbhsmPermission> dbhsmPermissions = dbhsmPermissionMapper.selectDbhsmPermissionByPermissionIds(permissionIds);
        permissionGroupEditDto.setDbhsmPermissionUnionPermissionGroupList(dbhsmPermissions);
        return permissionGroupEditDto;
    }

    /**
     * 查询数据库权限组信息列表
     *
     * @param dbhsmPermissionGroup 数据库权限组信息
     * @return 数据库权限组信息
     */
    @Override
    public List<DbhsmPermissionGroup> selectDbhsmPermissionGroupList(DbhsmPermissionGroup dbhsmPermissionGroup) {
        return dbhsmPermissionGroupMapper.selectDbhsmPermissionGroupList(dbhsmPermissionGroup);
    }

    /**
     * 新增数据库权限组信息
     *
     * @param dbhsmPermissionGroup 数据库权限组信息
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int insertDbhsmPermissionGroup(DbhsmPermissionGroup dbhsmPermissionGroup) {
        dbhsmPermissionGroup.setCreateTime(DateUtils.getNowDate());
        dbhsmPermissionGroup.setCreateBy(SecurityUtils.getUsername());
        int rows = dbhsmPermissionGroupMapper.insertDbhsmPermissionGroup(dbhsmPermissionGroup);
        insertDbhsmPermissionUnionPermissionGroup(dbhsmPermissionGroup);
        return rows;
    }

    /**
     * 修改数据库权限组信息
     *
     * @param dbhsmPermissionGroup 数据库权限组信息
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateDbhsmPermissionGroup(DbhsmPermissionGroup dbhsmPermissionGroup) {
        dbhsmPermissionGroup.setUpdateTime(DateUtils.getNowDate());
        dbhsmPermissionGroup.setUpdateBy(SecurityUtils.getUsername());
        dbhsmPermissionGroupMapper.deleteDbhsmPermissionUnionPermissionGroupByPermissionGroupId(dbhsmPermissionGroup.getPermissionGroupId());
        insertDbhsmPermissionUnionPermissionGroup(dbhsmPermissionGroup);
        return dbhsmPermissionGroupMapper.updateDbhsmPermissionGroup(dbhsmPermissionGroup);
    }

    /**
     * 批量删除数据库权限组信息
     *
     * @param permissionGroupIds 需要删除的数据库权限组信息主键
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteDbhsmPermissionGroupByPermissionGroupIds(Long[] permissionGroupIds) {
        dbhsmPermissionGroupMapper.deleteDbhsmPermissionUnionPermissionGroupByPermissionGroupIds(permissionGroupIds);
        return dbhsmPermissionGroupMapper.deleteDbhsmPermissionGroupByPermissionGroupIds(permissionGroupIds);
    }

    /**
     * 删除数据库权限组信息信息
     *
     * @param permissionGroupId 数据库权限组信息主键
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteDbhsmPermissionGroupByPermissionGroupId(Long permissionGroupId) {
        dbhsmPermissionGroupMapper.deleteDbhsmPermissionUnionPermissionGroupByPermissionGroupId(permissionGroupId);
        return dbhsmPermissionGroupMapper.deleteDbhsmPermissionGroupByPermissionGroupId(permissionGroupId);
    }

    @Override
    public String checkPermissionGroupNameUnique(String permissionGroupName) {
        DbhsmPermissionGroup dbhsmPermissionGroup = dbhsmPermissionGroupMapper.checkPermissionGroupNameUnique(permissionGroupName);
         return "";
    }

    /**
     * 新增权限和权限组关联信息
     *
     * @param dbhsmPermissionGroup 数据库权限组信息对象
     */
    public void insertDbhsmPermissionUnionPermissionGroup(DbhsmPermissionGroup dbhsmPermissionGroup) {
        List<Long> permissionIds = dbhsmPermissionGroup.getPermissionIds();
        Long permissionGroupId = dbhsmPermissionGroup.getPermissionGroupId();
        if (StringUtils.isNotNull(permissionIds)) {
            List<DbhsmPermissionUnionPermissionGroup> list = new ArrayList<DbhsmPermissionUnionPermissionGroup>();
            for (Long permissionId : permissionIds) {
                DbhsmPermissionUnionPermissionGroup dbhsmPermissionUnionPermissionGroup = new DbhsmPermissionUnionPermissionGroup();
                dbhsmPermissionUnionPermissionGroup.setPermissionGroupId(permissionGroupId);
                dbhsmPermissionUnionPermissionGroup.setPermissionId(permissionId);
                dbhsmPermissionUnionPermissionGroup.setCreateTime(DateUtils.getNowDate());
                dbhsmPermissionUnionPermissionGroup.setCreateBy(SecurityUtils.getUsername());
                list.add(dbhsmPermissionUnionPermissionGroup);
            }
            if (list.size() > 0) {
                dbhsmPermissionGroupMapper.batchDbhsmPermissionUnionPermissionGroup(list);
            }
        }
    }
}
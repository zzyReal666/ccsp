package com.spms.dbhsm.permissionGroup.controller;

import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.page.TableDataInfo;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.permissionGroup.domain.DbhsmPermissionGroup;
import com.spms.dbhsm.permissionGroup.service.IDbhsmPermissionGroupService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 数据库权限组信息Controller
 * 
 * @author diq
 * @date 2023-09-20
 */
@Api("数据库权限组信息")
@RestController
@RequestMapping("/permissionGroup")
public class DbhsmPermissionGroupController extends BaseController
{
    @Autowired
    private IDbhsmPermissionGroupService dbhsmPermissionGroupService;

    /**
     * 查询数据库权限组信息列表
     */
    @RequiresPermissions("permissionGroup:permissionGroup:list")
    @GetMapping("/list")
    public TableDataInfo list(DbhsmPermissionGroup dbhsmPermissionGroup)
    {
        startPage();
        List<DbhsmPermissionGroup> list = dbhsmPermissionGroupService.selectDbhsmPermissionGroupList(dbhsmPermissionGroup);
        return getDataTable(list);
    }

    /**
     * 导出数据库权限组信息列表
     */
    @RequiresPermissions("permissionGroup:permissionGroup:export")
    @Log(title = "数据库权限组信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmPermissionGroup dbhsmPermissionGroup)
    {
        List<DbhsmPermissionGroup> list = dbhsmPermissionGroupService.selectDbhsmPermissionGroupList(dbhsmPermissionGroup);
        ExcelUtil<DbhsmPermissionGroup> util = new ExcelUtil<DbhsmPermissionGroup>(DbhsmPermissionGroup.class);
        util.exportExcel(response, list, "数据库权限组信息数据");
    }

    /**
     * 获取数据库权限组信息详细信息
     */
    @RequiresPermissions("permissionGroup:permissionGroup:query")
    @GetMapping(value = "/{permissionGroupId}")
    public AjaxResult getInfo(@PathVariable("permissionGroupId") Long permissionGroupId)
    {
        return AjaxResult.success(dbhsmPermissionGroupService.selectDbhsmPermissionGroupByPermissionGroupId(permissionGroupId));
    }

    /**
     * 获取数据库权限组信息详细信息
     */
    @RequiresPermissions("permissionGroup:permissionGroup:query")
    @GetMapping(value = "getInfo2/{permissionGroupId}")
    public AjaxResult getInfo2(@PathVariable("permissionGroupId") Long permissionGroupId)
    {
        return AjaxResult.success(dbhsmPermissionGroupService.selectDbhsmPermissionGroupByPermissionGroupId2(permissionGroupId));
    }

    /**
     * 新增数据库权限组信息
     */
    @RequiresPermissions("permissionGroup:permissionGroup:add")
    @Log(title = "数据库权限组信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DbhsmPermissionGroup dbhsmPermissionGroup)
    {
        if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(dbhsmPermissionGroupService.checkPermissionGroupNameUnique(null,dbhsmPermissionGroup.getPermissionGroupName()))) {
            return AjaxResult.error("权限组名称已存在");
        }
        return toAjax(dbhsmPermissionGroupService.insertDbhsmPermissionGroup(dbhsmPermissionGroup));
    }

    /**
     * 修改数据库权限组信息
     */
    @RequiresPermissions("permissionGroup:permissionGroup:edit")
    @Log(title = "数据库权限组信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmPermissionGroup dbhsmPermissionGroup)
    {
        if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(dbhsmPermissionGroupService.checkPermissionGroupNameUnique(dbhsmPermissionGroup.getPermissionGroupId(),dbhsmPermissionGroup.getPermissionGroupName()))) {
            return AjaxResult.error("权限组名称已存在");
        }
        return toAjax(dbhsmPermissionGroupService.updateDbhsmPermissionGroup(dbhsmPermissionGroup));
    }

    /**
     * 删除数据库权限组信息
     */
    @RequiresPermissions("permissionGroup:permissionGroup:remove")
    @Log(title = "数据库权限组信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{permissionGroupIds}")
    public AjaxResult remove(@PathVariable Long[] permissionGroupIds)
    {
        return toAjax(dbhsmPermissionGroupService.deleteDbhsmPermissionGroupByPermissionGroupIds(permissionGroupIds));
    }
}

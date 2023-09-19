package com.spms.dbhsm.controller;

import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.page.TableDataInfo;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.spms.dbhsm.domain.DbhsmPermission;
import com.spms.dbhsm.service.IDbhsmPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 数据库权限Controller
 * 
 * @author diq
 * @date 2023-09-20
 */
@RestController
@RequestMapping("/permission")
public class DbhsmPermissionController extends BaseController
{
    @Autowired
    private IDbhsmPermissionService dbhsmPermissionService;

    /**
     * 查询数据库权限列表
     */
    @RequiresPermissions("dbhsm:permission:list")
    @GetMapping("/list")
    public TableDataInfo list(DbhsmPermission dbhsmPermission)
    {
        startPage();
        List<DbhsmPermission> list = dbhsmPermissionService.selectDbhsmPermissionList(dbhsmPermission);
        return getDataTable(list);
    }

    /**
     * 导出数据库权限列表
     */
    @RequiresPermissions("dbhsm:permission:export")
    @Log(title = "数据库权限", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmPermission dbhsmPermission)
    {
        List<DbhsmPermission> list = dbhsmPermissionService.selectDbhsmPermissionList(dbhsmPermission);
        ExcelUtil<DbhsmPermission> util = new ExcelUtil<DbhsmPermission>(DbhsmPermission.class);
        util.exportExcel(response, list, "数据库权限数据");
    }

    /**
     * 获取数据库权限详细信息
     */
    @RequiresPermissions("dbhsm:permission:query")
    @GetMapping(value = "/{permissionId}")
    public AjaxResult getInfo(@PathVariable("permissionId") Long permissionId)
    {
        return AjaxResult.success(dbhsmPermissionService.selectDbhsmPermissionByPermissionId(permissionId));
    }

    /**
     * 新增数据库权限
     */
    @RequiresPermissions("dbhsm:permission:add")
    @Log(title = "数据库权限", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DbhsmPermission dbhsmPermission)
    {
        return toAjax(dbhsmPermissionService.insertDbhsmPermission(dbhsmPermission));
    }

    /**
     * 修改数据库权限
     */
    @RequiresPermissions("dbhsm:permission:edit")
    @Log(title = "数据库权限", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmPermission dbhsmPermission)
    {
        return toAjax(dbhsmPermissionService.updateDbhsmPermission(dbhsmPermission));
    }

    /**
     * 删除数据库权限
     */
    @RequiresPermissions("dbhsm:permission:remove")
    @Log(title = "数据库权限", businessType = BusinessType.DELETE)
	@DeleteMapping("/{permissionIds}")
    public AjaxResult remove(@PathVariable Long[] permissionIds)
    {
        return toAjax(dbhsmPermissionService.deleteDbhsmPermissionByPermissionIds(permissionIds));
    }
}

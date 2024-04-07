package com.spms.dbhsm.encryptcolumns.controller;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.ccsp.system.api.systemApi.domain.SysDictData;
import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsDto;
import com.spms.dbhsm.encryptcolumns.service.IDbhsmEncryptColumnsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库加密列Controller
 *
 * @author diq
 * @date 2023-09-27
 */
@Api(tags = "数据库加密列")
@RestController
@RequestMapping("/encryptcolumns")
public class DbhsmEncryptColumnsController extends BaseController
{
    @Autowired
    private IDbhsmEncryptColumnsService dbhsmEncryptColumnsService;

    /**
     * 查询数据库加密列列表
     */
//    @RequiresPermissions("dbhsm:encryptcolumns:list")
    @ApiOperation(value = "获取数据库加密列列表")
    @GetMapping("/list")
    public AjaxResult list(DbhsmEncryptColumnsDto dbhsmEncryptColumns)
    {
        List<DbhsmEncryptColumns> list = null;
        try {
            list = dbhsmEncryptColumnsService.selectDbhsmEncryptColumnsList(dbhsmEncryptColumns);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return AjaxResult.success(list);
    }

    /**
     * 导出数据库加密列列表
     */
    @RequiresPermissions("dbhsm:encryptcolumns:export")
    @Log(title = "数据库加密列", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmEncryptColumnsDto dbhsmEncryptColumns)
    {
        List<DbhsmEncryptColumns> list = null;
        try {
            list = dbhsmEncryptColumnsService.selectDbhsmEncryptColumnsList(dbhsmEncryptColumns);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ExcelUtil<DbhsmEncryptColumns> util = new ExcelUtil<DbhsmEncryptColumns>(DbhsmEncryptColumns.class);
        util.exportExcel(response, list, "数据库加密列数据");
    }

    /**
     * 获取数据库加密列详细信息
     */
    @RequiresPermissions("dbhsm:encryptcolumns:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return AjaxResult.success(dbhsmEncryptColumnsService.selectDbhsmEncryptColumnsById(id));
    }

    /**
     * 获取达梦数据库加密算法列表
     */
    @RequiresPermissions("dbhsm:encryptcolumns:query")
    @GetMapping(value = "/listDMAlgList")
    public AjaxResult getDMAlg(DbhsmEncryptColumnsDto dbhsmEncryptColumns)
    {
        List<SysDictData> arrayList= new ArrayList<SysDictData>();
        try {
            arrayList = dbhsmEncryptColumnsService.selectDMAlg(dbhsmEncryptColumns);
        } catch (ZAYKException | SQLException e) {
            e.printStackTrace();
            return AjaxResult.error("获取达梦算法列表失败！");
        }
        return AjaxResult.success(arrayList);
    }

    /**
     * 新增数据库加密列
     */
//    @RequiresPermissions("dbhsm:encryptcolumns:add")
    @ApiOperation(value = "新增数据库加密列")
    @Log(title = "数据库加密列", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DbhsmEncryptColumnsAdd dbhsmEncryptColumns)
    {
        try {
            return toAjax(dbhsmEncryptColumnsService.insertDbhsmEncryptColumns(dbhsmEncryptColumns));
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage());
        }
    }
    @GetMapping("/getEncColumnsTaskStatus")
    public String getTaskStatus() {
        return dbhsmEncryptColumnsService.getTaskStatus();
    }
    /**
     * 修改数据库加密列
     */
    @ApiOperation(value = "修改数据库加密列")
    @RequiresPermissions("dbhsm:encryptcolumns:edit")
    @Log(title = "数据库加密列", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmEncryptColumns dbhsmEncryptColumns)
    {
        return toAjax(dbhsmEncryptColumnsService.updateDbhsmEncryptColumns(dbhsmEncryptColumns));
    }

    /**
     * 删除数据库加密列
     */
//    @RequiresPermissions("dbhsm:encryptcolumns:remove")
    @ApiOperation(value = "删除数据库加密列")
    @Log(title = "数据库加密列", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids) throws Exception {
        try {
            dbhsmEncryptColumnsService.deleteDbhsmEncryptColumnsByIds(ids);
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("删除失败！");
        }
        return AjaxResult.success();
    }

    /**
     * 数据库实例-用户-表树结构
     */
    @PostMapping(value = "/treeData")
    @ApiOperation(value = "数据库实例-用户-表树结构")
    public AjaxResult2 treeData()
    {
        return dbhsmEncryptColumnsService.treeData();
    }
}

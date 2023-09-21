package com.spms.dbInstance.controller;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.core.web.page.TableDataInfo;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.spms.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbInstance.service.IDbhsmDbInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 数据库实例Controller
 *
 * @author spms
 * @date 2023-09-19
 */
@RestController
@RequestMapping("/dbInstance")
public class DbhsmDbInstanceController extends BaseController
{
    @Autowired
    private IDbhsmDbInstanceService dbhsmDbInstanceService;

    /**
     * 查询数据库实例列表
     */
    @RequiresPermissions("dbhsm:dbInstance:list")
    @GetMapping("/list")
    public TableDataInfo list(DbhsmDbInstance dbhsmDbInstance)
    {
        startPage();
        List<DbhsmDbInstance> list = dbhsmDbInstanceService.selectDbhsmDbInstanceList(dbhsmDbInstance);
        return getDataTable(list);
    }

    /**
     * 导出数据库实例列表
     */
    @RequiresPermissions("dbhsm:dbInstance:export")
    @Log(title = "数据库实例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmDbInstance dbhsmDbInstance)
    {
        List<DbhsmDbInstance> list = dbhsmDbInstanceService.selectDbhsmDbInstanceList(dbhsmDbInstance);
        ExcelUtil<DbhsmDbInstance> util = new ExcelUtil<DbhsmDbInstance>(DbhsmDbInstance.class);
        util.exportExcel(response, list, "数据库实例数据");
    }

    /**
     * 获取数据库实例详细信息
     */
    @RequiresPermissions("dbhsm:dbInstance:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(dbhsmDbInstanceService.selectDbhsmDbInstanceById(id));
    }

    /**
     * 新增数据库实例
     */
    @RequiresPermissions("dbhsm:dbInstance:add")
    @Log(title = "数据库实例", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult2 add(@RequestBody DbhsmDbInstance dbhsmDbInstance)
    {
        try {
            dbhsmDbInstanceService.insertDbhsmDbInstance(dbhsmDbInstance);
        }catch (ZAYKException  e){
            e.printStackTrace();
            return AjaxResult2.error(e.getMessage());
        }
        return AjaxResult2.success();
    }

    /**
     * 修改数据库实例
     */
    @RequiresPermissions("dbhsm:dbInstance:edit")
    @Log(title = "数据库实例", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmDbInstance dbhsmDbInstance)
    {
        return toAjax(dbhsmDbInstanceService.updateDbhsmDbInstance(dbhsmDbInstance));
    }

    /**
     * 删除数据库实例
     */
    @RequiresPermissions("dbhsm:dbInstance:remove")
    @Log(title = "数据库实例", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(dbhsmDbInstanceService.deleteDbhsmDbInstanceByIds(ids));
    }
}

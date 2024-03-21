package com.spms.dbhsm.warningInfo.controller;

import java.util.List;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.spms.dbhsm.warningInfo.domain.DbhsmWarningInfo;
import com.spms.dbhsm.warningInfo.service.IDbhsmWarningInfoService;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.page.TableDataInfo;

/**
 * warningInfoController
 * 
 * @author diq
 * @date 2024-03-21
 */
@RestController
@RequestMapping("/warningInfo")
public class DbhsmWarningInfoController extends BaseController
{
    @Autowired
    private IDbhsmWarningInfoService dbhsmWarningInfoService;

    /**
     * 查询warningInfo列表
     */
    @RequiresPermissions("warningInfo:warningInfo:list")
    @GetMapping("/list")
    public TableDataInfo list(DbhsmWarningInfo dbhsmWarningInfo)
    {
        startPage();
        List<DbhsmWarningInfo> list = dbhsmWarningInfoService.selectDbhsmWarningInfoList(dbhsmWarningInfo);
        return getDataTable(list);
    }

    /**
     * 导出warningInfo列表
     */
    @RequiresPermissions("warningInfo:warningInfo:export")
    @Log(title = "warningInfo", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmWarningInfo dbhsmWarningInfo)
    {
        List<DbhsmWarningInfo> list = dbhsmWarningInfoService.selectDbhsmWarningInfoList(dbhsmWarningInfo);
        ExcelUtil<DbhsmWarningInfo> util = new ExcelUtil<DbhsmWarningInfo>(DbhsmWarningInfo.class);
        util.exportExcel(response, list, "warningInfo数据");
    }

    /**
     * 获取warningInfo详细信息
     */
    @RequiresPermissions("warningInfo:warningInfo:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(dbhsmWarningInfoService.selectDbhsmWarningInfoById(id));
    }

    /**
     * 新增warningInfo
     */
    @RequiresPermissions("warningInfo:warningInfo:add")
    @Log(title = "warningInfo", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DbhsmWarningInfo dbhsmWarningInfo)
    {
        return toAjax(dbhsmWarningInfoService.insertDbhsmWarningInfo(dbhsmWarningInfo));
    }

    /**
     * 修改warningInfo
     */
    @RequiresPermissions("warningInfo:warningInfo:edit")
    @Log(title = "warningInfo", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmWarningInfo dbhsmWarningInfo)
    {
        return toAjax(dbhsmWarningInfoService.updateDbhsmWarningInfo(dbhsmWarningInfo));
    }

    /**
     * 删除warningInfo
     */
    @RequiresPermissions("warningInfo:warningInfo:remove")
    @Log(title = "warningInfo", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(dbhsmWarningInfoService.deleteDbhsmWarningInfoByIds(ids));
    }
}

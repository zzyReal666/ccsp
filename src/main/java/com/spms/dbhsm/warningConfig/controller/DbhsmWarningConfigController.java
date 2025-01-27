package com.spms.dbhsm.warningConfig.controller;

import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.core.web.page.TableDataInfo;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.spms.dbhsm.warningConfig.domain.DbhsmWarningConfig;
import com.spms.dbhsm.warningConfig.service.IDbhsmWarningConfigService;
import com.spms.dbhsm.warningConfig.vo.DataBaseConnectionResponse;
import com.spms.dbhsm.warningConfig.vo.DbhsmWarningConfigListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * warningConfigController
 * 
 * @author diq
 * @date 2024-03-21
 */
@RestController
@RequestMapping("/warningConfig")
public class DbhsmWarningConfigController extends BaseController
{
    @Autowired
    private IDbhsmWarningConfigService dbhsmWarningConfigService;

    /**
     * 查询warningConfig列表
     */
    @RequiresPermissions("warningConfig:warningConfig:list")
    @GetMapping("/list")
    public TableDataInfo list(DbhsmWarningConfig dbhsmWarningConfig)
    {
        startPage();
        List<DbhsmWarningConfigListResponse> list = dbhsmWarningConfigService.selectDbhsmWarningConfigList(dbhsmWarningConfig);
        return getDataTable(list);
    }

    /**
     * 导出warningConfig列表
     */
    @RequiresPermissions("warningConfig:warningConfig:export")
    @Log(title = "warningConfig", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmWarningConfig dbhsmWarningConfig)
    {
        List<DbhsmWarningConfigListResponse> list = dbhsmWarningConfigService.selectDbhsmWarningConfigList(dbhsmWarningConfig);
        ExcelUtil<DbhsmWarningConfigListResponse> util = new ExcelUtil<DbhsmWarningConfigListResponse>(DbhsmWarningConfigListResponse.class);
        util.exportExcel(response, list, "warningConfig数据");
    }

    /**
     * 获取warningConfig详细信息
     */
    @RequiresPermissions("warningConfig:warningConfig:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(dbhsmWarningConfigService.selectDbhsmWarningConfigById(id));
    }

    /**
     * 新增warningConfig
     */
    @RequiresPermissions("warningConfig:warningConfig:add")
    @Log(title = "warningConfig", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DbhsmWarningConfig dbhsmWarningConfig)
    {
        return dbhsmWarningConfigService.insertDbhsmWarningConfig(dbhsmWarningConfig);
    }

    /**
     * 修改warningConfig
     */
    @RequiresPermissions("warningConfig:warningConfig:edit")
    @Log(title = "warningConfig", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmWarningConfig dbhsmWarningConfig)
    {
        return toAjax(dbhsmWarningConfigService.updateDbhsmWarningConfig(dbhsmWarningConfig));
    }

    /**
     * 删除warningConfig
     */
    @RequiresPermissions("warningConfig:warningConfig:remove")
    @Log(title = "warningConfig", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(dbhsmWarningConfigService.deleteDbhsmWarningConfigByIds(ids));
    }

    @GetMapping("/queryDataBaseConnection")
    public AjaxResult2<List<DataBaseConnectionResponse>> queryDataBaseConnection()
    {
        return dbhsmWarningConfigService.queryDataBaseConnection();
    }
}

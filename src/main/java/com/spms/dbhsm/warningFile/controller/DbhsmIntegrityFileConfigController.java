package com.spms.dbhsm.warningFile.controller;

import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.page.TableDataInfo;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.spms.dbhsm.warningFile.domain.DbhsmIntegrityFileConfig;
import com.spms.dbhsm.warningFile.service.IDbhsmIntegrityFileConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 文件完整性校验Controller
 *
 * @author diq
 * @date 2024-04-17
 */
@RestController
@RequestMapping("/integrityfile")
public class DbhsmIntegrityFileConfigController extends BaseController {
    @Autowired
    private IDbhsmIntegrityFileConfigService dbhsmIntegrityFileConfigService;

    /**
     * 查询文件完整性校验列表
     */
    @RequiresPermissions("dbhsm:integrityfile:list")
    @GetMapping("/list")
    public TableDataInfo list(DbhsmIntegrityFileConfig dbhsmIntegrityFileConfig) {
        startPage();
        List<DbhsmIntegrityFileConfig> list = dbhsmIntegrityFileConfigService.selectDbhsmIntegrityFileConfigList(dbhsmIntegrityFileConfig);
        return getDataTable(list);
    }

    /**
     * 导出文件完整性校验列表
     */
    @RequiresPermissions("dbhsm:integrityfile:export")
    @Log(title = "文件完整性校验", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmIntegrityFileConfig dbhsmIntegrityFileConfig) {
        List<DbhsmIntegrityFileConfig> list = dbhsmIntegrityFileConfigService.selectDbhsmIntegrityFileConfigList(dbhsmIntegrityFileConfig);
        ExcelUtil<DbhsmIntegrityFileConfig> util = new ExcelUtil<DbhsmIntegrityFileConfig>(DbhsmIntegrityFileConfig.class);
        util.exportExcel(response, list, "文件完整性校验数据");
    }

    /**
     * 获取文件完整性校验详细信息
     */
    @RequiresPermissions("dbhsm:integrityfile:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(dbhsmIntegrityFileConfigService.selectDbhsmIntegrityFileConfigById(id));
    }

    /**
     * 新增文件完整性校验
     */
    @RequiresPermissions("dbhsm:integrityfile:add")
    @Log(title = "文件完整性校验", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DbhsmIntegrityFileConfig dbhsmIntegrityFileConfig) {
        return toAjax(dbhsmIntegrityFileConfigService.insertDbhsmIntegrityFileConfig(dbhsmIntegrityFileConfig));
    }

    /**
     * 修改文件完整性校验
     */
    @RequiresPermissions("dbhsm:integrityfile:edit")
    @Log(title = "文件完整性校验", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmIntegrityFileConfig dbhsmIntegrityFileConfig) {
        return toAjax(dbhsmIntegrityFileConfigService.updateDbhsmIntegrityFileConfig(dbhsmIntegrityFileConfig));
    }

    /**
     * 删除文件完整性校验
     */
    @RequiresPermissions("dbhsm:integrityfile:remove")
    @Log(title = "文件完整性校验", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(dbhsmIntegrityFileConfigService.deleteDbhsmIntegrityFileConfigByIds(ids));
    }
}

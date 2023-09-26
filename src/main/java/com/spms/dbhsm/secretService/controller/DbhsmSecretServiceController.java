package com.spms.dbhsm.secretService.controller;

import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.page.TableDataInfo;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.spms.dbhsm.secretService.domain.DbhsmSecretService;
import com.spms.dbhsm.secretService.service.IDbhsmSecretServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 密码服务Controller
 * 
 * @author diq
 * @date 2023-09-25
 */
@RestController
@RequestMapping("/secretService")
public class DbhsmSecretServiceController extends BaseController
{
    @Autowired
    private IDbhsmSecretServiceService dbhsmSecretServiceService;

    /**
     * 查询密码服务列表
     */
    @RequiresPermissions("dbhsm:secretService:list")
    @GetMapping("/list")
    public TableDataInfo list(DbhsmSecretService dbhsmSecretService)
    {
        startPage();
        List<DbhsmSecretService> list = dbhsmSecretServiceService.selectDbhsmSecretServiceList(dbhsmSecretService);
        return getDataTable(list);
    }

    /**
     * 导出密码服务列表
     */
    @RequiresPermissions("dbhsm:secretService:export")
    @Log(title = "密码服务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmSecretService dbhsmSecretService)
    {
        List<DbhsmSecretService> list = dbhsmSecretServiceService.selectDbhsmSecretServiceList(dbhsmSecretService);
        ExcelUtil<DbhsmSecretService> util = new ExcelUtil<DbhsmSecretService>(DbhsmSecretService.class);
        util.exportExcel(response, list, "密码服务数据");
    }

    /**
     * 获取密码服务详细信息
     */
    @RequiresPermissions("dbhsm:secretService:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(dbhsmSecretServiceService.selectDbhsmSecretServiceById(id));
    }

    /**
     * 新增密码服务
     */
    @RequiresPermissions("dbhsm:secretService:add")
    @Log(title = "密码服务", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DbhsmSecretService dbhsmSecretService) throws Exception {
        try {
            return toAjax(dbhsmSecretServiceService.insertDbhsmSecretService(dbhsmSecretService));
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    /**
     * 修改密码服务
     */
    @RequiresPermissions("dbhsm:secretService:edit")
    @Log(title = "密码服务", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmSecretService dbhsmSecretService) throws IOException {
        return toAjax(dbhsmSecretServiceService.updateDbhsmSecretService(dbhsmSecretService));
    }

    /**
     * 删除密码服务
     */
    @RequiresPermissions("dbhsm:secretService:remove")
    @Log(title = "密码服务", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(dbhsmSecretServiceService.deleteDbhsmSecretServiceByIds(ids));
    }
}

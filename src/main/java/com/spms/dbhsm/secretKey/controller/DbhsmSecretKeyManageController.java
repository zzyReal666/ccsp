package com.spms.dbhsm.secretKey.controller;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.StringUtils;
import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.core.web.page.TableDataInfo;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.spms.common.constant.DbConstants;
import com.spms.dbhsm.secretKey.domain.DbhsmSecretKeyManage;
import com.spms.dbhsm.secretKey.service.IDbhsmSecretKeyManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 数据库密钥Controller
 *
 * @author ccsp
 * @date 2023-09-22
 */
@RestController
@RequestMapping("/secretKey")
public class DbhsmSecretKeyManageController extends BaseController
{
    @Autowired
    private IDbhsmSecretKeyManageService dbhsmSecretKeyManageService;

    /**
     * 查询数据库密钥列表
     */
    @RequiresPermissions("dbhsm:secretKey:list")
    @GetMapping("/list")
    public TableDataInfo list(DbhsmSecretKeyManage dbhsmSecretKeyManage)
    {
        startPage();
        List<DbhsmSecretKeyManage> list = dbhsmSecretKeyManageService.selectDbhsmSecretKeyManageList(dbhsmSecretKeyManage);
        return getDataTable(list);
    }
    /**
     * 查询数据库密钥列表加密列下拉使用
     */
    @RequiresPermissions("dbhsm:secretKey:list")
    @GetMapping("/listForDropDownBox")
    public TableDataInfo listForDropDownBox(DbhsmSecretKeyManage dbhsmSecretKeyManage)
    {
        List<DbhsmSecretKeyManage> list = dbhsmSecretKeyManageService.selectDbhsmSecretKeyManageList(dbhsmSecretKeyManage);
        return getDataTable(list);
    }

    /**
     * 导出数据库密钥列表
     */
    @RequiresPermissions("dbhsm:secretKey:export")
    @Log(title = "数据库密钥", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmSecretKeyManage dbhsmSecretKeyManage)
    {
        List<DbhsmSecretKeyManage> list = dbhsmSecretKeyManageService.selectDbhsmSecretKeyManageList(dbhsmSecretKeyManage);
        ExcelUtil<DbhsmSecretKeyManage> util = new ExcelUtil<DbhsmSecretKeyManage>(DbhsmSecretKeyManage.class);
        util.exportExcel(response, list, "数据库密钥数据");
    }

    /**
     * 获取数据库密钥详细信息
     */
    @RequiresPermissions("dbhsm:secretKey:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(dbhsmSecretKeyManageService.selectDbhsmSecretKeyManageById(id));
    }

    /**
     * 新增数据库密钥
     */
    @RequiresPermissions("dbhsm:secretKey:add")
    @Log(title = "数据库密钥", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DbhsmSecretKeyManage dbhsmSecretKeyManage) throws Exception {
        if (dbhsmSecretKeyManage.getSecretKeySource().intValue() == DbConstants.KEY_SOURCE_KMIP && StringUtils.isEmpty(dbhsmSecretKeyManage.getSecretKeyServer())){
            throw new Exception("请选择密码服务");
        }
        //校验密钥名称是否唯一
        if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(dbhsmSecretKeyManageService.checkSecretKeyUnique(dbhsmSecretKeyManage))) {
            return AjaxResult.error("密钥名称已存在");
        }
        //校验密钥索引是否已被创建过
        if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(dbhsmSecretKeyManageService.checkSecretKeyIndexUnique(dbhsmSecretKeyManage))) {
            return AjaxResult.error(dbhsmSecretKeyManage.getSecretKeyIndex() + "号密钥已创建,请使用其他密钥索引！");
        }
        return toAjax(dbhsmSecretKeyManageService.insertDbhsmSecretKeyManage(dbhsmSecretKeyManage));
    }

    /**
     * 修改数据库密钥
     */
    @RequiresPermissions("dbhsm:secretKey:edit")
    @Log(title = "数据库密钥", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmSecretKeyManage dbhsmSecretKeyManage)
    {
        //校验密钥名称是否唯一
        if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(dbhsmSecretKeyManageService.checkSecretKeyUniqueEdit(dbhsmSecretKeyManage))) {
            return AjaxResult.error("密钥名称已存在");
        }
        //校验密钥索引是否已被创建过
        if (DbConstants.DBHSM_GLOBLE_NOT_UNIQUE.equals(dbhsmSecretKeyManageService.checkSecretKeyIndexUniqueEdit(dbhsmSecretKeyManage))) {
            return AjaxResult.error(dbhsmSecretKeyManage.getSecretKeyIndex() + "号密钥已创建,请使用其他密钥索引！");
        }
        try {
            return toAjax(dbhsmSecretKeyManageService.updateDbhsmSecretKeyManage(dbhsmSecretKeyManage));
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error(e.getMessage() == "null"? "修改失败":e.getMessage());
        }
    }

    /**
     * 删除数据库密钥
     */
    @RequiresPermissions("dbhsm:secretKey:remove")
    @Log(title = "数据库密钥", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult2 remove(@PathVariable Long[] ids)
    {
        AjaxResult2 ajaxResult2= new AjaxResult2();
        try {
            ajaxResult2 = dbhsmSecretKeyManageService.deleteDbhsmSecretKeyManageByIds(ids);
        } catch (ZAYKException e) {
            e.printStackTrace();
            return AjaxResult2.error("null".equals(e.getMessage()) ? "删除失败":e.getMessage());
        }
        return ajaxResult2;
    }
}

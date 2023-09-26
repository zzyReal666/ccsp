package com.spms.dbhsm.dbUser.controller;

import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.core.web.page.TableDataInfo2;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.spms.dbhsm.dbUser.domain.DbhsmDbUser;
import com.spms.dbhsm.dbUser.domain.VO.DbhsmDbUserVO;
import com.spms.dbhsm.dbUser.service.IDbhsmDbUsersService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库用户Controller
 *
 * @author ccsp
 * @date 2023-09-25
 */
@RestController
@RequestMapping("/dbuser")
public class DbhsmDbUsersController extends BaseController
{
    @Autowired
    private IDbhsmDbUsersService dbhsmDbUsersService;

    /**
     * 查询数据库用户列表
     */
    @RequiresPermissions("dbUser:dbuser:list")
    @GetMapping("/list")
    public AjaxResult2<TableDataInfo2<DbhsmDbUserVO>> list(DbhsmDbUser dbhsmDbUser)
    {
        startPage();
        List<DbhsmDbUser> list = dbhsmDbUsersService.selectDbhsmDbUsersList(dbhsmDbUser);
        List<DbhsmDbUserVO> listVos = new ArrayList<>();
        list.forEach(dbUser ->{
            DbhsmDbUserVO dbhsmDbUserVO = new DbhsmDbUserVO();
            BeanUtils.copyProperties(dbUser, dbhsmDbUserVO);
            listVos.add(dbhsmDbUserVO);
        });
        return getDataList(listVos);
    }

    /**
     * 导出数据库用户列表
     */
    @RequiresPermissions("dbUser:dbuser:export")
    @Log(title = "数据库用户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmDbUser dbhsmDbUser)
    {
        List<DbhsmDbUser> list = dbhsmDbUsersService.selectDbhsmDbUsersList(dbhsmDbUser);
        ExcelUtil<DbhsmDbUser> util = new ExcelUtil<DbhsmDbUser>(DbhsmDbUser.class);
        util.exportExcel(response, list, "数据库用户数据");
    }

    /**
     * 获取数据库用户详细信息
     */
    @RequiresPermissions("dbUser:dbuser:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(dbhsmDbUsersService.selectDbhsmDbUsersById(id));
    }

    /**
     * 新增数据库用户
     */
    @RequiresPermissions("dbUser:dbuser:add")
    @Log(title = "数据库用户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DbhsmDbUser dbhsmDbUser)
    {
        return toAjax(dbhsmDbUsersService.insertDbhsmDbUsers(dbhsmDbUser));
    }

    /**
     * 修改数据库用户
     */
    @RequiresPermissions("dbUser:dbuser:edit")
    @Log(title = "数据库用户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmDbUser dbhsmDbUser)
    {
        return toAjax(dbhsmDbUsersService.updateDbhsmDbUsers(dbhsmDbUser));
    }

    /**
     * 删除数据库用户
     */
    @RequiresPermissions("dbUser:dbuser:remove")
    @Log(title = "数据库用户", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(dbhsmDbUsersService.deleteDbhsmDbUsersByIds(ids));
    }
}

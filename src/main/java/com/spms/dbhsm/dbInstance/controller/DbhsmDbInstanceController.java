package com.spms.dbhsm.dbInstance.controller;

import com.ccsp.common.core.exception.ZAYKException;
import com.ccsp.common.core.utils.poi.ExcelUtil;
import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.core.web.page.TableDataInfo2;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.ccsp.common.security.annotation.RequiresPermissions;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import com.spms.dbhsm.dbInstance.domain.VO.InstanceServerNameVO;
import com.spms.dbhsm.dbInstance.service.IDbhsmDbInstanceService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库实例Controller
 *
 * @author spms
 * @date 2023-09-19
 */
@Slf4j
@RestController
@RequestMapping("/dbInstance")
public class DbhsmDbInstanceController extends BaseController {
    @Autowired
    private IDbhsmDbInstanceService dbhsmDbInstanceService;

    /**
     * 查询数据库实例列表
     */
    @RequiresPermissions("dbhsm:dbInstance:list")
    @GetMapping("/list")
    public AjaxResult2<TableDataInfo2<DbhsmDbInstance>> list(DbhsmDbInstance dbhsmDbInstance) {
        startPage();
        List<DbhsmDbInstance> list = dbhsmDbInstanceService.selectDbhsmDbInstanceList(dbhsmDbInstance);
        return getDataList(list);
    }

    /**
     * 查询数据库实例列表
     */
    @RequiresPermissions("dbhsm:dbInstance:list")
    @GetMapping("/getDbInfo")
    public AjaxResult getDbInfo(Long id) {
        AjaxResult ajax = new AjaxResult();
        DbhsmDbInstance instance = dbhsmDbInstanceService.selectDbhsmDbInstanceById(id);
        DbInstanceGetConnDTO instanceGetConnDTO = new DbInstanceGetConnDTO();
        BeanUtils.copyProperties(instance, instanceGetConnDTO);
        ajax.put("instance", instanceGetConnDTO);
        ajax.put("dbTableSpace", dbhsmDbInstanceService.getDbTablespace(id));
        //获取PostgreSQL 架构（schema)
        ajax.put("dbPGSchema", dbhsmDbInstanceService.getDbSchema(id));
        ajax.put("pwdPolicyToDM", dbhsmDbInstanceService.getPwdPolicyToDM(id));
        return ajax;
    }

    /**
     * 查询数据库实例列表用户侧边栏使用
     */
    @RequiresPermissions("dbhsm:dbInstance:list")
    @GetMapping("/listDbInstanceSelect")
    public AjaxResult2 listDbInstanceSelect(InstanceServerNameVO instanceServerNameVO) {
        return AjaxResult2.success(dbhsmDbInstanceService.listDbInstanceSelect(instanceServerNameVO));
    }

    /**
     * 导出数据库实例列表
     */
    @RequiresPermissions("dbhsm:dbInstance:export")
    @Log(title = "数据库实例", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DbhsmDbInstance dbhsmDbInstance) {
        List<DbhsmDbInstance> list = dbhsmDbInstanceService.selectDbhsmDbInstanceList(dbhsmDbInstance);
        ExcelUtil<DbhsmDbInstance> util = new ExcelUtil<DbhsmDbInstance>(DbhsmDbInstance.class);
        util.exportExcel(response, list, "数据库实例数据");
    }

    /**
     * 获取数据库实例详细信息
     */
    @RequiresPermissions("dbhsm:dbInstance:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(dbhsmDbInstanceService.selectDbhsmDbInstanceById(id));
    }

    /**
     * 新增数据库实例
     */
//    @RequiresPermissions("dbhsm:dbInstance:add")
    @Log(title = "数据库实例", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult2 add(@RequestBody DbhsmDbInstance dbhsmDbInstance) {
        try {
            dbhsmDbInstanceService.insertDbhsmDbInstance(dbhsmDbInstance);
        } catch (ZAYKException | SQLException e) {
            log.info("新增数据库实例失败！" + e.getMessage());
            return AjaxResult2.error("新增数据库实例失败！" + e.getMessage());
        }
        return AjaxResult2.success();
    }

    /**
     * 修改数据库实例
     */
    @RequiresPermissions("dbhsm:dbInstance:edit")
    @Log(title = "数据库实例", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DbhsmDbInstance dbhsmDbInstance) {
        try {
            dbhsmDbInstanceService.updateDbhsmDbInstance(dbhsmDbInstance);
        } catch (Exception e) {
            log.info("修改数据库实例失败！" + e.getMessage());
            return AjaxResult.error("修改数据库实例失败！" + e.getMessage());
        }
        return AjaxResult.success();
    }

    /**
     * 删除数据库实例
     */
    @RequiresPermissions("dbhsm:dbInstance:remove")
    @Log(title = "数据库实例", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult2 remove(@PathVariable Long[] ids) {
        return dbhsmDbInstanceService.deleteDbhsmDbInstanceByIds(ids);
    }

    @ApiOperation(value = "测试连接")
    @PostMapping("/connectionTest")
    public AjaxResult2<Boolean> connectionTest(@RequestBody DbhsmDbInstance instance) {
        return dbhsmDbInstanceService.connectionTest(instance);
    }

    @ApiOperation(value = "测试加密")
    @GetMapping("/connectionEncrypt")
    public AjaxResult2<Boolean> connectionEncrypt(Long id) {
        return AjaxResult2.success(true);
    }


    @ApiOperation(value = "开启代理")
    @GetMapping("/openProxy")
    public AjaxResult2<Boolean> openProxy(Long id) {
        if (null == id) {
            return AjaxResult2.success("id不能为空",false);
        }
        try {
            return dbhsmDbInstanceService.openProxy(id);
        } catch (Exception e) {
            log.error("开启代理失败！数据库实例id:{}", id, e);
            return AjaxResult2.success("未知错误，请联系管理员",false);
        }
    }

    @ApiOperation(value = "测试连接代理")
    @GetMapping("/proxyTest")
    public AjaxResult2<Boolean> proxyTest(Long id) {
        if (null == id) {
            return AjaxResult2.success("id不能为空",false);
        }
        try {
            return dbhsmDbInstanceService.proxyTest(id);
        } catch (Exception e) {
            log.error("测试连接代理失败！数据库实例id:{}", id, e);
            return AjaxResult2.success("未知错误，请联系管理员",false);
        }

    }
}

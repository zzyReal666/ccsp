package com.spms.dbhsm.taskQueue.controller;

import com.ccsp.common.core.web.controller.BaseController;
import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.ccsp.common.core.web.page.TableDataInfo;
import com.ccsp.common.log.annotation.Log;
import com.ccsp.common.log.enums.BusinessType;
import com.spms.common.PageHelperUtil;
import com.spms.dbhsm.encryptcolumns.vo.EncryptColumns;
import com.spms.dbhsm.encryptcolumns.vo.UpEncryptColumnsRequest;
import com.spms.dbhsm.taskQueue.service.DbhsmTaskQueueService;
import com.spms.dbhsm.taskQueue.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-21 11:33 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Api(tags = "任务队列")
@RestController
@RequestMapping("/taskQueue")
public class DbhsmTaskQueueController extends BaseController {

    private final DbhsmTaskQueueService dbhsmTaskQueueService;

    public DbhsmTaskQueueController(DbhsmTaskQueueService dbhsmTaskQueueService) {
        this.dbhsmTaskQueueService = dbhsmTaskQueueService;
    }

    @GetMapping("/list")
    @ApiOperation(value = "数据库加密列表")
    public TableDataInfo list(TaskQueueListRequest request) {
        startPage();
        List<TaskQueueListResponse> list = dbhsmTaskQueueService.list(request);
        return getDataTable(list);
    }

    @PostMapping("/insertEncryptColumnsTask")
    @ApiOperation(value = "新增数据库加密/解密")
    @CrossOrigin(origins="*")
    public AjaxResult insertEncryptColumnsTask(@RequestBody TaskQueueInsertRequest request) {
        return dbhsmTaskQueueService.insertTask(request);
    }

    @PostMapping("/insertDecColumnsOnEnc")
    @ApiOperation(value = "添加至加密队列")
    public AjaxResult insertDecColumnsOnEnc(@RequestBody TaskDecColumnsOnEncRequest request){
        return dbhsmTaskQueueService.insertDecColumnsOnEnc(request);
    }

    @ApiOperation(value = "修改数据库加密列")
    @Log(title = "数据库加密列", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UpEncryptColumnsRequest dbhsmEncryptColumns) {
        return dbhsmTaskQueueService.updateDbhsmEncryptColumns(dbhsmEncryptColumns);
    }


    @ApiOperation(value = "加密/解密队列进度查询")
    @GetMapping("/queryEncOnDecProgress")
    public AjaxResult queryEncryptionProgress(Long taskId) {
        return dbhsmTaskQueueService.queryEncryptionProgress(taskId);
    }

    /*
     * @description 加密列详情
     * @author wzh [zhwang2012@yeah.net]
     * @date 11:29 2024/5/22
     * @param id  任务队列ID
     * @return taskMode  加密/解密类型
     */
    @GetMapping("/taskQueueDetails")
    @ApiOperation(value = "策略配置详情列表")
    public TableDataInfo taskQueueDetails(TaskQueueDetailsRequest request) {
        startPage();
        List<TaskPolicyDetailsResponse> list = dbhsmTaskQueueService.taskQueueDetails(request);
        List<TaskPolicyDetailsResponse> voList = PageHelperUtil.pageHelper(list);
        return getDataTable(list, voList);
    }


   /*
    * @description 策略配置详情使用
    * @author wzh [zhwang2012@yeah.net]
    * @date 16:24 2024/5/28
    * @param dbInstanceId
    * @param dbUserId
    * @param tableName
    * @return {@link TaskQueueListResponse}
    */
    @GetMapping("/queryDbInstanceInfo")
    @ApiOperation(value = "策略配置详情数据库实例信息")
    public AjaxResult2<TaskQueueListResponse> queryDbInstanceInfo(Long dbInstanceId,Long dbUserId, String tableName){
        return dbhsmTaskQueueService.queryDbInstanceInfo(dbInstanceId,dbUserId,tableName);
    }


    @GetMapping("/details")
    @ApiOperation(value = "加密列详情")
    public TableDataInfo details(@RequestParam(value = "taskId") Long taskId,
                                 @RequestParam(value = "detailsMode") String detailsMode) {
        startPage();
        List<EncryptColumns> list = dbhsmTaskQueueService.details(taskId, detailsMode);
        List<EncryptColumns> voList = PageHelperUtil.pageHelper(list);
        return getDataTable(list, voList);
    }


    @GetMapping("/taskQueueNoEncList")
    @ApiOperation(value = "获取加密列详情未加密的列")
    public AjaxResult2<List<EncryptColumns>> taskQueueNoEncList(@RequestParam(value = "taskId") Long taskId,
                                                                @RequestParam(value = "taskMode") String taskMode) {
        return dbhsmTaskQueueService.taskQueueNoEncList(taskId, taskMode);
    }

    @PostMapping(value = "/upOnDownTask")
    @ApiOperation(value = "启动/暂停(加密、解密)队列")
    public AjaxResult upEncryptColumns(@RequestBody TaskQueueRequest request) {
        return dbhsmTaskQueueService.upEncryptColumns(request);
    }

    @DeleteMapping
    @ApiOperation(value = "删除队列")
    public AjaxResult deleteEncryptColumns(Long taskId) {
        return dbhsmTaskQueueService.deleteEncryptColumns(taskId);
    }
}

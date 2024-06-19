package com.spms.dbhsm.taskQueue.service;

import com.ccsp.common.core.web.domain.AjaxResult;
import com.ccsp.common.core.web.domain.AjaxResult2;
import com.spms.dbhsm.encryptcolumns.vo.EncryptColumns;
import com.spms.dbhsm.encryptcolumns.vo.UpEncryptColumnsRequest;
import com.spms.dbhsm.taskQueue.vo.*;

import java.util.List;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-21 11:33 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

public interface DbhsmTaskQueueService {
    AjaxResult insertTask(TaskQueueInsertRequest request);

    List<TaskPolicyDetailsResponse> taskQueueDetails(TaskQueueDetailsRequest request) ;

    List<TaskQueueListResponse> list(TaskQueueListRequest request);

    AjaxResult upEncryptColumns(TaskQueueRequest request) throws Exception;

    AjaxResult updateDbhsmEncryptColumns(UpEncryptColumnsRequest dbhsmEncryptColumns);

    AjaxResult queryEncryptionProgress(Long taskId) ;

    AjaxResult2<List<EncryptColumns>> taskQueueNoEncList(String id, String taskMode);

    AjaxResult deleteEncryptColumns(Long taskId);

    List<EncryptColumns> details(Long taskId, String detailsMode);

    AjaxResult2<TaskQueueListResponse> queryDbInstanceInfo(Long dbInstanceId,Long dbUserId, String dbTableName);

    AjaxResult insertDecColumnsOnEnc(TaskDecColumnsOnEncRequest request);
}

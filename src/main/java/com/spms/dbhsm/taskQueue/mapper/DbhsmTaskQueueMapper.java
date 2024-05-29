package com.spms.dbhsm.taskQueue.mapper;

import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptColumns;
import com.spms.dbhsm.encryptcolumns.vo.EncryptColumns;
import com.spms.dbhsm.taskQueue.domain.DbhsmTaskQueue;
import com.spms.dbhsm.taskQueue.vo.TaskQueueListRequest;
import com.spms.dbhsm.taskQueue.vo.TaskQueueListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* <p> description: TODO the method is used to </p>
*
* <p> Powered by WangZh On 2024-01-16 09:01 </p>
* <p> @author WangZh [zhwang2012@yeah.net] </p>
* <p> @version 1.0 </p>
*/
@Mapper
public interface DbhsmTaskQueueMapper {

    /**
    * Description：根据对象-查询对象列表
    *
    * @param entity 数据实体
    * @return {@link List< DbhsmTaskQueue >}
    * @author WangZh
    * @date 2024/5/21 13:50
    */
    List<DbhsmTaskQueue> queryByEntities(DbhsmTaskQueue entity);

    /**
    * description 根据主键-查询对象
    *
    * @param  taskId 主键id
    * @return  DbhsmTaskQueue 数据实体
    * @author WangZh
    * @date 2024/5/21 13:50
    */
    DbhsmTaskQueue findByPrimaryKey(@Param("taskId")  Long taskId);

    /**
    * Description：根据主键-修改对象
    *
    * @param entity 数据实体
    * @author WangZh
    * @date 2022/8/3 13:50
    */
    void updateRecord(DbhsmTaskQueue entity);


    /**
    * description 新增
    *
    * @param entity 数据实体
    * @author WangZh
    * @date 2024/5/21 13:50
    */
    void insertRecord(DbhsmTaskQueue entity);


    /**
    * Description：根据条件-批量修改数据
    *
    * @param map 数据实体
    * @author WangZh
    * @date 2024/5/21 13:50
    */
	void updateRecords(Map<String,Object> map);

    /**
    * Description：批量新增
    *
    * @param list 数据实体list
    * @author WangZh
    * @date 2024/5/21 13:50
    */
	void insertBatch(List<DbhsmTaskQueue> list);

    //**************************以下方法为开发者补充*********************************/

    List<TaskQueueListResponse> querByEncryptColumnsList(TaskQueueListRequest request);

    List<DbhsmEncryptColumns> selectDbhsmEncryptColumnsDetails(@Param("taskId") Long taskId, @Param("taskMode") String taskMode);

    void deleteRecords(@Param("taskId")  Long taskId);

}

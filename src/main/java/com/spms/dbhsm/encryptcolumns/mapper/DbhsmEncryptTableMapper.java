package com.spms.dbhsm.encryptcolumns.mapper;

import com.spms.dbhsm.encryptcolumns.domain.DbhsmEncryptTable;
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
public interface DbhsmEncryptTableMapper {

    /**
     * Description：根据对象-查询对象列表
     *
     * @param entity 数据实体
     * @return {@link List< DbhsmEncryptTable >}
     * @author WangZh
     * @date 2024/5/21 13:49
     */
    List<DbhsmEncryptTable> queryByEntities(DbhsmEncryptTable entity);

    /**
     * description 根据主键-查询对象
     *
     * @param tableId 主键id
     * @return DbhsmEncryptTable 数据实体
     * @author WangZh
     * @date 2024/5/21 13:50
     */
    DbhsmEncryptTable findByPrimaryKey(@Param("tableId") String tableId);

    /**
     * Description：根据主键-修改对象
     *
     * @param entity 数据实体
     * @author WangZh
     * @date 2024/5/21 13:50
     */
    void updateRecord(DbhsmEncryptTable entity);


    /**
     * description 新增
     *
     * @param entity 数据实体
     * @author WangZh
     * @date 2021/4/1 9:00
     */
    void insertRecord(DbhsmEncryptTable entity);


    /**
     * Description：根据条件-批量修改数据
     *
     * @param map 数据实体
     * @author WangZh
     * @date 2024/5/21 13:52
     */
    void updateRecords(Map<String, Object> map);

    /**
     * Description：批量新增
     *
     * @param list 数据实体list
     * @author WangZh
     * @date 2024/5/21 13:52
     */
    void insertBatch(List<DbhsmEncryptTable> list);
    //**************************以下方法为开发者补充*********************************/

    void deleteRecords(@Param("tableId") String tableId);

}

package com.spms.common;

import com.ccsp.common.core.domain.R;
import com.ccsp.common.core.utils.SpringUtils;
import com.ccsp.system.api.hsmSvsTsaApi.SpmsDevBaseDataService;
import com.ccsp.system.api.hsmSvsTsaApi.domain.DevBaseData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;


/**
 * JSON数据处理类
 */
@Slf4j
@Data
public class JSONDataUtil {
    @Resource
    private static SpmsDevBaseDataService devBaseDataService;


    /**
     * 从数据库获取某个数据的值
     */
    public static String getSysDataToDB(String key) {

        if (devBaseDataService == null) {
            devBaseDataService = SpringUtils.getBean(SpmsDevBaseDataService.class);
        }

        R<DevBaseData> devBaseDataR = devBaseDataService.selectBaseDataByKey(key);
        if (devBaseDataR.getData() != null) {
            return devBaseDataR.getData().getDataValue();
        }
        return null;
    }
    /**
     * 往数据库设置某个数据的值
     */
    public static int setSysDataToDB(Object keyObj, Object resultBoolean) {
        if (devBaseDataService == null) {
            devBaseDataService = SpringUtils.getBean(SpmsDevBaseDataService.class);
        }

        R<DevBaseData> devBaseDataR = devBaseDataService.selectBaseDataByKey(keyObj.toString());
        if (devBaseDataR.getData() == null) {
            DevBaseData baseData = new DevBaseData();
            baseData.setDataKey(keyObj.toString());
            baseData.setDataValue(resultBoolean.toString());
            baseData.setStatus("0");
            R<Integer> integerR = devBaseDataService.insertSysBaseData(baseData);
            if(integerR != null){
                return integerR.getData();
            }else{
                log.error("修改(没有新增)配置文件失败：keyObj:{},resultBoolean:{}",keyObj,resultBoolean);
                return 0;
            }
        } else {
            devBaseDataR.getData().setDataValue(resultBoolean.toString());
            R<Integer> integerR = devBaseDataService.updateSysBaseData(devBaseDataR.getData());
            if(integerR != null){
                return integerR.getData();
            }else{
                log.error("修改配置文件失败：keyObj:{},resultBoolean:{}",keyObj,resultBoolean);
                return 0;
            }
        }
    }

}

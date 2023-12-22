package com.spms.common;

import com.alibaba.cloud.commons.io.FileUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.ccsp.common.core.domain.R;
import com.ccsp.common.core.utils.SpringUtils;
import com.ccsp.common.security.utils.DictUtils;
import com.ccsp.system.api.hsmSvsTsaApi.SpmsDevBaseDataService;
import com.ccsp.system.api.hsmSvsTsaApi.domain.DevBaseData;
import com.ccsp.system.api.systemApi.domain.SysDictData;
import com.spms.common.constant.DbConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * JSON数据处理类
 */
@Slf4j
@Data
public class JSONDataUtil {
    @Resource
    private static SpmsDevBaseDataService devBaseDataService;
    private static long reConnErrNum = 0;

    /**
     * 从数据库获取某个数据的值
     */

    public static String getSysDataToDB(String key) {

        if (devBaseDataService == null) {
            devBaseDataService = SpringUtils.getBean(SpmsDevBaseDataService.class);
        }

        try{
            R<DevBaseData> devBaseDataR = devBaseDataService.selectBaseDataByKey(key);
            reConnErrNum = 0;
            if (devBaseDataR.getData() != null) {
                return devBaseDataR.getData().getDataValue();
            }
        }catch (Exception e){
            e.printStackTrace();
            if (reConnErrNum < 5){
                log.info("系统模块连接异常，5秒后进行重连");
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException ex) {
                }
                ++reConnErrNum;
                getSysDataToDB(key);
            }
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

    /**
     * secretKeyType 密钥类型取值：CardKeyECC CardKeyRSA CardKeySM2 CardKeySYK
     * 根据密钥类型从配置文件sysdata中获取密钥生成方式
     */
    public static int getSecretKeyGenerateType(String secretKeyType)  {
        try {
            Object type = JSONDataUtil.getSysData(secretKeyType);
            if (type == null) {
                type = 1;
            }
            return Integer.parseInt(type.toString());
        }catch (Exception e){
            return 1;
        }

    }
    /**
     * 获得SysData某个数据的值
     */
    public static Object getSysData(String key) throws IOException {
        Object rtnString = null;
        Map<String, Object> rtnMap = null;
        File ff = getSourceFile(DbConstants.SysData);
        rtnMap = readJsonDataWithoutRequest(ff);

        if (null != rtnMap) {
            Object object = rtnMap.get(DbConstants.SysData);
            if (null != object && object instanceof Map) {
                rtnString = ((Map) object).get(key);
            }
        }
        return rtnString;
    }
    public static File getSourceFile(String filename) {
        String path = "";
        File file1 = null;
        try {
            List<SysDictData> svsDictData = DictUtils.getDictCache(DbConstants.SPMS_CONFIG_FILE_PATH);
            String certPath = "";
            for (SysDictData dictData : svsDictData) {
                if (DbConstants.jsonPath.equals(dictData.getDictLabel())) {
                    path = dictData.getDictValue();
                    File fD1 = new File(path);
                    if (!fD1.isDirectory()) {
                        fD1.mkdirs();
                    }
                    file1 = new File(path + filename + ".json");
                    if (!file1.isFile()) {
                        try {
                            file1.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            file1 = new File("/opt/config_file/jsonfile/SysData.json");
        }

        return file1;
    }
    public static Map<String, Object> readJsonDataWithoutRequest(File sourceFile) throws IOException {
        String jsonString = FileUtils.readFileToString(sourceFile);
        JSONObject jsonObject1 = JSONObject.parseObject(jsonString.trim());
        //排序开始
        String str = JSONObject.toJSONString(jsonObject1, SerializerFeature.MapSortField, SerializerFeature.PrettyFormat);
        JSONObject jsonObject = JSONObject.parseObject(str.trim());
        //排序结束
        Map<String, Object> result = toMap(jsonObject);
        return result;

    }
    /**
     * JSONObject转为map
     *
     * @param object json对象
     * @return 转化后的Map
     */
    public static Map<String, Object> toMap(JSONObject object) {
        Map<String, Object> map = new HashMap<String, Object>();

        for (Iterator<String> it = object.keySet().iterator(); it.hasNext(); ) {
            String key = (String) it.next();
            Object value;
            try {
                value = object.get(key);
                if (value instanceof JSONArray) {
                    value = toList((JSONArray) value);
                } else if (value instanceof JSONObject) {
                    value = toMap((JSONObject) value);
                }
                map.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return map;
    }

    /**
     * JSONArray转为List
     *
     * @param array json数组
     * @return 转化后的List
     */
    public static List<Object> toList(JSONArray array) {
        List<Object> list = new ArrayList<Object>();

        for (int i = 0; i < array.size(); i++) {
            Object value;
            try {
                value = array.get(i);
                if (value instanceof JSONArray) {
                    value = toList((JSONArray) value);
                } else if (value instanceof JSONObject) {
                    value = toMap((JSONObject) value);
                }
                list.add(value);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return list;
    }
}

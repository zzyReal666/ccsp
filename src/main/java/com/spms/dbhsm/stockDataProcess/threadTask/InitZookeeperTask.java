package com.spms.dbhsm.stockDataProcess.threadTask;

import cn.hutool.core.io.FileUtil;
import com.spms.common.Template.FreeMarkerTemplateEngine;
import com.spms.common.Template.TemplateEngine;
import com.spms.common.Template.TemplateEngineException;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * @author zzypersonally@gmail.com
 * @description 初始化Zookeeper线程
 * @since 2024/5/20 10:43
 */
@AllArgsConstructor
@Getter
@Setter
@ToString
public class InitZookeeperTask extends Thread {


    /**
     * 数据库信息
     */
    private DatabaseDTO databaseDTO;


    @Override
    public void run() {

        //生成配置文件
        TemplateEngine templateEngine = new FreeMarkerTemplateEngine();
        String filePath = "initConfig.ftl";
        try {
            templateEngine.setTemplateFromFile(filePath);

            //数据模型
            templateEngine.setDataModel(getDataModelMap());
            String result = templateEngine.process();

            //result 写入文件 /Users/zhangzhongyuan/config/ftl/config.yaml
            FileUtil.writeUtf8String(result, "/Users/zhangzhongyuan/config/ftl/config.yaml");
        } catch (TemplateEngineException e) {
            throw new RuntimeException(e);
        }
        //调用linux命令启动一个空的项目，使用生成的配置文件 用来初始化
        try {
            Runtime.getRuntime().exec("sh /Users/zhangzhongyuan/config/ftl/start.sh");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取数据模型
     *
     * @return 数据模型
     */
    private Map<String, Object> getDataModelMap() {
        Map<String, Object> dataModel = new HashMap<>();

        // mode配置
        dataModel.put("modeType", "Cluster");
        dataModel.put("repositoryType", "ZooKeeper");
        dataModel.put("namespace", databaseDTO.getId());
        dataModel.put("serverLists", "101.42.19.44:12181");
        dataModel.put("operationTimeoutMilliseconds", 50000);

        // 数据源配置
        dataModel.put("dataSourceClassName", "com.alibaba.druid.pool.DruidDataSource");
        dataModel.put("driverClassName", "com.mysql.cj.jdbc.Driver");
        dataModel.put("urlKey", "url");
        dataModel.put("url", databaseDTO.getConnectUrl());
        dataModel.put("username", databaseDTO.getServiceUser());
        dataModel.put("password", databaseDTO.getServicePassword());
        dataModel.put("maxPoolSize", 10);

        // 其他配置
        dataModel.put("sqlShow", true);
        return dataModel;
    }
}

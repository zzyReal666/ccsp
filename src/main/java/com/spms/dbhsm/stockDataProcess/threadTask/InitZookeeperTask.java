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
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
@Slf4j
public class InitZookeeperTask extends Thread {

    //数据库信息
    private DatabaseDTO databaseDTO;

    //模板引擎
    private static final TemplateEngine TEMPLATE_ENGINE = new FreeMarkerTemplateEngine();


    //生成的配置文件、init-zookeeper jar start.sh 脚本的路径
    private static final String ROOT_PATH = "/Users/zhangzhongyuan/config/ftl/";


    @Override
    public void run() {


        //生成<databaseId>.properties
        generateProperties();

        //生成<databaseId>.yaml
        generateConfig();

        //运行项目连接数据库，使用指定的配置文件
//        runInitZookeeper();
    }

    private void generateProperties() {
        String filePath = "properties.ftl";
        try {
            TEMPLATE_ENGINE.setTemplateFromFile(filePath);
            //数据模型
            TEMPLATE_ENGINE.setDataModel(new HashMap<String, String>() {
                {
                    put("databaseId", String.valueOf(databaseDTO.getId()));
                }
            });
            //result 写入文件 /Users/zhangzhongyuan/config/ftl
            FileUtil.writeUtf8String(TEMPLATE_ENGINE.process(), ROOT_PATH + databaseDTO.getId() + "/" + "application.properties");
        } catch (TemplateEngineException e) {
            throw new RuntimeException(e);
        }
    }

    //执行脚本 启动 空项目 init zookeeper
    private void runInitZookeeper() {
        try {

            // 创建 ProcessBuilder 对象，并设置要执行的命令（包括脚本名称和参数）
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", ROOT_PATH + databaseDTO.getId() + "run_init_zookeeper.sh", "mysql", ROOT_PATH + databaseDTO.getId() + "application.properties");

            // 启动进程
            Process process = processBuilder.start();

            // 读取进程的输出流
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("init-zookeeper logs: {}", line);

            }

            // 等待进程执行完成
            int exitCode = process.waitFor();

            // 打印进程执行结果
            log.info("init-zookeeper exit code: {}", exitCode);
        } catch (IOException | InterruptedException ignore) {

        }
    }


    private void generateConfig() {
        //生成配置文件
        String filePath = "initConfig.ftl";
        try {
            TEMPLATE_ENGINE.setTemplateFromFile(filePath);
            //数据模型
            TEMPLATE_ENGINE.setDataModel(getYamlDataModel());
            //result 写入文件 /Users/zhangzhongyuan/config/ftl
            FileUtil.writeUtf8String(TEMPLATE_ENGINE.process(), ROOT_PATH + databaseDTO.getId() + "/" + "config.yaml");
        } catch (TemplateEngineException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取数据模型
     *
     * @return 数据模型
     */
    private Map<String, String> getYamlDataModel() {
        Map<String, String> dataModel = new HashMap<>();

        // mode配置
        dataModel.put("modeType", "Cluster");
        dataModel.put("repositoryType", "ZooKeeper");
        dataModel.put("namespace", String.valueOf(databaseDTO.getId()));
        dataModel.put("serverLists", "101.42.19.44:12181");
        dataModel.put("operationTimeoutMilliseconds", "50000");

        // 数据源配置
        dataModel.put("dataSourceClassName", "com.alibaba.druid.pool.DruidDataSource");
        dataModel.put("driverClassName", "com.mysql.cj.jdbc.Driver");
        dataModel.put("urlKey", "url");
        dataModel.put("url", databaseDTO.getConnectUrl());
        dataModel.put("username", databaseDTO.getServiceUser());
        dataModel.put("password", databaseDTO.getServicePassword());
        dataModel.put("maxPoolSize", "10");

        return dataModel;
    }
}

package com.spms.dbhsm.stockDataProcess.threadTask;

import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
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
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), "templates");
        try {

            // 加载模板文件
            Template template = cfg.getTemplate("config_template.ftl");

            // 创建数据模型
            Map<String, Object> dataModel = getDataModelMap();

            // 合并模板和数据模型
            StringWriter out = new StringWriter();
            template.process(dataModel, out);

            // 写入到文件
            try (FileWriter fileWriter = new FileWriter("/Users/zhangzhongyuan/config/ftl/config.yaml")) {
                template.process(dataModel, fileWriter);
            }
        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e);
        }

        //调用linux命令启动一个空的项目，使用生成的配置文件
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
        dataModel.put("serverLists", "localhost:12181");
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

package com.spms.dbhsm.stockDataProcess.threadTask;

import com.spms.common.Template.FreeMarkerTemplateEngine;
import com.spms.common.Template.TemplateEngine;
import com.spms.common.Template.TemplateEngineException;
import com.spms.common.enums.PathEnum;
import com.spms.common.zookeeperUtils.ZookeeperUtils;
import com.spms.dbhsm.stockDataProcess.domain.dto.ColumnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.TableDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author zzypersonally@gmail.com
 * @description 更新zookeeper线程
 * @since 2024/5/20 15:03
 */
@AllArgsConstructor
@Getter
@Setter
@ToString
public class UpdateZookeeperTask extends Thread {

    private static final Logger log = LoggerFactory.getLogger(UpdateZookeeperTask.class);
    private DatabaseDTO databaseDTO;
    private boolean operateType;

    @Override
    public void run() {
        if (operateType) {
            addConfig();
        } else {
            deleteConfig();
        }
        ZookeeperUtils.close();
    }

    private void deleteConfig() {
        TableDTO tableDTO = databaseDTO.getTableDTOList().get(0);
        tableDTO.getColumnDTOList().forEach(col -> {
            //删除加密器
            deleteEncryptor(col);
            //修改加密规则
            updateRules(col);
        });
    }

    private void updateRules(ColumnDTO col) {
        String path = PathEnum.RULE.getValue().replace("${namespace}", databaseDTO.getDatabaseIp() + ":" + databaseDTO.getDatabasePort()).replace("${database}", databaseDTO.getDatabaseName()).replace("${tableName}", databaseDTO.getTableDTOList().get(0).getTableName());
        String fullPath = path + PathEnum.VERSION.getValue();
        String pattern = String.format("(?m)^\\s{2}%s:\\s*\\r?\\n" +               // 匹配字段名称行，例如 "  NAME:"
                        "\\s{4}cipher:\\s*\\r?\\n" +                // 匹配 "    cipher:" 行
                        "\\s{6}encryptorName:\\s*\\S+\\s*\\r?\\n" + // 匹配 "      encryptorName: STUDENT_NAME_encryptor" 行
                        "\\s{6}name:\\s*%s\\s*\\r?\\n" +            // 匹配 "      name: NAME" 行
                        "\\s{4}name:\\s*%s\\s*$",                   // 匹配 "    name: NAME" 行
                Pattern.quote(col.getColumnName()), Pattern.quote(col.getColumnName()), Pattern.quote(col.getColumnName()), Pattern.quote(col.getColumnName()));
        String config = ZookeeperUtils.replace(fullPath, pattern, "");
        log.info("修改加密策略,删除columnName:{}策略,修改后策略为:{}", col.getColumnName(), config);
        String[] lines = config.split("\n");
        if (lines.length == 2) {
            ZookeeperUtils.deleteNode(path);
            log.info("rows: " + lines.length + "  config为空，删除节点");
        } else {
            log.info("rows: " + lines.length + "  config不为空，更新节点");
        }
    }

    private void deleteEncryptor(ColumnDTO col) {
        String path = PathEnum.ENCRYPTOR.getValue().replace("${namespace}", databaseDTO.getDatabaseIp() + ":" + databaseDTO.getDatabasePort()).replace("${database}", databaseDTO.getDatabaseName()).replace("${encryptorName}", databaseDTO.getTableDTOList().get(0).getTableName() + "_" + col.getColumnName() + "_encryptor");
        ZookeeperUtils.deleteNode(path);
    }

    private String deleteFromOldConfig(String pattern, String oldConfig) {
        return oldConfig.replaceAll(pattern, "");
    }

    private void addConfig() {
        //添加加密器
        databaseDTO.getTableDTOList().get(0).getColumnDTOList().forEach(this::addEncryptor);
        //添加加密规则
        addEncryptRules();
    }

    private void addEncryptRules() {
        //基础路径
        String path = PathEnum.RULE.getValue().replace("${namespace}", databaseDTO.getDatabaseIp() + ":" + databaseDTO.getDatabasePort()).replace("${database}", databaseDTO.getDatabaseName()).replace("${tableName}", databaseDTO.getTableDTOList().get(0).getTableName());

        //新增活跃版本号 active_version:0
        ZookeeperUtils.updateNode("0", path + PathEnum.ACTIVE_VERSION.getValue());
        //新增加密规则  versions/0  目录下
        TemplateEngine templateEngine = new FreeMarkerTemplateEngine();
        String filePath = "encryptRules.ftl";
        try {
            //模版路径
            templateEngine.setTemplateFromFile(filePath);
            //数据模型
            Map<String, Object> dataModel = getRulesDataModel();
            //替换模版
            templateEngine.setDataModel(dataModel);
            //写入zk
            ZookeeperUtils.updateNode(templateEngine.process(), path + PathEnum.VERSION.getValue());
        } catch (TemplateEngineException e) {
            log.error("模版渲染异常", e);
            throw new RuntimeException("模版渲染异常", e);
        }
    }

    private void addEncryptor(ColumnDTO col) {
        //基础路径
        String path = PathEnum.ENCRYPTOR.getValue().replace("${namespace}", databaseDTO.getDatabaseIp() + ":" + databaseDTO.getDatabasePort()).replace("${database}", databaseDTO.getDatabaseName()).replace("${encryptorName}", databaseDTO.getTableDTOList().get(0).getTableName() + "_" + col.getColumnName() + "_encryptor");
        //新增活跃版本号
        ZookeeperUtils.updateNode("0", path + PathEnum.ACTIVE_VERSION.getValue());
        //新增加密器  versions/0  目录下
        TemplateEngine templateEngine = new FreeMarkerTemplateEngine();
        String filePath = "encryptor.ftl";
        try {
            //模版路径
            templateEngine.setTemplateFromFile(filePath);
            //数据模型 type key—index
            Map<String, Object> dataModel = getEncryptorDataModel(col);
            //替换模版
            templateEngine.setDataModel(dataModel);
            // 写入zk
            ZookeeperUtils.updateNode(templateEngine.process(), path + PathEnum.VERSION.getValue());
        } catch (Exception ignore) {
        }
    }

    /**
     * 加密器数据模型
     */
    private static Map<String, Object> getEncryptorDataModel(ColumnDTO col) {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("type", col.getEncryptAlgorithm());
        dataModel.put("keyIndex", col.getEncryptKeyIndex());
        //数据模型 props
        List<Map<String, String>> props = new ArrayList<>();
        col.getProperty().forEach((k, v) -> {
            Map<String, String> map = new HashMap<>();
            map.put("key", k);
            map.put("value", v);
            props.add(map);
        });
        dataModel.put("props", props);
        return dataModel;
    }

    /**
     * 加密规则数据模型
     */
    private Map<String, Object> getRulesDataModel() {
        //数据模型-tableName
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("tableName", databaseDTO.getTableDTOList().get(0).getTableName());
        //数据模型-columns列表
        List<Map<String, Object>> columns = new ArrayList<>();
        databaseDTO.getTableDTOList().get(0).getColumnDTOList().forEach(column -> {
            Map<String, Object> columnMap = new HashMap<>();
            columnMap.put("name", column.getColumnName());
            //加密
            Map<String, Object> cipher = new HashMap<>();
            cipher.put("encryptorName", databaseDTO.getTableDTOList().get(0).getTableName() + "_" + column.getColumnName() + "_encryptor");
            cipher.put("name", column.getColumnName());
            columnMap.put("cipher", cipher);
            columns.add(columnMap);
        });
        dataModel.put("columns", columns);
        return dataModel;
    }
}

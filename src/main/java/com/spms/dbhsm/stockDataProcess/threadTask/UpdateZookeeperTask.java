package com.spms.dbhsm.stockDataProcess.threadTask;

import com.spms.common.Template.FreeMarkerTemplateEngine;
import com.spms.common.Template.TemplateEngine;
import com.spms.common.Template.TemplateEngineException;
import com.spms.common.enums.PathEnum;
import com.spms.common.zookeeperUtils.ZookeeperUtils;
import com.spms.dbhsm.stockDataProcess.domain.dto.ColumnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private DatabaseDTO databaseDTO;

    @Override
    public void run() {
        databaseDTO.getTableDTOList().get(0).getColumnDTOList().forEach(col -> {
            //添加加密器
            addEncryptor(col);
            //添加加密规则
            addEncryptRules(col);
        });
        ZookeeperUtils.close();
    }

    private void addEncryptRules(ColumnDTO col) {
        //基础路径
        String path = PathEnum.RULE.getValue()
                .replace("${namespace}", databaseDTO.getDatabaseIp() + ":" + databaseDTO.getDatabasePort() + "/" + databaseDTO.getDatabaseName())
                .replace("${tableName}", databaseDTO.getTableDTOList().get(0).getTableName());

        //新增活跃版本号 active_version:0
        ZookeeperUtils.updateNode("0", path + PathEnum.ACTIVE_VERSION.getValue());

        //新增加密规则  versions/0  目录下
        TemplateEngine templateEngine = new FreeMarkerTemplateEngine();
        String filePath = "encryptRules.ftl";
        try {
            templateEngine.setTemplateFromFile(filePath);

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
                cipher.put("encryptorName", column.getColumnName() + "_encryptor");
                cipher.put("name", column.getColumnName());
                columnMap.put("cipher", cipher);

//                //todo 模糊查询和辅助查询
//                if (column.getAssistedQueryProps() != null) {
//                    Map<String, Object> assistedQuery = new HashMap<>();
//                    assistedQuery.put("encryptorName", column.getAssistedQueryProps().get("encryptor"));
//                    assistedQuery.put("name", column.getAssistedQueryProps().get("columnName"));
//                    columnMap.put("assistedQuery", assistedQuery);
//                }
//                if (column.getLikeQueryProps() != null) {
//                    Map<String, Object> likeQuery = new HashMap<>();
//                    likeQuery.put("encryptorName", column.getLikeQueryProps().get("alg"));
//                    likeQuery.put("name", column.getLikeQueryProps().get("columnName"));
//                    columnMap.put("likeQuery", likeQuery);
//                }

                columns.add(columnMap);
            });
            dataModel.put("columns", columns);

            //替换模版
            templateEngine.setDataModel(dataModel);

            //写入zk
            ZookeeperUtils.updateNode(templateEngine.process(), path + PathEnum.VERSION.getValue());
        } catch (TemplateEngineException ignore) {
        }


    }

    private void addEncryptor(ColumnDTO col) {
        //基础路径
        String path = PathEnum.ENCRYPTOR.getValue()
                .replace("${namespace}", databaseDTO.getDatabaseIp() + ":" + databaseDTO.getDatabasePort() + "/" + databaseDTO.getDatabaseName())
                .replace("${encryptorName}", col.getColumnName() + "_encryptor");

        //新增活跃版本号
        ZookeeperUtils.updateNode("0", path + PathEnum.ACTIVE_VERSION.getValue());

        //新增加密器  versions/0  目录下
        TemplateEngine templateEngine = new FreeMarkerTemplateEngine();
        String filePath = "encryptor.ftl";
        try {
            //模版路径
            templateEngine.setTemplateFromFile(filePath);

            //数据模型 type key—index
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

            //替换模版
            templateEngine.setDataModel(dataModel);

            // 写入zk
            ZookeeperUtils.updateNode(templateEngine.process(), path + PathEnum.VERSION.getValue());
        } catch (Exception ignore) {
        }
    }
}

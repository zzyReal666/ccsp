package com.spms.common.Template;

import com.spms.common.enums.PathEnum;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateEngineTest {

    @Test
    public void testTemlate() {
        // 创建模板引擎实例
        TemplateEngine templateEngine = new FreeMarkerTemplateEngine();

        // 设置模板内容
        String filePath = "encryptor.ftl";
        try {
            templateEngine.setTemplateFromFile(filePath);


            //数据模型
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("type", "SM4-ECB-padding");
            dataModel.put("keyIndex", "keyIndex1");

            List<Map<String, String>> props = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
                Map<String, String> map = new HashMap<>();
                map.put("key", "key" + i);
                map.put("value", "value" + i);
                props.add(map);
            }
            dataModel.put("props", props);

            templateEngine.setDataModel(dataModel);

            String process = templateEngine.process();

            System.out.println(process);



        } catch (TemplateEngineException e) {
            e.printStackTrace();
        }




    }
}
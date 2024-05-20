package com.spms.common.Template;

/**
 * @author zzypersonally@gmail.com
 * @description 模版引擎 用于加载模版
 * @since 2024/5/20 15:53
 */
public interface TemplateEngine {
    void setTemplate(String templateContent) throws TemplateEngineException;

    void setTemplateFromFile(String filePath) throws TemplateEngineException;

    void setDataModel(Object dataModel);

    String process() throws TemplateEngineException;
}

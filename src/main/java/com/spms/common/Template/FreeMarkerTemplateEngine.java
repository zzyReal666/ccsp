package com.spms.common.Template;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/5/20 15:53
 */
public class FreeMarkerTemplateEngine implements TemplateEngine {
    private final Configuration configuration;
    private Template template;
    private Object dataModel;

    public FreeMarkerTemplateEngine() {
        configuration = new Configuration(Configuration.VERSION_2_3_31);
        // 设置模板加载路径，从类路径加载
        configuration.setClassLoaderForTemplateLoading(FreeMarkerTemplateEngine.class.getClassLoader(), "templates");
    }

    @Override
    public void setTemplate(String templateContent) throws TemplateEngineException {
        try {
            template = new Template("template", templateContent, configuration);
        } catch (IOException e) {
            throw new TemplateEngineException("Failed to set template from content", e);
        }
    }

    @Override
    public void setTemplateFromFile(String filePath) throws TemplateEngineException {
        try {
            template = configuration.getTemplate(filePath);
        } catch (IOException e) {
            throw new TemplateEngineException("Failed to set template from file", e);
        }
    }

    @Override
    public void setDataModel(Object dataModel) {
        this.dataModel = dataModel;
    }

    @Override
    public String process() throws TemplateEngineException {
        try {
            StringWriter out = new StringWriter();
            template.process(dataModel, out);
            return out.toString();
        } catch (IOException | TemplateException e) {
            throw new TemplateEngineException("Failed to process template", e);
        }
    }
}

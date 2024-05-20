package com.spms.common.Template;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/5/20 15:54
 */
public class TemplateEngineException extends Exception{
    public TemplateEngineException(String message) {
        super(message);
    }

    public TemplateEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}

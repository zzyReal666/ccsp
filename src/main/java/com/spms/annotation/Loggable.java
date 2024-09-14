package com.spms.annotation;

import org.slf4j.event.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zzypersonally@gmail.com
 * @description
 * @since 2024/9/14 14:16
 */
@Target(ElementType.METHOD)  // 注解用于方法
@Retention(RetentionPolicy.RUNTIME)  // 注解在运行时有效
public @interface Loggable {

    // 设置日志级别
    Level logLevel() default Level.DEBUG;  // 默认级别为 DEBUG
}
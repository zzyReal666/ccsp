package com.spms.config.aspect;

import com.spms.annotation.Loggable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author zzypersonally@gmail.com
 * @description 日志切面类
 * @since 2024/9/14 13:56
 */
@Aspect
@Component
public class LoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    // 限制集合打印的最大条目数
    private static final int MAX_COLLECTION_PRINT_SIZE = 10;

    @Around("@annotation(com.spms.annotation.Loggable)")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Loggable loggable = method.getAnnotation(Loggable.class);

        Level logLevel = loggable.logLevel();
        String fullClassName = signature.getDeclaringTypeName();
        String methodName = method.getName();
        Object[] methodArgs = joinPoint.getArgs();  // 获取方法参数

        // 记录进入方法的日志，包括类名、方法名和参数（对集合参数进行特殊处理）
        logMessage(logLevel, "Entering method: " + fullClassName + "." + methodName +
                " with arguments: " + formatArguments(methodArgs));

        long startTime = System.currentTimeMillis();  // 记录开始时间
        Object result;
        try {
            result = joinPoint.proceed();  // 执行目标方法
        } catch (Throwable throwable) {
            logMessage(Level.ERROR, "Exception in method: " + fullClassName + "." + methodName);
            throw throwable;
        }
        long totalTime = System.currentTimeMillis() - startTime;  // 计算执行时间

        // 记录退出方法的日志，包括类名、方法名、返回值和执行时间
        logMessage(logLevel, "Exiting method: " + fullClassName + "." + methodName +
                " with result: " + result + " (Execution time: " + totalTime + " ms)");

        return result;
    }

    /**
     * 格式化参数，处理集合类型和普通参数
     */
    private String formatArguments(Object[] args) {
        StringBuilder formattedArgs = new StringBuilder();
        for (Object arg : args) {
            if (arg instanceof Collection) {
                Collection<?> collection = (Collection<?>) arg;
                formattedArgs.append("Collection(size=").append(collection.size()).append(", items=");
                formattedArgs.append(formatCollection(collection)).append("), ");
            } else if (arg instanceof Object[]) {
                Object[] array = (Object[]) arg;
                formattedArgs.append("Array(size=").append(array.length).append(", items=");
                formattedArgs.append(Arrays.toString(Arrays.copyOf(array, Math.min(array.length, MAX_COLLECTION_PRINT_SIZE))));
                formattedArgs.append("), ");
            } else {
                formattedArgs.append(arg).append(", ");
            }
        }
        return formattedArgs.length() > 0 ? formattedArgs.substring(0, formattedArgs.length() - 2) : "";
    }

    /**
     * 格式化集合，限制显示条目数
     */
    private String formatCollection(Collection<?> collection) {
        return collection.stream()
                .limit(MAX_COLLECTION_PRINT_SIZE)
                .collect(Collectors.toList())
                .toString();
    }

    private void logMessage(Level level, String message) {
        switch (level) {
            case TRACE:
                logger.trace(message);
                break;
            case INFO:
                logger.info(message);
                break;
            case WARN:
                logger.warn(message);
                break;
            case ERROR:
                logger.error(message);
                break;
            default:
                logger.debug(message);
                break;
        }
    }
}
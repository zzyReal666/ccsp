package com.spms.common.task;

import org.apache.poi.ss.formula.functions.T;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p> description: 创建定时任务 </p>
 *
 * <p> Powered by wzh On 2024-03-20 18:05 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

public class DynamicScheduledTask {

    private ScheduledExecutorService scheduler;

    public DynamicScheduledTask(Integer count) {
        this.scheduler = Executors.newScheduledThreadPool(count);
    }

    public void scheduleTask(Runnable task, long initialDelay, long period, TimeUnit unit) {
        // 取消之前可能存在的相同任务（如果有的话）
        scheduler.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public void cancelTask() {
        // 取消所有任务（如果需要的话）
        scheduler.shutdown();
    }


}

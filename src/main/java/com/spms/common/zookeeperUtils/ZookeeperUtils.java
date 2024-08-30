package com.spms.common.zookeeperUtils;

import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author zzypersonally@gmail.com
 * @description zookeeper工具
 * @since 2024/5/20 14:50
 */
@Component
@Slf4j
public class ZookeeperUtils {

    public static CuratorFramework client;

    @Value("${encrypt.zookeeper.url:localhost:2181}")
    private static String defaultUrl;
    private static int defaultSessionTimeoutMs = 10 * 1000;
    private static int defaultConnectionTimeoutMs = 2 * 1000;
    private static int defaultRetryTime = 1000;


    //region //================初始化 close================
    public static void init(String url, int sessionTimeoutMs, int connectionTimeoutMs, int retryTime) {
        defaultUrl = StringUtil.isNullOrEmpty(url) ? defaultUrl : url;
        defaultSessionTimeoutMs = sessionTimeoutMs == 0 ? defaultSessionTimeoutMs : sessionTimeoutMs;
        defaultConnectionTimeoutMs = connectionTimeoutMs == 0 ? defaultConnectionTimeoutMs : connectionTimeoutMs;
        defaultRetryTime = retryTime == 0 ? defaultRetryTime : retryTime;
        client = CuratorFrameworkFactory.builder().connectString(defaultUrl).sessionTimeoutMs(defaultSessionTimeoutMs).connectionTimeoutMs(defaultConnectionTimeoutMs).retryPolicy(new RetryOneTime(defaultRetryTime)).build();
        client.start();
    }


    public static void init(String url) {
        init(url, 5000, 5000, 3);
    }

    public static void init(String url, int sessionTimeoutMs) {
        init(url, sessionTimeoutMs, 0, 0);
    }

    public static void init(String url, int sessionTimeoutMs, int connectionTimeoutMs) {
        init(url, sessionTimeoutMs, connectionTimeoutMs, 0);
    }

    //close
    public static void close() {
        if (client != null) {
            client.close();
        }
    }

    //endregion

    public static void updateNode(String data, String nodePath, String url) {
        init(url);
        //不存在则创建节点并且添加数据 存在则直接添加数据
        try {
            Stat stat = client.checkExists().forPath(nodePath);
            if (stat == null) {
                client.create().creatingParentsIfNeeded().forPath(nodePath, data.getBytes());
            } else {
                client.setData().forPath(nodePath, data.getBytes());
            }
        } catch (Exception e) {
            log.error("updateNode error, nodePath:{}, url:{},data:{}", nodePath, url, data);
            throw new RuntimeException("zookeeper updateNode error", e);
        }
    }

    public static void updateNode(String data, String nodePath) {
        updateNode(data, nodePath, defaultUrl);
    }


    public static void deleteNode(String nodePath) {
        deleteNode(nodePath, defaultUrl);
    }

    public static void deleteNode(String nodePath, String url) {
        try {
            init(url);
            Stat stat = client.checkExists().forPath(nodePath);
            if (stat != null) {
                client.delete().deletingChildrenIfNeeded().forPath(nodePath);
            }
        } catch (Exception e) {
            log.error("deleteNode error, nodePath:{}, url:{}", nodePath, url);
            throw new RuntimeException("zookeeper deleteNode error", e);
        }
    }


    //是否存在该节点
    public static boolean existsNode(String nodePath, String url) {
        try {
            if (client == null) {
                init(url);
            }
            Stat stat = client.checkExists().forPath(nodePath);
            return stat != null;
        } catch (Exception e) {
            throw new RuntimeException("zookeeper existsNode error", e);
        }
    }
}

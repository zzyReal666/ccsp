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
@Slf4j
@Component
public class ZookeeperUtils {

    public static CuratorFramework client;

    @Value("${encrypt.zookeeper.url}")
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
        client = CuratorFrameworkFactory.builder().connectString(defaultUrl)      //zookeeper地址
                .sessionTimeoutMs(defaultSessionTimeoutMs) //会话超时时间
                .connectionTimeoutMs(defaultConnectionTimeoutMs) //连接超时时间
                .retryPolicy(new RetryOneTime(defaultRetryTime))    //重试策略
                .build();
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
            client = null;
        }
    }

    //endregion

    /**
     * 更新 节点
     *
     * @param data     最新的数据
     * @param nodePath 要更新的路径
     * @param url      zookeeper地址
     */
    public static void updateNode(String data, String nodePath, String url) {
        if (client == null) {
            init(url);
        }
        //不存在则创建节点并且添加数据 存在则直接添加数据
        try {
            Stat stat = client.checkExists().forPath(nodePath);
            if (stat == null) {
                client.create().creatingParentsIfNeeded().forPath(nodePath, data.getBytes());
            } else {
                client.setData().forPath(nodePath, data.getBytes());
            }
        } catch (Exception ignore) {
        }
    }

    public static void updateNode(String data, String nodePath) {
        log.info("updateNode nodePath:{}, data:{}", nodePath, data);
        updateNode(data, nodePath, defaultUrl);
    }


    public static void deleteNode(String nodePath) {
        log.info("deleteNode nodePath:{}", nodePath);
        deleteNode(nodePath, defaultUrl);
    }

    public static void deleteNode(String nodePath, String url) {
        try {
            if (client == null) {
                init(url);
            }
            Stat stat = client.checkExists().forPath(nodePath);
            if (stat != null) {
                client.delete().deletingChildrenIfNeeded().forPath(nodePath);
            }
        } catch (Exception ignored) {
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


    //读取节点数据
    public static String readNode(String nodePath) {
        try {
            if (client == null) {
                init(defaultUrl);
            }
            byte[] data = client.getData().forPath(nodePath);
            return new String(data);
        } catch (Exception e) {
            throw new RuntimeException("zookeeper readNode error", e);
        }
    }

    public static String readNode(String nodePath, String url) {
        try {
            if (client == null) {
                init(url);
            }
            byte[] data = client.getData().forPath(nodePath);
            return new String(data);
        } catch (Exception e) {
            throw new RuntimeException("zookeeper readNode error", e);
        }
    }

    /**
     * 替换节点中的内容
     *
     * @param nodePath   节点路径
     * @param url        zookeeper地址
     * @param oldContent 旧的字符串
     * @param newContent 新的字符串
     */
    public static String replace(String nodePath, String url, String oldContent, String newContent) {
        String data = readNode(nodePath, url);
        data = data.replaceAll(oldContent, newContent);
        //删除每行之间的空行
        data = data.replaceAll("(?m)^[\\s]*\\r?\\n", "");
        updateNode(data, nodePath, url);
        return data;
    }

    public static String replace(String nodePath, String oldContent, String newContent) {
        return replace(nodePath, defaultUrl, oldContent, newContent);
    }
}

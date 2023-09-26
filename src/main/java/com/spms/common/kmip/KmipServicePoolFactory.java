package com.spms.common.kmip;

import com.sc.kmip.client.KMSClientInterface;
import com.spms.dbhsm.secretService.domain.DbhsmSecretService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Hashtable;

/**
 * Created with CosmosRay
 *
 * @author
 * @date 2019/8/15
 * Funciton:该类是KMIP池的管理类
 */
@Slf4j
public class KmipServicePoolFactory {
    private static Hashtable<String, KMSClientInterface> hashtable = new Hashtable<>();

    /**
     * 绑定连接池
     *
     * @param secretService 对应的连接池
     */
    public static void bind(DbhsmSecretService secretService) throws IOException {
        if (IsBePool(secretService.getSecretService())) {
            log.info("{}该KMIP已经在池中",secretService.getSecretService());
            return;
        }

        //创建连接,如果创建成功，并放入池中
        KMSClientInterface kmsClientInterface = ZaKmipUtil.reconnKMIP(secretService.getSecretService());
        if (kmsClientInterface != null){
            hashtable.put(secretService.getSecretService(), kmsClientInterface);
        }
    }

    /**
     * 重新绑定连接池
     *
     * @param secretServiceOld   旧连接信息
     * @param secretServiceNew 新连接信息
     */
    public static void rebind(DbhsmSecretService secretServiceOld,DbhsmSecretService secretServiceNew) throws IOException {
        if (IsBePool(secretServiceOld.getSecretService())) {
            hashtable.remove(secretServiceNew.getSecretService());
        }

         //创建连接,如果创建成功，并放入池中
        KMSClientInterface kmsClientInterface = ZaKmipUtil.reconnKMIP(secretServiceNew.getSecretService());
        if (kmsClientInterface != null){
            hashtable.put(secretServiceNew.getSecretService(), kmsClientInterface);
        }
    }

    /**
     * 删除动名称为key的连接池
     *
     * @param secretService
     */
    public static void unbind(DbhsmSecretService secretService) {

        hashtable.remove(secretService.getSecretService());
    }

    /**
     * 查找动态数据连接池中是否存在名称为key的连接池
     *
     * @param key
     * @return
     */
    public static boolean IsBePool(String key) {
        return hashtable.containsKey(key);
    }

    /**
     * 根据key返回key对应的连接池
     *
     * @param key
     * @return
     */
    public static KMSClientInterface getKmipServicePool(String key) {
        if (!IsBePool(key)) {
            return null;
        }
        return hashtable.get(key);

    }
}


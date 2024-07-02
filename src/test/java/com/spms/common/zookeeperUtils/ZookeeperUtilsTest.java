package com.spms.common.zookeeperUtils;

import org.junit.Test;

public class ZookeeperUtilsTest {
    @Test
    public void test() {
        ZookeeperUtils.updateNode("123test", "/ZA-192.168.6.59:3306/metadata/zzyTest/rules/encrypt/encryptors/name_encryptor/active_version", "192.168.7.157:2181");
    }
}
package com.spms.dbhsm.dbInstance.service.impl;

import com.spms.common.Template.TemplateEngineException;
import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import org.junit.Test;

import static org.junit.Assert.*;

public class DbhsmDbInstanceServiceImplTest {

    DbhsmDbInstanceServiceImpl dbhsmDbInstanceService = new DbhsmDbInstanceServiceImpl();
    @Test
    public void  testGenerateConfigFile() throws TemplateEngineException {
        DbhsmDbInstance dbhsmDbInstance = new DbhsmDbInstance();

        dbhsmDbInstance.setDatabaseIp("192.168.7.113");
        dbhsmDbInstance.setDatabasePort("23306");
        dbhsmDbInstance.setServiceUser("root");
        dbhsmDbInstance.setServicePassword("123456");

        dbhsmDbInstance.setDatabaseServerName("zzyTest");
        dbhsmDbInstance.setDatabaseType("2");

        String s = dbhsmDbInstanceService.generateGlobalConfigFile(dbhsmDbInstance);
        System.out.println("global:");
        System.out.println(s);
        String s1 = dbhsmDbInstanceService.generateEncryptConfigFile(dbhsmDbInstance);
        System.out.println("encrypt:");
        System.out.println(s1);

    }
}
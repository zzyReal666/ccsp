package com.spms.dbhsm.stockDataProcess.adapter;

import com.spms.common.constant.DbConstants;
import com.spms.common.enums.DatabaseTypeEnum;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.stockDataProcess.domain.dto.DatabaseDTO;

/**
 * @author zzypersonally@gmail.com
 * @description 适配器，由 DatabaseDTO 转换为 DbInstanceGetConnDTO 从而直接创建数据库连接池
 * @since 2024/4/28 16:09
 */
public class DatabaseToDbInstanceGetConnDTOAdapter {

    /**
     * 适配方法
     *
     * @param databaseDTO 数据库信息 databaseType需要转换 databaseDTO中是mysql  DbInstanceGetConnDTO中是数字
     * @return DbInstanceGetConnDTO
     */
    public static DbInstanceGetConnDTO adapter(DatabaseDTO databaseDTO, AdapterType adapterType) {
        DbInstanceGetConnDTO dbInstanceGetConnDTO = new DbInstanceGetConnDTO();
        dbInstanceGetConnDTO.setDatabaseIp(databaseDTO.getDatabaseIp());

        //mysql
        if (databaseDTO.getDatabaseType().equals(DatabaseTypeEnum.MySQL.name())) {
            dbInstanceGetConnDTO.setDatabaseType(DbConstants.DB_TYPE_MYSQL);
        }
        //clickHouse
        if (databaseDTO.getDatabaseType().equals(DatabaseTypeEnum.ClickHouse.name())) {
            dbInstanceGetConnDTO.setDatabaseType(DbConstants.DB_TYPE_CLICKHOUSE);
        }
        dbInstanceGetConnDTO.setDatabasePort(databaseDTO.getDatabasePort());
        if (adapterType == AdapterType.DBA) {
            dbInstanceGetConnDTO.setDatabaseDba(databaseDTO.getDatabaseDba());
            dbInstanceGetConnDTO.setDatabaseDbaPassword(databaseDTO.getDatabaseDbaPassword());
        } else {
            dbInstanceGetConnDTO.setDatabaseDba(databaseDTO.getServiceUser());
            dbInstanceGetConnDTO.setDatabaseDbaPassword(databaseDTO.getServicePassword());
        }
        dbInstanceGetConnDTO.setDatabaseServerName(databaseDTO.getDatabaseName());
        return dbInstanceGetConnDTO;
    }


    /**
     * 枚举，用于区分适配器获取DBA还是业务账号
     * DBA账号用于更改表结构，业务账号用于数据的处理和代理时的连接
     */
    public enum AdapterType {
        DBA, SERVICE
    }

}

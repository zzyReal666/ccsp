package com.spms.common.pool;

import com.spms.common.constant.DbConstants;
import com.spms.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created with CosmosRay
 *
 * @author CosmosRay
 * @date 2019/8/16
 * Funciton: 数据源信息
 */
@Data
@Builder
@Slf4j
public class DataSource {
    /** 数据库服务名 */
    private String databaseServerName;
    /** 数据库类型 */
    private String databaseType;
    /**数据库连接驱动*/
    private String driverClass;
    /**数据库连接*/
    private String url;
    /** 数据库DBA */
    private String databaseDba;
    /** 数据库DBA密码 */
    private String databaseDbaPassword;

    public DataSource() {

    }

    public DataSource(String databaseServerName, String databaseType, String driverClass, String url, String databaseDba, String databaseDbaPassword) {
        this.databaseServerName = databaseServerName;
        this.databaseType = databaseType;
        this.driverClass = driverClass;
        this.url = url;
        this.databaseDba = databaseDba;
        this.databaseDbaPassword = databaseDbaPassword;
    }

    public static DataSource getDataSource(DbInstanceGetConnDTO instance) {
        return DataSource.builder()
                .databaseServerName(instance.getDatabaseServerName())
                .databaseType(instance.getDatabaseType())
                .driverClass(getDatabaseDriverClass(instance.getDatabaseType()))
                .url(getDatabaseUrl(instance))
                .databaseDba(instance.getDatabaseDba())
                .databaseDbaPassword(instance.getDatabaseDbaPassword())
                .build();
    }

    private static String getDatabaseUrl(DbInstanceGetConnDTO instance) {
        String databaseIp = instance.getDatabaseIp();
        String databaseType = instance.getDatabaseType();
        String databasePort = instance.getDatabasePort();
        String databaseDba = instance.getDatabaseDba();
        String databaseDbaPassword = instance.getDatabaseDbaPassword();
        String databaseExampleType = instance.getDatabaseExampleType();
        String databaseServerName = instance.getDatabaseServerName();
        String url = "";
        switch (databaseType) {
            case DbConstants.DB_TYPE_ORACLE:
                if (DbConstants.DB_EXAMPLE_TYPE_SID.equals(databaseExampleType)) {
                    url = "jdbc:oracle:thin:@" + databaseIp + ":" + databasePort + databaseExampleType  + databaseServerName;
                } else {
                    url = "jdbc:oracle:thin:@//" + databaseIp + ":" + databasePort  + databaseExampleType + databaseServerName;
                }
                break;
            case DbConstants.DB_TYPE_SQLSERVER:
                url = "jdbc:sqlserver://" + databaseIp + ":" + databasePort + ";DatabaseName=" + databaseServerName + ";encrypt=false;integratedSecurity=false;";
                break;
            case DbConstants.DB_TYPE_MYSQL:
                url = "jdbc:mysql://" + databaseIp + ":" + databasePort  + "/" + databaseServerName + "?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8&useSSL=false";
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        }
        log.info("database url: " + url);
        return url;
    }

    private static String getDatabaseDriverClass(String databaseType) {
        String driverClass = "";
        if (databaseType.equals(DbConstants.DB_TYPE_ORACLE)) {
            driverClass = "oracle.jdbc.driver.OracleDriver";
        } else if (databaseType.equals(DbConstants.DB_TYPE_SQLSERVER)) {
            driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        } else if (databaseType.equals(DbConstants.DB_TYPE_MYSQL)) {
            driverClass = "com.mysql.jdbc.Driver";
        }
        log.info("driver class: " + driverClass);
        return driverClass;
    }

}

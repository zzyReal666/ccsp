package com.spms.dbhsm.dbInstance.domain.DTO;

import com.spms.dbhsm.dbInstance.domain.DbhsmDbInstance;
import lombok.Builder;
import lombok.Data;

/**
 * 数据库实例对象 dbhsm_db_instance
 *
 * @author spms
 * @date 2023-09-19
 */
@Data
@Builder
public class DbOracleInstancePoolKeyDTO extends DbInstancePoolKeyDTO
{
    /** 数据库类型 */
    private String databaseType;

    /** 数据库IP地址 */
    private String databaseIp;

    /** 数据库端口号 */
    private String databasePort;

    /** 数据库服务名 */
    private String databaseServerName;

    /** 实例类型 */
    private String databaseExampleType;

    public DbOracleInstancePoolKeyDTO() {}

    public DbOracleInstancePoolKeyDTO(String databaseType, String databaseIp, String databasePort, String databaseServerName, String databaseExampleType) {
        this.databaseType = databaseType;
        this.databaseIp = databaseIp;
        this.databasePort = databasePort;
        this.databaseServerName = databaseServerName;
        this.databaseExampleType = databaseExampleType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DbOracleInstancePoolKeyDTO)) {
            return false;
        }

        DbOracleInstancePoolKeyDTO that = (DbOracleInstancePoolKeyDTO) o;

        if (getDatabaseType() != null ? !getDatabaseType().equals(that.getDatabaseType()) : that.getDatabaseType() != null) {
            return false;
        }
        if (getDatabaseIp() != null ? !getDatabaseIp().equals(that.getDatabaseIp()) : that.getDatabaseIp() != null) {
            return false;
        }
        if (getDatabasePort() != null ? !getDatabasePort().equals(that.getDatabasePort()) : that.getDatabasePort() != null) {
            return false;
        }
        if (getDatabaseServerName() != null ? !getDatabaseServerName().equals(that.getDatabaseServerName()) : that.getDatabaseServerName() != null) {
            return false;
        }
        return getDatabaseExampleType() != null ? getDatabaseExampleType().equals(that.getDatabaseExampleType()) : that.getDatabaseExampleType() == null;
    }

    @Override
    public int hashCode() {
        int result = getDatabaseType() != null ? getDatabaseType().hashCode() : 0;
        result = 31 * result + (getDatabaseIp() != null ? getDatabaseIp().hashCode() : 0);
        result = 31 * result + (getDatabasePort() != null ? getDatabasePort().hashCode() : 0);
        result = 31 * result + (getDatabaseServerName() != null ? getDatabaseServerName().hashCode() : 0);
        result = 31 * result + (getDatabaseExampleType() != null ? getDatabaseExampleType().hashCode() : 0);
        return result;
    }

    public static DbOracleInstancePoolKeyDTO getInstancePoolKeyDTO(DbhsmDbInstance instance){
        return DbOracleInstancePoolKeyDTO.builder()
                .databaseType(instance.getDatabaseType())
                .databaseIp(instance.getDatabaseIp())
                .databasePort(instance.getDatabasePort())
                .databaseServerName(instance.getDatabaseServerName())
                .databaseExampleType(instance.getDatabaseExampleType())
                .build();
    }
}

package com.spms.dbhsm.dbInstance.domain.DTO;

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
public class DbDMInstancePoolKeyDTO extends DbInstancePoolKeyDTO
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

    /** 数据库DBA */
    private String databaseDba;

    public DbDMInstancePoolKeyDTO() {}

    public DbDMInstancePoolKeyDTO(String databaseType, String databaseIp, String databasePort, String databaseServerName, String databaseExampleType, String databaseDba) {
        this.databaseType = databaseType;
        this.databaseIp = databaseIp;
        this.databasePort = databasePort;
        this.databaseServerName = databaseServerName;
        this.databaseExampleType = databaseExampleType;
        this.databaseDba = databaseDba;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DbDMInstancePoolKeyDTO)) {
            return false;
        }

        DbDMInstancePoolKeyDTO that = (DbDMInstancePoolKeyDTO) o;

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
        if (getDatabaseExampleType() != null ? !getDatabaseExampleType().equals(that.getDatabaseExampleType()) : that.getDatabaseExampleType() != null) {
            return false;
        }
        return getDatabaseDba() != null ? getDatabaseDba().equals(that.getDatabaseDba()) : that.getDatabaseDba() == null;
    }

    @Override
    public int hashCode() {
        int result = getDatabaseType() != null ? getDatabaseType().hashCode() : 0;
        result = 31 * result + (getDatabaseIp() != null ? getDatabaseIp().hashCode() : 0);
        result = 31 * result + (getDatabasePort() != null ? getDatabasePort().hashCode() : 0);
        result = 31 * result + (getDatabaseServerName() != null ? getDatabaseServerName().hashCode() : 0);
        result = 31 * result + (getDatabaseExampleType() != null ? getDatabaseExampleType().hashCode() : 0);
        result = 31 * result + (getDatabaseDba() != null ? getDatabaseDba().hashCode() : 0);
        return result;
    }

}

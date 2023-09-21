package com.spms.dbInstance.domain.DTO;

import com.ccsp.common.core.web.domain.BaseEntity;
import lombok.Data;

/**
 * 数据库实例对象 dbhsm_db_instance
 *
 * @author spms
 * @date 2023-09-19
 */
@Data
public class DbInstanceGetConnDTO extends BaseEntity
{
    /** 数据库类型 */
    private String databaseType;

    /** 数据库IP地址 */
    private String databaseIp;

    /** 数据库端口号 */
    private String databasePort;

    /** 数据库服务名 */
    private String databaseServerName;

    /** 实例类型 SID取值 ":" , 服务名取值 "/" */
    private String databaseExampleType;

    /** 数据库DBA */
    private String databaseDba;

    /** 数据库DBA密码 */
    private String databaseDbaPassword;


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DbInstanceGetConnDTO)) {
            return false;
        }

        DbInstanceGetConnDTO instance = (DbInstanceGetConnDTO) o;


        if (getDatabaseType() != null ? !getDatabaseType().equals(instance.getDatabaseType()) : instance.getDatabaseType() != null) {
            return false;
        }
        if (getDatabaseIp() != null ? !getDatabaseIp().equals(instance.getDatabaseIp()) : instance.getDatabaseIp() != null) {
            return false;
        }
        if (getDatabasePort() != null ? !getDatabasePort().equals(instance.getDatabasePort()) : instance.getDatabasePort() != null) {
            return false;
        }
        if (getDatabaseServerName() != null ? !getDatabaseServerName().equals(instance.getDatabaseServerName()) : instance.getDatabaseServerName() != null) {
            return false;
        }
        if (getDatabaseExampleType() != null ? !getDatabaseExampleType().equals(instance.getDatabaseExampleType()) : instance.getDatabaseExampleType() != null) {
            return false;
        }
        if (getDatabaseDba() != null ? !getDatabaseDba().equals(instance.getDatabaseDba()) : instance.getDatabaseDba() != null) {
            return false;
        }
        return getDatabaseDbaPassword() != null ? !getDatabaseDbaPassword().equals(instance.getDatabaseDbaPassword()) : instance.getDatabaseDbaPassword() != null;
    }

    @Override
    public int hashCode() {
        int result = getDatabaseType() != null ? getDatabaseType().hashCode() : 0;
        result = 31 * result + (getDatabaseIp() != null ? getDatabaseIp().hashCode() : 0);
        result = 31 * result + (getDatabasePort() != null ? getDatabasePort().hashCode() : 0);
        result = 31 * result + (getDatabaseServerName() != null ? getDatabaseServerName().hashCode() : 0);
        result = 31 * result + (getDatabaseExampleType() != null ? getDatabaseExampleType().hashCode() : 0);
        result = 31 * result + (getDatabaseDba() != null ? getDatabaseDba().hashCode() : 0);
        result = 31 * result + (getDatabaseDbaPassword() != null ? getDatabaseDbaPassword().hashCode() : 0);
        return result;
    }
}

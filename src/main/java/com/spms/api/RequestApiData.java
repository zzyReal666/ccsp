package com.spms.api;


/**
 * @author diq
 * @date 2023/09/26
 * @dec 描述
 */
public class RequestApiData {
    /**
     * 策略唯一标识
     */
    private String pid;
    /** 数据库类型 */
    private int databaseType;
    /**
     * 实例名
     */
    private String instanceName;
    /**
     * 数据库版本
     */
    private String databaseEdition;
    /**
     * 数据库IP地址
     */
    private String databaseIp;
    /**
     * 数据库端口号
     */
    private String databasePort;
    /** 实例类型： SID取值 ":" ；服务名取值 "/" */
    private String databaseExampleType;
    /**
     * 数据库名
     */
    private String databaseServerName;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 数据库表名称
     */
    private String dbTableName;
    /**
     * 加密列名
     */
    private String encryptColumns;

    /**
     * 密码
     */
    private String password;

    /**
     * 列类型
     */
    private String columnsType;

    /** 密码服务*/
    private String secretService;
    /**IP*/
    private String secretServiceIp;
    /** PORT */
    private String secretServicePort;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public int getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(int databaseType) {
        this.databaseType = databaseType;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getDatabaseEdition() {
        return databaseEdition;
    }

    public void setDatabaseEdition(String databaseEdition) {
        this.databaseEdition = databaseEdition;
    }

    public String getDatabaseIp() {
        return databaseIp;
    }

    public void setDatabaseIp(String databaseIp) {
        this.databaseIp = databaseIp;
    }

    public String getDatabasePort() {
        return databasePort;
    }

    public void setDatabasePort(String databasePort) {
        this.databasePort = databasePort;
    }

    public String getDatabaseExampleType() {
        return databaseExampleType;
    }

    public void setDatabaseExampleType(String databaseExampleType) {
        this.databaseExampleType = databaseExampleType;
    }

    public String getDatabaseServerName() {
        return databaseServerName;
    }

    public void setDatabaseServerName(String databaseServerName) {
        this.databaseServerName = databaseServerName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDbTableName() {
        return dbTableName;
    }

    public void setDbTableName(String dbTableName) {
        this.dbTableName = dbTableName;
    }

    public String getEncryptColumns() {
        return encryptColumns;
    }

    public void setEncryptColumns(String encryptColumns) {
        this.encryptColumns = encryptColumns;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getColumnsType() {
        return columnsType;
    }

    public void setColumnsType(String columnsType) {
        this.columnsType = columnsType;
    }

    public String getSecretService() {
        return secretService;
    }

    public void setSecretService(String secretService) {
        this.secretService = secretService;
    }

    public String getSecretServiceIp() {
        return secretServiceIp;
    }

    public void setSecretServiceIp(String secretServiceIp) {
        this.secretServiceIp = secretServiceIp;
    }

    public String getSecretServicePort() {
        return secretServicePort;
    }

    public void setSecretServicePort(String secretServicePort) {
        this.secretServicePort = secretServicePort;
    }

    @Override
    public String toString() {
        return "RequestApiData{" +
                "pid='" + pid + '\'' +
                ", databaseType='" + databaseType + '\'' +
                ", instanceName='" + instanceName + '\'' +
                ", databaseEdition='" + databaseEdition + '\'' +
                ", databaseIp='" + databaseIp + '\'' +
                ", databasePort='" + databasePort + '\'' +
                ", databaseExampleType='" + databaseExampleType + '\'' +
                ", databaseServerName='" + databaseServerName + '\'' +
                ", userName='" + userName + '\'' +
                ", dbTableName='" + dbTableName + '\'' +
                ", encryptColumns='" + encryptColumns + '\'' +
                ", password='" + password + '\'' +
                ", columnsType='" + columnsType + '\'' +
                ", secretService='" + secretService + '\'' +
                ", secretServiceIp='" + secretServiceIp + '\'' +
                ", secretServicePort='" + secretServicePort + '\'' +
                '}';
    }
}

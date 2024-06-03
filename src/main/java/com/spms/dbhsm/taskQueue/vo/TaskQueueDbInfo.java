package com.spms.dbhsm.taskQueue.vo;

import lombok.Data;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-22 10:34 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class TaskQueueDbInfo {

    private String dbInstanceName;

    private String databaseType;

    private String databaseIp;

    private String databasePort;

    private String databaseEdition;

    private String databaseDba;
}

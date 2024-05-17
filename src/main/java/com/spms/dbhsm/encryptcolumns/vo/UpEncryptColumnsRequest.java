package com.spms.dbhsm.encryptcolumns.vo;

import lombok.Data;

import java.util.List;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-15 15:48 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class UpEncryptColumnsRequest {

    private String dbInstanceId;

    private List<EncryptColumns> list;

    private Integer batchCount;

    private Integer threadCount;

    private String dbUserName;

    private String dbTable;

}

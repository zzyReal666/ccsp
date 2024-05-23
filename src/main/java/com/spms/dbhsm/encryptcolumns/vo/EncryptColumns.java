package com.spms.dbhsm.encryptcolumns.vo;

import lombok.Data;

/**
 * <p> description: TODO the method is used to </p>
 *
 * <p> Powered by wzh On 2024-05-16 13:38 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Data
public class EncryptColumns {
    private String encryptColumns;
    private String encryptType;
    private String columnsType;
    private String secretKeyId;
}

package com.spms.dbhsm.encryptcolumns.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
* <p> description: TODO the method is used to </p>
*
* <p> Powered by wzh On 2024-01-16 09:01 </p>
* <p> @author wzh [zhwang2012@yeah.net] </p>
* <p> @version 1.0 </p>
*/

@Data
public class DbhsmEncryptTable implements Serializable {

	private static final long serialVersionUID = 1L;

	/**id*/
	@ApiModelProperty(name = "tableId",value = "id")
	private Long tableId;

	/**数据库实例ID*/
	@ApiModelProperty(name = "instanceId",value = "数据库实例ID")
	private Long instanceId;

	/**表DDL语句*/
	@ApiModelProperty(name = "tableDdl",value = "表DDL语句")
	private String tableDdl;

	/**表名*/
	@ApiModelProperty(name = "tableName",value = "表名")
	private String tableName;

	/**表状态：0加密前-1加密中-2加密后-3已经全部还原*/
	@ApiModelProperty(name = "tableStatus",value = "表状态：0加密前-1加密中-2加密后-3已经全部还原")
	private Integer tableStatus;

	/**线程条数*/
	@ApiModelProperty(name = "threadCount",value = "线程条数")
	private Integer threadCount;

	/**每批条数*/
	@ApiModelProperty(name = "batchCount",value = "每批条数")
	private Integer batchCount;

	/**创建时间*/
	@ApiModelProperty(name = "createTime",value = "创建时间")
	private java.util.Date createTime;

	/**创建者*/
	@ApiModelProperty(name = "createBy",value = "创建者")
	private String createBy;

	/**更新时间*/
	@ApiModelProperty(name = "updateTime",value = "更新时间")
	private java.util.Date updateTime;

	/**更新者*/
	@ApiModelProperty(name = "updateBy",value = "更新者")
	private String updateBy;
}

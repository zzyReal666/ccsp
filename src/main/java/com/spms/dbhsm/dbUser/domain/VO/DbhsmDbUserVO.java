package com.spms.dbhsm.dbUser.domain.VO;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.spms.dbhsm.dbInstance.domain.DTO.DbInstanceGetConnDTO;
import com.spms.dbhsm.permissionGroup.domain.dto.PermissionGroupForUserDto;
import lombok.Data;

import java.util.Date;

/**
 * 数据库用户对象 dbhsm_db_users
 *
 * @author ccsp
 * @date 2023-09-25
 */
@Data
public class DbhsmDbUserVO
{
    /** 主键 */
    private Long id;
    /** 密码服务 */
    private String secretService;

    /** 用户ID(待加密数据库中的ID) */
    private String userId;

    /** 权限组ID */
    private Long permissionGroupId;

    /** 数据库实例ID */
    private Long databaseInstanceId;

    /** 用户名 */
    private String userName;

    /** 数据库实例DTO */
    private DbInstanceGetConnDTO dbInstanceGetConnDTO;

    /** 权限组 */
    private PermissionGroupForUserDto permissionGroupForUserDto;

    /** 0:web端自建的用户 1：系统已有 */
    private Integer isSelfBuilt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date created;

}

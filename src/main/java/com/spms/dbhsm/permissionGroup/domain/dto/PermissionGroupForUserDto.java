package com.spms.dbhsm.permissionGroup.domain.dto;

import com.ccsp.common.core.annotation.Excel;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 数据库权限组信息对象 dbhsm_permission_group
 *
 * @author diq
 * @date 2023-09-20
 */
@Data
public class PermissionGroupForUserDto
{
    /** 权限组ID */
    private Long permissionGroupId;

    /** 权限组名称 */
    @Excel(name = "权限组名称")
    @NotBlank(message = "权限组名称不能为空")
    private String permissionGroupName = "无";

    /** 角色状态（0正常 1停用） */
    @Excel(name = "角色状态", readConverterExp = "0=正常,1=停用")
    private String status;


}
package com.cutting.cuttingsystem.entitys;


import lombok.Data;

import java.io.Serializable;

import java.util.Date;

/**
 * 存储系统所有用户账号信息，区分管理员与生产人员双角色，实现账号权限管控与注册审批流程
 *
 * @TableName t_user
 */
@Data
public class TUser implements Serializable {

    /**
     * 用户唯一 ID
     */

    private Long userId;
    /**
     * 登录用户名，全局唯一
     */

    private String username;
    /**
     * 登录密码，MD5 加密存储
     */

    private String password;
    /**
     * 用户真实姓名
     */

    private String realName;
    /**
     * 联系电话
     */

    private String phone;
    /**
     * 角色类型：1 = 系统管理员，2 = 生产人员；注册账号默认为生产人员
     */
    private Integer roleType;
    /**
     * 账号状态：1 = 正常启用，2 = 禁用，3 = 待审批；注册后默认为待审批
     */
    private Integer accountStatus;
    /**
     * 最后一次登录时间
     */
    private Date lastLoginTime;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 备注信息
     */

    private String remark;

}

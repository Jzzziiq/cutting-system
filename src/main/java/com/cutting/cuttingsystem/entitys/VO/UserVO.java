package com.cutting.cuttingsystem.entitys.VO;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class UserVO {
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private Integer roleType;
    private Integer accountStatus;
    private Date lastLoginTime;
    private Date createTime;
    private String remark;
    private Long orgId;
    private String orgRole;
    private List<String> roles;
    private List<String> permissions;
}

package com.cutting.cuttingsystem.entitys;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 组织表
 *
 * @TableName t_organization
 */
@Data
@TableName("t_organization")
public class TOrganization implements Serializable {

    /**
     * 组织ID
     */
    @TableId
    private Long orgId;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 组织编码，用于注册时输入
     */
    private String orgCode;

    /**
     * 状态：1=正常，2=禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}

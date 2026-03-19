package com.cutting.cuttingsystem.entitys;


import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


/**
 * 客户表
 *
 * @TableName t_customer
 */
@Data
@TableName("t_customer")
public class TCustomer implements Serializable {
    /**
     * 客户唯一 ID
     */
    @TableId
    private Long customerId;
    /**
     * 用户 ID（数据隔离）
     */
    private Long userId;
    /**
     * 客户姓名/公司名
     */
    private String customerName;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 客户地址
     */
    private String address;
    /**
     * 是否启用：1=启用，0=禁用
     */
    private Integer isEnabled;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

}

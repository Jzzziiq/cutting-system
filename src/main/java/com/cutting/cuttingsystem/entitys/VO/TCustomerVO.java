package com.cutting.cuttingsystem.entitys.VO;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * 客户表
 *
 * @TableName t_customer
 */
@Data
public class TCustomerVO implements Serializable {
    /**
     * 客户唯一 ID
     */
    private Long customerId;
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
     * 备注
     */
    private String remark;

}

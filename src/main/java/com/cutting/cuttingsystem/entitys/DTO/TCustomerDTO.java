package com.cutting.cuttingsystem.entitys.DTO;


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
public class TCustomerDTO implements Serializable {
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
     * 备注
     */
    private String remark;

}

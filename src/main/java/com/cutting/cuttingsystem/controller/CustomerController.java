package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;
import com.cutting.cuttingsystem.entitys.DTO.TCustomerDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.entitys.TCustomer;
import com.cutting.cuttingsystem.entitys.VO.TCustomerVO;
import com.cutting.cuttingsystem.service.TCustomerService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
public class CustomerController {
    @Autowired
    TCustomerService customerService;

    /**
     * 分页查询
     */
    @GetMapping
    public Result pageQuery(QueryDTO query) {
        IPage<TCustomer> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<TCustomer> customerPage = customerService.page(page);

        IPage<TCustomerVO> customerVOPage = customerPage.convert(customer -> {
            TCustomerVO customerVO = new TCustomerVO();
            BeanUtils.copyProperties(customer, customerVO);
            return customerVO;
        });
        return Result.success(customerVOPage);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        TCustomer customer = customerService.getById(id);
        TCustomerVO customerVO = new TCustomerVO();
        BeanUtils.copyProperties(customer, customerVO);
        return Result.success(customerVO);
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable Integer id) {
        boolean res = customerService.removeById(id);
        return res ? Result.success() : Result.error("删除失败");
    }

    /**
     * 新增
     */
    @PostMapping
    public Result save(@RequestBody TCustomerDTO customerDTO) {
        TCustomer customer = new TCustomer();
        BeanUtils.copyProperties(customerDTO, customer);
        boolean res = customerService.save(customer);
        return res ? Result.success() : Result.error("添加失败");
    }
    /**
     * 修改
     * 前端修改状态时，传递修改后的状态，比如：当前是0禁用要修改为1启用，则传递1
     */
    @PostMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody TCustomerVO customerVO) {
        TCustomer customer = new TCustomer();
        BeanUtils.copyProperties(customerVO, customer);
        customer.setCustomerId(id);
        boolean res = customerService.updateById(customer);
        return res ? Result.success() : Result.error("修改失败");
    }
}



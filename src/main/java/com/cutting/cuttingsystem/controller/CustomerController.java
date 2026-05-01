package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;
import com.cutting.cuttingsystem.entitys.DTO.TCustomerDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TCustomer;
import com.cutting.cuttingsystem.entitys.VO.TCustomerVO;
import com.cutting.cuttingsystem.service.TCustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
@Validated
public class CustomerController {
    @Autowired
    private TCustomerService customerService;

    @GetMapping
    public Result pageQuery(@Valid QueryDTO query) {
        IPage<TCustomer> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<TCustomerVO> customerVOPage = customerService.page(page).convert(this::toVO);
        return Result.success(customerVOPage);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable @Positive(message = "id must be greater than 0") Integer id) {
        TCustomer customer = customerService.getById(id);
        if (customer == null) {
            return Result.error("customer not found");
        }
        return Result.success(toVO(customer));
    }

    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @Positive(message = "id must be greater than 0") Integer id) {
        boolean removed = customerService.removeById(id);
        return removed ? Result.success() : Result.error("delete customer failed");
    }

    @PostMapping
    public Result save(@RequestBody @Valid TCustomerDTO customerDTO) {
        TCustomer customer = new TCustomer();
        BeanUtils.copyProperties(customerDTO, customer);
        boolean saved = customerService.save(customer);
        return saved ? Result.success() : Result.error("add customer failed");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable @Positive(message = "id must be greater than 0") Long id,
                         @RequestBody @Valid TCustomerVO customerVO) {
        TCustomer customer = new TCustomer();
        BeanUtils.copyProperties(customerVO, customer);
        customer.setCustomerId(id);
        boolean updated = customerService.updateById(customer);
        return updated ? Result.success() : Result.error("update customer failed");
    }

    private TCustomerVO toVO(TCustomer customer) {
        TCustomerVO customerVO = new TCustomerVO();
        BeanUtils.copyProperties(customer, customerVO);
        return customerVO;
    }
}

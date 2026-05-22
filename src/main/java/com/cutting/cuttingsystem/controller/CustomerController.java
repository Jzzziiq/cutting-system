package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.DTO.BatchEnabledDTO;
import com.cutting.cuttingsystem.entitys.DTO.BatchIdsDTO;
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

import java.util.Map;

@RestController
@RequestMapping("/customers")
@Validated
@RequirePermission({"customer:read", "customer:write"})
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
    public Result getById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        TCustomer customer = customerService.getById(id);
        if (customer == null) {
            return Result.error("customer not found");
        }
        return Result.success(toVO(customer));
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "客户管理", action = "删除")
    public Result deleteById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        try {
            boolean removed = customerService.removeByIdIfUnused(id);
            return removed ? Result.success() : Result.error("delete customer failed");
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/batch")
    @AuditLog(module = "客户管理", action = "批量删除")
    public Result batchDelete(@RequestBody @Valid BatchIdsDTO batchIdsDTO) {
        try {
            boolean removed = customerService.removeByIdsIfUnused(batchIdsDTO.getIds());
            return removed ? Result.success(Map.of("affected", batchIdsDTO.getIds().size())) : Result.error("batch delete customer failed");
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/batch/status")
    @AuditLog(module = "客户管理", action = "批量启禁用")
    public Result batchUpdateStatus(@RequestBody @Valid BatchEnabledDTO batchEnabledDTO) {
        TCustomer customer = new TCustomer();
        customer.setIsEnabled(batchEnabledDTO.getIsEnabled());
        boolean updated = customerService.update(customer,
                new UpdateWrapper<TCustomer>().in("customer_id", batchEnabledDTO.getIds()));
        return updated ? Result.success(Map.of("affected", batchEnabledDTO.getIds().size())) : Result.error("batch update customer status failed");
    }

    @PostMapping
    @AuditLog(module = "客户管理", action = "新增")
    public Result save(@RequestBody @Valid TCustomerDTO customerDTO) {
        TCustomer customer = new TCustomer();
        BeanUtils.copyProperties(customerDTO, customer);
        boolean saved = customerService.save(customer);
        return saved ? Result.success() : Result.error("add customer failed");
    }

    @PutMapping("/{id}")
    @AuditLog(module = "客户管理", action = "编辑")
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

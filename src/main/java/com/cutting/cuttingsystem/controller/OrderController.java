package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;
import com.cutting.cuttingsystem.entitys.DTO.TOrderDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.entitys.VO.TOrderVO;
import com.cutting.cuttingsystem.service.TOrderService;
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
@RequestMapping("/orders")
@Validated
public class OrderController {
    @Autowired
    private TOrderService orderService;

    @GetMapping
    public Result pageQuery(@Valid QueryDTO query) {
        IPage<TOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<TOrderVO> orderVOPage = orderService.page(page).convert(this::toVO);
        return Result.success(orderVOPage);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        TOrderVO orderVO = orderService.getOrderDetail(id);
        if (orderVO == null) {
            return Result.error("order not found");
        }
        return Result.success(orderVO);
    }

    @PostMapping
    public Result save(@RequestBody @Valid TOrderDTO orderDTO) {
        return Result.success(orderService.createOrder(orderDTO));
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable @Positive(message = "id must be greater than 0") Long id,
                         @RequestBody @Valid TOrderDTO orderDTO) {
        boolean updated = orderService.updateOrder(id, orderDTO);
        return updated ? Result.success() : Result.error("update order failed");
    }

    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        boolean removed = orderService.removeById(id);
        return removed ? Result.success() : Result.error("delete order failed");
    }

    private TOrderVO toVO(TOrder order) {
        TOrderVO orderVO = new TOrderVO();
        BeanUtils.copyProperties(order, orderVO);
        return orderVO;
    }
}

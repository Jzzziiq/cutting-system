package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;
import com.cutting.cuttingsystem.entitys.DTO.TOrderItemDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TOrderItem;
import com.cutting.cuttingsystem.entitys.VO.TOrderItemVO;
import com.cutting.cuttingsystem.service.TOrderItemService;
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
@RequestMapping("/order-items")
@Validated
public class OrderItemController {
    @Autowired
    private TOrderItemService orderItemService;

    @GetMapping
    public Result pageQuery(@Valid QueryDTO query) {
        IPage<TOrderItem> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<TOrderItemVO> orderItemVOPage = orderItemService.page(page).convert(this::toVO);
        return Result.success(orderItemVOPage);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        TOrderItem orderItem = orderItemService.getById(id);
        if (orderItem == null) {
            return Result.error("order item not found");
        }
        return Result.success(toVO(orderItem));
    }

    @PostMapping
    public Result save(@RequestBody @Valid TOrderItemDTO orderItemDTO) {
        if (orderItemDTO.getOrderId() == null) {
            return Result.error("orderId is required");
        }
        TOrderItem orderItem = new TOrderItem();
        BeanUtils.copyProperties(orderItemDTO, orderItem);
        boolean saved = orderItemService.save(orderItem);
        return saved ? Result.success(toVO(orderItem)) : Result.error("add order item failed");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable @Positive(message = "id must be greater than 0") Long id,
                         @RequestBody @Valid TOrderItemDTO orderItemDTO) {
        TOrderItem orderItem = new TOrderItem();
        BeanUtils.copyProperties(orderItemDTO, orderItem);
        orderItem.setItemId(id);
        boolean updated = orderItemService.updateById(orderItem);
        return updated ? Result.success() : Result.error("update order item failed");
    }

    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        boolean removed = orderItemService.removeById(id);
        return removed ? Result.success() : Result.error("delete order item failed");
    }

    private TOrderItemVO toVO(TOrderItem orderItem) {
        TOrderItemVO orderItemVO = new TOrderItemVO();
        BeanUtils.copyProperties(orderItem, orderItemVO);
        return orderItemVO;
    }
}

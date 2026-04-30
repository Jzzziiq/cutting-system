package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;
import com.cutting.cuttingsystem.entitys.DTO.TLayoutResultDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TLayoutResult;
import com.cutting.cuttingsystem.entitys.VO.TLayoutResultVO;
import com.cutting.cuttingsystem.service.TLayoutResultService;
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

import java.util.List;

@RestController
@RequestMapping("/layout-results")
@Validated
public class LayoutResultController {
    @Autowired
    private TLayoutResultService layoutResultService;

    @GetMapping
    public Result pageQuery(@Valid QueryDTO query) {
        IPage<TLayoutResult> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<TLayoutResultVO> layoutResultVOPage = layoutResultService.page(page).convert(this::toVO);
        return Result.success(layoutResultVOPage);
    }

    @GetMapping("/order/{orderId}")
    public Result listByOrderId(@PathVariable @Positive(message = "orderId must be greater than 0") Long orderId) {
        List<TLayoutResultVO> resultList = layoutResultService
                .list(new QueryWrapper<TLayoutResult>().eq("order_id", orderId).orderByDesc("result_id"))
                .stream()
                .map(this::toVO)
                .toList();
        return Result.success(resultList);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        TLayoutResult layoutResult = layoutResultService.getById(id);
        if (layoutResult == null) {
            return Result.error("layout result not found");
        }
        return Result.success(toVO(layoutResult));
    }

    @PostMapping
    public Result save(@RequestBody @Valid TLayoutResultDTO layoutResultDTO) {
        TLayoutResult layoutResult = layoutResultService.createResult(layoutResultDTO);
        return Result.success(toVO(layoutResult));
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable @Positive(message = "id must be greater than 0") Long id,
                         @RequestBody @Valid TLayoutResultDTO layoutResultDTO) {
        boolean updated = layoutResultService.updateResult(id, layoutResultDTO);
        return updated ? Result.success() : Result.error("update layout result failed");
    }

    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        boolean removed = layoutResultService.removeById(id);
        return removed ? Result.success() : Result.error("delete layout result failed");
    }

    private TLayoutResultVO toVO(TLayoutResult layoutResult) {
        TLayoutResultVO layoutResultVO = new TLayoutResultVO();
        BeanUtils.copyProperties(layoutResult, layoutResultVO);
        return layoutResultVO;
    }
}

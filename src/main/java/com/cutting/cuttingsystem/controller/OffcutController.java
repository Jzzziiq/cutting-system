package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;
import com.cutting.cuttingsystem.entitys.DTO.TOffcutDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TOffcut;
import com.cutting.cuttingsystem.entitys.VO.TOffcutVO;
import com.cutting.cuttingsystem.service.TOffcutService;
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
@RequestMapping("/remnants")
@Validated
public class OffcutController {
    @Autowired
    private TOffcutService offcutService;

    @GetMapping
    public Result pageQuery(@Valid QueryDTO query) {
        IPage<TOffcut> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<TOffcutVO> offcutVOPage = offcutService.page(page).convert(this::toVO);
        return Result.success(offcutVOPage);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        TOffcut offcut = offcutService.getById(id);
        if (offcut == null) {
            return Result.error("remnant not found");
        }
        return Result.success(toVO(offcut));
    }

    @PostMapping
    public Result save(@RequestBody @Valid TOffcutDTO offcutDTO) {
        TOffcut offcut = new TOffcut();
        BeanUtils.copyProperties(offcutDTO, offcut);
        if (offcut.getStatus() == null) {
            offcut.setStatus(0);
        }
        if (offcut.getIsEnabled() == null) {
            offcut.setIsEnabled(1);
        }
        boolean saved = offcutService.save(offcut);
        return saved ? Result.success(toVO(offcut)) : Result.error("add remnant failed");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable @Positive(message = "id must be greater than 0") Long id,
                         @RequestBody @Valid TOffcutDTO offcutDTO) {
        TOffcut offcut = new TOffcut();
        BeanUtils.copyProperties(offcutDTO, offcut);
        offcut.setOffcutId(id);
        boolean updated = offcutService.updateById(offcut);
        return updated ? Result.success() : Result.error("update remnant failed");
    }

    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        boolean removed = offcutService.removeById(id);
        return removed ? Result.success() : Result.error("delete remnant failed");
    }

    private TOffcutVO toVO(TOffcut offcut) {
        TOffcutVO offcutVO = new TOffcutVO();
        BeanUtils.copyProperties(offcut, offcutVO);
        return offcutVO;
    }
}

package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.DTO.TBoardQueryDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.service.TBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/boards")
public class BoardsController {
    @Autowired
    TBoardService tBoardService;

    /**
     * 分页查询
     */
    @GetMapping
    public Result pageQuery(TBoardQueryDTO query) {
        // 创建分页参数对象
        IPage<TBoard> page = new Page<>(query.getPageNum(), query.getPageSize());
        
        // 执行分页查询
        IPage<TBoard> boardPage = tBoardService.page(page);
        
        // 返回分页结果（包含数据列表、总记录数、分页信息）
        return Result.success(boardPage);
    }

}

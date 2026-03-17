package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.DTO.TBoardDTO;
import com.cutting.cuttingsystem.entitys.DTO.TBoardQueryDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.entitys.TBoardVO.TBoardVO;
import com.cutting.cuttingsystem.service.TBoardService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        TBoard board = tBoardService.getById(id);
        TBoardVO  boardVO = new TBoardVO();
        BeanUtils.copyProperties(board, boardVO);
        return Result.success(boardVO);
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result deleteById(@PathVariable Integer id) {
        boolean res = tBoardService.removeById(id);
        return res ? Result.success() : Result.error("删除失败");
    }

    /**
     * 新增
     */
    @PostMapping
    public Result save(@RequestBody TBoardDTO boardDTO) {
        TBoard board = new TBoard();
        // 将DTO对象转换为实体对象
        BeanUtils.copyProperties(boardDTO, board);
        boolean res = tBoardService.save(board);
        return res ? Result.success() : Result.error("添加失败");
    }

    /**
     * 修改
     * 前端修改状态时，传递修改后的状态，比如：当前是0禁用要修改为1启用，则传递1
     */
    @PostMapping("/{id}")
    public Result update(@PathVariable Long id, @RequestBody TBoardVO tBoardVO) {
        TBoard board = new TBoard();
        BeanUtils.copyProperties(tBoardVO, board);
        board.setBoardId(id);
        boolean res = tBoardService.updateById(board);
        return res ? Result.success() : Result.error("修改失败");
    }
}

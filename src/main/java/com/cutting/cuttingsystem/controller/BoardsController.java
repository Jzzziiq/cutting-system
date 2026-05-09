package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.DTO.BatchEnabledDTO;
import com.cutting.cuttingsystem.entitys.DTO.BatchIdsDTO;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;
import com.cutting.cuttingsystem.entitys.DTO.TBoardDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.entitys.VO.TBoardVO;
import com.cutting.cuttingsystem.service.TBoardService;
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
@RequestMapping("/boards")
@Validated
@RequirePermission({"board:read", "board:write"})
public class BoardsController {
    @Autowired
    private TBoardService tBoardService;

    @GetMapping
    public Result pageQuery(@Valid QueryDTO query) {
        IPage<TBoard> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<TBoardVO> boardVOPage = tBoardService.page(page).convert(this::toVO);
        return Result.success(boardVOPage);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable @Positive(message = "id must be greater than 0") Integer id) {
        TBoard board = tBoardService.getById(id);
        if (board == null) {
            return Result.error("board not found");
        }
        return Result.success(toVO(board));
    }

    @DeleteMapping("/{id}")
    @AuditLog(module = "板材管理", action = "删除")
    public Result deleteById(@PathVariable @Positive(message = "id must be greater than 0") Integer id) {
        boolean removed = tBoardService.removeById(id);
        return removed ? Result.success() : Result.error("delete board failed");
    }

    @DeleteMapping("/batch")
    @AuditLog(module = "板材管理", action = "批量删除")
    public Result batchDelete(@RequestBody @Valid BatchIdsDTO batchIdsDTO) {
        boolean removed = tBoardService.removeByIds(batchIdsDTO.getIds());
        return removed ? Result.success(Map.of("affected", batchIdsDTO.getIds().size())) : Result.error("batch delete board failed");
    }

    @PutMapping("/batch/status")
    @AuditLog(module = "板材管理", action = "批量启禁用")
    public Result batchUpdateStatus(@RequestBody @Valid BatchEnabledDTO batchEnabledDTO) {
        TBoard board = new TBoard();
        board.setIsEnabled(batchEnabledDTO.getIsEnabled());
        boolean updated = tBoardService.update(board,
                new UpdateWrapper<TBoard>().in("board_id", batchEnabledDTO.getIds()));
        return updated ? Result.success(Map.of("affected", batchEnabledDTO.getIds().size())) : Result.error("batch update board status failed");
    }

    @PostMapping
    @AuditLog(module = "板材管理", action = "新增")
    public Result save(@RequestBody @Valid TBoardDTO boardDTO) {
        TBoard board = new TBoard();
        BeanUtils.copyProperties(boardDTO, board);
        boolean saved = tBoardService.save(board);
        return saved ? Result.success() : Result.error("add board failed");
    }

    @PutMapping("/{id}")
    @AuditLog(module = "板材管理", action = "编辑")
    public Result update(@PathVariable @Positive(message = "id must be greater than 0") Long id,
                         @RequestBody @Valid TBoardVO boardVO) {
        TBoard board = new TBoard();
        BeanUtils.copyProperties(boardVO, board);
        board.setBoardId(id);
        boolean updated = tBoardService.updateById(board);
        return updated ? Result.success() : Result.error("update board failed");
    }

    private TBoardVO toVO(TBoard board) {
        TBoardVO boardVO = new TBoardVO();
        BeanUtils.copyProperties(board, boardVO);
        return boardVO;
    }
}

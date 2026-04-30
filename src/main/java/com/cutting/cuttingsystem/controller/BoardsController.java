package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

@RestController
@RequestMapping("/boards")
@Validated
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
    public Result deleteById(@PathVariable @Positive(message = "id must be greater than 0") Integer id) {
        boolean removed = tBoardService.removeById(id);
        return removed ? Result.success() : Result.error("delete board failed");
    }

    @PostMapping
    public Result save(@RequestBody @Valid TBoardDTO boardDTO) {
        TBoard board = new TBoard();
        BeanUtils.copyProperties(boardDTO, board);
        boolean saved = tBoardService.save(board);
        return saved ? Result.success() : Result.error("add board failed");
    }

    @PutMapping("/{id}")
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

package com.cutting.cuttingsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;
import com.cutting.cuttingsystem.entitys.DTO.TLayoutResultDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TLayoutResult;
import com.cutting.cuttingsystem.entitys.TOrder;
import com.cutting.cuttingsystem.entitys.TaskStatus;
import com.cutting.cuttingsystem.entitys.VO.TLayoutResultVO;
import com.cutting.cuttingsystem.entitys.algorithm.DTO.SolutionResponseDTO;
import com.cutting.cuttingsystem.service.GCodeService;
import com.cutting.cuttingsystem.service.TLayoutResultService;
import com.cutting.cuttingsystem.service.TOrderService;
import com.cutting.cuttingsystem.service.TProductionTaskService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/layout-results")
@Validated
@RequirePermission("layout:read")
public class LayoutResultController {
    @Autowired
    private TLayoutResultService layoutResultService;

    @Autowired
    private TOrderService orderService;

    @Autowired
    private TProductionTaskService productionTaskService;

    @Autowired
    private GCodeService gCodeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.upload.local-dir:uploads}")
    private String uploadDir;

    @GetMapping
    public Result pageQuery(@Valid QueryDTO query) {
        IPage<TLayoutResult> page = new Page<>(query.getPageNum(), query.getPageSize());
        QueryWrapper<TLayoutResult> qw = new QueryWrapper<TLayoutResult>().orderByDesc("create_time");
        IPage<TLayoutResultVO> layoutResultVOPage = layoutResultService.page(page, qw).convert(this::toVO);
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
    @AuditLog(module = "排样结果", action = "新增")
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
    @AuditLog(module = "排样结果", action = "删除")
    public Result deleteById(@PathVariable @Positive(message = "id must be greater than 0") Long id) {
        boolean removed = layoutResultService.removeById(id);
        return removed ? Result.success() : Result.error("delete layout result failed");
    }

    @GetMapping("/{id}/nc")
    public void exportNcFile(@PathVariable @Positive(message = "id must be greater than 0") Long id,
                             @RequestParam(defaultValue = "1.5") double toolRadius,
                             @RequestParam(defaultValue = "3.0") double cutDepth,
                             @RequestParam(defaultValue = "3000") int cutFeed,
                             @RequestParam(defaultValue = "18000") int spindleSpeed,
                             HttpServletResponse response) throws IOException {
        TLayoutResult layoutResult = layoutResultService.getById(id);
        if (layoutResult == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "排版结果不存在");
            return;
        }

        String resultJson = layoutResult.getResultJson();
        List<SolutionResponseDTO> solutions = objectMapper.readValue(
                resultJson, new TypeReference<>() {});

        GCodeService.CutParams cutParams = new GCodeService.CutParams(cutDepth, cutFeed, spindleSpeed);
        String gcode = gCodeService.generateNc(solutions, toolRadius, cutParams);

        // 保存 NC 文件到磁盘
        Path ncDir = Paths.get(uploadDir, "nc");
        Files.createDirectories(ncDir);
        String filename = String.format("layout-%d-%s.nc", id,
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
        Path filePath = ncDir.resolve(filename);
        Files.writeString(filePath, gcode);

        // 更新数据库记录
        String relativePath = Paths.get("uploads", "nc", filename).toString().replace('\\', '/');
        layoutResult.setNcFilePath(relativePath);
        layoutResultService.updateById(layoutResult);

        // 流式返回下载
        response.setContentType("text/plain; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        response.getOutputStream().write(gcode.getBytes(StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    private TLayoutResultVO toVO(TLayoutResult layoutResult) {
        TLayoutResultVO vo = new TLayoutResultVO();
        BeanUtils.copyProperties(layoutResult, vo);
        if (layoutResult.getTaskStatus() != null) {
            vo.setTaskStatusLabel(TaskStatus.fromCode(layoutResult.getTaskStatus()).getLabel());
        }
        if (layoutResult.getOrderId() != null) {
            TOrder order = orderService.getById(layoutResult.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setOrderName(order.getProcessName());
                vo.setCustomer(order.getCustomerName());
            }
            productionTaskService.listByOrderId(layoutResult.getOrderId()).stream()
                    .findFirst()
                    .ifPresent(task -> {
                        vo.setAssigneeId(task.getAssigneeId());
                        vo.setAssigneeName(task.getAssigneeName());
                    });
        }
        return vo;
    }
}

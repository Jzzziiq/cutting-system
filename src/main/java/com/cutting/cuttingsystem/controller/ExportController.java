package com.cutting.cuttingsystem.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.*;
import com.cutting.cuttingsystem.entitys.VO.TAuditLogVO;
import com.cutting.cuttingsystem.entitys.VO.TBoardVO;
import com.cutting.cuttingsystem.entitys.VO.TCustomerVO;
import com.cutting.cuttingsystem.mapper.TAuditLogMapper;
import com.cutting.cuttingsystem.service.TBoardService;
import com.cutting.cuttingsystem.service.TCustomerService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class ExportController {

    @Autowired
    private TCustomerService customerService;

    @Autowired
    private TBoardService boardService;

    @Autowired
    private TAuditLogMapper auditLogMapper;

    @GetMapping("/customers/export")
    @RequirePermission({"customer:read", "customer:write"})
    public void exportCustomers(HttpServletResponse response) throws IOException {
        setExcelResponse(response, "customers.xlsx");
        List<TCustomerVO> list = customerService.list().stream().map(e -> {
            TCustomerVO vo = new TCustomerVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
        EasyExcel.write(response.getOutputStream(), TCustomerVO.class).sheet("客户").doWrite(list);
    }

    @GetMapping("/boards/export")
    @RequirePermission({"board:read", "board:write"})
    public void exportBoards(@RequestParam(required = false) List<Long> ids,
                             HttpServletResponse response) throws IOException {
        setExcelResponse(response, "boards.xlsx");
        List<TBoard> boards = ids == null || ids.isEmpty() ? boardService.list() : boardService.listByIds(ids);
        List<TBoardVO> list = boards.stream().map(e -> {
            TBoardVO vo = new TBoardVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
        EasyExcel.write(response.getOutputStream(), TBoardVO.class).sheet("板材").doWrite(list);
    }

    @GetMapping("/audit-logs/export")
    @RequirePermission("audit:read")
    public void exportAuditLogs(HttpServletResponse response) throws IOException {
        setExcelResponse(response, "audit-logs.xlsx");
        List<TAuditLogVO> list = auditLogMapper.selectLogList(null, null, null);
        EasyExcel.write(response.getOutputStream(), TAuditLogVO.class).sheet("审计日志").doWrite(list);
    }

    private void setExcelResponse(HttpServletResponse response, String filename) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }
}

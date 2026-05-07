package com.cutting.cuttingsystem.controller;

import com.alibaba.excel.EasyExcel;
import com.cutting.cuttingsystem.annotation.AuditLog;
import com.cutting.cuttingsystem.annotation.RequirePermission;
import com.cutting.cuttingsystem.entitys.DTO.TBoardDTO;
import com.cutting.cuttingsystem.entitys.DTO.TCustomerDTO;
import com.cutting.cuttingsystem.entitys.Result;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.entitys.TCustomer;
import com.cutting.cuttingsystem.service.TBoardService;
import com.cutting.cuttingsystem.service.TCustomerService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ImportController {

    @Autowired
    private TCustomerService customerService;

    @Autowired
    private TBoardService boardService;

    @PostMapping("/customers/import")
    @RequirePermission({"customer:read", "customer:write"})
    @AuditLog(module = "客户管理", action = "批量导入")
    public Result importCustomers(@RequestParam MultipartFile file) {
        return doImport(file, TCustomerDTO.class, dto -> {
            TCustomer entity = new TCustomer();
            BeanUtils.copyProperties(dto, entity);
            customerService.save(entity);
        });
    }

    @PostMapping("/boards/import")
    @RequirePermission({"board:read", "board:write"})
    @AuditLog(module = "板材管理", action = "批量导入")
    public Result importBoards(@RequestParam MultipartFile file) {
        return doImport(file, TBoardDTO.class, dto -> {
            TBoard entity = new TBoard();
            BeanUtils.copyProperties(dto, entity);
            boardService.save(entity);
        });
    }

    @GetMapping("/customers/template")
    @RequirePermission({"customer:read", "customer:write"})
    public void downloadCustomerTemplate(HttpServletResponse response) throws IOException {
        downloadTemplate(response, "customer-template.xlsx", "客户导入模板.xlsx");
    }

    @GetMapping("/boards/template")
    @RequirePermission({"board:read", "board:write"})
    public void downloadBoardTemplate(HttpServletResponse response) throws IOException {
        downloadTemplate(response, "board-template.xlsx", "板材导入模板.xlsx");
    }

    private <T> Result doImport(MultipartFile file, Class<T> headClass, java.util.function.Consumer<T> saver) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }
        try (InputStream is = file.getInputStream()) {
            List<T> rows = EasyExcel.read(is).head(headClass).sheet().doReadSync();
            int success = 0, fail = 0;
            for (T row : rows) {
                try {
                    saver.accept(row);
                    success++;
                } catch (Exception e) {
                    fail++;
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", rows.size());
            result.put("success", success);
            result.put("fail", fail);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("文件读取失败");
        }
    }

    private void downloadTemplate(HttpServletResponse response, String templateFile, String outputName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encoded = URLEncoder.encode(outputName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        ClassPathResource resource = new ClassPathResource("templates/" + templateFile);
        try (InputStream is = resource.getInputStream()) {
            is.transferTo(response.getOutputStream());
        }
    }
}

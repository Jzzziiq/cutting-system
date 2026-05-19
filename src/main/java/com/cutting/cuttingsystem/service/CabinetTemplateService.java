package com.cutting.cuttingsystem.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cutting.cuttingsystem.entitys.CabinetTemplate;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;

public interface CabinetTemplateService extends IService<CabinetTemplate> {
    IPage<CabinetTemplate> pageQuery(QueryDTO query, String category);
    CabinetTemplate getTemplateById(Long id);
    CabinetTemplate createTemplate(CabinetTemplate template);
    CabinetTemplate updateTemplate(Long id, CabinetTemplate template);
    void deleteTemplate(Long id);
}

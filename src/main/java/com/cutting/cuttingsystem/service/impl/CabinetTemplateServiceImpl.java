package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.CabinetTemplate;
import com.cutting.cuttingsystem.entitys.DTO.QueryDTO;
import com.cutting.cuttingsystem.mapper.CabinetTemplateMapper;
import com.cutting.cuttingsystem.service.CabinetTemplateService;
import com.cutting.cuttingsystem.util.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CabinetTemplateServiceImpl extends ServiceImpl<CabinetTemplateMapper, CabinetTemplate>
        implements CabinetTemplateService {

    @Override
    public IPage<CabinetTemplate> pageQuery(QueryDTO query, String category) {
        Long currentUserId = UserContext.getCurrentUserId();
        QueryWrapper<CabinetTemplate> qw = new QueryWrapper<>();
        qw.and(w -> w.eq("is_official", 1).or().eq("created_by", currentUserId));
        if (StringUtils.hasText(category)) {
            qw.eq("category", category);
        }
        qw.orderByDesc("is_official").orderByDesc("create_time");
        return page(new Page<>(query.getPageNum(), query.getPageSize()), qw);
    }

    @Override
    public CabinetTemplate getTemplateById(Long id) {
        CabinetTemplate template = getById(id);
        if (template == null) return null;
        Long currentUserId = UserContext.getCurrentUserId();
        if (currentUserId == null) currentUserId = 0L;
        if (template.getIsOfficial() != 1 && !currentUserId.equals(template.getCreatedBy())) {
            return null;
        }
        return template;
    }

    @Override
    public CabinetTemplate createTemplate(CabinetTemplate template) {
        template.setCreatedBy(UserContext.getCurrentUserId());
        template.setIsOfficial(0);
        save(template);
        return template;
    }

    @Override
    public CabinetTemplate updateTemplate(Long id, CabinetTemplate template) {
        CabinetTemplate existing = getById(id);
        if (existing == null) throw new RuntimeException("模板不存在");
        Long currentUserId = UserContext.getCurrentUserId();
        if (existing.getIsOfficial() == 1) throw new RuntimeException("不能编辑官方模板");
        if (!currentUserId.equals(existing.getCreatedBy())) throw new RuntimeException("只能编辑自己的模板");
        template.setId(id);
        template.setIsOfficial(0);
        template.setCreatedBy(existing.getCreatedBy());
        updateById(template);
        return template;
    }

    @Override
    public void deleteTemplate(Long id) {
        CabinetTemplate existing = getById(id);
        if (existing == null) throw new RuntimeException("模板不存在");
        Long currentUserId = UserContext.getCurrentUserId();
        if (existing.getIsOfficial() == 1) throw new RuntimeException("不能删除官方模板");
        if (!currentUserId.equals(existing.getCreatedBy())) throw new RuntimeException("只能删除自己的模板");
        removeById(id);
    }
}

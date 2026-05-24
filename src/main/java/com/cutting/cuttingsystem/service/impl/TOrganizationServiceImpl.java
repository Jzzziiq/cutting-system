package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TOrganization;
import com.cutting.cuttingsystem.mapper.TOrganizationMapper;
import com.cutting.cuttingsystem.service.TOrganizationService;
import org.springframework.stereotype.Service;

/**
 * 组织表Service实现
 */
@Service
public class TOrganizationServiceImpl extends ServiceImpl<TOrganizationMapper, TOrganization> implements TOrganizationService {
}

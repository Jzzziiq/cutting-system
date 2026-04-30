package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TOffcut;
import com.cutting.cuttingsystem.mapper.TOffcutMapper;
import com.cutting.cuttingsystem.service.TOffcutService;
import org.springframework.stereotype.Service;

@Service
public class TOffcutServiceImpl extends ServiceImpl<TOffcutMapper, TOffcut> implements TOffcutService {
}

package com.cutting.cuttingsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cutting.cuttingsystem.entitys.TBoard;
import com.cutting.cuttingsystem.mapper.TBoardMapper;
import com.cutting.cuttingsystem.service.TBoardService;
import org.springframework.stereotype.Service;

@Service
public class TBoardServiceImpl extends ServiceImpl<TBoardMapper, TBoard> implements TBoardService {
}

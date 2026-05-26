package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cutting.cuttingsystem.entitys.TAuditLog;
import com.cutting.cuttingsystem.entitys.VO.TAuditLogVO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface TAuditLogMapper extends BaseMapper<TAuditLog> {

    IPage<TAuditLogVO> selectLogPage(Page<TAuditLogVO> page,
                                     @Param("module") String module,
                                     @Param("userId") Long userId,
                                     @Param("status") Integer status);

    List<TAuditLogVO> selectLogList(@Param("module") String module,
                                    @Param("userId") Long userId,
                                    @Param("status") Integer status);
}

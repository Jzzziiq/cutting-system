package com.cutting.cuttingsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cutting.cuttingsystem.entitys.TOrganization;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

/**
 * 组织表Mapper
 */
@Component
public interface TOrganizationMapper extends BaseMapper<TOrganization> {

    @Select("SELECT * FROM t_organization WHERE org_code = #{orgCode}")
    TOrganization selectByOrgCode(String orgCode);
}

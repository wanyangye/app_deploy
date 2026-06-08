package com.bc.app_deploy.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bc.app_deploy.model.entity.PluginDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PluginMapper extends BaseMapper<PluginDO> {
}

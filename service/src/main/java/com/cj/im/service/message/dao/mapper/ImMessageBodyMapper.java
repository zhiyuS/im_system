package com.cj.im.service.message.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cj.im.service.message.dao.ImMessageBodyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface ImMessageBodyMapper extends BaseMapper<ImMessageBodyEntity> {
}

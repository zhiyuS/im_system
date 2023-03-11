package com.cj.im.service.group.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cj.im.service.group.dao.ImGroupEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Mapper
public interface ImGroupMapper extends BaseMapper<ImGroupEntity> {

    /**
     * @description 获取加入的群的最大seq
     * @author chackylee
     * @return java.lang.Long
     */
    @Select(" <script> " +
            " select max(sequence) from im_group where app_id = #{appId} and group_id in " +
            "<foreach collection='groupId' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " </script> ")
    Long getGroupMaxSeq(Collection<String> groupId, Integer appId);
}

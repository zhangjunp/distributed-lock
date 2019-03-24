package com.zhangjp.distributedlock.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
 * 创建时间 2019年三月08日 星期五 10:34
 * 作者: zhangjunping
 * 描述：
 */
@Repository
public interface TestMapper {

    @Select("select global_id from global_id;")
    Integer getGlobalId();

    @Update("update global_id set global_id = #{id} where id = 1")
    void updateGlobalId(Integer id);

}

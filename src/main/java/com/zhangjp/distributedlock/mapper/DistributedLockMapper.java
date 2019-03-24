package com.zhangjp.distributedlock.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 创建时间 2019年三月21日 星期四 17:22
 * 作者: zhangjunping
 * 描述：分布式锁，基于数据库mapper
 */
@Repository
public interface DistributedLockMapper {

    @Insert("INSERT INTO distributed_lock (\n" +
            "	lock_name,\n" +
            "	secret_key\n" +
            ") SELECT\n" +
            "	#{lockName},\n" +
            "	#{secretKey}\n" +
            "FROM\n" +
            "	DUAL\n" +
            "WHERE\n" +
            "	NOT EXISTS (\n" +
            "		SELECT\n" +
            "			lock_name\n" +
            "		FROM\n" +
            "			distributed_lock\n" +
            "		WHERE\n" +
            "		lock_name = #{lockName}\n" +
            ")")
    Integer getLock(@Param("lockName") String lockName, @Param("secretKey") String secretKey);

    @Delete("delete from distributed_lock where secret_key = #{secret_key}")
    Integer removeLock(String secret_key);
}

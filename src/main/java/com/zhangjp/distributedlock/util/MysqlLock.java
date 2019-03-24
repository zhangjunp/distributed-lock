package com.zhangjp.distributedlock.util;

import com.zhangjp.distributedlock.mapper.DistributedLockMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

/**
 * 创建时间 2019年三月21日 星期四 17:35
 * 作者: zhangjunping
 * 描述：基于Mysql数据库，insert into if not exist 实现分布式锁
 */
@Component
@Log4j2
public class MysqlLock {

    @Resource
    private DistributedLockMapper lockMapper;


    public String lockWithTimeout(String lockKey, Long timeOut){
        String lockName = "mysql_lock:" + lockKey;
        String secret_key = UUID.randomUUID().toString();
        long endTime = System.currentTimeMillis() + timeOut;
        while(System.currentTimeMillis()< endTime){
//            数据库设置唯一索引，只能有一个插入成功！ 插入成功代表拿到锁了
            Integer count = lockMapper.getLock(lockName,secret_key);
            if (null != count && count.equals(1)) {
                return secret_key;
            }
        }
        log.warn("{},获取锁时间超时",lockName);
        return null;
    }

    public boolean releaseLock(String secret_key){
        boolean flag = false;
        Integer count = lockMapper.removeLock(secret_key);
        if (null != count && count.equals(1)) {
            flag = true;
        }
        return flag;
    }
}

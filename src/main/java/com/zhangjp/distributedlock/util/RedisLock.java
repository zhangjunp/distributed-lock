package com.zhangjp.distributedlock.util;

import com.zhangjp.distributedlock.config.RedisConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
/**
 * 创建时间 2019年三月14日 星期四 11:30
 * 作者: zhangjunping
 * 描述：基于Redis2.8.0+ 以上 实现分布式锁
 *
 * 疑问1：为什么要保证，NX和EX是原子操作？
 * 解答1：客户端1 当设置key成功后，再设置该key的失效时间时（Ex 为了防止死锁出现），出现故障，其他客户端无法获取到锁，造成死锁现象！
 *        所以要保证setNx和setEx是原子操作，redis2.8.0+版本之后，set命令可以实现，之前需要借助lua脚本实现！
 *
 *疑问2：为什么Value是一个随机唯一字符串？
 * 解答2：客户端1号，获取锁成功执行业务逻辑，业务逻辑阻塞，redis中锁的超时时间到了，自动释放掉了该锁（客户端1的锁）
 *         此时： 客户端2号，重新获取到客户端2的锁，
 *         同时：客户端1号，业务逻辑执行完，执行释放锁的逻辑，将客户端2号加的锁释放掉了
 *         所以为了防止，此类现象的发生，每一个锁的释放，都应该由持有锁的线程释放，所以唯一字符串是这把锁的秘钥
 *
 * 疑问3：get(key) 比较 vlaue 和del 这三个操作必须是原子的么？
 * 解答3：是的！
 * 场景：
 *         客户端1获取锁成功。
 *         客户端1访问共享资源。
 *         客户端1为了释放锁，先执行’GET’操作获取随机字符串的值。
 *         客户端1判断随机字符串的值，与预期的值相等。
 *         客户端1由于某个原因阻塞住了很长时间。
 *         过期时间到了，锁自动释放了。
 *         客户端2获取到了对应同一个资源的锁。
 *         客户端1从阻塞中恢复过来，执行DEL操纵，释放掉了客户端2持有的锁。
 *
 *  实际上，在上述第2个问题和第3个问题的分析中，如果不是客户端阻塞住了，而是出现了大的网络延迟，也有可能导致类似的执行序列发生。
 *
 *  疑问4：
 *      前面这个算法中出现的锁的有效时间(timeOut)，设置成多少合适呢？
 *      如果设置太短的话，锁就有可能在客户端完成对于共享资源的访问之前过期，从而失去保护；
 *      如果设置太长的话，一旦某个持有锁的客户端释放锁失败，那么就会导致所有其它客户端都无法获取锁，从而长时间内无法正常工作。
 *
 */
@Component
@Log4j2
public class RedisLock {
    @Resource
    private RedisTemplate<String ,String> redisTemplate;

    @Resource
    private RedisConfig redisConfig;

    /***
     * <p>Description: 分布式锁，加锁</p>
     * @param lockKey	锁的名称
     * @param acquireTimeout 定义在没有获取锁之前,锁的超时时间 （单位：毫秒）
     * @param timeOut 锁的超时时间  防止死锁发生 （单位：毫秒）
     * @return java.lang.String 返回加完锁产生的密钥 唯一标识用于以后解锁使用
     * @author zhangjunping
     * @date 2019/3/14 14:15
     */
    public String lockWithTimeout(String lockKey, Long acquireTimeout, Long timeOut) {
        String retIdentifierValue;
        // 1.随机生成一个value
        String identifierValue = UUID.randomUUID().toString();
        // 2.定义锁的名称
        String lockName = "redis_lock" + lockKey;
        // 3.定义上锁成功之后,锁的超时时间  以及 获取NX Option设置信息
        int expireLock = (int) (timeOut / 1000);
        RedisStringCommands.SetOption setOption = RedisStringCommands.SetOption.ifAbsent();
        // 4.定义在没有获取锁之前,锁的超时时间
        long endTime = System.currentTimeMillis() + acquireTimeout;
        while (System.currentTimeMillis() < endTime) {
            // 5.使用setNX方法设置锁值 ()
//            connection.set(lockName.getBytes(),identifierValue.getBytes(), Expiration.seconds(expireLock),SET_IF_PRESENT);
//            修改之后的命令，将setNX和expire命令合成一组原命令，排除在设置setNX之后和expire之间突然断掉，造成死锁问题
//            此命令仅仅支持在redis  版本以上
            Boolean nx = redisTemplate.opsForValue().setIfAbsent(lockName, identifierValue, expireLock, TimeUnit.SECONDS);
            if (null != nx && nx) {
                // 6.判断返回结果如果为true,则可以成功获取锁,并且设置锁的超时时间
                retIdentifierValue = identifierValue;
//                log.info("lockName:{},唯一标志：{}; 抢占加锁成功......",lockName,retIdentifierValue);
                return retIdentifierValue;
            }
            // 8.否则情况下继续循环等待
        }
        log.warn("{},获取锁时间超时",lockName);
        return null;
    }



    /***
     * <p>Description: 根据锁的名称，以及该锁的密钥解锁</p>
     *
     * @param identifier 锁的密钥
     * @return boolean 是否解锁
     * @author zhangjunping
     * @date 2019/3/14 14:14
     */
    public boolean releaseLock(String lockKey, String identifier) {
        boolean flag = false;
        // 1.定义锁的名称
        String lockName = "redis_lock" + lockKey;
        // 2.如果value与redis中一致则直接删除，否则等待超时
        RedisScript delIfValueEqual = RedisScript.of(redisConfig.getLuaScript(), Long.class);
        List<String> keys = new ArrayList<>();
        keys.add(lockName);
        Long result = (Long) redisTemplate.execute(delIfValueEqual, keys, new Object[]{identifier});
        if (null != result && 1 == result) {
            log.info("lockName:{},唯一标志：{}; 解锁成功......",lockName,identifier);
            flag=true;
        }
        return flag;
    }

}

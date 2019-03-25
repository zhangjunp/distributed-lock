## 基于Mysql，Redis，Zookeeper分布式锁实现

### 一、 mysql
 1. 思路--依赖唯一索引，以及INSERT INTO NOT EXISTS 实现分布式锁
 
 2. 建库脚本 ：
 
 3. `CREATE TABLE `distributed_lock` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `lock_name` varchar(255) DEFAULT NULL COMMENT '锁的名称',
  `secret_key` varchar(255) DEFAULT NULL COMMENT '锁的描述',
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_LOCK_NAME` (`lock_name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=199 DEFAULT CHARSET=utf8 COMMENT='分布式锁记录表';`

4. 这种实现方式，依赖数据库（单点故障），以及容易造成死锁，性能低，可靠性不高，不建议在成产环境中使用！
 上述缺点可以通过其他方式补偿，但是直接使用DB来进行分布式锁的服务比较少，因为DB层本身是我们需要重点保护的对象，
 在高并发场景中，我们往往需要使用CDN，缓存，限流等手段来减少对DB层的操作，所以在数据库的分布式锁，容易造成性能瓶颈，不建议使用！
 
### 二、 Redis分布式锁
1. 思路--Redis天然单线程的特性，设置锁：借助SetNx 和 SetEx 命令加锁和设置失效时间，并返回秘钥
2. 释放锁:get(lockKey)得到的秘钥 和 返回的秘钥 做比较，一致，说明是该线程的锁，执行del命令

### 三、 实现Redis分布式锁的思考
   - 疑问1：为什么要保证，NX和EX是原子操作？
   - 解答1：客户端1 当设置key成功后，再设置该key的失效时间时（Ex 为了防止死锁出现），出现故障，其他客户端无法获取到锁，造成死锁现象！
         所以要保证setNx和setEx是原子操作，redis2.8.0+版本之后，set命令可以实现，之前需要借助lua脚本实现！
 
   - 疑问2：为什么Value是一个随机唯一字符串？
   * 解答：是的！
   * 场景
     *  客户端1号，获取锁成功执行业务逻辑，业务逻辑阻塞，redis中锁的超时时间到了，自动释放掉了该锁（客户端1的锁）。
     *  此时： 客户端2号，重新获取到客户端2的锁。
     *  同时：客户端1号，业务逻辑执行完，执行释放锁的逻辑，将客户端2号加的锁释放掉了。
     *  所以为了防止，此类现象的发生，每一个锁的释放，都应该由持有锁的线程释放，所以唯一字符串是这把锁的秘钥

 
   - 疑问3：get(key) 比较 vlaue 和del 这三个操作必须是原子的么？
   * 解答： 是的！
   * 场景
     *  客户端1获取锁成功,客户端1访问共享资源。
     *  客户端1为了释放锁，先执行’GET’操作获取随机字符串的值。
     *  客户端1判断随机字符串的值，与预期的值相等。
     *  客户端1由于某个原因阻塞住了很长时间。过期时间到了，锁自动释放了。
     *  客户端2获取到了对应同一个资源的锁。
     *  客户端1从阻塞中恢复过来，执行DEL操纵，释放掉了客户端2持有的锁。

   实际上，在上述第2个问题和第3个问题的分析中，如果不是客户端阻塞住了，而是出现了大的网络延迟，也有可能导致类似的执行序列发生。
 
   - 疑问4：
       * 前面这个算法中出现的锁的有效时间(timeOut)，设置成多少合适呢？
       * 如果设置太短的话，锁就有可能在客户端完成对于共享资源的访问之前过期，从而失去保护；
       * 如果设置太长的话，一旦某个持有锁的客户端释放锁失败，那么就会导致所有其它客户端都无法获取锁，从而长时间内无法正常工作。

### 四、 Redis分布式锁的最佳实践
> Redis的作者antirez给出了一个更好的实现，称为Redlock;参考地址：https://redis.io/topics/distlock
> 基于java的实现：https://github.com/redisson/redisson

### 五、 ZK分布式锁

1. 借助Zookeerper的临时节点特性
2. 借助Zookeerper的事件通知特性
         
    


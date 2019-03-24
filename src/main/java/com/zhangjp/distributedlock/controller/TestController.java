package com.zhangjp.distributedlock.controller;

import com.zhangjp.distributedlock.service.GeneratorNumService;
import com.zhangjp.distributedlock.util.MysqlLock;
import com.zhangjp.distributedlock.util.RedisLock;
import com.zhangjp.distributedlock.util.ZookeeperLock;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 创建时间 2019年三月21日 星期四 17:52
 * 作者: zhangjunping
 * 描述：TODO 如果是在集群里，多个客户端同时修改一个共享的数据就需要分布式锁
 */
@RestController
@RequestMapping("test")
public class TestController {

    @Resource
    private RedisLock redisLock;

    @Resource
    private MysqlLock mysqlLock;

//    @Resource
//    private ZookeeperLock zkLock;

    @Resource
    private GeneratorNumService numService;

    private static String FLOW_NUM_LOCK = "flow_num";

    @GetMapping("noLock")
    public void testNoLock(){
        for (int i = 0; i < 50; i++) {
            new Thread(()-> System.out.println(numService.getFlowNum())).start();
        }
    }

    @GetMapping("mysqlLock")
    public String mysqlLock(){
        for (int i = 0; i < 50; i++) {
            new Thread(()->{
                String s = mysqlLock.lockWithTimeout(FLOW_NUM_LOCK, 5000L);
                if (!StringUtils.isEmpty(s)) {
                    String flowNum = numService.getFlowNum();
                    System.out.println("numService.getFlowNum() = " + flowNum);
                    mysqlLock.releaseLock(s);
                }
            }).start();
        }
        return "OK";
    }


    @GetMapping("redisLock")
    public String redisLock(){
        for (int i = 0; i < 50; i++) {
            new Thread(()->{
                String s = redisLock.lockWithTimeout(FLOW_NUM_LOCK, 10000L,40000L);
                if (!StringUtils.isEmpty(s)) {
                    String flowNum = numService.getFlowNum();
                    System.out.println("numService.getFlowNum() = " + flowNum);
                    redisLock.releaseLock(FLOW_NUM_LOCK,s);
                }
            }).start();
        }
        return "OK";
    }

    @GetMapping("zkLock")
    public String zkLock(){
        for (int i = 0; i <50; i++) {
            new Thread(()->{
//                为了模拟分布式情况，这边每个线程下的zk连接都是新的连接
                ZookeeperLock zookeeperLock = new ZookeeperLock();
                try{
                    zookeeperLock.getLock();
                    String flowNum = numService.getFlowNum();
                    System.out.println("numService.getFlowNum() = " + flowNum);
                }finally {
                    zookeeperLock.unLock();
                }
            }).start();
        }
        return "OK";
    }
}

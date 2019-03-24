package com.zhangjp.distributedlock.util;

import lombok.extern.log4j.Log4j2;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

/**
 * 创建时间 2019年三月24日 星期日 13:03
 * 作者: zhangjunping
 */

@Log4j2
@Component
public class ZookeeperLock{
//    @Resource //为模拟 多个连接
    private ZkClient zkClient = new ZkClient("192.168.63.131:2181");
    // path路径
    private String lockPath =  "/path";
    //  信号量
//    CountDownLatch是通过一个计数器来实现的，计数器的初始值为线程的数量。
//    每当一个线程完成了自己的任务后，计数器的值就会减1。当计数器值到达0时，
//    它表示所有的线程已经完成了任务，然后在该锁上等待的线程就可以恢复执行任务。
    private CountDownLatch countDownLatch = null;
    /***
     * <p>Description: 获取锁</p>
     * @author zhangjunping
     * @date 2019/3/24 16:04
     */
    public void getLock() {
        if (tryLock()) {
            log.info("####获取锁成功######");
        } else {
            waitLock();
            getLock();
        }
    }

    /***
     * <p>Description: 尝试获取锁</p>
     * @return boolean 是否成功
     * @author zhangjunping
     * @date 2019/3/24 16:05
     */
    private boolean tryLock() {
        try {
            zkClient.createEphemeral(lockPath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /***
     * <p>Description: 等待获取锁，采用zk的特性，事件监听和事件通知</p>
     * @author zhangjunping
     * @date 2019/3/24 16:05
     */
    private void waitLock() {
        // 使用zk临时事件监听
        IZkDataListener iZkDataListener = new IZkDataListener() {
            public void handleDataDeleted(String path) throws Exception {
                if (countDownLatch != null) {
//                    锁的路径被删除后，唤醒等待的线程
                    countDownLatch.countDown();
                }
            }
            public void handleDataChange(String arg0, Object arg1) throws Exception {}
        };
        // 注册事件通知
        zkClient.subscribeDataChanges(lockPath, iZkDataListener);
        if (zkClient.exists(lockPath)) {
            countDownLatch = new CountDownLatch(1);
            try {
//                锁的路径已经存在说明已经有线程抢占成功了，进入等待被唤醒
                countDownLatch.await();
            } catch (Exception e) {
                log.error("",e);
            }
        }
        // 监听完毕后，移除事件通知
        zkClient.unsubscribeDataChanges(lockPath, iZkDataListener);
    }


    /***
     * <p>Description: 释放锁</p>
     * @author zhangjunping
     * @date 2019/3/24 16:07
     */
    public void unLock() {
        if (zkClient != null) {
            log.info("#######释放锁#########");
//            zkClient.delete(lockPath);
            zkClient.close();
        }
    }
}

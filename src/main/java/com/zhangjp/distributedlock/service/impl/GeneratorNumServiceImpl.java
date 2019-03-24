package com.zhangjp.distributedlock.service.impl;

import com.zhangjp.distributedlock.service.GeneratorNumService;
import org.springframework.stereotype.Service;

/**
 * 创建时间 2019年三月22日 星期五 10:21
 * 作者: zhangjunping
 * 描述：TODO
 */
@Service
public class GeneratorNumServiceImpl implements GeneratorNumService {
    private static int num = 0;

    @Override
    public String getFlowNum() {
        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return  ++num + "";
    }
}

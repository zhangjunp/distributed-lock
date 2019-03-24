package com.zhangjp.distributedlock.config;

import lombok.Getter;
import lombok.Setter;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 创建时间 2019年三月14日 星期四 11:14
 * 作者: zhangjunping
 * 描述：Redis 配置
 */
@EnableRedisRepositories
@ConfigurationProperties(prefix = "spring.redis")
@Component
public class RedisConfig {
    @Setter
    private String host;
    @Setter
    private String password;
    @Setter
    private int port;
    @Setter
    private int database;
    @Setter
    @Getter
    private String luaScript;

    @Bean
    public JedisConnectionFactory connectionFactory (){
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(host,port);
        configuration.setPassword(password);
        configuration.setDatabase(database);
        configuration.setDatabase(database);
        JedisConnectionFactory factory = new JedisConnectionFactory(configuration);
        JedisPoolConfig config = new JedisPoolConfig();
        // 设置最大连接数
        config.setMaxTotal(200);
        // 设置最大空闲数
        config.setMaxIdle(10);
        // 设置最大等待时间
        config.setMaxWaitMillis(1000 * 100);
        // 在borrow一个jedis实例时，是否需要验证，若为true，则所有jedis实例均是可用的
        config.setTestOnBorrow(true);
        factory.setPoolConfig(config);
        return factory;
    }

    @Bean(name = "redisTemplate")
    public RedisTemplate redisTemplate () {
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory());
        return redisTemplate;
    }

    @Bean
    public ZkClient zkClient(){
        return new ZkClient("127.0.0.1:2181");
    }
}

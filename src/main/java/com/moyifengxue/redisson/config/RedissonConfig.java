package com.moyifengxue.redisson.config;

import com.moyifengxue.redisson.aop.DistributedLockAop;
import com.moyifengxue.redisson.properties.RedissonProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RedissonProperties.class)
public class RedissonConfig {

    private final RedissonProperties redissonProperties;

    private volatile RedissonClient redissonClient;

    public RedissonConfig(RedissonProperties redissonProperties) {
        this.redissonProperties = redissonProperties;
    }

    @Bean(destroyMethod = "shutdown") // 服务停止后调用 shutdown 方法。
    public RedissonClient redissonClient() {
        if (redissonClient == null) {
            synchronized (RedissonConfig.class) {
                if (redissonClient == null) {
                    Config config = new Config();
                    // 单机模式。
                    config.useSingleServer().setAddress("redis://" + redissonProperties.getAddress());
                    // 看门狗的默认时间。
                    config.setLockWatchdogTimeout(redissonProperties.getLockWatchdogTimeout());
                    redissonClient = Redisson.create(config);
                }
            }
        }
        return redissonClient;
    }

    @Bean
    public DistributedLockAop distributedLockAop() {
        return new DistributedLockAop(redissonProperties, redissonClient());
    }

}
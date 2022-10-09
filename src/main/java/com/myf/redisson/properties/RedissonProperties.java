package com.myf.redisson.properties;

import com.myf.redisson.constants.LockModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.redisson")
public class RedissonProperties {
    // ******************* DistributedLockAop注解相关 ******************* //
    /** 默认租赁时间:30S */
    private long defaultLeaseTime = 30_000L;
    /** 默认等待时间:10S */
    private long defaultWaitTime = 10_000L;
    /** 默认锁模式,此处配置AUTO不生效 */
    private LockModel defaultLockModel;

    /** 锁前缀 */
    private String redisNameSpace = "myf";


    // ********************** Redisson构建相关 ************************ //
    // https://github.com/redisson/redisson/wiki/2.-%E9%85%8D%E7%BD%AE%E6%96%B9%E6%B3%95 //
    /** 看门狗默认超时时间，消耗1/3续租 */
    private long lockWatchdogTimeout = 30_000L;
    /** Redis地址：单机模式后期有需要调整 */
    private String address = "127.0.0.1:6379";
}
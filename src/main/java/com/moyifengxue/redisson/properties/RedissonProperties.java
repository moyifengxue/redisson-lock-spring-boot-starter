package com.moyifengxue.redisson.properties;

import com.moyifengxue.redisson.constants.LockModel;
import org.springframework.boot.context.properties.ConfigurationProperties;

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

    public long getDefaultLeaseTime() {
        return defaultLeaseTime;
    }

    public void setDefaultLeaseTime(long defaultLeaseTime) {
        this.defaultLeaseTime = defaultLeaseTime;
    }

    public long getDefaultWaitTime() {
        return defaultWaitTime;
    }

    public void setDefaultWaitTime(long defaultWaitTime) {
        this.defaultWaitTime = defaultWaitTime;
    }

    public LockModel getDefaultLockModel() {
        return defaultLockModel;
    }

    public void setDefaultLockModel(LockModel defaultLockModel) {
        this.defaultLockModel = defaultLockModel;
    }

    public String getRedisNameSpace() {
        return redisNameSpace;
    }

    public void setRedisNameSpace(String redisNameSpace) {
        this.redisNameSpace = redisNameSpace;
    }

    public long getLockWatchdogTimeout() {
        return lockWatchdogTimeout;
    }

    public void setLockWatchdogTimeout(long lockWatchdogTimeout) {
        this.lockWatchdogTimeout = lockWatchdogTimeout;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
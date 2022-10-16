package com.moyifengxue.redisson.properties;

import com.moyifengxue.redisson.constants.LockModel;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.redis.distributed.lock")
public class DistributedLockProperties {
    /** 默认租赁时间:30S */
    private long defaultLeaseTime = 30_000L;
    /** 默认等待时间:10S */
    private long defaultWaitTime = 10_000L;
    /** 默认锁模式,此处配置AUTO不生效 */
    private LockModel defaultLockModel;
    /** 锁前缀 */
    private String redisNameSpace = "myf";

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
}

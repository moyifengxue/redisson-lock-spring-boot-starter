package com.myf.redisson.annotation;

import com.myf.redisson.constants.LockModel;

import java.lang.annotation.*;

/**
 * 分布式锁
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    /**
     * 锁的模式:默认自动模式,当参数只有一个使用 REENTRANT 参数多个使用RED_LOCK
     *
     * @return 锁模式
     */
    LockModel lockModel() default LockModel.AUTO;

    /**
     * 增加key的前缀
     * 设计中预将列表数组转化为多个key使用联锁——"#{#apple.getArray()}"——将数组的每一项作为一个锁。
     * "redisson-#{#apple.getArray()}" 显然会被认为是一个字符串，而不会使用联锁，应将前缀放到keyPrefix中
     */
    String keyPrefix() default "";

    /**
     * 如果keys有多个AUTO模式使用红锁
     *
     * @return keys
     */
    String[] keys() default {};

    /**
     * 租赁时间，默认为0取默认配置，-1，为无限续租。
     * @return 租赁时间
     */
    long leaseTime() default 0;

    /**
     * 等待时间，默认为0取默认配置，-1，为一直等待
     * @return 等待时间
     */
    long waitTime() default 0;
}


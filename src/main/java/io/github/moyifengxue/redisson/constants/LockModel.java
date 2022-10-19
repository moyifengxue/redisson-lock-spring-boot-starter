package io.github.moyifengxue.redisson.constants;

/**
 * @author zgx
 */
public enum LockModel {
    /**
     * 可重入锁
     */
    REENTRANT,
    /**
     * 公平锁
     */
    FAIR,
    /**
     * 联锁
     */
    MULTIPLE,
    /**
     * 红锁
     */
    RED_LOCK,
    /**
     * 读锁
     */
    READ,
    /**
     * 写锁
     */
    WRITE,
    /**
     * 自动模式,当参数只有一个使用 REENTRANT 参数多个 RED_LOCK
     */
    AUTO
}
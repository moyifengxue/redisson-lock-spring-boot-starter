package io.github.moyifengxue.redisson.aop;

import io.github.moyifengxue.redisson.annotation.DistributedLock;
import io.github.moyifengxue.redisson.constants.LockModel;
import io.github.moyifengxue.redisson.exception.LockException;
import io.github.moyifengxue.redisson.properties.DistributedLockProperties;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.RedissonMultiLock;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁AOP
 *
 * @author zgx
 */
@Aspect
public class DistributedLockAop {
    /**
     * 日志
     */
    private final Logger log = LoggerFactory.getLogger(getClass());
    /**
     * SpEL表达式
     */
    private final ExpressionParser parser = new SpelExpressionParser();
    /**
     * 表达式解析上下文，只有#{}里的内容才会被作为SqEL表达式解析
     */
    private final TemplateParserContext parserContext = new TemplateParserContext();

    private final DistributedLockProperties distributedLockProperties;
    private final RedissonClient redissonClient;

    public DistributedLockAop(DistributedLockProperties distributedLockProperties, RedissonClient redissonClient) {
        this.distributedLockProperties = distributedLockProperties;
        this.redissonClient = redissonClient;
    }

    /**
     * 切入点
     *
     * @param distributedLock 分布式锁注解
     */
    @Pointcut("@annotation(distributedLock)")
    public void log(DistributedLock distributedLock) {
    }

    @Around(value = "log(distributedLock)", argNames = "proceedingJoinPoint,distributedLock")
    public Object aroundPrintLog(ProceedingJoinPoint proceedingJoinPoint, DistributedLock distributedLock) throws Throwable {
        String[] keys = distributedLock.keys();
        if (keys.length == 0) {
            throw new LockException("keys不能为空");
        }
        String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(((MethodSignature) proceedingJoinPoint.getSignature()).getMethod());
        Object[] args = proceedingJoinPoint.getArgs();

        long leaseTime = distributedLock.leaseTime() == 0 ? distributedLockProperties.getDefaultLeaseTime() : distributedLock.leaseTime();
        long waitTime = distributedLock.waitTime() == 0 ? distributedLockProperties.getDefaultWaitTime() : distributedLock.waitTime();
        // 锁模式
        LockModel lockModel = distributedLock.lockModel();
        if (lockModel.equals(LockModel.AUTO)) {
            lockModel = Optional.ofNullable(distributedLockProperties.getDefaultLockModel())
                    .orElse(keys.length > 1 ? LockModel.RED_LOCK : LockModel.REENTRANT);
        }
        if (!lockModel.equals(LockModel.MULTIPLE) && !lockModel.equals(LockModel.RED_LOCK) && keys.length > 1) {
            throw new LockException("参数有多个,锁模式为 -> " + lockModel.name() + ".无法锁定");
        }
        log.debug("锁模式 -> {},等待锁定时间 -> {}秒.锁定最长时间 -> {}秒", lockModel.name(), waitTime / 1000, leaseTime / 1000);

        boolean res = false;
        RLock rLock = null;
        switch (lockModel) {
            case FAIR:
                rLock = redissonClient.getFairLock(getValueBySpEL(keys[0], distributedLock.keyPrefix(), parameterNames, args).get(0));
                break;
            case RED_LOCK:
                List<RLock> rLocks = new ArrayList<>();
                for (String key : keys) {
                    List<String> valueBySpEL = getValueBySpEL(key, distributedLock.keyPrefix(), parameterNames, args);
                    for (String s : valueBySpEL) {
                        rLocks.add(redissonClient.getLock(s));
                    }
                }
                RLock[] locks = new RLock[rLocks.size()];
                int index = 0;
                for (RLock r : rLocks) {
                    locks[index++] = r;
                }
                rLock = new RedissonRedLock(locks);
                break;
            case MULTIPLE:
                rLocks = new ArrayList<>();

                for (String key : keys) {
                    List<String> valueBySpEL = getValueBySpEL(key, distributedLock.keyPrefix(), parameterNames, args);
                    for (String s : valueBySpEL) {
                        rLocks.add(redissonClient.getLock(s));
                    }
                }
                locks = new RLock[rLocks.size()];
                index = 0;
                for (RLock r : rLocks) {
                    locks[index++] = r;
                }
                rLock = new RedissonMultiLock(locks);
                break;
            case REENTRANT:
                List<String> valueBySpEL = getValueBySpEL(keys[0], distributedLock.keyPrefix(), parameterNames, args);
                //如果SpEL表达式是数组或者LIST 则使用红锁
                if (valueBySpEL.size() == 1) {
                    rLock = redissonClient.getLock(valueBySpEL.get(0));
                } else {
                    locks = new RLock[valueBySpEL.size()];
                    index = 0;
                    for (String s : valueBySpEL) {
                        locks[index++] = redissonClient.getLock(s);
                    }
                    rLock = new RedissonRedLock(locks);
                }
                break;
            case READ:
                RReadWriteLock readLock = redissonClient.getReadWriteLock(getValueBySpEL(keys[0], distributedLock.keyPrefix(), parameterNames, args).get(0));
                rLock = readLock.readLock();
                break;
            case WRITE:
                RReadWriteLock writeLock = redissonClient.getReadWriteLock(getValueBySpEL(keys[0], distributedLock.keyPrefix(), parameterNames, args).get(0));
                rLock = writeLock.writeLock();
                break;
        }

        //执行aop
        String clasName = proceedingJoinPoint.getTarget().getClass().getName();
        String methodName = proceedingJoinPoint.getSignature().getName();
        if (rLock != null) {
            try {
                if (waitTime == -1) {
                    res = true;
                    //一直等待加锁
                    rLock.lock(leaseTime, TimeUnit.MILLISECONDS);
                } else {
                    res = rLock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
                }
                if (res) {
                    return proceedingJoinPoint.proceed();
                } else {
                    throw new LockException(String.format("%s.%s -- 获取锁失败", clasName, methodName));
                }
            } finally {
                if (res) {
                    rLock.unlock();
                }
            }
        }
        throw new LockException(String.format("%s.%s -- 创建锁失败", clasName, methodName));
    }


    /**
     * 解析SpEL表达式
     *
     * @param key            key
     * @param keyPrefix      前缀
     * @param parameterNames 参数列表
     * @param values         值
     * @return keys
     */
    private List<String> getValueBySpEL(String key, String keyPrefix, String[] parameterNames, Object[] values) {
        List<String> keys = new ArrayList<>();
        keyPrefix = StringUtils.hasLength(keyPrefix) ? keyPrefix + ":" : "";
        if (!key.contains("#")) {
            String resultKey = distributedLockProperties.getRedisNameSpace() + ":" + keyPrefix + key;
            keys.add(resultKey);
            return keys;
        }
        //SpEL上下文
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], values[i]);
        }
        Expression expression = parser.parseExpression(key, parserContext);
        Object value = expression.getValue(context);
        if (value != null) {
            // 数组列表使用联锁，key中不能含有杂质，"redisson-#{#apple.getArray()}" 显然会被认为是一个字符串，应将前缀放到keyPrefix中
            if (value instanceof List) {
                List<?> valueList = (List<?>) value;
                for (Object o : valueList) {
                    keys.add(distributedLockProperties.getRedisNameSpace() + ":" + keyPrefix + o.toString());
                }
            } else if (value.getClass().isArray()) {
                Object[] obj = (Object[]) value;
                for (Object o : obj) {
                    keys.add(distributedLockProperties.getRedisNameSpace() + ":" + keyPrefix + o.toString());
                }
            } else {
                keys.add(distributedLockProperties.getRedisNameSpace() + ":" + keyPrefix + value);
            }
        }
        log.debug("SpEL表达式key = {},value = {}", key, keys);
        return keys;
    }

}

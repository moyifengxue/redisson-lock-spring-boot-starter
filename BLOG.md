<div align="center">
    <a href="https://moyifeng.blog.csdn.net/"> <img src="https://badgen.net/badge/MYF/莫逸风BLOG/4ab8a1?icon=rss"></a>
    <a href="https://gitee.com/zhangguangxiang"> <img src="https://badgen.net/badge/MYF/莫逸风Gitee/4ab8a1?icon=git"></a>
</div>




@[TOC]

### 【企业级解决方案一】Redisson分布式锁Starter

#### 1. Redisson

Redisson是架设在Redis基础上的一个Java驻内存数据网格（In-Memory Data Grid）。充分的利用了Redis键值数据库提供的一系列优势，基于Java实用工具包中常用接口，为使用者提供了一系列具有分布式特性的常用工具类。使得原本作为协调单机多线程并发程序的工具包获得了协调分布式多机多线程并发系统的能力，大大降低了设计和研发大规模分布式系统的难度。同时结合各富特色的分布式服务，更进一步简化了分布式环境中程序相互之间的协作。

[【Redisson项目介绍—官方文档】](https://github.com/redisson/redisson/wiki/Redisson项目介绍)

#### 2. 自定义Starter

SpringBoot提供的starter以spring-boot-starter-xxx的方式命名，官方建议自定义starter使用xxx-spring-boot-starter规则命名，以区分SpringBoot生态提供的starter。

##### 2.1 新建工程名称为`redisson-lock-spring-boot-starter`，导入依赖


```xml
<!-- 自动配置 -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-autoconfigure</artifactId>
  <version>2.7.4</version>
</dependency>
<!-- Redisson -->
<dependency>
  <groupId>org.redisson</groupId>
  <artifactId>redisson</artifactId>
  <version>3.17.7</version>
</dependency>
<!-- 功能需要 -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-aop</artifactId>
  <version>2.7.4</version>
</dependency>
```

##### 2.2 编写分布式锁相关逻辑

**DistributedLock：分布式锁注解**

```java
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
```

**LockModel：支持锁模式**

```java
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
```

**LockException：自定义异常类**

```java
public class LockException extends RuntimeException{
  public LockException() {
  }

  public LockException(String message) {
    super(message);
  }

  public LockException(String message, Throwable cause) {
    super(message, cause);
  }

  public LockException(Throwable cause) {
    super(cause);
  }

  public LockException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
```

**RedissonProperties：配置类**

```java
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
		// get\set省略
}
```

##### 2.3 编写配置类RedissonConfig

```java
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
```

##### 2.4 resources下创建/META-INF/spring.factories，在该文件中配置自动配置类

```yaml
org.springframework.boot.autoconfigure.EnableAutoConfiguration=config.com.moyifengxue.redisson.RedissonConfig
```

#### 3. 测试

##### 3.1 新建SpringBoot工程lock-domo，创建时勾选web模块

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>

  <dependency>
    <groupId>com.myf</groupId>
    <artifactId>redisson-lock-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
  </dependency>

  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
  </dependency>
</dependencies>
```

##### 3.2 新建hello接口，使用分布式锁注解，注意看redis中数据

```java
@RestController
@RequestMapping("/hello-word")
@RequiredArgsConstructor
public class HelloController {
    @GetMapping("/hello")
    @DistributedLock(keys = "hello")
    public String Hello() throws InterruptedException {
        System.out.println(new Date());
        TimeUnit.SECONDS.sleep(10L);
        return "success";
    }
}
```

> 🏄🏻作者简介：CSDN博客专家，华为云云享专家，阿里云专家博主，疯狂coding的普通码农一枚
>
> 🚴🏻‍♂️个人主页：[莫逸风](https://moyifeng.blog.csdn.net/)&ensp;&emsp;
>
> 🇨🇳喜欢文章欢迎大家👍🏻点赞🙏🏻关注⭐️收藏📄评论↗️转发
>
> 👨🏻‍💻关联文章
>
>   🏹  [【Redisson分布式锁—官方文档】](https://github.com/redisson/redisson/wiki/8.-%E5%88%86%E5%B8%83%E5%BC%8F%E9%94%81%E5%92%8C%E5%90%8C%E6%AD%A5%E5%99%A8)
>
>   🏹  [【SimpleFunction系列二.1】渐进式理解Redis分布式锁](https://blog.csdn.net/qq_38723677/article/details/126132697)
>
>   🏹  [【SimpleFunction系列二.2】SpringBoot注解整合Redisson分布式锁](https://moyifeng.blog.csdn.net/article/details/126307218)
>
>   🏹  [【SimpleFunction系列二.3】Redisson分布式锁8种锁模式剖析](https://moyifeng.blog.csdn.net/article/details/126307879)


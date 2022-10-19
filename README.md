<div align="center">
    <a href="https://moyifeng.blog.csdn.net/"> <img src="https://badgen.net/badge/MYF/莫逸风BLOG/4ab8a1?icon=rss"></a>
    <a href="https://gitee.com/zhangguangxiang"> <img src="https://badgen.net/badge/MYF/莫逸风Gitee/4ab8a1?icon=git"></a>
</div>

## Redisson-DistributedLock启动器

将Redisson与SpringBoot库集成。依赖于Spring Data Redis模块。
初始支持SpringBoot 2.7.4

## Quick Start
1. 引入依赖
```xml
<dependency>
   <groupId>io.github.moyifengxue</groupId>
   <artifactId>redisson-lock-spring-boot-starter</artifactId>
   <version>2.7.4</version>
</dependency>
```
2. 将设置添加到application.yaml中
使用常见的 spring boot 设置：
```yaml
spring:
  redis:
    database: 
    host:
    port:
    password:
    ssl: 
    timeout:
    cluster:
      nodes:
    sentinel:
      master:
      nodes:
```
使用 Redisson 设置：
```yaml
spring:
  redis:
   redisson: 
      file: classpath:redisson.yaml
      config: |
        clusterServersConfig:
          idleConnectionTimeout: 10000
          connectTimeout: 10000
          timeout: 3000
          retryAttempts: 3
          retryInterval: 1500
          failedSlaveReconnectionInterval: 3000
          failedSlaveCheckInterval: 60000
          password: null
          subscriptionsPerConnection: 5
          clientName: null
          loadBalancer: !<org.redisson.connection.balancer.RoundRobinLoadBalancer> {}
          subscriptionConnectionMinimumIdleSize: 1
          subscriptionConnectionPoolSize: 50
          slaveConnectionMinimumIdleSize: 24
          slaveConnectionPoolSize: 64
          masterConnectionMinimumIdleSize: 24
          masterConnectionPoolSize: 64
          readMode: "SLAVE"
          subscriptionMode: "SLAVE"
          nodeAddresses:
          - "redis://127.0.0.1:7004"
          - "redis://127.0.0.1:7001"
          - "redis://127.0.0.1:7000"
          scanInterval: 1000
          pingConnectionInterval: 0
          keepAlive: false
          tcpNoDelay: false
        threads: 16
        nettyThreads: 32
        codec: !<org.redisson.codec.MarshallingCodec> {}
        transportMode: "NIO"
```
3. 分布式锁使用`DistributedLock`
   1. LockModel 
      
      支持多种锁模式，默认使用自动模式（AUTO）,当参数只有一个使用 REENTRANT 参数多个 RED_LOCK。
   2. keyPrefix
   
      锁前缀，设计中预将列表数组转化为多个key使用联锁——"#{#apple.getArray()}"——将数组的每一项作为一个锁。"redisson-#{#apple.getArray()}" 显然会被认为是一个字符串，而不会使用联锁，应将前缀放到keyPrefix中
   3. keys
   
      分布式锁key，#{#apple.getName()}，将参数apple中的name作为分布式锁的key。
   4. leaseTime

      租赁时间，默认为0取默认配置，-1，为无限续租。
   5. waitTime

      等待时间，默认为0取默认配置，-1，为一直等待
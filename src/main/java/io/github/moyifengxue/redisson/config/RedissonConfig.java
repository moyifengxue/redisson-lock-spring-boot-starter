package io.github.moyifengxue.redisson.config;

import io.github.moyifengxue.redisson.aop.DistributedLockAop;
import io.github.moyifengxue.redisson.customize.RedissonAutoConfigurationCustomizer;
import io.github.moyifengxue.redisson.properties.DistributedLockProperties;
import io.github.moyifengxue.redisson.properties.RedissonProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisOperations;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConditionalOnClass({Redisson.class, RedisOperations.class})
@AutoConfigureBefore(RedisAutoConfiguration.class)
@EnableConfigurationProperties({RedissonProperties.class, DistributedLockProperties.class, RedisProperties.class})
public class RedissonConfig {

    private static final String REDIS_PROTOCOL_PREFIX = "redis://";
    private static final String REDISS_PROTOCOL_PREFIX = "rediss://";

    private List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers;
    private final RedisProperties redisProperties;
    private final DistributedLockProperties distributedLockProperties;
    private final RedissonProperties redissonProperties;
    private final ApplicationContext ctx;
    private volatile RedissonClient redissonClient;

    public RedissonConfig(RedisProperties redisProperties, DistributedLockProperties distributedLockProperties, RedissonProperties redissonProperties, ApplicationContext ctx) {
        this.redisProperties = redisProperties;
        this.distributedLockProperties = distributedLockProperties;
        this.redissonProperties = redissonProperties;
        this.ctx = ctx;
    }

    @Autowired(required = false)
    public void setRedissonAutoConfigurationCustomizers(List<RedissonAutoConfigurationCustomizer> redissonAutoConfigurationCustomizers) {
        this.redissonAutoConfigurationCustomizers = redissonAutoConfigurationCustomizers;
    }

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() throws IOException {
        if (redissonClient == null) {
            synchronized (RedissonConfig.class) {
                if (redissonClient == null) {
                    redissonClient = redisson();
                }
            }
        }
        return redissonClient;
    }

    /**
     * 构建RedissonClient
     * source : redisson-spring-boot-starter
     * <a href="https://github.com/redisson/redisson/blob/master/redisson-spring-boot-starter/src/main/java/org/redisson/spring/starter/RedissonAutoConfiguration.java">...</a>
     *
     * @return 构建RedissonClient
     * @throws IOException fromYAML
     */
    public RedissonClient redisson() throws IOException {
        Config config;
        Duration timeoutValue = redisProperties.getTimeout();
        int timeout;
        if (null == timeoutValue) {
            timeout = 10000;
        } else {
            timeout = ((Long) timeoutValue.toMillis()).intValue();
        }

        String username = redisProperties.getUsername();

        if (redissonProperties.getConfig() != null) {
            try {
                config = Config.fromYAML(redissonProperties.getConfig());
            } catch (IOException e) {
                throw new IllegalArgumentException("Can't parse config", e);
            }
        } else if (redissonProperties.getFile() != null) {
            try {
                InputStream is = getConfigStream();
                config = Config.fromYAML(is);
            } catch (IOException e) {
                throw new IllegalArgumentException("Can't parse config", e);
            }
        } else if (redisProperties.getSentinel() != null) {
            List<String> nodesValue = redisProperties.getSentinel().getNodes();
            String[] nodes = convert(nodesValue);
            config = new Config();
            config.useSentinelServers()
                    .setMasterName(redisProperties.getSentinel().getMaster())
                    .addSentinelAddress(nodes)
                    .setDatabase(redisProperties.getDatabase())
                    .setConnectTimeout(timeout)
                    .setUsername(username)
                    .setPassword(redisProperties.getPassword());
        } else if (redisProperties.getCluster() != null) {
            List<String> nodesObject = redisProperties.getCluster().getNodes();
            String[] nodes = convert(nodesObject);
            config = new Config();
            config.useClusterServers()
                    .addNodeAddress(nodes)
                    .setConnectTimeout(timeout)
                    .setUsername(username)
                    .setPassword(redisProperties.getPassword());
        } else {
            config = new Config();
            String prefix = REDIS_PROTOCOL_PREFIX;
            if (redisProperties.isSsl()) {
                prefix = REDISS_PROTOCOL_PREFIX;
            }
            config.useSingleServer()
                    .setAddress(prefix + redisProperties.getHost() + ":" + redisProperties.getPort())
                    .setConnectTimeout(timeout)
                    .setDatabase(redisProperties.getDatabase())
                    .setUsername(username)
                    .setPassword(redisProperties.getPassword());
        }
        if (redissonAutoConfigurationCustomizers != null) {
            for (RedissonAutoConfigurationCustomizer customizer : redissonAutoConfigurationCustomizers) {
                customizer.customize(config);
            }
        }
        return Redisson.create(config);
    }

    @Bean
    public DistributedLockAop distributedLockAop() throws IOException {
        return new DistributedLockAop(distributedLockProperties, redissonClient());
    }

    private String[] convert(List<String> nodesObject) {
        List<String> nodes = new ArrayList<>(nodesObject.size());
        for (String node : nodesObject) {
            if (!node.startsWith(REDIS_PROTOCOL_PREFIX) && !node.startsWith(REDISS_PROTOCOL_PREFIX)) {
                nodes.add(REDIS_PROTOCOL_PREFIX + node);
            } else {
                nodes.add(node);
            }
        }
        return nodes.toArray(new String[nodes.size()]);
    }

    private InputStream getConfigStream() throws IOException {
        Resource resource = ctx.getResource(redissonProperties.getFile());
        return resource.getInputStream();
    }
}

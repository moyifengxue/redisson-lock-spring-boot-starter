package io.github.moyifengxue.redisson.customize;

import org.redisson.config.Config;

/**
 * Callback interface that can be implemented by beans wishing to customize
 * the {@link org.redisson.api.RedissonClient} autoconfiguration
 *
 * @author Nikos Kakavas (<a href="https://github.com/nikakis">...</a>)
 */
@FunctionalInterface
public interface RedissonAutoConfigurationCustomizer {
    /**
     * Customize the RedissonClient configuration.
     * @param configuration the {@link Config} to customize
     */
    void customize(final Config configuration);
}

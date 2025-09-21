package com.ebingo.backend.system.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonReactiveClient redissonReactiveClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379") // change if needed
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(10);

        RedissonClient client = Redisson.create(config);
        return client.reactive();
    }
}


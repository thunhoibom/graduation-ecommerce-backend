package org.monostudio.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.monostudio.config.cache.CacheNames;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisCacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues()
            .computePrefixWith(cacheName -> "monostudio::" + cacheName + "::")
            .entryTtl(Duration.ofMinutes(5));

        Map<String, RedisCacheConfiguration> ttlByCache = new HashMap<>();
        ttlByCache.put(CacheNames.PUBLIC_PRODUCTS_LIST, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        ttlByCache.put(CacheNames.PUBLIC_PRODUCT_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        ttlByCache.put(CacheNames.PUBLIC_CATEGORY_TREE, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        ttlByCache.put(CacheNames.PUBLIC_CATEGORY_BY_CODE, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        ttlByCache.put(CacheNames.PUBLIC_SHIPPING_METHODS, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        ttlByCache.put(CacheNames.PUBLIC_DISCOUNT_VALIDATION, defaultConfig.entryTtl(Duration.ofMinutes(2)));
        ttlByCache.put(CacheNames.ADMIN_DASHBOARD_STATS, defaultConfig.entryTtl(Duration.ofSeconds(60)));
        ttlByCache.put(CacheNames.ADMIN_REVENUE_STATS, defaultConfig.entryTtl(Duration.ofSeconds(60)));
        ttlByCache.put(CacheNames.ADMIN_TOP_PRODUCTS, defaultConfig.entryTtl(Duration.ofSeconds(60)));
        ttlByCache.put(CacheNames.ADMIN_LOW_STOCK, defaultConfig.entryTtl(Duration.ofSeconds(45)));
        ttlByCache.put(CacheNames.ADMIN_ORDER_STATUS_BREAKDOWN, defaultConfig.entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(ttlByCache)
            .transactionAware()
            .build();
    }
}

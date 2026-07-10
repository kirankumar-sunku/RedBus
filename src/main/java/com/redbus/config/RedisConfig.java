package com.redbus.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

/**
 * Redis Cache Configuration
 *
 * Cache names and their TTLs:
 *  - "buses"          → 15 min  (bus search results by from:to:date)
 *  - "availableSeats" → 15 min  (available seat numbers per busId)
 *  - "bookingHistory" → 15 min  (matches JWT expiry; explicitly evicted on signOut / forgotPassword)
 */
@Configuration
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        // ObjectMapper with Java 8 time support and full type info for polymorphic deserialization
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // Base cache config: String keys, JSON values, no null caching
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // Per-cache TTL configuration
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("buses",          baseConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("availableSeats", baseConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("bookingHistory", baseConfig.entryTtl(Duration.ofMinutes(15))); // matches JWT expiry; evicted explicitly on signOut/forgotPassword

        // June 16
        cacheConfigs.put("passenger", baseConfig.entryTtl(Duration.ofMinutes(9)));
        cacheConfigs.put("cancelTicket", baseConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("bus_deleted", baseConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("allBuses", baseConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("seatHolds", baseConfig.entryTtl(Duration.ofMinutes(9)));
        cacheConfigs.put("ticketDetails", baseConfig.entryTtl(Duration.ofMinutes(9)));

        return RedisCacheManager.builder(connectionFactory)
                //.cacheDefaults(baseConfig.entryTtl(Duration.ofMinutes(10)))
                .cacheDefaults(baseConfig)  // June - 16
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}

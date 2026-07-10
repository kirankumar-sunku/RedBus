package com.redbus.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisConfig – CacheManager bean")
class RedisConfigTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
    }

    @Test
    @DisplayName("cacheManager() should return a non-null RedisCacheManager")
    void cacheManager_returnsNonNull() {
        CacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        assertThat(cacheManager).isNotNull()
                .isInstanceOf(RedisCacheManager.class);
    }

    @Test
    @DisplayName("cacheManager() should pre-configure all nine named caches")
    void cacheManager_containsAllExpectedCacheNames() {
        RedisCacheManager cacheManager =
                (RedisCacheManager) redisConfig.cacheManager(connectionFactory);
        cacheManager.afterPropertiesSet();

        Collection<String> cacheNames = cacheManager.getCacheNames();

        assertThat(cacheNames)
                .hasSize(9)
                .containsExactlyInAnyOrder(
                        "buses", "availableSeats", "bookingHistory",
                        "passenger", "cancelTicket", "bus_deleted",
                        "allBuses", "seatHolds", "ticketDetails");
    }

    @Test
    @DisplayName("cacheManager() should create a 'buses' cache with 30-minute TTL")
    void cacheManager_busesCacheHasCorrectTtl() {
        RedisCacheManager cacheManager =
                (RedisCacheManager) redisConfig.cacheManager(connectionFactory);

        var busesCache = cacheManager.getCache("buses");
        assertThat(busesCache).isNotNull();
        assertThat(busesCache.getName()).isEqualTo("buses");
    }

    @Test
    @DisplayName("cacheManager() should create an 'availableSeats' cache")
    void cacheManager_availableSeatsCacheExists() {
        RedisCacheManager cacheManager =
                (RedisCacheManager) redisConfig.cacheManager(connectionFactory);

        assertThat(cacheManager.getCache("availableSeats")).isNotNull();
    }

    @Test
    @DisplayName("cacheManager() should create a 'bookingHistory' cache")
    void cacheManager_bookingHistoryCacheExists() {
        RedisCacheManager cacheManager =
                (RedisCacheManager) redisConfig.cacheManager(connectionFactory);

        assertThat(cacheManager.getCache("bookingHistory")).isNotNull();
    }
}

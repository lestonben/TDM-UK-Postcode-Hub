package com.project.tdm.application.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.tdm.application.dto.RouteDetailsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisZSetCommands;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;

@Repository
public class PostcodeRedisDao {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public List<String> getPostcodeFromCache(String redisKey, String query, int nums) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String prefix = query.trim().toUpperCase();

        Range<String> range = Range.closed(prefix, prefix + "\uFFFF");
        Limit limit = RedisZSetCommands.Limit.limit().count(nums);

        Set<String> results = redisTemplate.opsForZSet().rangeByLex(redisKey, range, limit);

        if (results == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(results);
    }

    public void addPostcodeToCache(String redisKey, String postcode) {
        if (postcode != null && !postcode.trim().isEmpty()) {
            redisTemplate.opsForZSet().add(redisKey, postcode.trim().toUpperCase(), 0);
        }
    }

    public RouteDetailsDTO getRouteFromCache(String redisKey) {
        String jsonString = redisTemplate.opsForValue().get(redisKey);

        try {
            if (jsonString == null || jsonString.isBlank()) {
                return null;
            }

            return objectMapper.readValue(jsonString, RouteDetailsDTO.class);
        }
        catch (Exception ex) {
            System.out.println("Exception Caught: " + ex.getMessage());
            return null;
        }
    }

    public void addRouteToCache(String redisKey, RouteDetailsDTO routeDetails) throws JsonProcessingException {
        String jsonString = objectMapper.writeValueAsString(routeDetails);

        redisTemplate.opsForValue().set(redisKey, jsonString, Duration.ofHours(24));
    }

    public void evictRouteCache(String updatedPostcode) {
        String CACHE_KEY_POSTCODE = updatedPostcode.replace(" ", "");

        // Clear routes where this postcode is the starting point (SW1A1AA:*)
        Set<String> keysAsStart = redisTemplate.keys(CACHE_KEY_POSTCODE + ":*");
        if (keysAsStart != null && !keysAsStart.isEmpty()) {
            System.out.println("Hit from evictRouteCache");
            redisTemplate.delete(keysAsStart);
        }

        // Clear routes where this postcode is the destination (*:SW1A1AA)
        Set<String> keysAsEnd = redisTemplate.keys("*:" + CACHE_KEY_POSTCODE);
        if (keysAsEnd != null && !keysAsEnd.isEmpty()) {
            System.out.println("Hit from evictRouteCache");
            redisTemplate.delete(keysAsEnd);
        }
    }
}

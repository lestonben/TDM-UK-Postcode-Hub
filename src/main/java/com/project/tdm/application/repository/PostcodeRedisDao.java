package com.project.tdm.application.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.tdm.application.dto.RouteDetailsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(PostcodeRedisDao.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public List<String> getPostcodeFromCache(String redisKey, String query, int nums) {
        logger.info("getPostcodeFromCache(): redis key = {}, query = {}, size = {}", redisKey, query, nums);

        if (query == null || query.trim().isEmpty()) {
            logger.info("getPostcodeFromCache(): no query is found.");
            return Collections.emptyList();
        }

        String prefix = query.trim().toUpperCase();

        Range<String> range = Range.closed(prefix, prefix + "\uFFFF");
        Limit limit = RedisZSetCommands.Limit.limit().count(nums);

        Set<String> results = redisTemplate.opsForZSet().rangeByLex(redisKey, range, limit);
        logger.info("getPostcodeFromCache(): search results: {}", ((results == null) ? 0 : results.size()));

        if (results == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(results);
    }

    public void addPostcodeToCache(String redisKey, String postcode) {
        if (postcode != null && !postcode.trim().isEmpty()) {
            redisTemplate.opsForZSet().add(redisKey, postcode.trim().toUpperCase(), 0);
            logger.info("addPostcodeToCache(): registered the key = {}, postcode = {} to the redis", redisKey, postcode);
        }
    }

    public RouteDetailsDTO getRouteFromCache(String redisKey) {
        logger.info("getRouteFromCache(): getting route details from cache for key = {} in Redis", redisKey);

        String jsonString = redisTemplate.opsForValue().get(redisKey);
        try {
            if (jsonString == null || jsonString.isBlank()) {
                logger.info("getRouteFromCache(): no available route details found from redis.");
                return null;
            }

            logger.info("getRouteFromCache(): a route details found from redis.");
            return objectMapper.readValue(jsonString, RouteDetailsDTO.class);
        }
        catch (Exception ex) {
            logger.error("getRouteFromCache(): unexpected error occurred ", ex);
            return null;
        }
    }

    public void addRouteToCache(String redisKey, RouteDetailsDTO routeDetails) throws JsonProcessingException {
        String jsonString = objectMapper.writeValueAsString(routeDetails);

        redisTemplate.opsForValue().set(redisKey, jsonString, Duration.ofHours(24));
        logger.info("addRouteToCache(): registered the key = {} to the redis", redisKey);
    }

    public void evictRouteCache(String updatedPostcode) {
        String CACHE_KEY_POSTCODE = updatedPostcode.replace(" ", "");

        try {
            // Clear routes where this postcode is the starting point (SW1A1AA:*)
            Set<String> keysAsStart = redisTemplate.keys(CACHE_KEY_POSTCODE + ":*");
            if (keysAsStart != null && !keysAsStart.isEmpty()) {
                redisTemplate.delete(keysAsStart);
                logger.info("evictRouteCache(): Redis has removed this match key: {}", keysAsStart);
            }

            // Clear routes where this postcode is the destination (*:SW1A1AA)
            Set<String> keysAsEnd = redisTemplate.keys("*:" + CACHE_KEY_POSTCODE);
            if (keysAsEnd != null && !keysAsEnd.isEmpty()) {
                redisTemplate.delete(keysAsEnd);
                logger.info("evictRouteCache(): Redis has removed this match key: {}", keysAsStart);
            }
        }
        catch (Exception ex) {
            logger.error("evictRouteCache(): unexpected error occurred ", ex);
        }
    }
}

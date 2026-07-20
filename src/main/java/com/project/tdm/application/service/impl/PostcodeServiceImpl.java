package com.project.tdm.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.tdm.application.dto.RouteDetailsDTO;
import com.project.tdm.application.entity.PostcodeEntity;
import com.project.tdm.application.repository.PostcodeRedisDao;
import com.project.tdm.application.repository.PostcodeRepo;
import com.project.tdm.application.service.PostcodeService;
import com.project.tdm.application.util.BaseConstants;
import com.project.tdm.security.util.GeoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PostcodeServiceImpl implements PostcodeService {

    private static final Logger logger = LoggerFactory.getLogger(PostcodeServiceImpl.class);
    @Autowired
    private PostcodeRedisDao postcodeRedisDao;

    private PostcodeRepo postcodeRepo;

    @Autowired
    public void setPostcodeRepo(PostcodeRepo postcodeRepo) {
        this.postcodeRepo = postcodeRepo;
    }

    @Override
    public PostcodeEntity getPostcode(String postcode) {
        String formattedPostcode = postcode.trim().toUpperCase();
        logger.info("getPostcode(): retrieving postcode details with formatted postcode = {}", formattedPostcode);

        return postcodeRepo.findPostcode(formattedPostcode)
                .orElseThrow(()-> new IllegalArgumentException(formattedPostcode + " is not available in the database."));
    }

    @Override
    public List<String> getPostcodeSuggestions(String query, int num) {
        // safety handling for searching postcode details for a null/empty input query
        if (query == null || query.trim().isEmpty()) {
            logger.info("getPostcodeSuggestions(): returning for a null/empty input query = {}", query);
            return List.of();
        }

        String formattedQuery = query.trim().toUpperCase();
        logger.info("getPostcodeSuggestions(): retrieving postcode suggestions with formatted postcode = {}", formattedQuery);

        List<String> redisResults = postcodeRedisDao.getPostcodeFromCache(BaseConstants.REDIS_KEY_PC_AUTOCOMPLETE, formattedQuery, num);
        if (!redisResults.isEmpty()) {
            logger.info("getPostcodeSuggestions(): returning postcode suggestions retrieved from redis, resultSize: {}", redisResults.size());
            return redisResults;
        }

        List<String> dbResults = postcodeRepo.findPostcodeSuggestions(formattedQuery, PageRequest.of(0, num));

        if (!dbResults.isEmpty()) {
            dbResults.forEach(postcode -> postcodeRedisDao.addPostcodeToCache(BaseConstants.REDIS_KEY_PC_AUTOCOMPLETE, postcode));
        }
        logger.info("getPostcodeSuggestions(): returning postcode suggestions retrieved from database, resultSize: {}", dbResults.size());
        return dbResults;
    }

    @Override
    public RouteDetailsDTO getPostcodesRoute(String postcodeFrom, String postcodeTo) throws JsonProcessingException {
        String formattedFrom = postcodeFrom.trim().toUpperCase();
        String formattedTo = postcodeTo.trim().toUpperCase();
        logger.info("getPostcodesRoute(): postcode formattedFrom = {}, formattedTo = {}", formattedFrom, formattedTo);

        String first = formattedFrom.compareTo(formattedTo) < 0 ? formattedFrom : formattedTo;
        String second = formattedFrom.compareTo(formattedTo) < 0 ? formattedTo : formattedFrom;
        String CACHE_KEY = first.replace(" ", "")+ ":" + second.replace(" ", "");
        logger.info("getPostcodesRoute(): rearranged postcodes first = {}, second = {}, CACHE_KEY = {}", first, second, CACHE_KEY);

        RouteDetailsDTO redisRoute = postcodeRedisDao.getRouteFromCache(CACHE_KEY);
        if (redisRoute != null) {
            logger.info("getPostcodesRoute(): returning route details retrieved from redis");
            return redisRoute;
        }

        RouteDetailsDTO dbRoute = generateRouteDetails(formattedFrom, formattedTo, first, second);

        postcodeRedisDao.addRouteToCache(CACHE_KEY, dbRoute);
        logger.info("getPostcodesRoute(): returning route details retrieved from database");
        return dbRoute;
    }


    @Override
    public String insertOrUpdatePostcode(String postcode, Double latitude, Double longitude) {
        String formattedPostcode = postcode.trim().toUpperCase();

        Optional<PostcodeEntity> existingPostcode = postcodeRepo.findPostcode(formattedPostcode);
        boolean isUpdate = existingPostcode.isPresent();
        logger.info("insertOrUpdatePostcode(): postcode = {}, isUpdate = {}", formattedPostcode, isUpdate);

        PostcodeEntity postcodeEntity = (isUpdate) ? existingPostcode.get() : new PostcodeEntity();
        postcodeEntity.setPostcode(formattedPostcode);
        postcodeEntity.setLatitude(latitude);
        postcodeEntity.setLongitude(longitude);

        postcodeRepo.save(postcodeEntity);

        try {
            if (isUpdate) {
                postcodeRedisDao.evictRouteCache(formattedPostcode);
            }
            else {
                postcodeRedisDao.addPostcodeToCache(BaseConstants.REDIS_KEY_PC_AUTOCOMPLETE, formattedPostcode);
            }
        }
        catch (Exception e) {
            logger.error("insertOrUpdatePostcode(): unexpected error occurred during operations in Redis ", e);
        }

        return (isUpdate) ? "Postcode mapping has been updated successfully." : "New postcode mapping has been created successfully.";
    }

    private RouteDetailsDTO generateRouteDetails(String formattedFrom, String formattedTo, String firstPostcode, String secondPostcode) {
        logger.info("generateRouteDetails(): getting postcode details for postcodeFrom = {}, postcodeTo = {}", firstPostcode, secondPostcode);

        List<PostcodeEntity> postcodeList = getPostcodes(firstPostcode, secondPostcode);
        PostcodeEntity postcodeFrom = postcodeList.stream().filter(p -> p.getPostcode().equals(formattedFrom)).findFirst().orElse(null);
        PostcodeEntity postcodeTo = postcodeList.stream().filter(p -> p.getPostcode().equals(formattedTo)).findFirst().orElse(null);

        if (postcodeFrom == null || postcodeTo == null) {
            throw new RuntimeException();
        }

        double distance = GeoUtil.calculateDistance(postcodeFrom.getLatitude(), postcodeFrom.getLongitude(), postcodeTo.getLatitude(), postcodeTo.getLongitude());
        logger.info("generateRouteDetails(): generating a route details for postcodeFrom = {}, postcodeTo = {}", postcodeFrom, postcodeTo);

        return new RouteDetailsDTO(
                formattedFrom, postcodeFrom.getLatitude(), postcodeFrom.getLongitude(),
                formattedTo, postcodeTo.getLatitude(), postcodeTo.getLongitude(),
                distance);
    }

    private List<PostcodeEntity> getPostcodes(String postcodeFrom, String postcodeTo) {
        logger.info("getPostcodes(): searching postcode details from DB, for postcodeFrom = {}, postcodeTo = {}", postcodeFrom, postcodeTo);

        List<PostcodeEntity> results = postcodeRepo.findPostcodes(postcodeFrom, postcodeTo);
        if (results != null && results.size() == 2) {
            logger.info("getPostcodes(): returning the postcode details, listSize: {}", results.size());
            return results;
        }

        results = results != null ? results : List.of();

        boolean isFromFound = results.stream().map(PostcodeEntity::getPostcode).anyMatch(postal -> postal.equals(postcodeFrom));
        boolean isToFound = results.stream().map(PostcodeEntity::getPostcode).anyMatch(postal -> postal.equals(postcodeTo));

        String errorMessage = (!isFromFound && !isToFound) ? "Both postcodes (" + postcodeFrom + " and " + postcodeTo + ") are unavailable."
                : (!isFromFound) ? "Starting postcode (" + postcodeFrom + ") is unavailable."
                : "Destination postcode (" + postcodeTo + ") is unavailable.";

        logger.warn("getPostcode(): failed with reason: {}", errorMessage);
        throw new IllegalArgumentException(errorMessage);
    }
}

package com.project.tdm.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.tdm.application.dto.RouteDetailsDTO;
import com.project.tdm.application.entity.PostcodeEntity;
import com.project.tdm.application.repository.PostcodeRedisDao;
import com.project.tdm.application.repository.PostcodeRepo;
import com.project.tdm.application.service.PostcodeService;
import com.project.tdm.application.util.BaseConstants;
import com.project.tdm.security.util.GeoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PostcodeServiceImpl implements PostcodeService {

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

        return postcodeRepo.findPostcode(formattedPostcode)
                .orElseThrow(()-> new IllegalArgumentException(formattedPostcode + " is not available in the database."));
    }

    @Override
    public List<String> getPostcodeSuggestions(String query, int num) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String formattedQuery = query.trim().toUpperCase();

        List<String> redisResults = postcodeRedisDao.getPostcodeFromCache(BaseConstants.REDIS_KEY_PC_AUTOCOMPLETE, formattedQuery, num);
        if (!redisResults.isEmpty()) {
            System.out.println("retrieved postcodes from redis.");
            return redisResults;
        }

        List<String> dbResults = postcodeRepo.findPostcodeSuggestions(formattedQuery, PageRequest.of(0, num));

        if (!dbResults.isEmpty()) {
            dbResults.forEach(postcode -> postcodeRedisDao.addPostcodeToCache(BaseConstants.REDIS_KEY_PC_AUTOCOMPLETE, postcode));
        }

        System.out.println("retrieved postcodes from database.");
        return dbResults;
    }

    @Override
    public RouteDetailsDTO getPostcodesRoute(String postcodeFrom, String postcodeTo) throws JsonProcessingException {
        String formattedFrom = postcodeFrom.trim().toUpperCase();
        String formattedTo = postcodeTo.trim().toUpperCase();

        String first = formattedFrom.compareTo(formattedTo) < 0 ? formattedFrom : formattedTo;
        String second = formattedFrom.compareTo(formattedTo) < 0 ? formattedTo : formattedFrom;
        String CACHE_KEY = first.replace(" ", "")+ ":" + second.replace(" ", "");

        RouteDetailsDTO redisRoute = postcodeRedisDao.getRouteFromCache(CACHE_KEY);
        if (redisRoute != null) {
            System.out.println("retrieved route details from redis.");
            return redisRoute;
        }

        RouteDetailsDTO dbRoute = generateRouteDetails(formattedFrom, formattedTo, first, second);

        postcodeRedisDao.addRouteToCache(CACHE_KEY, dbRoute);

        System.out.println("retrieved route details from database.");
        return dbRoute;
    }


    @Override
    public String insertOrUpdatePostcode(String postcode, Double latitude, Double longitude) {
        String formattedPostcode = postcode.trim().toUpperCase();

        Optional<PostcodeEntity> existingPostcode = postcodeRepo.findPostcode(formattedPostcode);
        boolean isUpdate = existingPostcode.isPresent();

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
        } catch (Exception e) {
            System.out.println("Redis operation failed: " + e.getMessage());
        }

        return (isUpdate) ? "Postcode mapping has been updated successfully." : "New postcode mapping has been created successfully.";
    }

    private RouteDetailsDTO generateRouteDetails(String formattedFrom, String formattedTo, String firstPostcode, String secondPostcode) {
        List<PostcodeEntity> postcodeList = getPostcodes(firstPostcode, secondPostcode);

        PostcodeEntity postcodeFrom = postcodeList.stream().filter(p -> p.getPostcode().equals(formattedFrom)).findFirst().orElse(null);
        PostcodeEntity postcodeTo = postcodeList.stream().filter(p -> p.getPostcode().equals(formattedTo)).findFirst().orElse(null);

        if (postcodeFrom == null || postcodeTo == null) {
            throw new RuntimeException();
        }

        double distance = GeoUtil.calculateDistance(postcodeFrom.getLatitude(), postcodeFrom.getLongitude(), postcodeTo.getLatitude(), postcodeTo.getLongitude());

        return new RouteDetailsDTO(
                formattedFrom, postcodeFrom.getLatitude(), postcodeFrom.getLongitude(),
                formattedTo, postcodeTo.getLatitude(), postcodeTo.getLongitude(),
                distance);
    }

    private List<PostcodeEntity> getPostcodes(String postcodeFrom, String postcodeTo) {
        List<PostcodeEntity> results = postcodeRepo.findPostcodes(postcodeFrom, postcodeTo);
        if (results != null && results.size() == 2) {
            return results;
        }

        results = results != null ? results : List.of();

        boolean isFromFound = results.stream().map(PostcodeEntity::getPostcode).anyMatch(postal -> postal.equals(postcodeFrom));
        boolean isToFound = results.stream().map(PostcodeEntity::getPostcode).anyMatch(postal -> postal.equals(postcodeTo));

        String errorMessage = (!isFromFound && !isToFound) ? "Both postcodes (" + postcodeFrom + " and " + postcodeTo + ") are unavailable."
                : (!isFromFound) ? "Starting postcode (" + postcodeFrom + ") is unavailable."
                : "Destination postcode (" + postcodeTo + ") is unavailable.";

        throw new IllegalArgumentException(errorMessage);
    }
}

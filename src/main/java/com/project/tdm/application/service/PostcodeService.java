package com.project.tdm.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.tdm.application.dto.RouteDetailsDTO;
import com.project.tdm.application.entity.PostcodeEntity;

import java.util.List;

public interface PostcodeService {
    PostcodeEntity getPostcode(String postcode);
    List<String> getPostcodeSuggestions(String query, int num);
    RouteDetailsDTO getPostcodesRoute(String postcodeFrom, String postcodeTo) throws JsonProcessingException;
    String insertOrUpdatePostcode(String postcode, Double latitude, Double longitude);
}

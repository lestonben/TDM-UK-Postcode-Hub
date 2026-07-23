package com.project.tdm.application.controller;

import com.project.tdm.application.dto.RouteDetailsDTO;
import com.project.tdm.application.entity.PostcodeEntity;
import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.service.PostcodeService;
import com.project.tdm.application.utilities.constant.BaseConstants;
import com.project.tdm.security.util.CookieUtil;
import com.project.tdm.security.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PostcodeController {

    private static final Logger logger = LoggerFactory.getLogger(PostcodeController.class);

    @Autowired
    private CookieUtil cookieUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PostcodeService postcodeService;

    @RequestMapping(value = "/api/postcodes/getCurrentUser", method = RequestMethod.GET)
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Object principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, String> response = new HashMap<>();
        response.put("username", ((UserEntity) principal).getUsername());
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/api/postcodes/suggest", method = RequestMethod.GET)
    public ResponseEntity<?> suggestPostcodes(@RequestParam("query") String input, @RequestParam("nums") int nums) {
        try {
            logger.info("suggestPostcodes(): received postcode suggestions request for input = {}", input);

            List<String> resList = postcodeService.getPostcodeSuggestions(input, nums);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("result", resList);
            logger.info("suggestPostcodes(): successfully retrieved {} postcode suggestions for input = {}", resList.size(), input);

            return ResponseEntity.ok(responseMap);
        }
        catch (Exception ex) {
            logger.error("suggest(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @RequestMapping(value = "/api/postcodes/searchRoute", method = RequestMethod.GET)
    public ResponseEntity<?> searchRoute(@RequestParam("postCodeFrom") String postCodeFrom, @RequestParam("postCodeTo") String postCodeTo) {
        try {
            logger.info("searchRoute(): received route request for postCodeFrom = {}, postCodeTo = {}", postCodeFrom, postCodeTo);

            RouteDetailsDTO routeDetail = postcodeService.getPostcodesRoute(postCodeFrom, postCodeTo);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("result", routeDetail);
            logger.info("searchRoute(): successfully retrieved the route details = {}", routeDetail.toString());

            return ResponseEntity.ok(responseMap);
        }
        catch (IllegalArgumentException ex) {
            logger.warn("searchRoute(): errorMessage = {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
            logger.error("searchRoute(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @RequestMapping(value = "/api/postcodes/searchQuery", method = RequestMethod.GET)
    public ResponseEntity<?> searchPostcode(@RequestParam("postcode") String postcode) {
        try {
            logger.info("searchPostcode(): received search postcode request for postcode = {}", postcode);

            PostcodeEntity postcodeDetail = postcodeService.getPostcode(postcode);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("result", postcodeDetail);
            logger.info("searchPostcode(): successfully retrieved the postcode details = {}", postcodeDetail.toString());

            return ResponseEntity.ok(responseMap);
        }
        catch (IllegalArgumentException ex) {
            logger.warn("searchPostcode(): errorMessage = {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
            logger.error("searchPostcode(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }

    @RequestMapping(value = "/api/postcodes/insertOrUpdate", method = RequestMethod.POST)
    public ResponseEntity<?> insertOrUpdatePostcode(@RequestBody Map<String, String> paramsMap) {
        String postcode = paramsMap.get("postcode");
        Double latitude = Double.valueOf(paramsMap.get("latitude"));
        Double longitude = Double.valueOf(paramsMap.get("longitude"));
        logger.info("insertOrUpdatePostcode(): received create/update request for postcode = {}, latitude = {}, longitude = {}", postcode, latitude, longitude);

        try {
            String message = postcodeService.insertOrUpdatePostcode(postcode, latitude, longitude);
            logger.info("insertOrUpdatePostcode(): postcode = {}, messageResult = {}", postcode, message);

            return ResponseEntity.ok(message);
        }
        catch (Exception ex) {
            logger.error("insertOrUpdatePostcode(): unexpected error occurred ", ex);
            return ResponseEntity.internalServerError().body(BaseConstants.UNEXPECTED_ERROR_MSG);
        }
    }
}

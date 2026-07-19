package com.project.tdm.application.controller;

import com.project.tdm.application.dto.RouteDetailsDTO;
import com.project.tdm.application.entity.PostcodeEntity;
import com.project.tdm.application.entity.UserEntity;
import com.project.tdm.application.service.PostcodeService;
import com.project.tdm.application.util.BaseConstants;
import com.project.tdm.security.util.CookieUtil;
import com.project.tdm.security.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class PostcodeController {

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
        List<String> resList = postcodeService.getPostcodeSuggestions(input, nums);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("result", resList);

        return ResponseEntity.ok(responseMap);
    }

    @RequestMapping(value = "/api/postcodes/searchRoute", method = RequestMethod.GET)
    public ResponseEntity<?> searchRoute(@RequestParam("postCodeFrom") String postCodeFrom, @RequestParam("postCodeTo") String postCodeTo) {
        try {
            RouteDetailsDTO routeDetail = postcodeService.getPostcodesRoute(postCodeFrom, postCodeTo);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("result", routeDetail);

            return ResponseEntity.ok(responseMap);
        }
        catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
            return ResponseEntity.internalServerError().body("An unexpected error occurred. Please try again.");
        }
    }

    @RequestMapping(value = "/api/postcodes/searchQuery", method = RequestMethod.GET)
    public ResponseEntity<?> searchPostcode(@RequestParam("postcode") String postcode) {
        try {
            PostcodeEntity postcodeDetail = postcodeService.getPostcode(postcode);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("result", postcodeDetail);

            return ResponseEntity.ok(responseMap);
        }
        catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            return ResponseEntity.internalServerError().body("An unexpected error occurred. Please try again.");
        }
    }

    @RequestMapping(value = "/api/postcodes/insertOrUpdate", method = RequestMethod.POST)
    public ResponseEntity<?> insertOrUpdatePostcode(@RequestBody Map<String, String> paramsMap) {
        String postcode = paramsMap.get("postcode");
        Double latitude = Double.valueOf(paramsMap.get("latitude"));
        Double longitude = Double.valueOf(paramsMap.get("longitude"));

        try {
            String message = postcodeService.insertOrUpdatePostcode(postcode, latitude, longitude);

            return ResponseEntity.ok(message);
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
            return ResponseEntity.internalServerError().body("An unexpected error occurred. Please try again.");
        }
    }
}

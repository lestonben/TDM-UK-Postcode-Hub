package com.project.tdm.application.utilities.util;

public class PostcodeValidationUtil {

    private static final String POSTCODE_REGEX = "^[A-Z]{1,2}[0-9][A-Z0-9]? ?[0-9][A-Z]{2}$";

    public static boolean isValidPostcode(String postcode) {
        if (postcode == null) return false;
        // Remove spaces for uniform checking, or match against standard format
        return postcode.matches(POSTCODE_REGEX);
    }

    public static boolean isValidCoordinates(Double lat, Double lon) {
        if (lat == null || lon == null) return false;
        // Check standard Earth coordinate boundaries
        boolean isValidLat = (lat >= -90.0 && lat <= 90.0);
        boolean isValidLon = (lon >= -180.0 && lon <= 180.0);
        return isValidLat && isValidLon;
    }
}

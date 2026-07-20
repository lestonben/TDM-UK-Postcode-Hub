package com.project.tdm.application.dto;

import java.io.Serializable;

public class RouteDetailsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fromPostcode;
    private double fromLat;
    private double fromLng;
    private String toPostcode;
    private double toLat;
    private double toLng;
    private double distance;

    public RouteDetailsDTO(){}

    public RouteDetailsDTO(String fromPostcode, double fromLat, double fromLng, String toPostcode, double toLat, double toLng, double distance) {
        this.fromPostcode = fromPostcode;
        this.fromLat = fromLat;
        this.fromLng = fromLng;
        this.toPostcode = toPostcode;
        this.toLat = toLat;
        this.toLng = toLng;
        this.distance = distance;
    }

    public String getFromPostcode() {
        return fromPostcode;
    }

    public void setFromPostcode(String fromPostcode) {
        this.fromPostcode = fromPostcode;
    }

    public double getFromLat() {
        return fromLat;
    }

    public void setFromLat(double fromLat) {
        this.fromLat = fromLat;
    }

    public double getFromLng() {
        return fromLng;
    }

    public void setFromLng(double fromLng) {
        this.fromLng = fromLng;
    }

    public String getToPostcode() {
        return toPostcode;
    }

    public void setToPostcode(String toPostcode) {
        this.toPostcode = toPostcode;
    }

    public double getToLat() {
        return toLat;
    }

    public void setToLat(double toLat) {
        this.toLat = toLat;
    }

    public double getToLng() {
        return toLng;
    }

    public void setToLng(double toLng) {
        this.toLng = toLng;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return "RouteDetailsDTO{" +
                "fromPostcode='" + fromPostcode + '\'' +
                ", fromLat=" + fromLat +
                ", fromLng=" + fromLng +
                ", toPostcode='" + toPostcode + '\'' +
                ", toLat=" + toLat +
                ", toLng=" + toLng +
                ", distance=" + distance +
                '}';
    }
}

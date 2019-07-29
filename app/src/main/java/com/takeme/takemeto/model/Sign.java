package com.takeme.takemeto.model;

import java.io.Serializable;

public class Sign implements Serializable {
    String destination;
    String downloadUrl;
    String from;
    LatLon latLon;

    public Sign() {
    }

    public Sign(String destination, String from) {
        this.destination = destination;
        this.from = from;
    }

    public Sign(String destination, String downloadUrl, String from) {
        this.destination = destination;
        this.downloadUrl = downloadUrl;
        this.from = from;
    }

    public Sign(String destination, String from, LatLon latLon) {
        this.destination = destination;
        this.from = from;
        this.latLon = latLon;
    }

    public Sign(String destination, String downloadUrl, String from, LatLon latLon) {
        this.destination = destination;
        this.downloadUrl = downloadUrl;
        this.from = from;
        this.latLon = latLon;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public LatLon getLatLon() {
        return latLon;
    }

    public void setLatLon(LatLon latLon) {
        this.latLon = latLon;
    }

    @Override
    public String toString() {
        return "Sign{" +
                "destination='" + destination + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", from='" + from + '\'' +
                ", latLon=" + latLon +
                '}';
    }
}

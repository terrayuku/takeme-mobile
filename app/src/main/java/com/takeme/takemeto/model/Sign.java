package com.takeme.takemeto.model;

import com.google.android.libraries.places.api.model.Place;

import java.io.Serializable;

public class Sign implements Serializable {
    Place destination;
    String downloadUrl;
    Place from;
    String userUID;

    public Sign() {
    }

    public Sign(Place destination, Place from) {
        this.destination = destination;
        this.from = from;
    }

    public Sign(Place destination, String downloadUrl, Place from) {
        this.destination = destination;
        this.downloadUrl = downloadUrl;
        this.from = from;
    }

    public Sign(Place destination, String downloadUrl, Place from, String userUID) {
        this.destination = destination;
        this.downloadUrl = downloadUrl;
        this.from = from;
        this.userUID = userUID;
    }

    public Sign(Place destination, Place from, double lat, double lon) {
        this.destination = destination;
        this.downloadUrl = downloadUrl;
        this.from = from;
    }

    public Place getDestination() {
        return destination;
    }

    public void setDestination(Place destination) {
        this.destination = destination;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Place getFrom() {
        return from;
    }

    public void setFrom(Place from) {
        this.from = from;
    }

    public String getUserUID() {
        return userUID;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }

    @Override
    public String toString() {
        return "Sign{" +
                "destination=" + destination +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", from=" + from +
                ", userUID='" + userUID + '\'' +
                '}';
    }
}

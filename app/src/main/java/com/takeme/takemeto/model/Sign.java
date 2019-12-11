package com.takeme.takemeto.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.android.libraries.places.api.model.Place;
import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Sign implements Serializable {
    Place destination;
    String downloadUrl;
    Place from;
    String userUID;
    String price;

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

    public Sign(Place destination, Place from, String price) {
        this.destination = destination;
        this.from = from;
        this.price = price;
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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("destination", destination);
        result.put("from", from);
        result.put("price", price);
        result.put("userUID", userUID);
        result.put("downloadUrl", downloadUrl);

        return result;
    }

    @Override
    public String toString() {
        return "Sign{" +
                "destination=" + destination +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", from=" + from +
                ", userUID='" + userUID + '\'' +
                ", price='" + price + '\'' +
                '}';
    }
}

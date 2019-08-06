package com.takeme.takemeto.impl;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Location implements Serializable {

    public Location() {
    }

    public Address address(String address, Activity activity) {
        Address location = null;

        if (address != null || !address.equals("")) {
            Geocoder geocoder = new Geocoder(activity);

            try {
                System.out.println("Geocode " + geocoder.getFromLocationName(address, 1).isEmpty());
                if(!geocoder.getFromLocationName(address, 1).isEmpty()) {
                    location = geocoder.getFromLocationName(address, 1).get(0);
                    // if feature is a country
                    if (location.getFeatureName().equalsIgnoreCase("South Africa")) {
                        location.setFeatureName(address);
                    }
                } else { // use device location then append the searched value
                    Locale locale = new Locale(address);
                    location = new Address(locale);
                    location.setFeatureName(address);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        System.out.println("Location: " + location);
        return location;
    }
}

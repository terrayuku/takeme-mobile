package com.takeme.takemeto.impl;

import android.content.Intent;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.takeme.takemeto.DisplaySignActivity;

import java.io.Serializable;
import java.util.Arrays;

public class Location implements Serializable {

    public Location() {
    }

    public SpannableStringBuilder message(String extra, Intent intent) {
        SpannableStringBuilder spannableStringBuilder = null;

        if(intent.getStringExtra(extra) != null) {
            spannableStringBuilder = new SpannableStringBuilder(intent.getStringExtra(extra));
            spannableStringBuilder.setSpan(
                    new ForegroundColorSpan(extra.equalsIgnoreCase(DisplaySignActivity.THANKYOU) ? Color.BLACK : Color.RED),
                    0,
                    intent.getStringExtra(extra).length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
            );

            return spannableStringBuilder;
        }

        return spannableStringBuilder;
    }

    public AutocompleteSupportFragment setPlace(AutocompleteSupportFragment fragment, String hint) {
        fragment.setHint(hint);
        fragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG));

        fragment.setLocationBias(RectangularBounds.newInstance(
                new LatLng(-34.277857, 18.2359139),
                new LatLng(-23.9116035, 29.380895)));

        return fragment;
    }
}

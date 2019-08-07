package com.takeme.takemeto.impl;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class Analytics {

    private FirebaseAnalytics analytics;


    public void setAnalytics(FirebaseAnalytics analytics, String name, String id, String message) {
        this.analytics = analytics;
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, message);
        analytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle);
    }
}

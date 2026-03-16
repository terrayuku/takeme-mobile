package com.takeme.takemeto.impl;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.takeme.takemeto.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlacesAutoCompleteAdapter extends ArrayAdapter<AutocompletePrediction> implements Filterable {

    private static final String TAG = "PlacesAutoComplete";
    private final PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private List<AutocompletePrediction> predictions = new ArrayList<>();

    private static final RectangularBounds SOUTH_AFRICA_BOUNDS = RectangularBounds.newInstance(
            new LatLng(-34.277857, 18.2359139),
            new LatLng(-23.9116035, 29.380895));

    public PlacesAutoCompleteAdapter(Context context, PlacesClient placesClient) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        this.placesClient = placesClient;
        this.sessionToken = AutocompleteSessionToken.newInstance();
    }

    /** Reset session token after a place is selected (for billing). */
    public void resetSession() {
        sessionToken = AutocompleteSessionToken.newInstance();
    }

    @Override
    public int getCount() {
        return predictions.size();
    }

    @Override
    public AutocompletePrediction getItem(int position) {
        return predictions.get(position);
    }

    public String getPlaceId(int position) {
        return predictions.get(position).getPlaceId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_place_prediction, parent, false);
        }
        AutocompletePrediction prediction = getItem(position);
        if (prediction != null) {
            TextView primary = convertView.findViewById(R.id.tv_primary);
            TextView secondary = convertView.findViewById(R.id.tv_secondary);
            primary.setText(prediction.getPrimaryText(null));
            secondary.setText(prediction.getSecondaryText(null));
        }
        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint == null || constraint.length() < 2) {
                    return results;
                }

                Log.d(TAG, "Searching for: " + constraint);

                try {
                    FindAutocompletePredictionsRequest request =
                            FindAutocompletePredictionsRequest.builder()
                                    .setSessionToken(sessionToken)
                                    .setLocationBias(SOUTH_AFRICA_BOUNDS)
                                    .setQuery(constraint.toString())
                                    .build();

                    FindAutocompletePredictionsResponse response =
                            Tasks.await(placesClient.findAutocompletePredictions(request), 10, TimeUnit.SECONDS);

                    List<AutocompletePrediction> list = response.getAutocompletePredictions();
                    Log.d(TAG, "Got " + list.size() + " predictions");
                    results.values = list;
                    results.count = list.size();
                } catch (Exception e) {
                    Log.e(TAG, "Autocomplete error: " + e.getMessage(), e);
                }
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                predictions.clear();
                if (results != null && results.values != null) {
                    predictions.addAll((List<AutocompletePrediction>) results.values);
                }
                notifyDataSetChanged();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                if (resultValue instanceof AutocompletePrediction) {
                    // Show only the primary place name (e.g. "Cape Town"), not the full address
                    return ((AutocompletePrediction) resultValue).getPrimaryText(null);
                }
                return super.convertResultToString(resultValue);
            }
        };
    }
}

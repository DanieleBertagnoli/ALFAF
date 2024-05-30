package com.project.alfaf.activities;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.project.alfaf.R;
import com.project.alfaf.databinding.ActivityEmergencyMapBinding;

import java.util.ArrayList;

public class EmergencyMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityEmergencyMapBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEmergencyMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Retrieve last known positions from intent
        ArrayList<String> lastKnownPositions = getIntent().getStringArrayListExtra("lastKnownPositions");

        if (lastKnownPositions != null) {
            for (String position : lastKnownPositions) {
                String[] latLng = position.split(" ");
                double latitude = Double.parseDouble(latLng[0]);
                double longitude = Double.parseDouble(latLng[1]);
                String date = latLng[2];
                String time = latLng[3];

                LatLng markerPosition = new LatLng(latitude, longitude);
                mMap.addMarker(new MarkerOptions().position(markerPosition).title(date + " " + time));
            }
            if (!lastKnownPositions.isEmpty()) {
                String[] firstPosition = lastKnownPositions.get(0).split(" ");
                double firstLatitude = Double.parseDouble(firstPosition[0]);
                double firstLongitude = Double.parseDouble(firstPosition[1]);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(firstLatitude, firstLongitude), 10));
            }
        }
    }
}

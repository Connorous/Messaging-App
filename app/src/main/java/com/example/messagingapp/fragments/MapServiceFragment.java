package com.example.messagingapp.fragments;

import androidx.annotation.Nullable;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.messagingapp.R;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.widget.Toast;

public class MapServiceFragment extends Fragment implements OnMapReadyCallback {

    //map view for the google map
    private MapView mapView;
    //google map object
    private GoogleMap map;
    //manager for getting user's location
    private LocationManager locationManager;
    //markers used on map
    private Marker mapMarker;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_map_service, container, false);
        //get reference to mapview
        mapView = (MapView) fragmentView.findViewById(R.id.mapView);
        //setup mapview
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        //set get location button to call get location method
        fragmentView.findViewById(R.id.getLocationButton).setOnClickListener(v -> {
            getLocation();
        });
        //return fragment view
        return fragmentView;
    }

    //when the map is ready setup the map object
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        //give it a marker somewhere in Sydney
        mapMarker = map.addMarker(new MarkerOptions().position(new LatLng(-34, 151)));
        //move map view to location somewhere in Sydney
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-34, 151), 10));
        //ask for permission to get user's location
        showPermissionDialog();
        //create a location manager for getting location
        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
    }

    //gets the current location of the user and sets the map to this location
    private void getLocation() {
        //check app permission for location
        if (ActivityCompat.checkSelfPermission(
                getContext(),Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //if permissions are not given ask for permissions
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        //if the permissions are given get location using gps
        else {
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //check locationGPS was gotten, otherwise it would be null
            if (locationGPS != null) {
                //get latitude and longitude of current location
                double latitude = locationGPS.getLatitude();
                double longitude = locationGPS.getLongitude();
                //if the map is set up remove the current marker, put a marker on current location,
                //and set map to current location
                if (map != null) {
                    mapMarker.remove();
                    mapMarker = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 10));
                }
            }
            //if the GPS service is null, show a message saying location could not be found
            else {
                Toast.makeText(getContext(), "Unable to find location.", Toast.LENGTH_SHORT).show();
            }
        }
    }



    //on re opening the map restart the mapview
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    //on closing the app pause the mapview
    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    //on the fragment destruction, destroy the mapview
    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    //on saving instance state do the same for the mapview
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    //on low memory set the mapview to low memory mode
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    //asks for permission to get user's location
    private void showPermissionDialog() {
        //check build version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //check location permission, if it is not permitted then show an message asking for permission
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //check for permission to show post notifications, if so continue
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.POST_NOTIFICATIONS)) {
                    //create alert asking permission to get the users location
                    new AlertDialog.Builder(getContext())
                            .setTitle("Permission Required")
                            .setMessage("This permission is needed to get your location")
                            //set ok button to ser permission to allowed
                            .setPositiveButton("OK", (dialog, listenerInterface) ->
                                    ActivityCompat.requestPermissions(getActivity(), new String[]{
                                            android.Manifest.permission.ACCESS_FINE_LOCATION
                                    }, 101))
                            //set cancel button to set permission to no allowed
                            .setNegativeButton("Cancel", (dialog, listenerInterface) -> dialog.dismiss())
                            //create and show the alert
                            .create()
                            .show();
                }
                // If it is not permitted to show post notifications, show a message asking for permission
                else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    }, 101);
                }
            }
        }
    }
}
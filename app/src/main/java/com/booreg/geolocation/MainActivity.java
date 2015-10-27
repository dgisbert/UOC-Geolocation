package com.booreg.geolocation;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.commons.lang3.StringUtils;

/**
 * Controller class for main activity
 */

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapLongClickListener, OnMarkerDragListener
{
    private GoogleMap map;
    private Marker    carMarker;

    private SharedPreferences sharedPreferences;

    private LatLng currentCarPosition = null;

    private static final String PREFERENCES_NAME = "GEOLOCATION_PREFERENCES_NAME";

    private static final String PREFERENCES_LOCATION_LONGITUDE = "PREFERENCES_LOCATION_LONGITUDE";
    private static final String PREFERENCES_LOCATION_LATITUDE  = "PREFERENCES_LOCATION_LATITUDE";

    //*****************************************************************************************************************
    // Private inner classes
    //*****************************************************************************************************************

    /**
     * Moves the camera map to the specified latitude/longitude
     */

    private void setMapLocation(GoogleMap map, LatLng latLng)
    {
        map.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    /**
     * Moves the camera map to the specified location
     */

    private void setMapLocation(GoogleMap map, Location location)
    {
        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

        setMapLocation(map, currentPosition);
    }

    /**
     * Puts a car Marker on the map, removing previous one if exists. Car marker is draggabe.
     */

    private void setCarMarkerLocation(LatLng latLng)
    {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.car)).draggable(true);

        if (carMarker != null) carMarker.remove();
        carMarker = map.addMarker(markerOptions);
    }

    /**
     * Saves current latitude/longitude on SharedPreferences
     */

    private void saveLatLng(SharedPreferences sharedPreferences, LatLng latLng)
    {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(PREFERENCES_LOCATION_LATITUDE, Double.toString(latLng.latitude));
        editor.putString(PREFERENCES_LOCATION_LONGITUDE, Double.toString(latLng.longitude));

        editor.apply();
    }

    /**
     * Listener class that gets the current Location position and moves the camera map according to it.s
     */

    private class CurrentLocationListener implements LocationListener
    {
        @Override
        public void onLocationChanged(Location location)
        {
            setMapLocation(map, location);
        }

        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override public void onProviderEnabled (String provider) {}
        @Override public void onProviderDisabled(String provider) {}
    }

    /**
     * Asks the system Location Service to set the current GPS position.
     */

    private void centerCurrentPositionInMap()
    {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED))
        {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            // Initially we move the map to the last known GPS position

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            setMapLocation(map, location);

            // Once positioned we ask to update the map to the current position

            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new CurrentLocationListener(), null);
        }
    }

    //*****************************************************************************************************************
    // Protected section
    //*****************************************************************************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Retrieve shared preferences with current car latitude and longitude to show it on map (if exists)

        sharedPreferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);

        String latitude  = sharedPreferences.getString(PREFERENCES_LOCATION_LATITUDE, "");
        String longitude = sharedPreferences.getString(PREFERENCES_LOCATION_LONGITUDE, "");

        if (!StringUtils.isBlank(latitude) && !StringUtils.isBlank(longitude))
        {
            currentCarPosition = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    //*****************************************************************************************************************
    // Public section
    //*****************************************************************************************************************


    /**
     * Initializes map parameters and centers map on current car position if it has been established before or, if not,
     * it centers map on current user position.
     */

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        map = googleMap;

        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);

        map.setOnMapLongClickListener(this);
        map.setOnMarkerDragListener(this);

        if (currentCarPosition != null)
        {
            centerCarInMap(null);
            setCarMarkerLocation(currentCarPosition);
        }
        else centerCurrentPositionInMap();
    }

    /**
     * Gets the position long-clicked on Map, saves it on SharedPreferences and resets the position of car icon o the map.
     */

    @Override
    public void onMapLongClick(LatLng latLng)
    {
        currentCarPosition = latLng;

        saveLatLng(sharedPreferences, currentCarPosition);

        setCarMarkerLocation(currentCarPosition);
    }

    /**
     * Saves the dragged target position on SharedPreferences and resets the position of car icon o the map.
     */

    @Override
    public void onMarkerDragEnd(Marker marker)
    {
        currentCarPosition = marker.getPosition();

        saveLatLng(sharedPreferences, currentCarPosition);
    }

    /**
     * Centers the map on car location responding to button click.
     */

    public void centerCarInMap(View view)
    {
        if (currentCarPosition == null) Toast.makeText(this, R.string.TXT00002, Toast.LENGTH_SHORT).show();
        else setMapLocation(map, currentCarPosition);
    }

    /**
     * Centers the map on user's current position responding to button click.
     */

    public void centerCurrentPositionInMap(View view)
    {
        centerCurrentPositionInMap();
    }

    @Override public void onMarkerDragStart(Marker marker) {}
    @Override public void onMarkerDrag(Marker marker) {}
}

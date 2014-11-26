package com.icechen1.sunwatch;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import java.util.Calendar;
import java.util.Date;

import static humanize.Humanize.naturalTime;

public class MainActivity extends Activity implements  GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GooglePlayServicesClient.ConnectionCallbacks {

    private GoogleApiClient mGoogleClient;
    private LocationRequest locationRequest;
    private Location mlocation;
    private TextView mSunRiseTextView;
    private TextView mSunSetTextView;
    private TextView mSunRiseTextViewTBA;
    private TextView mSunSetTextViewTBA;
    private RelativeLayout mOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mSunRiseTextView = (TextView) stub.findViewById(R.id.sunrise_textview);
                mSunSetTextView = (TextView) stub.findViewById(R.id.sunset_textview);

                mSunRiseTextViewTBA = (TextView) stub.findViewById(R.id.sunrise_tba);
                mSunSetTextViewTBA = (TextView) stub.findViewById(R.id.sunset_tba);

                mOverlay = (RelativeLayout) stub.findViewById(R.id.overlay);
            }
        });
        //getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS_;
        // Build a new GoogleApiClient
        mGoogleClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }
    public Calendar getNextSunRiseEvent() {
        Calendar datetime = Calendar.getInstance();
        com.luckycatlabs.sunrisesunset.dto.Location location = new com.luckycatlabs.sunrisesunset.dto.Location(mlocation.getLatitude(), mlocation.getLongitude());
        SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, datetime.getTimeZone());
        //Do not return events already over
        Calendar res = calculator.getOfficialSunriseCalendarForDate(datetime);
        if(res.before(datetime)){
            Calendar newDate = (Calendar) datetime.clone();
            newDate.set(Calendar.DAY_OF_YEAR, datetime.get(Calendar.DAY_OF_YEAR)+1);
            return calculator.getOfficialSunriseCalendarForDate(newDate);
        }else{
            return res;
        }
    }

    public Calendar getNextSunSetEvent() {
        Calendar datetime = Calendar.getInstance();
        com.luckycatlabs.sunrisesunset.dto.Location location = new com.luckycatlabs.sunrisesunset.dto.Location(mlocation.getLatitude(), mlocation.getLongitude());
        SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, datetime.getTimeZone());
        //Do not return events already over
        Calendar res = calculator.getOfficialSunsetCalendarForDate(datetime);
        if(res.before(datetime)){
            Calendar newDate = (Calendar) datetime.clone();
            newDate.set(Calendar.DAY_OF_YEAR, datetime.get(Calendar.DAY_OF_YEAR)+1);
            return calculator.getOfficialSunsetCalendarForDate(newDate);
        }else{
            return res;
        }
    }
    // Connect to Google Play Services when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleClient.connect();
    }

    // Register as a listener when connected
    @Override
    public void onConnected(Bundle connectionHint) {
        // Create the LocationRequest object
        locationRequest = LocationRequest.create();
        // Use high accuracy
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        // Set the update interval to 2 seconds
        locationRequest.setInterval(2);
        // Set the fastest update interval to 2 seconds
        locationRequest.setFastestInterval(2);
        // Set the minimum displacement
        locationRequest.setSmallestDisplacement(2);

        // Register listener using the LocationRequest object
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleClient, locationRequest, this);
        //Update using last known location
        updateViews(LocationServices.FusedLocationApi.getLastLocation(mGoogleClient));
    }

    @Override
    public void onDisconnected() {

    }

    // Disconnect from Google Play Services when the Activity stops
    @Override
    protected void onStop() {

        if (mGoogleClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleClient, this);
            mGoogleClient.disconnect();
        }
        super.onStop();
    }

    // Placeholders for required connection callbacks
    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    @Override
    public void onLocationChanged(Location location) {
        updateViews(location);
    }

    void updateViews(Location location){
        //Null check
        if(location == null){
            mOverlay.setVisibility(View.VISIBLE);
            return;
        }else{
            mOverlay.setVisibility(View.GONE);
        }
        // Display the latitude and longitude in the UI
        mlocation = location;
        // mTextView.setText("Latitude:  " + String.valueOf( location.getLatitude()) +
        //         "\nLongitude:  " + String.valueOf( location.getLongitude()));

        Calendar sunrise = getNextSunRiseEvent();
        Calendar sunset = getNextSunSetEvent();
        java.text.DateFormat timeFormatter = DateFormat.getTimeFormat(this);
        mSunRiseTextView.setText(timeFormatter.format(sunrise.getTime()));
        mSunSetTextView.setText(timeFormatter.format(sunset.getTime()));

        mSunRiseTextViewTBA.setText(naturalTime(sunrise.getTime()));
        mSunSetTextViewTBA.setText(naturalTime(sunset.getTime()));
    }
}

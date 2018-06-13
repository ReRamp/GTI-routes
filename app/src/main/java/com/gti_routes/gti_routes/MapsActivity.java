package com.gti_routes.gti_routes;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private Location lastlocation;
    private LocationListener locationListener;
    private Marker currentLocationmMarker;
    private Marker testmMarker;
    private ArrayList<LatLng> routeCoords = new ArrayList<LatLng>();
    public static final int REQUEST_LOCATION_CODE = 99;
    double latitude,longitude;
    private int n = 0;
    private boolean recording = false;
    public Button button = null;
    private FirebaseDatabase mDatabase;
    public ArrayList<ArrayList<LatLng>> RouteList = new ArrayList<>();

    public static class DataG {
        public DataG()
        {
        }

        public ArrayList<ArrayList<LatLng>> routes;
    }
// ...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        RouteList = new ArrayList<ArrayList<LatLng>>();

        button = findViewById(R.id.button_start_record);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(recording){
                    recording = false;
                    button.setText(R.string.startRecording);
                    saveRoute(routeCoords);
                    routeCoords.clear();
                    n = 0;
                }else if(!recording){
                    recording = true;
                    button.setText(R.string.stopRecording);
                }
            }
        });
        mDatabase = FirebaseDatabase.getInstance();
        mDatabase.getReference().addValueEventListener(postListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode)
        {
            case REQUEST_LOCATION_CODE:
                if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) !=  PackageManager.PERMISSION_GRANTED)
                    {
                        if(client == null)
                        {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else
                {
                    Toast.makeText(this,"Permission Denied" , Toast.LENGTH_LONG).show();
                }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        checkLocationPermission();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
        Log.d("aidan", "onMapReady");
        if(client.isConnected()){
            Log.d("aidan", "Client is connected");
            LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
        }
        //testMarker();

    }

    private void testMarker() {
        Marker test;
        MarkerOptions testOptions = new MarkerOptions();
        LatLng testCoord = new LatLng(47, 47);
        testOptions.position(testCoord);
        //testOptions.title("test");
        testOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        testmMarker = mMap.addMarker(testOptions);
    }

    protected synchronized void buildGoogleApiClient(){
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        client.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        //Log.v("onLocationChanged function entered");
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        lastlocation = location;
        if(recording) {
            if (currentLocationmMarker != null) {
                dropPin(currentLocationmMarker.getPosition());
                currentLocationmMarker.remove();
            }
            Log.d("lat = ", "" + latitude);
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Location");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            markerOptions.visible(false);
            currentLocationmMarker = mMap.addMarker(markerOptions);
            //mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            //mMap.animateCamera(CameraUpdateFactory.zoomBy(10));

            if(recording){

                drawLine(routeCoords);
            }

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();

        locationRequest.setInterval(1000);
        //locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(client,locationRequest,this);
            //LocationServices.getFusedLocationProviderClient();
        }
    }

    public void drawLine(ArrayList<LatLng> route){
        if (n >= 1) {
            //routeCoords = https://roads.googleapis.com/v1/snapToRoads?path=-35.27801,149.12958|-35.28032,149.12907|-35.28099,149.12929|-35.28144,149.12984|-35.28194,149.13003|-35.28282,149.12956|-35.28302,149.12881|-35.28473,149.12836&interpolate=true&key=YOUR_API_KEY
            //Polyline line = mMap.addPolyline(new PolylineOptions().addAll(routeCoords.).width(5).color(Color.RED));
            for (int i = 0; i < n - 1; i++) {
                Polyline line = mMap.addPolyline(new PolylineOptions()
                        .add(route.get(i), route.get(i + 1))
                        .width(5)
                        .color(Color.RED));
            }
        }
    }

    ValueEventListener postListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            DataG data = dataSnapshot.getValue(DataG.class);
          //  Log.v("lol", data.toString());
//            RouteList = dataSnapshot.getValue();

            Log.v("sams", RouteList.toString());

            for (ArrayList<LatLng> x : RouteList)
            {
                drawLine(x);
            }
            // ...

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w("SAM", "loadPost:onCancelled", databaseError.toException());
            // ...
        }
    };

    public void saveRoute(ArrayList<LatLng> route){

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("Data");

        Log.v("sam", route.toString());

        DataG d = new DataG();

        RouteList.add(route);
        d.routes = RouteList;

        ref.setValue(d);
    }

    public boolean checkLocationPermission() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)  != PackageManager.PERMISSION_GRANTED )
        {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION))
            {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
            }
            else
            {
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION },REQUEST_LOCATION_CODE);
            }
            return false;

        }
        else
            return true;
    }

    public void dropPin(LatLng newPin){
        MarkerOptions markerOptions = new MarkerOptions();
        //LatLng newPin = new LatLng(location.getLatitude(), location.getLongitude());
        markerOptions.position(newPin);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        markerOptions.visible(true);

        routeCoords.add(newPin);
        n++;
    }
    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
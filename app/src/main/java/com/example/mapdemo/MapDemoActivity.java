package com.example.mapdemo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

@RuntimePermissions
public class MapDemoActivity extends AppCompatActivity implements GoogleMap.OnMapLongClickListener {

    public static final String TAG = "MapDemoActivity";

    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private LocationRequest mLocationRequest;
    Location mCurrentLocation;
    private long UPDATE_INTERVAL = 60000;  /* 60 secs */
    private long FASTEST_INTERVAL = 5000; /* 5 secs */

    private final static String KEY_LOCATION = "location";
    private int DEFAULT_ZOOM = 18;

    /*
     * Define a request code to send to Google Play services This code is
     * returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_demo_activity);

        // Checks for API key in string resources
        if (TextUtils.isEmpty(getResources().getString(R.string.google_maps_api_key))) {
            throw new IllegalStateException("You forgot to supply a Google Maps API key");
        }

        // Checking if Bundle savedInstanceState contains user's current location
        if (savedInstanceState != null && savedInstanceState.keySet().contains(KEY_LOCATION)) {
            // Since KEY_LOCATION was found in the Bundle, we can be sure that mCurrentLocation
            // is not null.
            mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
        }

        // Find map fragment in map_demo_activity.xml
        mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        if (mapFragment != null) {
            // Begin asynchronous call for Google Map
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                // Kick off actions when map is loaded
                public void onMapReady(GoogleMap map) {
                    // Map has loaded!
                    // Set map type --
                    // -- Supported map types include: MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE
                    // -- MAP_TYPE_TERRAIN, MAP_TYPE_HYBRID
                    map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    // Load map
                    loadMap(map);
                    // Set the title/info text on top of pin
                    map.setInfoWindowAdapter(new CustomWindowAdapter(getLayoutInflater()));
                }
            });
        } else {
            Toast.makeText(this, "Error - Map Fragment was null!!", Toast.LENGTH_SHORT).show();
        }
    }

    protected void loadMap(GoogleMap googleMap) {
        // Initialize map variable using passed GoogleMap
        map = googleMap;
        if (map != null) {
            // Map is ready!

            // Calls our onMapLongClick method, which inflates a DialogAlert (message_item.xml)
            map.setOnMapLongClickListener(this);

            Log.i(TAG, "Map Fragment was loaded properly!");

            // Check current location once -- requires permission
            MapDemoActivityPermissionsDispatcher.getMyLocationWithPermissionCheck(this);
            // Continuously updates current location -- requires permission
            MapDemoActivityPermissionsDispatcher.startLocationUpdatesWithPermissionCheck(this);
        } else {
            Toast.makeText(this, "Error - Map was null!!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Checks permissions and grant location services if granted
        MapDemoActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @SuppressWarnings({"MissingPermission"})
    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    // Built-in function to check current user location
    void getMyLocation() {
        // Sets location
        map.setMyLocationEnabled(true);
        // Sets button to pin-point and move user camera to current location on Google Maps
        map.getUiSettings().setMyLocationButtonEnabled(true);

        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);
        // Checks the user's last checked in location
        locationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            // Pan camera to user's current location as method is invoked and location != null
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(),
                                            location.getLongitude()), DEFAULT_ZOOM));
                            // Call onLocationChanged if the location is not null
                            onLocationChanged(location);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MapDemoActivity", "Error trying to get last GPS location");
                        e.printStackTrace();
                    }
                });
    }

    /*
     * Called when the Activity becomes visible.
    */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /*
     * Called when the Activity is no longer visible.
	 */
    @Override
    protected void onStop() {
        super.onStop();
    }

    private boolean isGooglePlayServicesAvailable() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates", "Google Play services is available.");
            return true;
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(errorDialog);
                errorFragment.show(getSupportFragmentManager(), "Location Updates");
            }

            return false;
        }
    }

    @Override
    // This method will only kick off if the app is started, the user exited to another app and came back
    protected void onResume() {
        super.onResume();
        if (mCurrentLocation != null) {
            Log.d(TAG, "GPS location was found!");
            // Get current coordinates with latitude and longitute
            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            // Update the camera to move to current coordinates and set zoom value to 17 (10 is default)
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 17);
            // Invoke camera move animation
            map.animateCamera(cameraUpdate);
        } else {
            Log.d(TAG, "Current location was null, enable GPS on emulator!");
        }
        // Continuous location updates -- requires permission
        MapDemoActivityPermissionsDispatcher.startLocationUpdatesWithPermissionCheck(this);
    }

    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    // Get continuous location updates on user's current coordinates
    protected void startLocationUpdates() {
        // LocationRequest is a Play Services built-in method
        mLocationRequest = new LocationRequest();
        // Sets battery/power priority settings when checking location continuously
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        // Sets standard interval for checking location, regardless of priority
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Sets fastest interval for checking location taking into account battery/power priority
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);
        //noinspection MissingPermission
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }

    public void onLocationChanged(Location location) {
        // GPS may be turned off
        if (location == null) {
            return;
        }

        // Report to the UI that the location was updated
        mCurrentLocation = location;
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Log.i(TAG, "Updated Location: " + msg);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
        // Parcelable bundle that passes user's current location
    }

    @Override
    // This call is made when the user long clicks anywhere on the map
    public void onMapLongClick(LatLng latLng) {
        showAlertDialogForPoint(latLng);
    }

    // Method to display AlertDialog to user
    private void showAlertDialogForPoint(final LatLng latLng) {
        // This block inflates the dialog UI and creates the pop up for the user
        View messageView = LayoutInflater.from(MapDemoActivity.this).inflate(R.layout.message_item, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(messageView);
        final AlertDialog alertDialog = alertDialogBuilder.create();

        // Configure dialog button (OK)
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    // Method is called when user pressed OK on dialog alert
                    public void onClick(DialogInterface dialog, int which) {
                        // Define color of marker icon
                        BitmapDescriptor defaultMarker =
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);

                        // Define custom marker -- Original Code
                        /* BitmapDescriptor customMarker =
                                BitmapDescriptorFactory.fromResource(R.drawable.house_flag); */

                        // Define custom marker from vector asset
                        BitmapDescriptor customMarker =
                                bitmapDescriptorFromVector(MapDemoActivity.this, R.drawable.ic_gmaps_pin);
                        // Extract content from alert dialog
                        String title = ((EditText) alertDialog.findViewById(R.id.etTitle)).
                                getText().toString();
                        String snippet = ((EditText) alertDialog.findViewById(R.id.etSnippet)).
                                getText().toString();
                        // Creates and adds marker to the map
                        Marker marker = map.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(title)
                                .snippet(snippet)
                                .icon(customMarker));

                        // Animated pin drop effect
                        dropPinEffect(marker);
                    }
                });

        // Configure dialog button (Cancel)
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { dialog.cancel(); }
                });

        // Display the dialog
        alertDialog.show();
    }

    private void dropPinEffect(final Marker marker) {
        // Handler allows us to repeat a code block after a specified delay
        final android.os.Handler handler = new android.os.Handler();
        final long start = SystemClock.uptimeMillis();
        final long duration = 1500;

        // Use the bounce interpolator
        final android.view.animation.Interpolator interpolator =
                new BounceInterpolator();

        // Animate marker with a bounce updating its position every 15ms
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                // Calculate t for bounce based on elapsed time
                float t = Math.max(
                        1 - interpolator.getInterpolation((float) elapsed
                                / duration), 0);
                // Set the anchor
                marker.setAnchor(0.5f, 1.0f + 14 * t);

                if (t > 0.0) {
                    // Post this event again 15ms from now.
                    handler.postDelayed(this, 15);
                } else { // done elapsing, show window
                    marker.showInfoWindow();
                }
            }
        });
    }

    // This method converts vector assets into Bitmaps for use as map markers
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

}

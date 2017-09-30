package mydomain.homeward;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.ToneGenerator;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

public class MainActivity extends AppCompatActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,LocationListener {

    private float[] orientation = new float[3];
    private boolean orientationAvailable;
    private float[] magneticField = new float[3];
    private boolean magneticFieldAvailable;
    private float[] gravity = new float[3];
    private boolean gravityAvailable;
    private float[] proximity = new float[1];
    private boolean proximityAvailable;
    private float[] light = new float[1];
    private boolean lightAvailable;

    private SensorManager sensorManager;
    private Display screen;

    private GoogleApiClient googleAPIClient;
    private LocationRequest locationRequest;
    private Location birthLocation;
    private Location currentLocation;
    private float bearingToHome;
    private float realDiffrenceToHome;
    private int roundedDifferenceToHome;
    private ProgressBar progressBar;

    private Vibrator buzzer;
    final long[][] menuBuzz =
            {{0,250,50,250,500,250,50,250,500,250,50,250,500,250,50,250}};

    //-----------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        screen = ((WindowManager)getSystemService(WINDOW_SERVICE)).
                getDefaultDisplay();
        googleAPIClient = new GoogleApiClient.Builder(this).
                addConnectionCallbacks(this).addOnConnectionFailedListener(this).
                addApi(LocationServices.API).build();
        birthLocation = new Location("");
        birthLocation.setLongitude(-73.935242);
        birthLocation.setLatitude(40.730610);

        progressBar = (ProgressBar)findViewById(R.id.progressBar);

        buzzer = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);


    }
    //-----------------------------------------------------------------------------
    public void onStart() {
        Log.i("test", "about to start connecting");
        googleAPIClient.connect();
        super.onStart();
    }
    public void onStop() {
        Log.i("test", "is stopped");

        if(googleAPIClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    googleAPIClient, this);
            googleAPIClient.disconnect();
        }
        super.onStop();
    }


    @Override
    public void onResume() {

        super.onResume();

        googleAPIClient.connect();

        magneticFieldAvailable = startSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gravityAvailable = startSensor(Sensor.TYPE_ACCELEROMETER);
        orientationAvailable = magneticFieldAvailable && gravityAvailable;
       // proximityAvailable = startSensor(Sensor.TYPE_PROXIMITY);
        //lightAvailable = startSensor(Sensor.TYPE_LIGHT);
    }
    //-----------------------------------------------------------------------------
    @Override
    public void onPause() {

        super.onPause();
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleAPIClient, this);
        googleAPIClient.disconnect();

        sensorManager.unregisterListener(this);
    }
    //-----------------------------------------------------------------------------
    public void onConfigurationChanged(Configuration newConfig) {

        final String[]orientations  =
                {"Upright","Left side","Upside down","Right side"};

        super.onConfigurationChanged(newConfig);
       // ((TextView)findViewById(R.id.orientation)).setText("The rotation is " +
         //       orientations[screen.getRotation()]);
    }
    //-----------------------------------------------------------------------------
    private boolean startSensor(int sensorType) {

        if (sensorManager.getSensorList(sensorType).isEmpty()) {
            return(false);
        } else {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(sensorType),SensorManager.SENSOR_DELAY_NORMAL);
            return(true);
        }
    }
    //-----------------------------------------------------------------------------
    public void onSensorChanged(SensorEvent event) {

        boolean gravityChanged, magneticFieldChanged, orientationChanged;
        float R[] = new float[9];
        float I[] = new float[9];
        float newOrientation[] = new float[3];

        gravityChanged = magneticFieldChanged = orientationChanged = false;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravityChanged = arrayCopyChangeTest(event.values, gravity,
                        3, 1.0f);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticFieldChanged = arrayCopyChangeTest(event.values,
                        magneticField, 3, 1.0f);
                break;
            default:
                break;
        }

        if ((gravityChanged || magneticFieldChanged) &&
                SensorManager.getRotationMatrix(R, I, gravity, magneticField)) {
            SensorManager.getOrientation(R, newOrientation);
            newOrientation[0] = (float) Math.toDegrees(newOrientation[0]);
            newOrientation[1] = (float) Math.toDegrees(newOrientation[1]);
            newOrientation[2] = (float) Math.toDegrees(newOrientation[2]);


            realDiffrenceToHome = Math.abs(bearingToHome - newOrientation[0]);
            roundedDifferenceToHome = (int)Math.floor(realDiffrenceToHome);

            progressBar.setProgress(90 - roundedDifferenceToHome);

            if(roundedDifferenceToHome < 5) {

                Log.i("test", "vibrated");
                buzzer.vibrate(menuBuzz[(int)(Math.random()*menuBuzz.length)],-1);
            }




            orientationChanged = arrayCopyChangeTest(newOrientation,
                    orientation, 3, 5.0f); //----5 degrees
        }

        if (orientationChanged || gravityChanged){
            updateSensorDisplay();
        }
    }
    //-----------------------------------------------------------------------------
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    //-----------------------------------------------------------------------------
    private boolean arrayCopyChangeTest(float[] from,float[] to,int length,
                                        float amountForChange) {

        int copyIndex;
        boolean changed = false;

        for (copyIndex=0;copyIndex<length;copyIndex++) {
            if (Math.abs(from[copyIndex] - to[copyIndex]) > amountForChange) {
                to[copyIndex] = from[copyIndex];
                changed = true;
            }
        }
        return(changed);
    }
    //-----------------------------------------------------------------------------
    private void updateSensorDisplay() {

        String sensorValues = "";
        final String format = "%5.1f";

        sensorValues += "Orientation\n";
        if (orientationAvailable) {
            sensorValues +=
                    "A " + String.format(format, orientation[0]) + ", " +
                            "P " + String.format(format, orientation[1]) + ", " +
                            "R " + String.format(format, orientation[2]) + "\n\n";
        } else {
            sensorValues += "Not available\n\n";
        }

        sensorValues += "Gravity\n";
        if (gravityAvailable) {
            sensorValues +=
                    "X " + String.format(format, gravity[0]) + "," +
                            "Y " + String.format(format, gravity[1]) + "," +
                            "Z " + String.format(format, gravity[2]) + "\n\n";
        } else {
            sensorValues += "Not available\n\n";
        }

        sensorValues += "Proximity\n";
        if (proximityAvailable) {
            sensorValues +=
                    "P " + String.format(format, proximity[0]) + "\n\n";
        } else {
            sensorValues += "Not available\n\n";
        }

        sensorValues += "Light\n";
        if (lightAvailable) {
            sensorValues +=
                    "L " + String.format(format, light[0]) + "\n\n";
        } else {
            sensorValues += "Not available\n\n";
        }

       // ((TextView)findViewById(R.id.sensors)).setText(sensorValues);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i("test", "is connected");
        String errorMessage = "";
        LocationSettingsRequest.Builder settingsBuilder;
        PendingResult<LocationSettingsResult> pendingResult;
        LocationSettingsResult settingsResult;

        locationRequest = new LocationRequest();
        locationRequest.setInterval(getResources().getInteger(
                R.integer.time_between_location_updates_ms));
        locationRequest.setFastestInterval(getResources().getInteger(
                R.integer.time_between_location_updates_ms) / 2);
        locationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        settingsBuilder = new LocationSettingsRequest.Builder();
        settingsBuilder.addLocationRequest(locationRequest);
        pendingResult = LocationServices.SettingsApi.checkLocationSettings(
                googleAPIClient,settingsBuilder.build());

        startLocationUpdates();
    }
    private void startLocationUpdates() {
        Log.i("test", "starting location updates");
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleAPIClient,locationRequest,this);
        } catch (SecurityException e) {
            Toast.makeText(this,"Cannot get updates",Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleAPIClient, this);
        currentLocation = location;
        bearingToHome = location.bearingTo(birthLocation);

        magneticFieldAvailable = startSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gravityAvailable = startSensor(Sensor.TYPE_ACCELEROMETER);
        orientationAvailable = magneticFieldAvailable && gravityAvailable;





    }



    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


//-----------------------------------------------------------------------------
}
//=============================================================================


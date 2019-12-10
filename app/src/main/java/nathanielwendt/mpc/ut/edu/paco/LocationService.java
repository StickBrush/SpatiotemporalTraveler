package nathanielwendt.mpc.ut.edu.paco;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.ut.mpc.utils.LSTFilter;
import com.ut.mpc.utils.STPoint;

import java.util.ArrayList;
import java.util.List;

import nathanielwendt.mpc.ut.edu.paco.utils.SQLiteRTree;

public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final String ACTION_LOC = "ACTION_LOC";
    public static final String LAT_LONG_DATA = "LAT_LONG_DATA";
    protected LocationRequest mLocationRequest;
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    List<STPoint> buffer = new ArrayList<STPoint>();
    private final int BUFFER_SIZE = 25;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("LST", "creating service");
        buildGoogleApiClient();
    }

    private List<LSTFilter> getInitializedFilters(){
        List<LSTFilter> filters = new ArrayList<>();
        LatLng defLoc = Constants.DEFAULT_LAT_LNG;
        STPoint refPoint = new STPoint((float) defLoc.longitude, (float) defLoc.latitude, 0);

        SQLiteRTree rtree = new SQLiteRTree(this, "RTreeMain");
        LSTFilter filterMain = new LSTFilter(rtree);
        filterMain.setRefPoint(refPoint);
        filterMain.setSmartInsert(false);
        filters.add(filterMain);

        SQLiteRTree rtreeMid = new SQLiteRTree(this, "RTreeMid");
        rtreeMid.forceCreateTable();
        LSTFilter filterMid = new LSTFilter(rtreeMid);
        filterMid.setRefPoint(refPoint);
        filterMid.setSmartInsert(true);
        filterMid.setSmartInsertThresh(0.8);
        filters.add(filterMid);

        SQLiteRTree rtreeSparse = new SQLiteRTree(this, "RTreeSparse");
        LSTFilter filterSparse = new LSTFilter(rtreeSparse);
        rtreeSparse.forceCreateTable();
        filterSparse.setRefPoint(refPoint);
        filterSparse.setSmartInsert(true);
        filterSparse.setSmartInsertThresh(0.2);
        filters.add(filterSparse);

        return filters;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("LST", "stopping service");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        Log.d("LST", "clearing buffer and loading to filter");
        clearBuffer();
    }

    private void clearBuffer(){
        Log.d("LST", "clearing buffer and loading to filter");

        List<LSTFilter> filters = getInitializedFilters();

        for(STPoint point : buffer){
            for(LSTFilter filter : filters){
                filter.insert(point);
            }
        }
        buffer.clear();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("LST", "on start command");
        mGoogleApiClient.connect();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(Constants.LOC_POLLING_INTERVAL);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(Constants.LOC_FASTEST_INTERVAL);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); //TODO: PRIORITY_BALANCED_POWER_ACCURACY instead?
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }


    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLastLocation != null) {
            Intent intent = new Intent();
            intent.setAction(ACTION_LOC);
            double[] latlong = new double[]{mLastLocation.getLatitude(), mLastLocation.getLongitude()};
            intent.putExtra(LAT_LONG_DATA, latlong);
            sendBroadcast(intent);
        }
        else
            Toast.makeText(this, "No location detected. Make sure location is enabled on the device.", Toast.LENGTH_LONG).show();
        startLocationUpdates();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i("basic-location-sample", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i("basic-location-sample", "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location l) {
        STPoint currPoint = new STPoint((float) l.getLongitude(),
                (float) l.getLatitude(), System.currentTimeMillis());
        Log.d("LST", currPoint.toString());

        buffer.add(currPoint);
        if(buffer.size() >= BUFFER_SIZE){
            clearBuffer();
//            Log.d("LST", "clearing buffer and loading to filter");
//            SQLiteRTree rtree = new SQLiteRTree(this, "RTreeMain");
//            LSTFilter filter = new LSTFilter(rtree);
//            for(STPoint point : buffer){
//                filter.insert(point);
//            }
//            buffer.clear();
        }

    }
}
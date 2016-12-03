package nathanielwendt.mpc.ut.edu.paco;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.LatLng;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.ut.mpc.utils.LSTFilter;
import com.ut.mpc.utils.STPoint;
import com.ut.mpc.utils.STRegion;

import nathanielwendt.mpc.ut.edu.paco.utils.SQLiteRTree;

public class MainActivity extends AppCompatActivity implements PlacesFragment.OnFragmentInteractionListener,
        MapFragment.MapFragmentListener, CreatePlaceFragment.CreatePlaceFragmentDoneListener {

    private ViewGroup viewGroup;
    private static final int LOCATION_PERMISSION = 1;
    private LatLng lastLoc = Constants.DEFAULT_LAT_LNG;
    private Toolbar toolbar;
    private FragmentHelper fHelper;

    private String TOOLBAR_TRACKING = "PACO (Tracking)";
    private String TOOLBAR_PLAIN = "PACO";
    private boolean tracking = false;

    private LSTFilter filter;

    static {
        System.loadLibrary("sqliteX");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        SharedPreferences sharedpreferences = getSharedPreferences("Places", Context.MODE_PRIVATE);
//        sharedpreferences.edit().clear().commit();
        SQLiteRTree rtree = new SQLiteRTree(this, "RTreeMain");
        filter = new LSTFilter(rtree);
        filter.setRefPoint(new STPoint((float) lastLoc.longitude, (float) lastLoc.latitude, 0));

        Dexter.initialize(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(toggleTracking);

        fHelper = new FragmentHelper(R.id.container, getSupportFragmentManager());

        LastKnownLocationReceiver myReceiver = new LastKnownLocationReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationService.ACTION_LOC);
        registerReceiver(myReceiver, intentFilter);

        viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);

        fHelper.show("PlacesFragment", new PlacesFragment(), true);
    }

    private class LastKnownLocationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            double lastLoc[] = arg1.getDoubleArrayExtra(LocationService.LAT_LONG_DATA);
            MainActivity.this.lastLoc = new LatLng(lastLoc[0], lastLoc[1]);
        }
    }

    View.OnClickListener toggleTracking = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
            tracking = !tracking;
            if(tracking){
                Dexter.checkPermission(locationPermissionListener, Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                stopTracking();
            }
        }
    };

    private PermissionListener locationPermissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted(PermissionGrantedResponse response) {
            startTracking();
        }

        @Override
        public void onPermissionDenied(PermissionDeniedResponse response) {
            showSnackBar("Location permissions not granted, cannot track");
            tracking = false;
        }

        @Override
        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
            token.continuePermissionRequest();
        }
    };

    @Override
    public void showSnackBar(String text){
        Snackbar.make(viewGroup, text, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
    }

    public void startTracking(){
        startService(new Intent(MainActivity.this, LocationService.class));
        showSnackBar("started tracking");
        toolbar.setTitle(TOOLBAR_TRACKING);
    }

    public void stopTracking(){
        stopService(new Intent(MainActivity.this, LocationService.class));
        showSnackBar("stopped tracking");
        toolbar.setTitle(TOOLBAR_PLAIN);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        String tag;
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_show_mapview) {
            tag = "MapFragment";
            Fragment mapFragment = new MapFragment();
            fHelper.show(tag, mapFragment);
            return true;
        } else if(id == R.id.action_show_places) {
            tag = "PlacesFragment";
            fHelper.show(tag, new PlacesFragment());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(String id) {

    }

    @Override
    public double windowPoK(STRegion region) {
        return filter.windowPoK(region);
    }

    @Override
    public LatLng lastLoc(){
        return lastLoc;
    }

    @Override
    public void createPlace(STRegion region){
        fHelper.show("CreatePlaceFragment", CreatePlaceFragment.newInstance(region));
    }

    @Override
    public void onCreatePlaceDone() {
        fHelper.show("PlacesFragment", new PlacesFragment(), true);
    }
}

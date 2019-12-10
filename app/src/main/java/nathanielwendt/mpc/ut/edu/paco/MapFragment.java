package nathanielwendt.mpc.ut.edu.paco;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.ut.mpc.utils.GPSLib;
import com.ut.mpc.utils.STPoint;
import com.ut.mpc.utils.STRegion;

import org.florescu.android.rangeseekbar.RangeSeekBar;

/**
 * Created by nathanielwendt on 11/25/16.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private MapFragmentListener mapListener;

    private STRegion nextPlaceRegion;
    private GoogleMap map;
    private SupportMapFragment mapFragment;
    private RangeSeekBar rangeSeekBar;
    private TextView minTimeLabel, maxTimeLabel;
    private int minHourOffset = -72;
    private int maxHourOffset = 0;

    public MapFragment(){}

    @Override
    public void onPause() {
        super.onPause();
        clearMap();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_map, container, false);
        super.onActivityCreated(savedInstanceState);
        FragmentManager fm = getChildFragmentManager();
        mapFragment = (SupportMapFragment) fm.findFragmentById(R.id.mapview);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
        }
        mapFragment.getMapAsync(this);

        rangeSeekBar = (RangeSeekBar) view.findViewById(R.id.slider_bars);
        rangeSeekBar.setOnRangeSeekBarChangeListener(rangeSeekChangeListener);

        minTimeLabel = (TextView) view.findViewById(R.id.seek_lower_label);
        maxTimeLabel = (TextView) view.findViewById(R.id.seek_upper_label);

        view.findViewById(R.id.query_btn).setOnClickListener(queryBtnListener);
        view.findViewById(R.id.clear_btn).setOnClickListener(clearBtnListener);
        view.findViewById(R.id.pin_btn).setOnClickListener(pinBtnListener);

        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mapListener = (MapFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement mapFragListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mapListener = null;
    }

    @Override
    public void onMapReady(GoogleMap map){
        this.map = map;
        this.map.setOnMapLongClickListener(longClickListener);
        resetMapPos();
    }

    public void resetMapPos(){
        LatLng lastLoc = mapListener.lastLoc();
        CameraPosition defPos = new CameraPosition.Builder().target(lastLoc).zoom(Constants.DEFAULT_MAP_ZOOM).build();
        this.map.animateCamera(CameraUpdateFactory.newCameraPosition(defPos));
    }

    public void clearMap(){
        if(map != null){
            map.clear();
        }
        nextPlaceRegion = null;
    }

    // Note that this breaks if user is querying across the prime meridian because the sign of longitude changes
    // We ignore this problem at the North/South pole as well because this is very unlikely to occur in practice
    public void addPokGraphic(double pok, LatLng northeast, LatLng southwest){
        double latSpan = northeast.latitude - southwest.latitude;
        double longSpan = northeast.longitude - southwest.longitude;
        LatLng mapCenter = new LatLng(southwest.latitude + latSpan / 2.0, southwest.longitude + longSpan / 2.0);
        String displayPok = String.format("%.2f", pok * 100.0) + "%";
        map.addMarker(new MarkerOptions().position(mapCenter).icon(createPureTextIcon(displayPok, 150.0f)));
    }

    private RangeSeekBar.OnRangeSeekBarChangeListener rangeSeekChangeListener = new RangeSeekBar.OnRangeSeekBarChangeListener() {
        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
            minHourOffset = (int) minValue;
            maxHourOffset = (int) maxValue;
            minTimeLabel.setText(minHourOffset + "hr");
            if(maxHourOffset == 0){
                maxTimeLabel.setText("now");
            } else {
                maxTimeLabel.setText(maxHourOffset + "hr");
            }
        }
    };

    private GoogleMap.OnMapLongClickListener longClickListener = new GoogleMap.OnMapLongClickListener() {
        @Override
        public void onMapLongClick(LatLng latLng) {
            clearMap();

            STPoint loc = new STPoint((float) latLng.longitude, (float) latLng.latitude, (float) System.currentTimeMillis());

            double latOffset = GPSLib.latOffsetFromDistance(loc, Constants.LOCATION_RADIUS);
            double lonOffset = GPSLib.longOffsetFromDistance(loc, Constants.LOCATION_RADIUS);
            STPoint mins = new STPoint((float) (latLng.longitude - lonOffset), (float) (latLng.latitude - latOffset));
            STPoint maxs = new STPoint((float) (latLng.longitude + lonOffset), (float) (latLng.latitude + latOffset));

            nextPlaceRegion = new STRegion(mins, maxs);
            map.addCircle(new CircleOptions().center(latLng).radius(Constants.LOCATION_RADIUS * 1000).
                    fillColor(Color.BLUE).strokeColor(Color.TRANSPARENT));
            map.addMarker(new MarkerOptions().position(latLng));
        }
    };

    public BitmapDescriptor createPureTextIcon(String text, float size) {

        Paint textPaint = new Paint(); // Adapt to your needs

        textPaint.setTextSize(size);
        float textWidth = textPaint.measureText(text);
        float textHeight = textPaint.getTextSize();
        int width = (int) (textWidth);
        int height = (int) (textHeight);

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);

        canvas.translate(0, height);

        // For development only:
        // Set a background in order to see the
        // full size and positioning of the bitmap.
        // Remove that for a fully transparent icon.
        //canvas.drawColor(Color.LTGRAY);

        canvas.drawText(text, 0, 0, textPaint);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(image);
        return icon;
    }

    private View.OnClickListener queryBtnListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
            LatLng northeast = bounds.northeast;
            LatLng southwest = bounds.southwest;
            Log.d("LST", northeast.toString());
            Log.d("LST", southwest.toString());

            clearMap();

            long nowMS = System.currentTimeMillis();
            float minMS = nowMS + (60 * 60 * 1000 * minHourOffset); //range values should be negative, effectively subtracting
            float maxMS = nowMS + (60 * 60 * 1000 * maxHourOffset); //range values should be negative, effectively subtracting

//            int minTimeProgress = 1;//minTimeBar.getProgress() + 1; //0 is 1am
//            int maxTimeProgress = 1;//maxTimeBar.getProgress() + 1; //0 is 1am
//
//            Calendar now = Calendar.getInstance();
//            int hour = now.get(Calendar.HOUR_OF_DAY);
//            if(hour == 0){ hour = 24; } //wraparound to match sliders
//
//            int minute = now.get(Calendar.MINUTE);
//            int second = now.get(Calendar.SECOND);
//            int millis = now.get(Calendar.MILLISECOND);
//
//            double overshoot = (((hour * 60) + minute) * 60 + second) * 1000 + millis;
//
//            //double overshoot = (((hour * 60) + (minute * 60)) + second) * 1000 + millis;
//            //double currMS = System.currentTimeMillis() - overshoot - (3600 * 24 * 1000); //normalized to the hour ref period
//            double startOfDayMS = System.currentTimeMillis() - overshoot; //normalized to the hour ref period
//
//            System.out.println(overshoot);
//            System.out.println(startOfDayMS);
//
//            double minMS, maxMS;
//
//            minMS = startOfDayMS + (minTimeProgress * 60 * 60 * 1000);
//            maxMS = startOfDayMS + (maxTimeProgress * 60 * 60 * 1000);

            //minMS = currMS - ((hour - minTimeProgress) * 60 * 60 * 1000);
            //maxMS = currMS - ((hour - maxTimeProgress) * 60 * 60 * 1000);

            STPoint minPoint = new STPoint((float) southwest.longitude, (float) southwest.latitude, minMS);
            STPoint maxPoint = new STPoint((float) northeast.longitude, (float) northeast.latitude, maxMS);
            STRegion mapRegion = new STRegion(minPoint, maxPoint);

            Log.d("LST", mapRegion.toString());
            double pok = mapListener.windowPoK(mapRegion);
            Log.d("LST", String.valueOf(pok));

            addPokGraphic(pok, northeast, southwest);
        }
    };

    private View.OnClickListener clearBtnListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            clearMap();
        }
    };


    private View.OnClickListener pinBtnListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if(nextPlaceRegion != null){
                mapListener.createPlace(nextPlaceRegion);
            } else {
                Snackbar.make(v, "Must select region to create a new place", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        }
    };

    private String progressToLabel(int progress){
        String result;
        progress += 1;
        if(progress <= 12){
            if(progress == 12){
                result = "12:00 pm";
            } else {
                result = String.valueOf(progress) + ":00 am";
            }
        } else {
            if(progress == 24){
                result = "12:00 am";
            } else {
                result = String.valueOf(progress - 12) + ":00 pm";
            }
        }
        return result;
    }

    public interface MapFragmentListener {
        double windowPoK(STRegion region);
        LatLng lastLoc();
        void createPlace(STRegion region);
    }
}

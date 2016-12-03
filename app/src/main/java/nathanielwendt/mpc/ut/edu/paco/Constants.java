package nathanielwendt.mpc.ut.edu.paco;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by user on 1/20/15.
 */
public class Constants {
    public static int map_radius = 1000;

    //alpha, red, green, blue
    public static int[][] outlineC = {
            {255, 3, 74, 98}, //blue
            {255, 60, 104, 0}, //green
            {255, 207, 117, 7},//orange
            {255, 186, 14, 18} //red
    };
    public static int[][] fillC = {
            {127, 43, 129, 157}, //blue
            {127, 83, 144, 0}, //green
            {127, 255, 159, 42},//orange
            {127, 251, 79, 83} //red
    };

    public static final int LOC_POLLING_INTERVAL = 1000 * 10; // ms
    public static final int LOC_FASTEST_INTERVAL = 1000; // ms

    public static final int DEFAULT_MAP_ZOOM = 11;
    //public static final LatLng DEFAULT_LAT_LNG = new LatLng(30.3175497, -97.7175272); //austin
    public static final LatLng DEFAULT_LAT_LNG = new LatLng(46.0711, 11.1165); //trento


    public static final double LOCATION_RADIUS = 0.2; //km
    public static final double TIME_RADIUS = 60 * 60 * 24 * 1000; //1 day in ms

    public static final double highPoKThresh = .8;
    public static final double mediumPoKThresh = .6;
    public static final double stdPoKThresh = .4;
    public static final double lowPoKThresh = .1;

    public static final String highPoKLabel = "Expert Explorer";
    public static final String mediumPoKLabel = "Strong Tourist";
    public static final String stdPokLabel = "Standard Viewing";
    public static final String lowPoKLabel = "Quick Sighting";
    public static final String noPoKLabel = "Undiscovered";

    public static final String APP_TITLE = "MyTourist";
    public static final String APP_TITLE_TRACKING = APP_TITLE + " (tracking)";
}

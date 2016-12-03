package nathanielwendt.mpc.ut.edu.paco.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ut.mpc.utils.STRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nathanielwendt.mpc.ut.edu.paco.PlaceData;

/**
 * Created by nathanielwendt on 11/26/16.
 */
public class PlaceStore {
    private static final String PREF_TAG = "Places";
    Context activity;

    public PlaceStore(Activity activity){
        this.activity = activity;
    }

    public void put(String key, String posterPath, STRegion bounds){
        SharedPreferences sharedpreferences = activity.getSharedPreferences(PREF_TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        if("".equals(key)){
            key = "DEFAULT";
        }
        String data = posterPath + "**" + bounds.toString();
        Log.d("LST", "putting shared prefs name: " + key + " data: " + data);
        editor.putString(key, data);
        editor.commit();
    }

    public List<PlaceData> getPlaces(){
        SharedPreferences sharedpreferences = activity.getSharedPreferences(PREF_TAG, Context.MODE_PRIVATE);
        Map<String,?> keys = sharedpreferences.getAll();
        List<PlaceData> places = new ArrayList<PlaceData>();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            String placeName = entry.getKey();
            String[] data = entry.getValue().toString().split("\\*\\*");
            Log.d("LST", data[0]);
            Log.d("LST", data[1]);
            String uri = data[0];
            STRegion bounds = STRegion.fromString(data[1]);
            PlaceData nextPlace = new PlaceData(placeName, uri, bounds);
            places.add(nextPlace);
        }
        return places;
    }

    public void removePlace(int position){
        List<PlaceData> places = getPlaces();
        PlaceData placeToRemove = places.get(position);

        SharedPreferences sharedPreferences = activity.getSharedPreferences(PREF_TAG, Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(placeToRemove.getName()).apply();
    }


}

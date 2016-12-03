package nathanielwendt.mpc.ut.edu.paco;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.ut.mpc.utils.STRegion;

// The definition of our task class
public class PoKTask extends AsyncTask<Object, Integer, Double> {
    TextView textView;
    PlaceData place;
    PlacesFragment.OnFragmentInteractionListener mListener;

    @Override
    protected Double doInBackground(Object... params) {
        STRegion region = (STRegion) params[0];
        textView = (TextView) params[1];
        place = (PlaceData) params[2];
        mListener = (PlacesFragment.OnFragmentInteractionListener) params[3];
        double result = mListener.windowPoK(region);
        return result;
    }

    @Override
    protected void onPostExecute(Double result) {
        super.onPostExecute(result);
        Log.d("LST", "PoK value for region >> " + String.valueOf(result));
        String label;
        if(result >= Constants.highPoKThresh){
            label = Constants.highPoKLabel;
        } else if(result >= Constants.mediumPoKThresh){
            label = Constants.mediumPoKLabel;
        } else if(result >= Constants.stdPoKThresh){
            label = Constants.stdPokLabel;
        } else if(result >= Constants.lowPoKThresh){
            label = Constants.lowPoKLabel;
        } else {
            label = Constants.noPoKLabel;
        }
        textView.setText(label);
        place.setCoverage(label);
    }
}
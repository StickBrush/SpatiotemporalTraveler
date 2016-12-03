package nathanielwendt.mpc.ut.edu.paco;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.ut.mpc.utils.STRegion;

import java.util.List;

import nathanielwendt.mpc.ut.edu.paco.utils.PlaceStore;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class PlacesFragment extends Fragment {

    private List<PlaceData> places;
    private OnFragmentInteractionListener mListener;
    private boolean storagePermissions;

    /**
     * The fragment's ListView/GridView.
     */
    private ListView placesList;

    private PlaceStore placeStore;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ArrayAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PlacesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

//    public List<PlaceData> getPlaces(){
//        MainActivity activity = (MainActivity) getActivity();
//        SharedPreferences sharedpreferences = activity.getSharedPreferences("Places", Context.MODE_PRIVATE);
//        Map<String,?> keys = sharedpreferences.getAll();
//        List<PlaceData> places = new ArrayList<PlaceData>();
//        for(Map.Entry<String,?> entry : keys.entrySet()){
//            String placeName = entry.getKey();
//            String[] data = entry.getValue().toString().split("\\*\\*");
//            Log.d("LST", data[0]);
//            Log.d("LST", data[1]);
//            String uri = data[0];
//            STRegion bounds = STRegion.fromString(data[1]);
//            PlaceData nextPlace = new PlaceData(placeName, uri, bounds);
//            places.add(nextPlace);
//        }
//        return places;
//    }

    @Override
    public void onStart() {
        super.onStart();
        if(mAdapter != null){
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_place, container, false);

        Dexter.checkPermissions(storagePermissionsListener, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        placesList = (ListView) view.findViewById(R.id.place_list_view);

        MainActivity activity = (MainActivity) getActivity();
        placeStore = new PlaceStore(activity);

        Log.d("LST", "oncreate view for places");
        //populatePlaceCoverages();
        return view;
    }

    private MultiplePermissionsListener storagePermissionsListener = new MultiplePermissionsListener() {
        @Override
        public void onPermissionsChecked(MultiplePermissionsReport report) {
            places = placeStore.getPlaces();
            mAdapter = new PlaceDataAdapter(getActivity(), mListener, places);
            placesList.setAdapter(mAdapter);
        }

        @Override
        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
            token.continuePermissionRequest();
        }
    };

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden) {
            int prevSize = places.size();
            places = placeStore.getPlaces();
            if(places.size() > prevSize){
                //super hacky way to update list view...
                mAdapter = new PlaceDataAdapter(getActivity(), mListener, places);
                placesList.setAdapter(mAdapter);
            } else {
                populatePlaceCoverages();
            }
        }
    }

    private void populatePlaceCoverages(){
        for(PlaceData place : places){
            View v = placesList.getChildAt(places.indexOf(place));
            if(v == null){
                Log.d("LST","place is empty" + place.getName());
            } else {
                TextView tv = (TextView) v.findViewById(R.id.coverage);
                STRegion reg = place.getRegion();
                Object[] arr = new Object[]{reg, tv, place, mListener};
                new PoKTask().execute(arr);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("LST", "on pause");
//        placesList.setAdapter(null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        Log.d("LST", "on detach");
        //       placesList.setAdapter(null);
    }


//    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        if (null != mListener) {
//            // Notify the active callbacks interface (the activity, if the
//            // fragment is attached to one) that an item has been selected.
//            mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
//        }
//    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = placesList.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(String id);

        public double windowPoK(STRegion region);
    }

}
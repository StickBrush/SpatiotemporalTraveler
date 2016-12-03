package nathanielwendt.mpc.ut.edu.paco;

/*
 * Copyright (c) 2014 Rex St. John on behalf of AirPair.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.ut.mpc.utils.STRegion;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import nathanielwendt.mpc.ut.edu.paco.utils.PlaceStore;

/**
 * Take a picture directly from inside the app using this fragment.
 *
 * Reference: http://developer.android.com/training/camera/cameradirect.html
 * Reference: http://stackoverflow.com/questions/7942378/android-camera-will-not-work-startpreview-fails
 * Reference: http://stackoverflow.com/questions/10913181/camera-preview-is-not-restarting
 *
 * Created by Rex St. John (on behalf of AirPair.com) on 3/4/14.
 */
public class CreatePlaceFragment extends Fragment {
    private static final String ARG_REGION = "arg_region";
    private static String TAG = "LST";

    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private boolean previewOn;
    private final static int CAMERA_FPS = 5000;
    protected SharedPreferences sharedPrefs;
    protected String uploadUrl;

    private ImageView poster;
    private SurfaceView camPreview;
    protected Button createButton;
    protected Button cameraButton;
    protected Button storageButton;
    protected Boolean previewMode = false;
    protected EditText placeTitle;

    protected String posterPath;
    protected STRegion placeBounds;
    protected boolean storagePermissions = false;

    private static final int SELECT_PHOTO = 101;

    private CreatePlaceFragmentDoneListener mListener;

    public static CreatePlaceFragment newInstance(STRegion region) {
        CreatePlaceFragment fragment = new CreatePlaceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_REGION, region.toString());
        fragment.setArguments(args);
        return fragment;
    }

    public CreatePlaceFragment() {
        // Required empty public constructor
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
    public interface CreatePlaceFragmentDoneListener extends FragmentUtilities {
        public void onCreatePlaceDone();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String boundsObjAsString = getArguments().getString(ARG_REGION);
            placeBounds = STRegion.fromString(boundsObjAsString);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (CreatePlaceFragmentDoneListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement PlacesFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        cleanupCamera();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_native_camera, container, false);

        Dexter.checkPermissions(storagePermissionsListener, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        //capture ui elements
        this.poster = (ImageView) view.findViewById(R.id.place_poster);
        this.camPreview = (SurfaceView) view.findViewById(R.id.camPreview);
        this.cameraButton = (Button) view.findViewById(R.id.button_camera);
        this.storageButton = (Button) view.findViewById(R.id.button_storage);
        this.createButton = (Button) view.findViewById(R.id.button_create);
        this.placeTitle = (EditText) view.findViewById(R.id.place_title);

        //setup ui elements
        this.cameraButton.setOnClickListener(cameraListener);
        this.storageButton.setOnClickListener(storageListener);
        this.createButton.setOnClickListener(createListener);

        setupPreview();
        return view;
    }

    private MultiplePermissionsListener storagePermissionsListener = new MultiplePermissionsListener() {
        @Override
        public void onPermissionsChecked(MultiplePermissionsReport report) {
            storagePermissions = true;
        }

        @Override
        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
            token.continuePermissionRequest();
        }
    };

    Button.OnClickListener cameraListener = new Button.OnClickListener() {
        public void onClick(View v) {
            Dexter.checkPermission(cameraPermissionListener, Manifest.permission.CAMERA);
        }
    };


    private PermissionListener cameraPermissionListener = new PermissionListener() {
        @Override
        public void onPermissionGranted(PermissionGrantedResponse response) {
            if(CreatePlaceFragment.this.previewMode){
                Log.d(TAG, "Taking picture");
                camera.takePicture(null, null, myPictureCallback_JPG);
                CreatePlaceFragment.this.previewMode = false;
                cameraButton.setText(R.string.btn_camera_def);
            } else {
                Log.d(TAG, "Resetting to preview mode");
                poster.setVisibility(View.INVISIBLE);
                camPreview.setVisibility(View.VISIBLE);
                camera.startPreview();
                cameraButton.setText(R.string.btn_camera_preview);
                CreatePlaceFragment.this.previewMode = true;
            }
        }

        @Override
        public void onPermissionDenied(PermissionDeniedResponse response) {
            mListener.showSnackBar("Camera permissions denied, cannot start camera");
        }

        @Override
        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
            token.continuePermissionRequest();
        }
    };

    Button.OnClickListener storageListener = new Button.OnClickListener() {
        public void onClick(View v) {
            if(storagePermissions){
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            } else {
                Dexter.checkPermissions(storagePermissionsListener, Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

        }
    };

    Button.OnClickListener createListener = new Button.OnClickListener() {
        public void onClick(View v) {
            if(storagePermissions){
                PlaceStore placeStore = new PlaceStore(getActivity());
                placeStore.put(placeTitle.getText().toString(), posterPath, placeBounds);
                mListener.onCreatePlaceDone();
            } else {
                Dexter.checkPermissions(storagePermissionsListener, Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    };

    /**
     * Photo Selection result
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_PHOTO  && resultCode == Activity.RESULT_OK) {
            MainActivity activity = (MainActivity) getActivity();
            Bitmap bitmap = getBitmapFromCameraData(data, activity);
            poster.setImageBitmap(bitmap);

            Uri selectedImageUri = data.getData();

            String filemanagerstring = selectedImageUri.getPath();
            posterPath = getRealPathFromURI(activity, selectedImageUri);
            Log.d("LST", posterPath);
        }
    }

    private String getRealPathFromURI(Context mContext, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(mContext, contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }


    /**
     * Use for decoding camera response data.
     *
     * @param data
     * @param context
     * @return
     */
    public static Bitmap getBitmapFromCameraData(Intent data, Context context){
        Uri selectedImage = data.getData();
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver().query(selectedImage,filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        return BitmapFactory.decodeFile(picturePath);
    }

//    Button.OnClickListener takePicture = new Button.OnClickListener() {
//        public void onClick(View v) {
//            if(NativeCameraFragment.this.paused){
//                //reset camera
//                Log.d(TAG, "resetting camera");
//                camera.startPreview();
//                NativeCameraFragment.this.paused = false;
//                NativeCameraFragment.this.usePictureBtn.setEnabled(false);
//                NativeCameraFragment.this.takePictureBtn.setText(takePictureDefault);
//            } else {
//                Log.d(TAG, "snapping picture");
//                camera.takePicture(null, null, myPictureCallback_JPG);
//                NativeCameraFragment.this.paused = true;
//                NativeCameraFragment.this.usePictureBtn.setEnabled(true);
//                NativeCameraFragment.this.takePictureBtn.setText("New Pic");
//            }
//            //camera.stopPreview();
//            //camera.release();
//            //previewOn = false; // Don't release the camera in surfaceDestroyed()
//        }
//    };
//
//    Button.OnClickListener usePicture = new Button.OnClickListener() {
//        public void onClick(View v) {
//            Intent intent = new Intent(NativeCameraFragment.this, Upload.class);
//            Bundle bundle = new Bundle();
//            bundle.putByteArray("imageData", NativeCameraFragment.this.imageData);
//            intent.putExtras(bundle);
//            startActivity(intent);
//        }
//    };

    protected void setupPreview(){
        //getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceHolder = camPreview.getHolder();
        surfaceHolder.addCallback(new SurfaceHolderCallback());
    }

    private void cleanupCamera(){
        if (previewOn) {
            camera.setPreviewCallback(null);
            camera.stopPreview(); //stop the preview
            camera.release();  //release the camera for using it later (or if another app want to use)
            previewOn = false;
        }
    }

    public void onDestroy(){
        super.onDestroy();
        cleanupCamera();
    }

    // camera preview stuff
    class SurfaceHolderCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (null != camera) {
                try {
                    //Camera.Parameters params = camera.getParameters(); // must change the camera parameters to fix a bug in XE1
                    //params.setPreviewFpsRange(CAMERA_FPS, CAMERA_FPS);
                    //camera.setParameters(params);

                    camera.setPreviewDisplay(surfaceHolder);
                    //camera.startPreview();
                    previewOn = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            cleanupCamera();
        }
    }

    //Not used for now, but we may use in the future
    public static Camera getCameraInstance() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (RuntimeException re) {
            Log.e(TAG, "Camera is null.", re);
        }
        return camera;
    }


    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback(){

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile();
            if (pictureFile == null){
                Toast.makeText(getActivity(), "Image retrieval failed.", Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(processPhoto(data));
                fos.close();

                if(pictureFile.exists()){
                    posterPath = pictureFile.getAbsolutePath();
                    Bitmap myBitmap = BitmapFactory.decodeFile(posterPath);
                    poster.setImageBitmap(myBitmap);

                }
                poster.setVisibility(View.VISIBLE);
                camPreview.setVisibility(View.INVISIBLE);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private byte[] processPhoto(byte[] data){
        Log.d(TAG, "picture processing");
        Bitmap originalImg = BitmapFactory.decodeByteArray(data, 0, data.length);

        //resize image
        Bitmap resizedImg = Bitmap.createScaledBitmap(originalImg, (int) (originalImg.getWidth() * 0.2),
                (int) (originalImg.getHeight() * 0.2), true);
        //rotate image to match view for user
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotatedImg = Bitmap.createBitmap(resizedImg, 0, 0, resizedImg.getWidth(),
                resizedImg.getHeight(), matrix, true);
        //convert to byte format for sending to server
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedImg.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }


    /**
     * Used to return the camera File output.
     * @return
     */
    private File getOutputMediaFile(){

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "SpatioTemporalTraveler");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("Camera Guide", "Required media storage does not exist");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");

        //DialogHelper.showDialog( "Success!","Your picture has been saved!",getActivity());

        return mediaFile;
    }

//        @Override
//        public void onPictureTaken(byte[] arg0, Camera arg1) {
//            Log.d(TAG, "picture taken");
//            Bitmap originalImg = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);
//
//            //resize image
//            Bitmap resizedImg = Bitmap.createScaledBitmap(originalImg,(int)(originalImg.getWidth()*0.2),
//                    (int)(originalImg.getHeight()*0.2), true);
//            //rotate image to match view for user
//            Matrix matrix = new Matrix();
//            matrix.postRotate(90);
//            Bitmap rotatedImg = Bitmap.createBitmap(resizedImg, 0, 0, resizedImg.getWidth(),
//                    resizedImg.getHeight(), matrix, true);
//            //convert to byte format for sending to server
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            rotatedImg.compress(Bitmap.CompressFormat.PNG, 100, stream);
//            NativeCameraFragment.this.imageData = stream.toByteArray();
//        }
//    };



}
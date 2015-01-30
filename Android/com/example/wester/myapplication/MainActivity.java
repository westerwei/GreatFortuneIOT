package com.example.wester.myapplication;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.wester.myapplication.mqtt.CmdMqttCallback;
import com.example.wester.myapplication.restclient.Contact;
import com.example.wester.myapplication.restclient.Device;
import com.example.wester.myapplication.restclient.IotDemoInterface;
import com.example.wester.myapplication.restclient.RegisterResp;
import com.example.wester.myapplication.restclient.UnregisterResp;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.bytedeco.javacpp.opencv_contrib;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit.RestAdapter;
import org.bytedeco.javacv.*;
import org.bytedeco.javacpp.*;
import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.*;
import static org.bytedeco.javacpp.opencv_calib3d.*;
import static org.bytedeco.javacpp.opencv_objdetect.*;
import static org.bytedeco.javacpp.opencv_contrib.*;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, MqttCallback{

    private static final String TAG = "MyActivity";

    public static final String EXTRA_MESSAGE = "message";

    public static final String PROPERTY_REG_ID = "registration_id";

    private static final String PROPERTY_APP_VERSION = "appVersion";

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter btAdapter;

    private ArrayList<BluetoothDevice> btDeviceList = new ArrayList<BluetoothDevice>();

    private BroadcastReceiver ActionFoundReceiver;

// OpenCV Face Detection relevant variables
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private static final Scalar    FEMALE_FACE_RECT_COLOR     = new Scalar(255, 0, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
//    public static final int        NATIVE_DETECTOR     = 1;

    private MenuItem               mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;
    private MenuItem               mItemType;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;
//    private DetectionBasedTracker  mNativeDetector;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.05f;
    private int                    mAbsoluteFaceSize   = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;

    private opencv_contrib.FaceRecognizer genderModel;

    MqttClient mqtt;

    MqttClient remoteCmd;

    private long previousVideoTime;

    public MainActivity(){
        super();
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
//        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

//        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    //System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources

                        InputStream is = getResources().openRawResource(R.raw.haarcascade);
                        File cascadeDir = getDir("opencv_cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "haarcascade.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        os.flush();
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                        }
//                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();


                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

//                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

//    MediaRecorder recorder;

//    Camera camera = null;

//    int camId;

    public static SurfaceView mSurfaceView;

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "327188506944";

    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context appContext;

    String regid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        Log.d(TAG, "Initialize My first Android App~~");
        System.out.println("Initialize My first Android App~~");

        appContext = getApplicationContext();


        // Check device for Play Services APK.
        if (checkPlayServices()) {
            // If this check succeeds, proceed with normal processing.
            // Otherwise, prompt user to get valid Play Services APK.
            this.gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(appContext);

            if (regid.isEmpty()) {
                registerGCM();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

//        showCamera();

        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        try {
            Log.d(TAG, sdDir.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }


        try{
            mqtt = new MqttClient("tcp://" + AppConstants.remoteHostIP + ":1883","AndroidDevice", new MemoryPersistence());
            remoteCmd = new MqttClient("tcp://" + AppConstants.remoteHostIP + ":1883","AndroidDeviceCmd", new MemoryPersistence());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            Log.d(TAG, "Connecting to broker");
            mqtt.connect(connOpts);
            remoteCmd.connect(connOpts);
            Log.d(TAG, "Connected");

            mqtt.setCallback(this);
            remoteCmd.setCallback(new CmdMqttCallback(this));
            mqtt.subscribe("mymqtt");
            remoteCmd.subscribe("cmdJSON");

        }
        catch (MqttException e){
            e.printStackTrace();
        }


//        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fdView);
//        mOpenCvCameraView.setCvCameraViewListener(this);
//        mOpenCvCameraView.enableView();


//        Context mAppContext = this.getApplicationContext();
//        TelephonyManager tm = (TelephonyManager)mAppContext.getSystemService(Context.TELEPHONY_SERVICE);
//        String mPhoneNumber = tm.getLine1Number();
//        String did = tm.getDeviceId();
//        String subId = tm.getSubscriberId();
//        Log.d(TAG, subId + " : " + did + " : " + mPhoneNumber);


    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = appContext.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerGCM() {

        new AsyncTask(){

            //@Override
            protected Object doInBackground(Object[] objects) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(appContext);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.
                    // The request to your server should be authenticated if your app
                    // is using accounts.
                    //sendRegistrationIdToBackend();

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the
                    // message using the 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(appContext, regid);

                    Log.d(TAG, msg);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    msg = "Error :" + ex.getMessage();

                    Log.e(TAG, msg, ex);
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }

                return msg;
            }

            //@Override
            protected void onPostExecute(String msg) {
                Log.d(TAG, msg + "\n");
            }


        }.execute(null, null, null);



    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // You need to do the Play Services APK check here too.
    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
//            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
//                        new Size(30, 30), new Size());
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
//        else if (mDetectorType == NATIVE_DETECTOR) {
//            if (mNativeDetector != null)
//                mNativeDetector.detect(mGray, faces);
//        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        int maleCnt = 0;
        int femaleCnt = 0;
        for (int i = 0; i < facesArray.length; i++){
            //Mat faceMatOri = mRgba.submat(facesArray[i].y, facesArray[i].y + facesArray[i].height, facesArray[i].x, facesArray[i].x + facesArray[i].width);
            Mat faceMatOri = mRgba.submat(facesArray[i]);
            Size sz = new Size(120, 120);
            Mat faceMatAdjtmp = new Mat();
            Imgproc.resize(faceMatOri, faceMatAdjtmp, sz);

            Mat grayMatAdj = new Mat();
            Log.i(TAG, "faceMatAdjtmp.channels(): " + faceMatAdjtmp.channels());
            Log.i(TAG, "faceMatAdjtmp.empty(): " + faceMatAdjtmp.empty());
            Log.i(TAG, "faceMatAdjtmp.isContinuous(): " + faceMatAdjtmp.isContinuous());
            if(faceMatAdjtmp.channels() >= 3){
                Imgproc.cvtColor(faceMatAdjtmp, grayMatAdj, Imgproc.COLOR_BGR2GRAY);
            }
            else{
//                grayMatAdj = faceMatAdjtmp;
                continue;
                //return mRgba;
            }


            Log.i(TAG, "grayMatAdj.isContinuous() : " + grayMatAdj.isContinuous());
            byte faceData[] = new byte[(int)grayMatAdj.total() * grayMatAdj.channels()];
            grayMatAdj.get(0,0,faceData);
//            CvSize resize = new CvSize();
//            resize.height(115);
//            resize.width(115);
            CvSize cvSize = new CvSize();
            cvSize.height(120);
            cvSize.width(120);
            IplImage oriFace = IplImage.createHeader(120, 120, IPL_DEPTH_8U, 1);
//            IplImage resizeImage = IplImage.create(resize, oriFace.depth(), oriFace.nChannels());
            BytePointer rawImageData = new BytePointer(faceData);
            cvSetData(oriFace, rawImageData, oriFace.widthStep());
//            cvResize(oriFace, resizeImage);


            opencv_core.Mat faceMatDelegate = new opencv_core.Mat(oriFace, false);

            //opencv_core.Mat grayFaceMat = new opencv_core.Mat();
            //opencv_core.Mat grayFacePredict = new opencv_core.Mat();
            //cvtColor(faceMatDelegate, grayFacePredict, CV_BGR2GRAY);

//            Log.i(TAG, "faceMatDelegate.data() : " + faceMatDelegate.data() + " : " + faceMatDelegate.isContinuous());
//            Log.i(TAG, "faceMatDelegate.data() : " + faceMatDelegate.empty());
//            Log.i(TAG, "faceMatDelegate.data() : " + faceMatDelegate.channels());

            //BytePointer bp = new BytePointer();
            //bp.put(faceData);
            //faceMatDelegate.data(bp);
            //faceMatDelegate.data(bp);
            //faceMatDelegate.data().put(faceData);
            int fd = genderModel.predict(faceMatDelegate);
            if(fd == 1){
                Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
                maleCnt++;
            }
            else{
                Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FEMALE_FACE_RECT_COLOR, 3);
                femaleCnt++;
            }


        }
//        try {
//            Thread.sleep(50);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        final String displayText = "Male : " + maleCnt + "  |  Female : " + femaleCnt;
        final int pMale = maleCnt;
        final int pFemale = femaleCnt;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tt = (TextView)findViewById(R.id.genderDetectTxtView);
                VideoView vv = (VideoView)findViewById(R.id.videoView);
                tt.setText(displayText + " --- " + vv.getCurrentPosition());

                if(!vv.isPlaying()){
                    String video = Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/M&M.mp4";
                    if(pMale > pFemale){
                        video = Environment
                                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Chrysler.mp4";
                        vv.setVideoPath(video);
//                        vv.setVideoURI(Uri.parse("http://192.168.1.27:8066/avengers2.mp4"));
                        //
                    }
                    else if(pMale < pFemale){
                        video = Environment
                                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/Burberry.mp4";
                        vv.setVideoPath(video);
                    }
                    else{
                        Log.i(TAG,"Use Default video");
                        vv.setVideoPath(video);
                    }

                    vv.start();
                }

            }
        });


        return mRgba;
    }

    @Override
    public void connectionLost(Throwable throwable) {
        Log.d(TAG, "Connection to MQTT has been lost; Re-connect again");
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        try {
            mqtt.connect(connOpts);
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        Log.d(TAG, "Got message in : " + new String(mqttMessage.getPayload()));
        final String msg = new String(mqttMessage.getPayload());
        final Activity act = this;
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(MainActivity.this, "Got message from MQTT : " + msg, Toast.LENGTH_LONG);
                toast.show();
                TextView tt = (TextView)findViewById(R.id.genderDetectTxtView);
                VideoView vv = (VideoView)findViewById(R.id.videoView);
                tt.setText("Got message from MQTT : " + msg);
            }
        });


    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.d(TAG, "Delivery to MQTT has been finished");
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            ListView lv = (ListView) getActivity().findViewById(R.id.cntListView);
            Cursor cur = getContacts();
            String[] fields = new String[] {ContactsContract.Data.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};

            SimpleCursorAdapter adapter =
                    new SimpleCursorAdapter(getActivity(),
                            R.layout.contacts_list_item,
                            cur,
                            fields,
                            new int[] {R.id.textView3,R.id.textView4});
            Log.d(TAG, "LV: " + lv);
            lv.setAdapter(adapter);

            MainActivity.mSurfaceView = (SurfaceView)getActivity().findViewById(R.id.videoView);

            MediaController mc = new MediaController(getActivity());
            VideoView tmpVV = (VideoView)getActivity().findViewById(R.id.videoView);
            mc.setAnchorView(tmpVV);
            tmpVV.setMediaController(mc);

        }

        private Cursor getContacts() {
            Context mAppContext = getActivity().getApplicationContext();
            return mAppContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        }
    }

    public void testRestClient(View view) {
        Log.d(TAG, "Click button to test");
        //RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://192.168.1.11:1150").build();
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://" + AppConstants.remoteHostIP + ":1150").build();
        IotDemoInterface iotDemoService = restAdapter.create(IotDemoInterface.class);
        List<Contact> cntList = iotDemoService.getContactList("0999999999");
        Log.d(TAG, "Size: " + cntList.size());
        for (Contact c : cntList){
            Log.d(TAG, "contactName: " + c.getContactName());
            Log.d(TAG, "contact: " + c.getContact());
            Log.d(TAG, "type: " + c.getType());
        }
    }

    public void testMQTT(View view){
        Log.d(TAG, "Test MQTT!!");
        try{
//            MqttClient mqtt = new MqttClient("tcp://192.168.1.101:1883","AndroidDevice", new MemoryPersistence());
//            MqttConnectOptions connOpts = new MqttConnectOptions();
//            connOpts.setCleanSession(true);
//            Log.d(TAG, "Connecting to broker");
//            mqtt.connect(connOpts);
//            Log.d(TAG, "Connected");
            Log.d(TAG, "Test publishing message");
            MqttMessage msg = new MqttMessage("Message From Android Devices".getBytes());
            msg.setQos(2);
            mqtt.publish("mymqtt",msg);
            Log.d(TAG, "Message is published");
//            mqtt.disconnect();
        }
        catch (MqttException e){
            e.printStackTrace();
        }

    }

    public void unregisterDevice(View view){

        Log.d(TAG, "Click button to un-register all contact list from server!!");
//        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://192.168.1.11:1150").build();
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://" + AppConstants.remoteHostIP + ":1150").build();
        IotDemoInterface iotDemoService = restAdapter.create(IotDemoInterface.class);
        Context mAppContext = this.getApplicationContext();
        TelephonyManager tm = (TelephonyManager)mAppContext.getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tm.getLine1Number();
        if(mPhoneNumber == null || mPhoneNumber.equals("")){
            mPhoneNumber = "18721687597";
        }
        UnregisterResp resp = iotDemoService.unregisterDevice(mPhoneNumber);

        Log.d(TAG, resp.getResult());

        //--------------------------
//        SurfaceView sv = new SurfaceView(getApplicationContext());
//        try {
//            camera.setPreviewDisplay(sv.getHolder());
//            Camera.Parameters parameters = camera.getParameters();
//
//            //set camera parameters
//            camera.setParameters(parameters);
//            //camera.startPreview();
//            camera.takePicture(null, null, new PhotoHandler(getApplicationContext()));
//
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }


        //Get a surface
        //SurfaceHolder sHolder = sv.getHolder();
        //tells Android that this surface will have its data constantly replaced
        //sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


//        camera.takePicture(null, null,
//                new PhotoHandler(getApplicationContext()));
    }

    public void testBlueTooth(View view){

        //Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        ActionFoundReceiver = new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(TAG, "\n  Device: " + device.getName() + ", " + device + ", " + device.getType() +
                            ", " + device.getBluetoothClass()

                            );

                    btDeviceList.add(device);
                } else {
                    if(BluetoothDevice.ACTION_UUID.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                        for (int i=0; i<uuidExtra.length; i++) {
                            Log.d(TAG, "\n  Device: " + device.getName() + ", " + device + ", Service: " + uuidExtra[i].toString());
                        }
                    } else {
                        if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                            Log.d(TAG, "\nDiscovery Started...");
                        } else {
                            if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                                Log.d(TAG, "\nDiscovery Finished");
                                Iterator<BluetoothDevice> itr = btDeviceList.iterator();
                                while (itr.hasNext()) {
                                // Get Services for paired devices
                                    BluetoothDevice device = itr.next();
                                    Log.d(TAG, "\nGetting Services for " + device.getName() + ", " + device);
                                    if(!device.fetchUuidsWithSdp()) {
                                        Log.d(TAG, "\nSDP Failed for " + device.getName());
                                    }

                                }
                            }
                        }
                    }
                }
            }
         };

        registerReceiver(ActionFoundReceiver, filter); // Don't forget to unregister during onDestroy

        // Getting the Bluetooth adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "\nAdapter: " + btAdapter);

        CheckBTState();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (btAdapter != null) {
            btAdapter.cancelDiscovery();
        }
        if(ActionFoundReceiver != null){
            unregisterReceiver(ActionFoundReceiver);
        }
        if(mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }

        try {
            mqtt.disconnect();
            remoteCmd.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }


    }

    private void CheckBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // If it isn't request to turn it on
        // List paired devices
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            Log.d(TAG,"\nBluetooth NOT supported. Aborting.");
            return;
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG,"\nBluetooth is enabled...");

                // Starting the device discovery
                btAdapter.startDiscovery();
            } else {
                Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public void registerDevice(View view){
        Log.d(TAG, "Click button to register all contact list to server!!");
        //RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://192.168.1.11:1150").build();
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://" + AppConstants.remoteHostIP + ":1150").build();
        IotDemoInterface iotDemoService = restAdapter.create(IotDemoInterface.class);

        Context mAppContext = this.getApplicationContext();
        TelephonyManager tm = (TelephonyManager)mAppContext.getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tm.getLine1Number();
        if(mPhoneNumber == null || mPhoneNumber.equals("")){
            mPhoneNumber = "18721687597";
        }
        String did = Settings.Secure.getString(appContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);;

        String subId = tm.getSubscriberId();

        Device d = new Device();
        d.setPhone(mPhoneNumber);
        d.setDeviceId(did);
        d.setUserId(subId);
        d.setRegId(regid);

        Cursor cursor = null;
        try {
            cursor = mAppContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            int contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);
            int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int phoneNumberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int photoIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_ID);
            cursor.moveToFirst();
            List<Contact> contactList = new ArrayList<Contact>();
            do {
                String idContact = cursor.getString(contactIdIdx);
                String name = cursor.getString(nameIdx);
                String phoneNumber = cursor.getString(phoneNumberIdx);
                Contact c = new Contact();
                c.setContact(phoneNumber);
                c.setContactName(name);
                c.setType("PHONE");

                contactList.add(c);

                Log.d(TAG, name + " : " + phoneNumber);

            } while (cursor.moveToNext());

            d.setContacts(contactList);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        RegisterResp resp = iotDemoService.registerDevice(d);

        Log.d(TAG, "Register Result: " + resp.getPostId() + " | " + resp.getResult());

//        Log.d(TAG, "Size: " + cntList.size());
//        for (Contact c : cntList){
//            Log.d(TAG, "contactName: " + c.getContactName());
//            Log.d(TAG, "contact: " + c.getContact());
//            Log.d(TAG, "type: " + c.getType());
//        }
//
//        List<Contact> cntList = iotDemoService.registerDevice();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        Log.d(TAG, "GPSU result code:" + resultCode);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "GPS connec fail~");
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.e(TAG, "1105 This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    public void executeAdPush(View view){
        initialFisherFacesRecognizer();
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fdView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableView();

//        VideoView vv = (VideoView)findViewById(R.id.videoView);
//        vv.setVideoPath(Environment
//                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/M&M.mp4");
//        vv.start();

    }

    private void initialFisherFacesRecognizer(){
        try{
            genderModel = createFisherFaceRecognizer();

            InputStream is = getResources().openRawResource(R.raw.gendermodel);
            File modelDir = getDir("opencv_gendermodel", Context.MODE_PRIVATE);
            File modelFile = new File(modelDir, "gendermodel.xml");
            FileOutputStream os = new FileOutputStream(modelFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
            is.close();
            os.close();

            genderModel.load(modelFile.getAbsolutePath());

        }
        catch(Exception e){
            throw new RuntimeException(e);
        }


    }

//------------------------------------------

    public void cmdConnectionLost(Throwable throwable) {
        Log.d(TAG, "Cmd connection to MQTT has been lost; Re-connect again");
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        try {
            remoteCmd.connect(connOpts);
            remoteCmd.setCallback(new CmdMqttCallback(this));
            remoteCmd.subscribe("cmdJSON");
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }


    public void cmdMessageArrived(String s, MqttMessage mqttMessage) throws Exception {
        Log.d(TAG, "Got CMD message in : " + new String(mqttMessage.getPayload()));
        final String msg = new String(mqttMessage.getPayload());
//        final Activity act = this;
//        MainActivity.this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast toast = Toast.makeText(MainActivity.this, "Got message from MQTT : " + msg, Toast.LENGTH_LONG);
//                toast.show();
//                TextView tt = (TextView)findViewById(R.id.genderDetectTxtView);
//                VideoView vv = (VideoView)findViewById(R.id.videoView);
//                tt.setText("Got message from MQTT : " + msg);
//            }
//        });


    }


    public void cmdDeliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.d(TAG, "Delivery to CMD MQTT has been finished");
    }

    public long getPreviousVideoTime() {
        return previousVideoTime;
    }

    public void setPreviousVideoTime(long previousVideoTime) {
        this.previousVideoTime = previousVideoTime;
    }

}

package com.example.wester.myapplication;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import com.example.wester.myapplication.restclient.DeviceMediaData;
import com.example.wester.myapplication.restclient.MediaDataRESTClient;
import com.example.wester.myapplication.restclient.RegisterResp;
import com.example.wester.myapplication.websocket.WebSocketClientHelper;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Wester on 2014/11/5.
 */
public class GcmIntentService extends IntentService {

    public static final int NOTIFICATION_ID = 1;

    public static final String TAG = "GcmIntentService";

    private NotificationManager mNotificationManager;

    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    private String phoneNum;

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        TelephonyManager tm = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tm.getLine1Number();
        if(mPhoneNumber == null || mPhoneNumber.equals("")){
            mPhoneNumber = "18721687597";
        }
        this.phoneNum = mPhoneNumber;
        Cursor cursor = getApplicationContext().getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        int cntLen = cursor.getCount();
        Log.d(TAG, "CNT Len:" + cntLen);
        String name = null;
        String phone = null;
        String cmdType = null;
        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " +
                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                cmdType = extras.getString("cmdType");

                // create a handler to post messages to the main thread
                Handler mHandler = new Handler(getMainLooper());

                if(cmdType.equalsIgnoreCase("add_contact")){
                    name = extras.getString("contactName");
                    phone = extras.getString("contact");

                    Log.d(TAG, "Get message command:" + name);

                    Log.d(TAG, "Get message command:" + phone);

                    this.addContact(name, phone);

                    // create a handler to post messages to the main thread
//                    Handler mHandler = new Handler(getMainLooper());
                    final String fName = name;
                    final String fPhone = phone;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(getApplicationContext(), fName + "'s phone number[" + fPhone + "] has been added to contact list!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                }
                else if(cmdType.equalsIgnoreCase("take_pic")){
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(getApplicationContext(),"Start to take picture!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                    showCamera();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(getApplicationContext(),"Take picture automatically!!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
//                    for(int i=0;i<10;i+{
//
//                        final int fi = i;
//
//                        try {
//                            Thread.sleep(1000);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }


                }
                else if(cmdType.equalsIgnoreCase("recording")){
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(getApplicationContext(),"Start recording procedure for 20 sec!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                    recording();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(getApplicationContext(),"Recording procedure has been completed!", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    });
                }

                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                sendNotification("Received: " + extras.toString());
                Log.i(TAG, "Received: " + extras.toString());



            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);

//        showAlertDiaolg(name, phone);
    }

    private File getVideoFile() {
        File sdDir = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "CameraAPIDemo");
//        return new File(sdDir, "CameraAPIDemo");
        if (!sdDir.exists() && !sdDir.mkdirs()) {

            Log.e(TAG, "Can't create directory to save video.");
//            Toast.makeText(context, "Can't create directory to save image.",
//                    Toast.LENGTH_LONG).show();
            return null;

        }
        else{
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
            String date = dateFormat.format(new Date());
            String videoFileName = "Video_" + date + ".mp4";

            String filename = sdDir.getPath() + File.separator + videoFileName;

            File videoFile = new File(filename);


            return videoFile;
        }


    }

    private void recording(){
        int cameraId = findFrontFacingCamera();
        Log.d(TAG, "Cam id:" + cameraId);
        final Camera camera = Camera.open(cameraId);
        SurfaceView sv = new SurfaceView(getApplicationContext());
        try {
            camera.setPreviewDisplay(sv.getHolder());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Camera.Parameters parameters = camera.getParameters();
        camera.setParameters(parameters);


        MediaRecorder recorder = new MediaRecorder();
        camera.unlock();
        recorder.setCamera(camera);
        recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        CamcorderProfile cpHigh = CamcorderProfile
                .get(cameraId, CamcorderProfile.QUALITY_CIF);
        recorder.setProfile(cpHigh);
        File videoFile = getVideoFile();
        if(videoFile == null){
            return;
        }else{
            Log.d(TAG, "Video File: " + videoFile.getPath());
        }
//        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(videoFile.getPath());

        recorder.setMaxDuration(7000); // 20 seconds
//        recorder.setMaxFileSize(5000000); // Approximately 5 megabytes

        recorder.setPreviewDisplay(MainActivity.mSurfaceView.getHolder().getSurface());

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Prepare video fail~", e);
        }
        final String mFilePath = videoFile.getPath();
        final String mPhoneNum = this.phoneNum;
        final String mFileName = videoFile.getName();
        recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int i, int i2) {
                Log.d(TAG, "In video listener.......");
                if (i == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    Log.d(TAG,"Maximum Duration Reached");
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    camera.release();
                    Log.d(TAG, "Video File:" + mFilePath);

                    Log.d(TAG, "Start to video back to server");
                    try{
                        FileInputStream fis = new FileInputStream(mFilePath);
                        byte[] videoData = new byte[(int)fis.available()];
                        fis.read(videoData);
                        String dataBase64 = Base64.encodeToString(videoData, Base64.URL_SAFE);
                        DeviceMediaData media = new DeviceMediaData();
                        media.setPhone(mPhoneNum);
                        media.setMediaFile(mFileName);
                        media.setMediaType("video");
                        media.setMediaData(dataBase64);
                        RegisterResp resp = MediaDataRESTClient.sendMediaData(media);
                        Log.d(TAG, "Sending video back to server is done: " + resp.getResult() + " | " + resp.getPostId());

                        WebSocketClientHelper.sendMediaNotification(media.getPhone(), media.getMediaType(), media.getMediaFile());
                    }
                    catch(Exception e){
                        Log.e(TAG, "POST video file fails!", e);
                        throw new RuntimeException(e);

                    }



//                    Handler mHandler = new Handler(getMainLooper());
//                    mHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast toast = Toast.makeText(getApplicationContext(),"Recording procedure has been stopped!", Toast.LENGTH_LONG);
//                            toast.show();
//                        }
//                    });
                }
            }
        });

        recorder.start();


        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Fail to wait for recording!!!", e);
        }

    }

    private void showCamera(){
        // do we have a camera?
        PhotoHandler photoHandler = new PhotoHandler(getApplicationContext());
        photoHandler.setPhone(this.phoneNum);
        if (!getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "No camera on this device", Toast.LENGTH_LONG)
                    .show();
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId < 0) {
                Toast.makeText(this, "No front facing camera found.",
                        Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Camera ID:" + cameraId);
                Camera camera = Camera.open(cameraId);
                //camera.takePicture(null, null, new PhotoHandler(getApplicationContext()));
                SurfaceView sv = new SurfaceView(getApplicationContext());
                try {
                    camera.setPreviewDisplay(sv.getHolder());
                    Thread.sleep(1000);
                    Camera.Parameters parameters = camera.getParameters();

                    //set camera parameters
                    camera.setParameters(parameters);
                    //camera.startPreview();
                    camera.takePicture(null, null, photoHandler);
                    Thread.sleep(2000);
                    camera.release();

                } catch (Throwable e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "Take a pic.....");


            }
        }
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        if(numberOfCameras > 0){
            cameraId = 0;
        }
//        for (int i = 0; i < numberOfCameras; i++) {
//            Camera.CameraInfo info = new Camera.CameraInfo();
//            Camera.getCameraInfo(i, info);
//            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                Log.d(TAG, "Camera found");
//                cameraId = i;
//                break;
//            }
//        }
        return cameraId;
    }


    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        //.setSmallIcon(R.drawable.ic_stat_gcm)
                        .setContentTitle("GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void addContact(String contactName, String contact) {
        ArrayList<ContentProviderOperation> op_list = new ArrayList<ContentProviderOperation>();
        op_list.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        //.withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DEFAULT)
                .build());

        // first and last names
        op_list.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contactName)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, "")
                .build());

        op_list.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                .build());
        op_list.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)

                .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Email.DATA, contactName + "@xyz.com")
                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                .build());

        try{
            ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, op_list);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void showAlertDiaolg(String name, String phone){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                getApplicationContext());

        // set title
        alertDialogBuilder.setTitle("Your Title");

        // set dialog message
        alertDialogBuilder
                .setMessage(name + "'s phone number[" + phone + "] has been added to contact list!")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        dialog.cancel();
                    }
                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    private WebSocketClient getMediaWSConnect(){
        URI uri;
        try {
//            uri = new URI("ws://192.168.1.11:1150/websocket/addmedia");
            uri = new URI("ws://" + AppConstants.remoteHostIP + ":1150/websocket/addmedia");
        } catch (URISyntaxException e) {
            Log.e(TAG,"Create WebSocket connection fail....", e);
            return null;
        }

        final WebSocketClient wsConnect = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i(TAG, "Connect to media notification WebSocket");
                this.send("ping");
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "Get message from media notification WS: " + message);
//                final String message = s;
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        TextView textView = (TextView)findViewById(R.id.messages);
//                        textView.setText(textView.getText() + "\n" + message);
//                    }
//                });
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "Close media notification WebSocket");
            }

            @Override
            public void onError(Exception ex) {
                Log.e(TAG, "Fail to run the WS client for media notification", ex);
            }
        };

        return wsConnect;
    }

//    public boolean starMediaRecording(){
//        SurfaceView mSurfaceView = MainActivity.m
//        Camera mServiceCamera = Camera.open();
//        Camera.Parameters params = mServiceCamera.getParameters();
//        mServiceCamera.setParameters(params);
//        Camera.Parameters p = mServiceCamera.getParameters();
//
//        final List<Camera.Size> listSize = p.getSupportedPreviewSizes();
//        Camera.Size mPreviewSize = listSize.get(2);
//        p.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
//        p.setPreviewFormat(PixelFormat.YCbCr_420_SP);
//        mServiceCamera.setParameters(p);
//
//        try {
//            mServiceCamera.setPreviewDisplay(mSurfaceHolder);
//            mServiceCamera.startPreview();
//        }
//        catch (IOException e) {
//            Log.e(TAG, e.getMessage());
//            e.printStackTrace();
//        }
//
//        mServiceCamera.unlock();
//
//        mMediaRecorder = new MediaRecorder();
//        mMediaRecorder.setCamera(mServiceCamera);
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
//        mMediaRecorder.setOutputFile("/sdcard/filenamevideo.mp4");
//        mMediaRecorder.setVideoFrameRate(30);
//        mMediaRecorder.setVideoSize(mPreviewSize.width, mPreviewSize.height);
//        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
//
//        mMediaRecorder.prepare();
//        mMediaRecorder.start();
//
//        mRecordingStatus = true;
//
//        return true;
//
//    }
}

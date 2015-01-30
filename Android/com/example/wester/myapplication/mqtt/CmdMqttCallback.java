package com.example.wester.myapplication.mqtt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import com.example.wester.myapplication.AppConstants;
import com.example.wester.myapplication.MainActivity;
import com.example.wester.myapplication.PhotoHandler;
import com.example.wester.myapplication.restclient.DeviceMediaData;
import com.example.wester.myapplication.restclient.IOTDeviceMediaData;
import com.example.wester.myapplication.restclient.MediaDataRESTClient;
import com.example.wester.myapplication.restclient.RegisterResp;
import com.example.wester.myapplication.websocket.WebSocketClientHelper;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Wester on 2014/12/11.
 */
public class CmdMqttCallback implements MqttCallback {

    private Activity mainAct;

    private String TAG = "CmdMqttCallback";

    private String phoneNum;

    private NotificationManager mNotificationManager;

    public CmdMqttCallback(Activity mainAct){
        this.mainAct = mainAct;
    }

    @Override
    public void connectionLost(Throwable throwable) {

        ((MainActivity)mainAct).cmdConnectionLost(throwable);
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        Log.d(TAG, "Got CMD message in : " + new String(mqttMessage.getPayload()));
        final String msg = new String(mqttMessage.getPayload());
        JSONObject jsonCmd = new JSONObject(msg);
        String cmdType = jsonCmd.getString("cmdType");

        TelephonyManager tm = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tm.getLine1Number();
        if(mPhoneNumber == null || mPhoneNumber.equals("")){
            mPhoneNumber = "18721687597";
        }
        this.phoneNum = mPhoneNumber;
        //((MainActivity)mainAct).cmdMessageArrived(s, mqttMessage);
        String name = null;
        String phone = null;
        if(cmdType.equalsIgnoreCase("add_contact")){
            name = jsonCmd.getString("contactName");
            phone = jsonCmd.getString("contact");

            Log.d(TAG, "Get message command:" + name);

            Log.d(TAG, "Get message command:" + phone);

            this.addContact(name, phone);

            // create a handler to post messages to the main thread
//                    Handler mHandler = new Handler(getMainLooper());
            final String fName = name;
            final String fPhone = phone;
            mainAct.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(getApplicationContext(), fName + "'s phone number[" + fPhone + "] has been added to contact list!", Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
        else if(cmdType.equalsIgnoreCase("take_pic")){
            mainAct.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(getApplicationContext(),"Start to take picture!", Toast.LENGTH_LONG);
                    toast.show();
                }
            });
            showCamera();
            mainAct.runOnUiThread(new Runnable() {
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
//            mainAct.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast toast = Toast.makeText(getApplicationContext(),"Start recording procedure for 20 sec!", Toast.LENGTH_LONG);
//                    toast.show();
//                }
//            });
            recording();
//            mainAct.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast toast = Toast.makeText(getApplicationContext(),"Recording procedure has been completed!", Toast.LENGTH_LONG);
//                    toast.show();
//                }
//            });
        }

        Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
        // Post notification of received message.
        //sendNotification("Received: " + extras.toString());
//        Log.i(TAG, "Received: " + extras.toString());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.d(TAG, "Delivery to CMD MQTT has been finished");
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
        long currentTime = (new Date()).getTime();
        long preVideoTime = ((MainActivity)mainAct).getPreviousVideoTime();
        if(currentTime - preVideoTime < (1000l * 30 * 1)){
            Log.i(TAG, "Not necessary to run recording!");
            return;
        }
        int cameraId = findFrontFacingCamera();
        Log.d(TAG, "Cam id:" + cameraId);
        Camera cam = null;
        try{
            cam = Camera.open(cameraId);
        }
        catch(RuntimeException camE){
            Log.i(TAG, "Camera is running..........");
        }

        mainAct.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(),"Start recording procedure for 20 sec!", Toast.LENGTH_LONG);
                toast.show();
            }
        });

        final Camera camera = cam;
//        SurfaceView sv = new SurfaceView(getApplicationContext());
        try {
//            camera.setPreviewDisplay(sv.getHolder());
            camera.setPreviewDisplay(MainActivity.mSurfaceView.getHolder());
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
                        IOTDeviceMediaData media = new IOTDeviceMediaData();
                        media.setDeviceSerialNO(mPhoneNum);
                        media.setMediaFile(mFileName);
                        media.setMediaType("video");
                        media.setMediaData(dataBase64);
                        RegisterResp resp = MediaDataRESTClient.sendMediaData2(media);
                        Log.d(TAG, "Sending video back to server is done: " + resp.getResult() + " | " + resp.getPostId());

                        //WebSocketClientHelper.sendMediaNotification(media.getPhone(), media.getMediaType(), media.getMediaFile());
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

        ((MainActivity)mainAct).setPreviousVideoTime((new Date()).getTime());

        mainAct.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(),"Recording procedure has been completed!", Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    private void showCamera(){
        // do we have a camera?
        PhotoHandler photoHandler = new PhotoHandler(getApplicationContext());
        photoHandler.setPhone(this.phoneNum);

        if (!mainAct.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(mainAct , "No camera on this device", Toast.LENGTH_LONG)
                    .show();
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId < 0) {
                Toast.makeText(mainAct, "No front facing camera found.",
                        Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Camera ID:" + cameraId);
                Camera camera = Camera.open(cameraId);
                //camera.takePicture(null, null, new PhotoHandler(getApplicationContext()));
//                SurfaceView sv = new SurfaceView(getApplicationContext());
                try {
//                    camera.setPreviewDisplay(sv.getHolder());
                    camera.setPreviewDisplay(MainActivity.mSurfaceView.getHolder());
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
//        mNotificationManager = (NotificationManager)
//                mainAct.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MainActivity.class), 0);
//
//        NotificationCompat.Builder mBuilder =
//                new NotificationCompat.Builder(this)
//                        //.setSmallIcon(R.drawable.ic_stat_gcm)
//                        .setContentTitle("GCM Notification")
//                        .setStyle(new NotificationCompat.BigTextStyle()
//                                .bigText(msg))
//                        .setContentText(msg);
//
//        mBuilder.setContentIntent(contentIntent);
//        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
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
            ContentProviderResult[] results = mainAct.getContentResolver().applyBatch(ContactsContract.AUTHORITY, op_list);
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

    private Context getApplicationContext(){
        return mainAct.getApplicationContext();
    }
}

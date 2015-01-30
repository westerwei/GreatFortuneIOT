package com.example.wester.myapplication;

import android.content.Context;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.example.wester.myapplication.restclient.DeviceMediaData;
import com.example.wester.myapplication.restclient.MediaDataRESTClient;
import com.example.wester.myapplication.restclient.RegisterResp;
import com.example.wester.myapplication.websocket.WebSocketClientHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Wester on 2014/11/6.
 */
public class PhotoHandler implements Camera.PictureCallback {

    private final Context context;

    String TAG = "PhotoHandler";

    private String phone;

    public PhotoHandler(Context context) {
        this.context = context;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        File pictureFileDir = getDir();

        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {

            Log.e(TAG, "Can't create directory to save image.");
//            Toast.makeText(context, "Can't create directory to save image.",
//                    Toast.LENGTH_LONG).show();
            return;

        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());
        String photoFile = "Picture_" + date + ".jpg";

        String filename = pictureFileDir.getPath() + File.separator + photoFile;

        File pictureFile = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            //fos.flush();
            fos.close();
//            Toast.makeText(context, "New Image saved:" + photoFile,
//                    Toast.LENGTH_LONG).show();
            Log.d(TAG, "Start to send pic back to server");
            String dataBase64 = Base64.encodeToString(data, Base64.URL_SAFE);
            DeviceMediaData media = new DeviceMediaData();
            media.setPhone(this.phone);
            media.setMediaFile(pictureFile.getName());
            media.setMediaType("pic");
            media.setMediaData(dataBase64);
            //RegisterResp resp = MediaDataRESTClient.sendMediaData(media);
            //Log.d(TAG, "Sending pic back to server is done: " + resp.getResult() + " | " + resp.getPostId());

            //WebSocketClientHelper.sendMediaNotification(media.getPhone(), media.getMediaType(), media.getMediaFile());
        } catch (Exception error) {
            Log.e(TAG, "File" + filename + "not saved: "
                    + error.getMessage(), error);
//            Toast.makeText(context, "Image could not be saved.",
//                    Toast.LENGTH_LONG).show();
        }
    }

    private File getDir() {
        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(sdDir, "CameraAPIDemo");
    }

    public void setPhone(String phone){
        this.phone = phone;
    }
}
